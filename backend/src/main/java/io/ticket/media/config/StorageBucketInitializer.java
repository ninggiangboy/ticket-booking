package io.ticket.media.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.CreateBucketRequest;
import software.amazon.awssdk.services.s3.model.HeadBucketRequest;
import software.amazon.awssdk.services.s3.model.NoSuchBucketException;

/**
 * Creates the private bucket on startup when it is missing, so readiness can turn UP (DOC-62 §3,
 * §5).
 */
@Component
class StorageBucketInitializer implements ApplicationRunner {

  private static final Logger log = LoggerFactory.getLogger(StorageBucketInitializer.class);
  private static final int ATTEMPTS = 20;

  private final S3Client s3;
  private final StorageProperties props;

  StorageBucketInitializer(S3Client s3, StorageProperties props) {
    this.s3 = s3;
    this.props = props;
  }

  @Override
  public void run(ApplicationArguments args) throws InterruptedException {
    RuntimeException last = null;
    for (int attempt = 1; attempt <= ATTEMPTS; attempt++) {
      try {
        ensureBucket();
        return;
      } catch (RuntimeException e) {
        last = e;
        log.warn(
            "storage not ready (attempt {}/{}): {}",
            attempt,
            ATTEMPTS,
            e.getClass().getSimpleName());
        Thread.sleep(2000);
      }
    }
    throw new IllegalStateException("object storage unreachable at " + props.endpoint(), last);
  }

  private void ensureBucket() {
    try {
      s3.headBucket(HeadBucketRequest.builder().bucket(props.bucket()).build());
    } catch (NoSuchBucketException e) {
      s3.createBucket(CreateBucketRequest.builder().bucket(props.bucket()).build());
      log.info("created storage bucket {}", props.bucket());
    }
  }
}
