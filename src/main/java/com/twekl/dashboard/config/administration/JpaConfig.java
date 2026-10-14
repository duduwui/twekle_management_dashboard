package com.twekl.dashboard.config.administration;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import jakarta.persistence.EntityManagerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.orm.jpa.JpaTransactionManager;
import org.springframework.orm.jpa.LocalContainerEntityManagerFactoryBean;
import org.springframework.orm.jpa.vendor.HibernateJpaVendorAdapter;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.EnableTransactionManagement;

import javax.sql.DataSource;
import java.util.Properties;

/**
 * Pure Spring MVC JPA & Hibernate Configuration
 * Manages DataSource (HikariCP) and EntityManagerFactory with automatic schema management.
 */
import org.springframework.beans.factory.annotation.Autowired;

@Configuration
@EnableTransactionManagement
@EnableJpaRepositories(basePackages = { "com.twekl.dashboard.repository.administration" })
@ComponentScan(basePackages = {
    "com.twekl.dashboard.service.administration",
    "com.twekl.dashboard.security.administration"
})
@org.springframework.context.annotation.Import({DataInitializer.class, CacheConfig.class})
@org.springframework.context.annotation.PropertySource("classpath:application.properties")
public class JpaConfig {

    private final org.springframework.core.env.Environment environment;

    @Autowired
    public JpaConfig(org.springframework.core.env.Environment environment) {
        this.environment = environment;
    }

    @Bean
    public DataSource dataSource() {
        HikariConfig config = new HikariConfig();
        config.setJdbcUrl(environment.getRequiredProperty("database.url"));
        config.setUsername(environment.getRequiredProperty("database.username"));
        config.setPassword(environment.getRequiredProperty("database.password"));
        config.setDriverClassName("com.mysql.cj.jdbc.Driver");
        config.setMaximumPoolSize(environment.getProperty("database.pool.maximum", Integer.class, 5));
        config.setMinimumIdle(environment.getProperty("database.pool.minimum", Integer.class, 1));
        config.setConnectionTimeout(5000);
        return new HikariDataSource(config);
    }

    @Bean
    public LocalContainerEntityManagerFactoryBean entityManagerFactory(DataSource dataSource) {
        LocalContainerEntityManagerFactoryBean em = new LocalContainerEntityManagerFactoryBean();
        em.setDataSource(dataSource);
        em.setPackagesToScan("com.twekl.dashboard.model.administration");

        HibernateJpaVendorAdapter vendorAdapter = new HibernateJpaVendorAdapter();
        vendorAdapter.setGenerateDdl(false);
        em.setJpaVendorAdapter(vendorAdapter);

        Properties jpaProperties = new Properties();
        jpaProperties.put("hibernate.dialect", "org.hibernate.dialect.MySQLDialect");
        jpaProperties.put("hibernate.physical_naming_strategy", UpperCasePhysicalNamingStrategy.class.getName());
        jpaProperties.put("hibernate.implicit_naming_strategy", "org.hibernate.boot.model.naming.ImplicitNamingStrategyJpaCompliantImpl");
        jpaProperties.put("hibernate.show_sql", "false");
        jpaProperties.put("hibernate.format_sql", "false");
        jpaProperties.put("hibernate.hbm2ddl.auto", environment.getProperty("database.schema-action", "validate"));
        em.setJpaProperties(jpaProperties);

        return em;
    }

    @Bean
    public PlatformTransactionManager transactionManager(EntityManagerFactory emf) {
        JpaTransactionManager transactionManager = new JpaTransactionManager();
        transactionManager.setEntityManagerFactory(emf);
        return transactionManager;
    }
}

