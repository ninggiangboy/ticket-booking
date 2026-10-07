package fixture.modulith.payment.service;

import fixture.modulith.reservation.service.HoldService;

/** ARC-11: payment reaches into another module's service package. */
public class PayService {
  public void pay() {
    new HoldService().hold();
  }
}
