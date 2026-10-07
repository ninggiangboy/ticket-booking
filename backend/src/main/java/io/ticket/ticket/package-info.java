/** Issued tickets. Dependency rules: DOC-12 §2.2. */
@ApplicationModule(
    displayName = "Issued tickets",
    allowedDependencies = {"common", "event"})
package io.ticket.ticket;

import org.springframework.modulith.ApplicationModule;
