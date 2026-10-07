/** Payments and webhooks. Dependency rules: DOC-12 §2.2. */
@ApplicationModule(
    displayName = "Payments and webhooks",
    allowedDependencies = {
      "common",
      "reservation",
      "order",
      "inventory",
      "ticket",
      "notification",
      "event",
      "auth"
    })
package io.ticket.payment;

import org.springframework.modulith.ApplicationModule;
