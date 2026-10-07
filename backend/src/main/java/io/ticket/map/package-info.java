/** Seat maps. Dependency rules: DOC-12 §2.2. */
@ApplicationModule(
    displayName = "Seat maps",
    allowedDependencies = {"common", "event", "inventory", "media"})
package io.ticket.map;

import org.springframework.modulith.ApplicationModule;
