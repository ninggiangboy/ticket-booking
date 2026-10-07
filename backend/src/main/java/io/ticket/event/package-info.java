/** Events and ticket types. Dependency rules: DOC-12 §2.2. */
@ApplicationModule(
    displayName = "Events and ticket types",
    allowedDependencies = {"common", "inventory"})
package io.ticket.event;

import org.springframework.modulith.ApplicationModule;
