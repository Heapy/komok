package by.itx.std.dao.config;

import by.itx.std.utils.Profiles;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.context.annotation.PropertySource;
import org.springframework.core.env.Environment;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.instrument.classloading.InstrumentationLoadTimeWeaver;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.orm.hibernate4.HibernateExceptionTranslator;
import org.springframework.orm.jpa.JpaTransactionManager;
import org.springframework.orm.jpa.LocalContainerEntityManagerFactoryBean;
import org.springframework.orm.jpa.vendor.Database;
import org.springframework.orm.jpa.vendor.HibernateJpaVendorAdapter;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.EnableTransactionManagement;

import javax.persistence.EntityManagerFactory;
import javax.sql.DataSource;
import java.util.Properties;

// TODO: /Ruslan Ibragimov/ Analyze config. Transactions, translators, and so on must be reviewed.
@Configuration
@EnableTransactionManagement
@EnableJpaRepositories(basePackages = "by.itx.std.dao")
@ComponentScan(basePackages = "by.itx.std.dao")
public class DaoConfig {

    @Configuration
    @PropertySource({ "classpath:dev.properties" })
    @Profile(value = { Profiles.DEFAULT, Profiles.DEVELOPMENT })
    static class DevConfiguration {
    }

    @Configuration
    @PropertySource({ "classpath:dev-remote.properties" })
    @Profile(value = { Profiles.DEVELOPMENT_REMOTE })
    static class DevRemoteConfiguration {
    }

    @Configuration
    @PropertySource({ "classpath:prod.properties" })
    @Profile(value = { Profiles.PRODUCTION })
    static class ProductionConfiguration {
    }

    @Configuration
    @PropertySource({ "classpath:test.properties" })
    @Profile(value = { Profiles.TEST })
    static class TestConfiguration {
    }

    @Autowired
    private Environment env;

    @Bean
    public PlatformTransactionManager transactionManager() {
        EntityManagerFactory factory = entityManagerFactory().getObject();
        return new JpaTransactionManager(factory);
    }

    @Bean
    public LocalContainerEntityManagerFactoryBean entityManagerFactory() {
        LocalContainerEntityManagerFactoryBean factory = new LocalContainerEntityManagerFactoryBean();

        HibernateJpaVendorAdapter vendorAdapter = new HibernateJpaVendorAdapter();
        vendorAdapter.setGenerateDdl(Boolean.valueOf(env.getProperty("hibernate.generate.ddl")));
        vendorAdapter.setShowSql(Boolean.valueOf(env.getProperty("hibernate.show_sql")));
        vendorAdapter.setDatabase(Database.valueOf(env.getProperty("database")));

        factory.setDataSource(dataSource());
        factory.setJpaVendorAdapter(vendorAdapter);
        factory.setPackagesToScan("by.itx.std.dao");

        Properties jpaProperties = new Properties();
        jpaProperties.put("hibernate.hbm2ddl.auto", env.getProperty("hibernate.hbm2ddl.auto"));
        factory.setJpaProperties(jpaProperties);

        factory.afterPropertiesSet();
        factory.setLoadTimeWeaver(new InstrumentationLoadTimeWeaver());
        return factory;
    }

    @Bean
    public HibernateExceptionTranslator hibernateExceptionTranslator() {
        return new HibernateExceptionTranslator();
    }

    @Bean
    public DataSource dataSource() {
        DriverManagerDataSource dataSource = new DriverManagerDataSource();
        dataSource.setDriverClassName(env.getProperty("jdbc.driverClassName"));
        dataSource.setUrl(env.getProperty("jdbc.url"));
        dataSource.setUsername(env.getProperty("jdbc.username"));
        dataSource.setPassword(env.getProperty("jdbc.password"));
        return dataSource;
    }
}
