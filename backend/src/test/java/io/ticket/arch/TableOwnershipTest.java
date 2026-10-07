package io.ticket.arch;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.file.Path;
import java.util.Map;
import org.junit.jupiter.api.Test;

/** ARC-05, ARC-14: SQL in a module names only the tables the module owns. */
class TableOwnershipTest {

  @Test
  void productionSqlStaysInsideTheOwningModule() throws Exception {
    var sources = SqlSourceScanner.read(Path.of("src/main/java"));
    assertThat(sources).isNotEmpty();
    assertThat(SqlSourceScanner.ownershipViolations(sources)).isEmpty();
  }

  @Test
  void ticketModuleQueryingReservationIsReported() { // ARC-14
    var violations =
        SqlSourceScanner.ownershipViolations(
            Map.of(
                "io/ticket/ticket/repository/TicketRepository.java",
                "interface TicketRepository { @Query(\"SELECT t.* FROM ticket t JOIN reservation r ON r.reservation_id = t.x\") void f(); }"));
    assertThat(violations)
        .hasSize(1)
        .first()
        .asString()
        .contains("reservation owned by reservation");
  }

  @Test
  void textBlocksAreScannedAndCommentsAreNot() {
    var violations =
        SqlSourceScanner.ownershipViolations(
            Map.of(
                "io/ticket/order/service/OrderService.java",
                "// UPDATE ticket SET x = 1\nclass A { String q = \"\"\"\n  UPDATE ticket SET status = 'VOID'\n  \"\"\"; }"));
    assertThat(violations).hasSize(1);
    assertThat(
            SqlSourceScanner.ownershipViolations(
                Map.of("io/ticket/order/service/B.java", "/* DELETE FROM ticket */ class B {}")))
        .isEmpty();
  }

  @Test
  void invariantMayReadAnyTable() {
    assertThat(
            SqlSourceScanner.ownershipViolations(
                Map.of(
                    "io/ticket/invariant/service/InvariantChecker.java",
                    "class C { String q = \"SELECT count(*) FROM orders o JOIN ticket t ON t.order_id = o.order_id\"; }")))
        .isEmpty();
  }
}
