package io.ticket.notification.service;

import io.ticket.common.mail.AppProperties;
import io.ticket.common.mail.EmailDeliveryException;
import io.ticket.common.mail.EmailRenderer;
import io.ticket.common.mail.MailProperties;
import io.ticket.common.mail.MailSender;
import io.ticket.common.mail.OutgoingEmail;
import io.ticket.notification.config.OutboxProperties;
import io.ticket.notification.entity.OutboxMessage;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/**
 * One relay pass (DOC-27 §5.2): claim in a short transaction, send outside any transaction, record
 * each outcome in its own short transaction. At-least-once: a crash after SMTP accepted and before
 * SENT is written sends again.
 */
@Service
public class OutboxRelayService {

  private static final Logger log = LoggerFactory.getLogger(OutboxRelayService.class);
  private static final int MAX_ERROR_LENGTH = 500;

  private final OutboxQueueService queue;
  private final MailModelFactory models;
  private final EmailRenderer renderer;
  private final MailSender sender;
  private final AppProperties app;
  private final Duration budget;

  OutboxRelayService(
      OutboxQueueService queue,
      MailModelFactory models,
      EmailRenderer renderer,
      MailSender sender,
      AppProperties app,
      OutboxProperties outbox,
      MailProperties mail) {
    this.queue = queue;
    this.models = models;
    this.renderer = renderer;
    this.sender = sender;
    this.app = app;
    this.budget = outbox.relay().batchBudget();
    // DR-104: every claimed row must be sent or handed back before its lease runs out
    if (budget.plus(mail.smtp().timeout()).compareTo(outbox.relay().lease()) >= 0) {
      throw new IllegalStateException(
          "outbox.relay.batch-budget + mail.smtp.timeout must be shorter than outbox.relay.lease");
    }
  }

  /** Runs one pass and returns how many rows were sent. */
  public int runOnce() {
    List<OutboxMessage> claimed = queue.claim();
    if (claimed.isEmpty()) {
      return 0;
    }
    long deadline = System.nanoTime() + budget.toNanos();
    int sent = 0;
    for (int i = 0; i < claimed.size(); i++) {
      if (System.nanoTime() > deadline) {
        List<UUID> rest = new ArrayList<>();
        claimed.subList(i, claimed.size()).forEach(m -> rest.add(m.outboxId()));
        queue.giveBack(rest);
        log.warn("outbox relay out of time budget, handed back {} rows", rest.size());
        break;
      }
      if (deliver(claimed.get(i))) {
        sent++;
      }
    }
    return sent;
  }

  private boolean deliver(OutboxMessage message) {
    try {
      Map<String, Object> payload = models.parse(message.payloadJson());
      Locale locale = models.locale(payload);
      var content =
          renderer.render(
              message.kind().template(), locale, models.model(message.kind(), payload, locale));
      sender.send(
          new OutgoingEmail(
              models.recipient(payload), message.outboxId() + "@" + app.domain(), locale, content));
      queue.sent(message.outboxId());
      return true;
    } catch (EmailDeliveryException e) {
      record(message, e.getMessage(), e.permanent());
    } catch (RuntimeException e) {
      // rendering or payload problems count as an attempt too, so a broken row cannot loop forever
      record(message, e.getClass().getSimpleName() + ": " + e.getMessage(), false);
    }
    return false;
  }

  /** Never puts the recipient address in the log or in {@code last_error} (DR-22). */
  private void record(OutboxMessage message, String error, boolean permanent) {
    String text =
        error == null ? "error" : error.substring(0, Math.min(error.length(), MAX_ERROR_LENGTH));
    boolean failed = queue.failed(message.outboxId(), text, permanent);
    if (failed) {
      log.error(
          "outbox message failed for good: id={} kind={} attempts={}",
          message.outboxId(),
          message.kind(),
          message.attempts());
    } else {
      log.warn(
          "outbox send failed, will retry: id={} kind={} attempts={}",
          message.outboxId(),
          message.kind(),
          message.attempts());
    }
  }
}
