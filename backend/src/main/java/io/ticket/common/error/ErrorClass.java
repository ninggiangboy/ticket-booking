package io.ticket.common.error;

/** How an error is logged and retried (DOC-35 §1). */
public enum ErrorClass {
  /** Valid request refused by a business rule; an expected outcome, never logged above WARN. */
  BUSINESS,
  /** Infrastructure or a third party is overloaded or unreachable; retrying later may succeed. */
  TRANSIENT,
  /** A bug, a broken invariant or a misconfiguration. */
  DEFECT
}
