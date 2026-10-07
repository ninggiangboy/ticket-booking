/** Ticket inventory and claims. Dependency rules: DOC-12 §2.2. */
@ApplicationModule(
    displayName = "Ticket inventory and claims",
    allowedDependencies = {"common"})
package io.ticket.inventory;

import org.springframework.modulith.ApplicationModule;
