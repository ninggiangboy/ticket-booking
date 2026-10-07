package io.ticket.common.mail;

/**
 * SMTP did not accept the message. {@code permanent} is true for a 5xx refusal of the recipient:
 * retrying is pointless.
 */
public class EmailDeliveryException extends RuntimeException {

  private final boolean permanent;

  public EmailDeliveryException(String message, boolean permanent, Throwable cause) {
    super(message, cause);
    this.permanent = permanent;
  }

  public boolean permanent() {
    return permanent;
  }
}
