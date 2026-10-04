package io.teampulse.testsupport;

import io.teampulse.testsupport.persistence.PostgreSQLDatabaseCleaner;
import io.teampulse.testsupport.transaction.TransactionManagerProbe;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * Shared database isolation for integration tests backed by JPA.
 */
public abstract class AbstractJpaIntegrationTest {

    @Autowired
    private PostgreSQLDatabaseCleaner databaseCleaner;

    @Autowired
    protected TransactionManagerProbe transactionProbe;

    @Autowired
    protected TransactionTemplate transactionTemplate;

    @BeforeEach
    protected final void prepareTest() {
        databaseCleaner.clean();
        transactionProbe.reset();
    }

    /**
     * Executes fixture setup in a dedicated transaction, then resets the probe
     * so transaction assertions observe only the system under test.
     */
    protected final void inTransactionTemplate(Runnable operation) {
        transactionTemplate.executeWithoutResult(ignored -> operation.run());
        transactionProbe.reset();
    }

    @AfterEach
    protected final void cleanDatabaseAfterTest() {
        databaseCleaner.clean();
    }
}
