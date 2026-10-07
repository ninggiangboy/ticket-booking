package fixture.layer.bad.job;

import fixture.layer.bad.controller.BadController;

/** An entry point calling another entry point. */
public class BadJob {
  public Object run() {
    return new BadController().handle();
  }
}
