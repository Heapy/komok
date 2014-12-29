package by.itx.std.web.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurerAdapter;

/**
 * Enables support for @Controller-annotated classes.
 */
@Configuration
@EnableWebMvc
public class WebMvc extends WebMvcConfigurerAdapter {
    // Extend if needed
}
