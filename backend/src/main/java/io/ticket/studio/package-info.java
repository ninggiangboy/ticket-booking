/** Organizer studio API. Dependency rules: DOC-12 §2.2. */
@ApplicationModule(
    displayName = "Organizer studio API",
    allowedDependencies = {
      "common",
      "event",
      "map",
      "media",
      "inventory",
      "order",
      "ticket",
      "notification",
      "reservation",
      "auth"
    })
package io.ticket.studio;

import org.springframework.modulith.ApplicationModule;
