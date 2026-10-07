/** Orders. Dependency rules: DOC-12 §2.2. */
@ApplicationModule(
    displayName = "Orders",
    allowedDependencies = {"common", "event", "ticket"})
package io.ticket.order;

import org.springframework.modulith.ApplicationModule;
