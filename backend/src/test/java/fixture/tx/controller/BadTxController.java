package fixture.tx.controller;

import org.springframework.transaction.annotation.Transactional;

public class BadTxController {
  @Transactional
  public void handle() {}
}
