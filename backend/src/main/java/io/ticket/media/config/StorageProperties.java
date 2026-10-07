package io.ticket.media.config;

import jakarta.validation.constraints.NotBlank;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

/**
 * Object storage settings (DOC-34 §3.9). Any S3-compatible endpoint works without code changes
 * (DR-38).
 */
@Validated
@ConfigurationProperties("storage.s3")
public record StorageProperties(
    @NotBlank String endpoint,
    @NotBlank String region,
    @NotBlank String bucket,
    @NotBlank String accessKey,
    @NotBlank String secretKey,
    boolean pathStyle) {}
