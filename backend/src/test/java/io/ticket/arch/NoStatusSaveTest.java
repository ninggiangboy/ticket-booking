package io.ticket.arch;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

/** ARC-06, ARC-13: a status is never changed through save(). */
class NoStatusSaveTest {

  @Test
  void productionCodeNeverSavesAStatusAggregate() {
    ArchitectureRules.noStatusSave("io.ticket").check(ArchitectureRules.importMain("io.ticket"));
  }

  @Test
  void savingAnOrderAfterInsertTurnsTheRuleRed() {
    var classes = ArchitectureRules.importFixture("fixture.save");
    assertThatThrownBy(() -> ArchitectureRules.noStatusSave("fixture.save").check(classes))
        .hasMessageContaining("BadOrderService");
  }
}
