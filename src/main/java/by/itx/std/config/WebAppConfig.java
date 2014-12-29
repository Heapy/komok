package by.itx.std.config;

import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;

/**
 * Configure spring to search beans in "by.itx.std" packages.
 */
@Configuration
@ComponentScan(basePackages = "by.itx.std")
public class WebAppConfig {
}
