package io.teampulse.identity;

import io.teampulse.testsupport.persistence.PersistenceIntegrationTestConfiguration;
import io.teampulse.testsupport.transaction.JpaTransactionManagerProbeConfiguration;
import io.teampulse.testsupport.transaction.TransactionManagerProbe;
import jakarta.inject.Inject;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.transaction.support.TransactionTemplate;

@SpringBootTest(classes = IdentityTestApplication.class, webEnvironment = SpringBootTest.WebEnvironment.NONE)
@Import({PersistenceIntegrationTestConfiguration.class, JpaTransactionManagerProbeConfiguration.class})
public abstract class AbstractIntegrationTest {

    @Inject
    protected TransactionManagerProbe transactionProbe;

    @Inject
    protected TransactionTemplate transactionTemplate;


    @BeforeEach
    void cleanDatabase() {
        transactionProbe.reset();
    }

    /**
     * Executes fixture setup in a dedicated transaction, then resets the probe
     * so transaction assertions observe only the system under test.
     */
    protected final void inTransactionTemplate(Runnable operation) {
        transactionTemplate.executeWithoutResult(
            ignored -> operation.run()
        );

        transactionProbe.reset();
    }
}
