package io.ticket.arch;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.tngtech.archunit.core.importer.ImportOption;
import org.junit.jupiter.api.Test;
import org.springframework.modulith.core.ApplicationModules;

/**
 * ARC-11, ARC-12: the Modulith check really fails on a service import and on a dependency cycle.
 */
class ModulithRulesTest {

  /** Fixtures are test classes, which Modulith skips unless told otherwise. */
  private static final ImportOption INCLUDE_TESTS = location -> true;

  @Test
  void importingAnotherModulesServiceFailsVerify() { // ARC-11
    assertThatThrownBy(() -> ApplicationModules.of("fixture.modulith", INCLUDE_TESTS).verify())
        .hasMessageContaining("HoldService");
  }

  @Test
  void aCycleBetweenModulesFailsVerify() { // ARC-12
    assertThatThrownBy(() -> ApplicationModules.of("fixture.cycle", INCLUDE_TESTS).verify())
        .hasMessageContaining("Cycle");
  }
}
