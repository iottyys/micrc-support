package io.ttyys.micrc.persistence.springboot;

import com.zaxxer.hikari.HikariDataSource;
import lombok.extern.slf4j.Slf4j;
import org.h2.tools.Server;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.boot.autoconfigure.AutoConfigureBefore;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.h2.H2ConsoleProperties;
import org.springframework.boot.autoconfigure.jdbc.DataSourceProperties;
import org.springframework.boot.autoconfigure.orm.jpa.HibernateJpaAutoConfiguration;
import org.springframework.boot.autoconfigure.transaction.TransactionManagerCustomizers;
import org.springframework.boot.jdbc.DatabaseDriver;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.orm.jpa.JpaTransactionManager;
import org.springframework.transaction.PlatformTransactionManager;

import java.sql.SQLException;

@Configuration
@AutoConfigureBefore(HibernateJpaAutoConfiguration.class)
public class PersistenceAutoConfiguration {

    @Bean
    @Primary
    @ConditionalOnMissingBean
    public PlatformTransactionManager transactionManager(
            ObjectProvider<TransactionManagerCustomizers> transactionManagerCustomizers) {
        return jpaTransactionManager(transactionManagerCustomizers);
    }

    @Bean
    @ConditionalOnMissingBean
    public JpaTransactionManager jpaTransactionManager(
            ObjectProvider<TransactionManagerCustomizers> transactionManagerCustomizers) {
        JpaTransactionManager transactionManager = new JpaTransactionManager();
        transactionManagerCustomizers.ifAvailable((customizers) -> customizers.customize(transactionManager));
        return transactionManager;
    }

    @Configuration
    @ConditionalOnClass(HikariDataSource.class)
    @ConditionalOnBean({ DataSourceProperties.class, H2ConsoleProperties.class })
    @Slf4j
    static class H2EmbeddedDatabaseConfiguration implements BeanPostProcessor {
        private final DataSourceProperties properties;

        public H2EmbeddedDatabaseConfiguration(DataSourceProperties properties) {
            this.properties = properties;
        }

        @Override
        public Object postProcessBeforeInitialization(Object bean, String beanName) throws BeansException {
            if (bean instanceof HikariDataSource) {
                HikariDataSource dataSource = (HikariDataSource) bean;
                if (DatabaseDriver.fromJdbcUrl(properties.determineUrl()).equals(DatabaseDriver.H2)) {
                    dataSource.setJdbcUrl(dataSource.getJdbcUrl() + ";MODE=MYSQL");
                }
            }
            if (bean instanceof H2ConsoleProperties) {
                H2ConsoleProperties h2ConsoleProperties = (H2ConsoleProperties) bean;
                h2ConsoleProperties.setEnabled(true);
            }
            return bean;
        }

        @Bean
        public Server tcpServer() throws SQLException {
            Server server = Server.createTcpServer("-tcp", "-tcpAllowOthers").start();
            log.info("H2 database tcp server start: " + server.getURL());
            return server;
        }
    }
}
