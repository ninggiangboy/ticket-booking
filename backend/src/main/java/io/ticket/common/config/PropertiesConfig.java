package io.ticket.common.config;

import org.springframework.boot.context.properties.ConfigurationPropertiesScan;
import org.springframework.context.annotation.Configuration;

/**
 * Registers every {@code @ConfigurationProperties} record of every module. Kept out of the
 * application class so web slice tests do not need all properties.
 */
@Configuration
@ConfigurationPropertiesScan("io.ticket")
class PropertiesConfig {}
