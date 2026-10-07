package io.ticket.arch;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.file.Path;
import java.util.Map;
import org.junit.jupiter.api.Test;

/** ARC-07, ARC-15: {@code io.ticket.invariant} reads and never writes. */
class InvariantReadOnlyTest {

  @Test
  void productionInvariantCodeContainsNoWrites() throws Exception {
    var sources = SqlSourceScanner.read(Path.of("src/main/java"));
    assertThat(SqlSourceScanner.invariantWriteViolations(sources)).isEmpty();
  }

  @Test
  void anUpdateInTheCheckerIsReported() { // ARC-15
    assertThat(
            SqlSourceScanner.invariantWriteViolations(
                Map.of(
                    "io/ticket/invariant/service/InvariantChecker.java",
                    "class C { String q = \"UPDATE orders SET status = 'PAID'\"; }")))
        .hasSize(1);
  }

  @Test
  void writesElsewhereAreNotThisRulesBusiness() {
    assertThat(
            SqlSourceScanner.invariantWriteViolations(
                Map.of(
                    "io/ticket/order/service/OrderService.java",
                    "class C { String q = \"UPDATE orders SET status = 'PAID'\"; }")))
        .isEmpty();
  }
}
