package io.ticket.arch;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

/**
 * ARC-03: {@code @Transactional} lives in services only, and nothing opens a transaction by hand.
 */
class TransactionRulesTest {

  @Test
  void productionCodeOpensTransactionsOnlyInServices() {
    ArchitectureRules.transactionsOnlyInServices("io.ticket")
        .check(ArchitectureRules.importMain("io.ticket"));
  }

  @Test
  void productionCodeNeverDrivesTransactionsByHand() {
    ArchitectureRules.noProgrammaticTransactions("io.ticket")
        .check(ArchitectureRules.importMain("io.ticket"));
  }

  @Test
  void transactionTemplateInAServiceTurnsTheRuleRed() {
    var classes = ArchitectureRules.importFixture("fixture.tx");
    assertThatThrownBy(
            () -> ArchitectureRules.noProgrammaticTransactions("fixture.tx").check(classes))
        .hasMessageContaining("TemplateTxService");
  }

  @Test
  void transactionalOnAControllerTurnsTheRuleRed() {
    var classes = ArchitectureRules.importFixture("fixture.tx");
    assertThatThrownBy(
            () -> ArchitectureRules.transactionsOnlyInServices("fixture.tx").check(classes))
        .hasMessageContaining("BadTxController");
  }
}
