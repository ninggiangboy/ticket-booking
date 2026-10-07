package io.ticket.arch;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

/** ARC-02, ARC-04, ARC-10: layer direction and controller/entity separation. */
class LayerRulesTest {

  @Test
  void productionCodeFollowsTheLayers() {
    var classes = ArchitectureRules.importMain("io.ticket");
    ArchitectureRules.layers("io.ticket").check(classes);
    ArchitectureRules.entryPointsDoNotCallEachOther("io.ticket").check(classes);
    ArchitectureRules.controllersAvoidEntities("io.ticket").check(classes);
  }

  @Test
  void controllerCallingARepositoryTurnsTheLayerRuleRed() { // ARC-10
    var classes = ArchitectureRules.importFixture("fixture.layer");
    assertThatThrownBy(() -> ArchitectureRules.layers("fixture.layer").check(classes))
        .hasMessageContaining("BadController")
        .hasMessageContaining("BadRepository");
  }

  @Test
  void entryPointCallingAnEntryPointTurnsTheLayerRuleRed() {
    var classes = ArchitectureRules.importFixture("fixture.layer");
    assertThatThrownBy(
            () -> ArchitectureRules.entryPointsDoNotCallEachOther("fixture.layer").check(classes))
        .hasMessageContaining("BadJob");
  }

  @Test
  void controllerUsingAnEntityTurnsRuleFourRed() {
    var classes = ArchitectureRules.importFixture("fixture.layer");
    assertThatThrownBy(
            () -> ArchitectureRules.controllersAvoidEntities("fixture.layer").check(classes))
        .hasMessageContaining("BadEntity");
  }
}
