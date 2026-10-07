package io.ticket.media.config;

import org.springframework.boot.health.contributor.Health;
import org.springframework.boot.health.contributor.HealthIndicator;
import org.springframework.stereotype.Component;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.HeadBucketRequest;

/**
 * Readiness requires the configured bucket to exist (DOC-62 §3). Component name `storage` is the
 * health key.
 */
@Component("storage")
class StorageHealthIndicator implements HealthIndicator {

  private final S3Client s3;
  private final StorageProperties props;

  StorageHealthIndicator(S3Client s3, StorageProperties props) {
    this.s3 = s3;
    this.props = props;
  }

  @Override
  public Health health() {
    try {
      s3.headBucket(HeadBucketRequest.builder().bucket(props.bucket()).build());
      return Health.up().build();
    } catch (RuntimeException e) {
      return Health.down().withDetail("error", e.getClass().getSimpleName()).build();
    }
  }
}
