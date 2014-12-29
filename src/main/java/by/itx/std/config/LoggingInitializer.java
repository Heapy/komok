package by.itx.std.config;

import by.itx.std.utils.Profiles;
import com.google.common.collect.ImmutableMap;
import org.apache.log4j.Appender;
import org.apache.log4j.AsyncAppender;
import org.apache.log4j.ConsoleAppender;
import org.apache.log4j.DailyRollingFileAppender;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.apache.log4j.PatternLayout;

import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;
import javax.servlet.annotation.WebListener;
import java.util.Map;

@WebListener
public class LoggingInitializer implements ServletContextListener {

    private static final String PRODUCTION_LOG_PATH = "../logs/std.log";

    private static final String DEVELOPMENT_PATTERN = "%-5p %d{HH:mm:ss} %C{1}(%L): %m%n";

    private static final ImmutableMap<String, Level> DEVELOPMENT_CATEGORIES = ImmutableMap.of(
            "org.hibernate", Level.INFO,
            "org.springframework", Level.INFO,
            "by.itx.std", Level.TRACE,
            "java.sql", Level.DEBUG
    );

    private static final String PRODUCTION_PATTERN = "%d %-5p [%c] %m%n";
    private static final ImmutableMap<String, Level> PRODUCTION_CATEGORIES = ImmutableMap.of(
            "org.hibernate", Level.WARN,
            "org.springframework", Level.WARN,
            "by.itx.std", Level.INFO,
            "java.sql", Level.WARN
    );

    @Override
    public void contextInitialized(ServletContextEvent sce) {
        String profile = System.getProperty("spring.profiles.active");

        if (profile == null) {
            developmentLogger();
            return;
        }

        switch (profile) {
            case Profiles.DEVELOPMENT:
                developmentLogger();
                break;
            case Profiles.PRODUCTION:
                productionLogger();
                break;
            case Profiles.DEVELOPMENT_REMOTE:
                developmentRemoteLogger();
                break;
            case Profiles.TEST:
                testLogger();
                break;
            default:
                developmentLogger();
                break;
        }

    }

    @Override
    public void contextDestroyed(ServletContextEvent sce) {
        // Do nothing
    }

    private void developmentLogger() {
        Logger.getRootLogger().addAppender(getConsoleAppender(Level.TRACE, DEVELOPMENT_PATTERN));
        setupCategories(DEVELOPMENT_CATEGORIES);
    }

    private void developmentRemoteLogger() {
        Logger.getRootLogger().addAppender(getConsoleAppender(Level.TRACE, DEVELOPMENT_PATTERN));
        setupCategories(DEVELOPMENT_CATEGORIES);
    }

    private void productionLogger() {
        Appender console = getConsoleAppender(Level.WARN, PRODUCTION_PATTERN);
        Appender file = getFileAppender(Level.WARN, PRODUCTION_PATTERN, PRODUCTION_LOG_PATH);
        setupCategories(PRODUCTION_CATEGORIES);
        Logger.getRootLogger().addAppender(getAsyncAppender(console, file));
    }

    private void testLogger() {
        Logger.getRootLogger().addAppender(getConsoleAppender(Level.TRACE, DEVELOPMENT_PATTERN));
        setupCategories(DEVELOPMENT_CATEGORIES);
    }

    private void setupCategories(Map<String, Level> categories) {
        for (Map.Entry<String, Level> category : categories.entrySet()) {
            Logger.getLogger(category.getKey()).setLevel(category.getValue());
        }
    }

    private Appender getConsoleAppender(Level level, String pattern) {
        ConsoleAppender appender = new ConsoleAppender();
        appender.setLayout(new PatternLayout(pattern));
        appender.setThreshold(level);
        appender.activateOptions();
        return appender;
    }

    private Appender getFileAppender(Level level, String pattern, String file) {
        DailyRollingFileAppender appender = new DailyRollingFileAppender();
        appender.setFile(file);
        appender.setLayout(new PatternLayout(pattern));
        appender.setThreshold(level);
        appender.setAppend(true);
        appender.activateOptions();
        return appender;
    }

    private Appender getAsyncAppender(Appender... appenders) {
        AsyncAppender asyncAppender = new AsyncAppender();
        for (Appender appender : appenders) {
            asyncAppender.addAppender(appender);
        }
        return asyncAppender;
    }
}
