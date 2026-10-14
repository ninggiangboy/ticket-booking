package fixture.tx.service;

import org.springframework.transaction.support.TransactionTemplate;

public class TemplateTxService {
  private final TransactionTemplate tx;

  public TemplateTxService(TransactionTemplate tx) {
    this.tx = tx;
  }

  public void handle() {
    tx.executeWithoutResult(status -> {});
  }
}
