package io.teampulse.testsupport.transaction;

import jakarta.persistence.EntityManagerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.orm.jpa.JpaTransactionManager;

@TestConfiguration(proxyBeanMethods = false)
public class JpaTransactionManagerProbeConfiguration {

    @Bean
    TransactionManagerProbe transactionManagerProbe() {
        return new TransactionManagerProbe();
    }

    @Bean
    @Primary
    JpaTransactionManager transactionManager(
        ObjectProvider<EntityManagerFactory> entityManagerFactoryProvider,
        TransactionManagerProbe transactionProbe
    ) {
        return new ObservingJpaTransactionManager(
            entityManagerFactoryProvider.getObject(),
            transactionProbe
        );
    }
}
