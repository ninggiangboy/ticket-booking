package io.ticket.common.error;

/** A third party (Stripe, SMTP) did not answer in time or answered with a server error. */
public class ProviderUnavailableException extends TransientException {

  private ProviderUnavailableException(ErrorCode code, int retryAfterSeconds) {
    super(code, retryAfterSeconds);
  }

  public static ProviderUnavailableException email(int retryAfterSeconds) {
    return new ProviderUnavailableException(
        ErrorCode.EMAIL_PROVIDER_UNAVAILABLE, retryAfterSeconds);
  }

  public static ProviderUnavailableException payment(int retryAfterSeconds) {
    return new ProviderUnavailableException(
        ErrorCode.PAYMENT_PROVIDER_UNAVAILABLE, retryAfterSeconds);
  }
}
