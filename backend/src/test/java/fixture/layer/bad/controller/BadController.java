package fixture.layer.bad.controller;

import fixture.layer.bad.entity.BadEntity;
import fixture.layer.bad.repository.BadRepository;

/** ARC-10: a controller that calls a repository and handles an entity. */
public class BadController {
  private final BadRepository repository = new BadRepository();

  public Object handle() {
    new BadEntity();
    return repository.find();
  }
}
