package io.ticket.notification;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import io.ticket.common.mail.EmailDeliveryException;
import io.ticket.common.mail.MailProperties;
import io.ticket.common.mail.MailSender;
import io.ticket.common.mail.OutgoingEmail;
import io.ticket.common.mail.SmtpMailSender;
import io.ticket.notification.service.OutboxRelayService;
import io.ticket.support.IntegrationTestBase;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.transaction.IllegalTransactionStateException;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * P1-06: outbox relay with lease and backoff, sending through SMTP (OM-08…10, EML in DOC-27 §12).
 */
@Import(OutboxRelayIT.FlakySmtp.class)
class OutboxRelayIT extends IntegrationTestBase {

  /** Fails the first N sends, then delegates to the real SMTP sender. */
  static class FlakyMailSender implements MailSender {
    final AtomicInteger failuresLeft = new AtomicInteger();
    final AtomicInteger permanentFailuresLeft = new AtomicInteger();
    final MailSender real;

    FlakyMailSender(MailSender real) {
      this.real = real;
    }

    @Override
    public void send(OutgoingEmail email) {
      if (permanentFailuresLeft.getAndUpdate(n -> Math.max(0, n - 1)) > 0) {
        throw new EmailDeliveryException("550 mailbox unavailable", true, null);
      }
      if (failuresLeft.getAndUpdate(n -> Math.max(0, n - 1)) > 0) {
        throw new EmailDeliveryException("connection refused", false, null);
      }
      real.send(email);
    }
  }

  @TestConfiguration
  static class FlakySmtp {
    @Bean
    @Primary
    FlakyMailSender flakyMailSender(JavaMailSender javaMailSender, MailProperties properties) {
      return new FlakyMailSender(new SmtpMailSender(javaMailSender, properties));
    }
  }

  @Autowired OutboxRelayService relay;
  @Autowired NotificationApi notifications;
  @Autowired FlakyMailSender mail;
  @Autowired JdbcTemplate jdbc;
  @Autowired TransactionTemplate tx;

  @BeforeEach
  void reset() {
    jdbc.execute("TRUNCATE outbox");
    clearMailpit();
    mail.failuresLeft.set(0);
    mail.permanentFailuresLeft.set(0);
  }

  private UUID enqueueTickets() {
    Map<String, Object> payload =
        Map.of(
            "orderId",
            UUID.randomUUID().toString(),
            "to",
            "an.nguyen@example.com",
            "locale",
            "vi",
            "event",
            Map.of(
                "eventId", UUID.randomUUID().toString(),
                "name", "Hòa nhạc Giao Mùa",
                "venue", "Nhà hát Thành phố",
                "startsAt", "2026-11-14T13:00:00Z",
                "timezone", "Asia/Ho_Chi_Minh"),
            "amount",
            1500000,
            "currency",
            "VND",
            "tickets",
            List.of(
                Map.of(
                    "code",
                    "GM-4K7P-92XD",
                    "ticketTypeName",
                    "VIP",
                    "unitPrice",
                    750000,
                    "label",
                    Map.of("section", "Khán đài A", "row", "C", "seat", "9"))),
            "ordersPath",
            "/me/tickets");
    return tx.execute(status -> notifications.enqueue(OutboxKind.EMAIL_TICKETS, payload));
  }

  private Map<String, Object> row(UUID id) {
    return jdbc.queryForMap(
        "SELECT status, attempts, last_error,"
            + " extract(epoch FROM next_attempt_at - now()) AS wait_seconds FROM outbox WHERE outbox_id = ?",
        id);
  }

  private void makeDue(UUID id) {
    jdbc.update(
        "UPDATE outbox SET next_attempt_at = now() - interval '1 second' WHERE outbox_id = ?", id);
  }

  @Test
  void threeSmtpFailuresBackOffThenTheFourthAttemptSendsExactlyOneMail() {
    UUID id = enqueueTickets();
    mail.failuresLeft.set(3);

    double[] expected = {10, 20, 40};
    for (int attempt = 1; attempt <= 3; attempt++) {
      assertThat(relay.runOnce()).isZero();
      var row = row(id);
      assertThat(row.get("status")).isEqualTo("PENDING");
      assertThat(row.get("attempts")).isEqualTo(attempt);
      assertThat(((Number) row.get("wait_seconds")).doubleValue())
          .isBetween(expected[attempt - 1] - 3, expected[attempt - 1]);
      assertThat((String) row.get("last_error")).contains("connection refused");
      makeDue(id);
    }

    assertThat(relay.runOnce()).isEqualTo(1);
    var row = row(id);
    assertThat(row.get("status")).isEqualTo("SENT");
    assertThat(row.get("attempts")).isEqualTo(4);

    String messages = mailpitMessages();
    assertThat(messages).contains("\"total\":1");
    assertThat(messages).contains("Vé của bạn cho Hòa nhạc Giao Mùa");
  }

  @Test
  void theMessageCarriesTheFixedMessageIdBothBodiesAndRealContent() {
    UUID id = enqueueTickets();
    assertThat(relay.runOnce()).isEqualTo(1);

    String listing = mailpitMessages();
    String mailpitId = listing.replaceAll("(?s).*\"ID\":\"([^\"]+)\".*", "$1");
    String message = mailpitMessage(mailpitId);
    assertThat(message).contains("\"MessageID\":\"" + id + "@ticket.localhost\"");
    assertThat(message).contains("an.nguyen@example.com");
    assertThat(message).contains("GM-4K7P-92XD");
    assertThat(message).contains("Khán đài A");
    assertThat(message).contains("Thứ Bảy 14.11.2026 · 20:00");
    assertThat(message).contains("1.500.000");
    assertThat(message).contains("http://localhost:8080/me/tickets");
  }

  @Test
  void aPendingRowIsClaimedOnceWhileItsLeaseHolds() {
    UUID id = enqueueTickets();
    mail.failuresLeft.set(1);
    assertThat(relay.runOnce()).isZero(); // claimed and failed → backoff
    assertThat(relay.runOnce()).isZero(); // not due: nothing to claim
    assertThat(row(id).get("attempts")).isEqualTo(1);
  }

  @Test
  void twelveFailuresEndInFailedAndTheRowIsNotClaimedAgain() {
    UUID id = enqueueTickets();
    mail.failuresLeft.set(100);
    for (int i = 1; i <= 12; i++) {
      relay.runOnce();
      makeDue(id);
    }
    var row = row(id);
    assertThat(row.get("status")).isEqualTo("FAILED");
    assertThat(row.get("attempts")).isEqualTo(12);
    assertThat(relay.runOnce()).isZero();
    assertThat(row(id).get("attempts")).isEqualTo(12);
  }

  @Test
  void aPermanentRefusalFailsImmediately() {
    UUID id = enqueueTickets();
    mail.permanentFailuresLeft.set(1);
    relay.runOnce();
    var row = row(id);
    assertThat(row.get("status")).isEqualTo("FAILED");
    assertThat(row.get("attempts")).isEqualTo(1);
  }

  @Test
  void lastErrorNeverContainsTheRecipientAddress() {
    UUID id = enqueueTickets();
    mail.failuresLeft.set(1);
    relay.runOnce();
    assertThat((String) row(id).get("last_error")).doesNotContain("@example.com");
  }

  @Test
  void aKindWithoutATemplateCountsAsAnAttemptInsteadOfLoopingForever() {
    UUID id =
        tx.execute(
            s ->
                notifications.enqueue(
                    OutboxKind.EMAIL_REFUND_PENDING,
                    Map.of("to", "b@example.com", "locale", "en")));
    relay.runOnce();
    var row = row(id);
    assertThat(row.get("attempts")).isEqualTo(1);
    assertThat((String) row.get("last_error")).contains("no template");
  }

  @Test
  void enqueueOutsideATransactionIsAProgrammingError() {
    assertThatThrownBy(() -> notifications.enqueue(OutboxKind.EMAIL_TICKETS, Map.of()))
        .isInstanceOf(IllegalTransactionStateException.class);
  }

  @Test
  void enqueueAllWritesOneRowPerPayload() {
    tx.executeWithoutResult(
        s ->
            notifications.enqueueAll(
                OutboxKind.EMAIL_REFUND_PENDING,
                List.of(
                    Map.of("to", "a@x.test"), Map.of("to", "b@x.test"), Map.of("to", "c@x.test"))));
    assertThat(
            jdbc.queryForObject("SELECT count(*) FROM outbox WHERE status = 'PENDING'", Long.class))
        .isEqualTo(3);
  }

  @Test
  void rollingBackTheBusinessTransactionLeavesNoOutboxRow() {
    tx.executeWithoutResult(
        s -> {
          notifications.enqueue(OutboxKind.EMAIL_TICKETS, Map.of("to", "a@x.test"));
          s.setRollbackOnly();
        });
    assertThat(jdbc.queryForObject("SELECT count(*) FROM outbox", Long.class)).isZero();
  }
}
