package fixture.save.service;

import fixture.save.repository.OrderRepository;

/** ARC-13: changes an order through save(). */
public class BadOrderService {
  private final OrderRepository orders;

  public BadOrderService(OrderRepository orders) {
    this.orders = orders;
  }

  public void markPaid(Object order) {
    orders.save(order);
  }
}
