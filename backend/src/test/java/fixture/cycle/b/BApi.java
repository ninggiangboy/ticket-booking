package fixture.cycle.b;

import fixture.cycle.a.AApi;

/** ARC-12: a and b depend on each other. */
public interface BApi {
  AApi a();
}
