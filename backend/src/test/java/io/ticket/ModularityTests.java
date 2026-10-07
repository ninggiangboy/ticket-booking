package io.ticket;

import org.junit.jupiter.api.Test;
import org.springframework.modulith.core.ApplicationModules;

/** ARC-01: only package roots are used across modules, no cycles, only declared dependencies. */
class ModularityTests {

  @Test
  void moduleStructureIsValid() {
    ApplicationModules modules = ApplicationModules.of(TicketApplication.class);
    modules.forEach(m -> System.out.println("MODULE " + m.getName()));
    modules.verify();
  }
}
