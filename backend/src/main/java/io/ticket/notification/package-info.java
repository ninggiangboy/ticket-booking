/** Transactional outbox and email relay. Dependency rules: DOC-12 §2.2. */
@ApplicationModule(
    displayName = "Transactional outbox and email relay",
    allowedDependencies = {"common"})
package io.ticket.notification;

import org.springframework.modulith.ApplicationModule;
