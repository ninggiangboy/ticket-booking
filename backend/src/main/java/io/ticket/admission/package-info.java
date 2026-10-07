/** Waiting room and admission control. Dependency rules: DOC-12 §2.2. */
@ApplicationModule(
    displayName = "Waiting room and admission control",
    allowedDependencies = {"common", "event", "inventory"})
package io.ticket.admission;

import org.springframework.modulith.ApplicationModule;
