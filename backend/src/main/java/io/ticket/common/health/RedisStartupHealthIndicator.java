package io.ticket.common.health;

import java.util.concurrent.atomic.AtomicBoolean;
import org.springframework.boot.health.contributor.Health;
import org.springframework.boot.health.contributor.HealthIndicator;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.stereotype.Component;

/**
 * Readiness needs Redis to answer PING once, at startup. Losing Redis later must not take readiness
 * down: the system keeps selling without it (DR-56, DOC-62 §3), so after the first success this
 * indicator stays UP.
 */
@Component("redisStartup")
class RedisStartupHealthIndicator implements HealthIndicator {

  private final RedisConnectionFactory connectionFactory;
  private final AtomicBoolean reachedOnce = new AtomicBoolean();

  RedisStartupHealthIndicator(RedisConnectionFactory connectionFactory) {
    this.connectionFactory = connectionFactory;
  }

  @Override
  public Health health() {
    if (reachedOnce.get()) {
      return Health.up().build();
    }
    try (var connection = connectionFactory.getConnection()) {
      connection.ping();
      reachedOnce.set(true);
      return Health.up().build();
    } catch (RuntimeException e) {
      return Health.down().withDetail("error", e.getClass().getSimpleName()).build();
    }
  }
}
