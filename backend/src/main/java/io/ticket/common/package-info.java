/**
 * Shared technical code with no business dependencies. Open so every module may use its packages
 * (DOC-12 §2.2).
 */
@ApplicationModule(displayName = "Common", type = ApplicationModule.Type.OPEN)
package io.ticket.common;

import org.springframework.modulith.ApplicationModule;
