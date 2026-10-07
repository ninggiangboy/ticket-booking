/** Holds and idempotency. Dependency rules: DOC-12 §2.2. */
@ApplicationModule(
    displayName = "Holds and idempotency",
    allowedDependencies = {"common", "event", "inventory", "order", "admission"})
package io.ticket.reservation;

import org.springframework.modulith.ApplicationModule;
