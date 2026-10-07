package io.ticket.common.mail;

/**
 * Sends one message over SMTP with the configured timeout. Shared by magic link and the outbox
 * relay (DR-96).
 */
public interface MailSender {

  /**
   * @throws EmailDeliveryException when the message was not accepted by the SMTP server
   */
  void send(OutgoingEmail email);
}
