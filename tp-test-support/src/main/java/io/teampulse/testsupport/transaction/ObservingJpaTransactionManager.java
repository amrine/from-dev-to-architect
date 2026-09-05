package io.teampulse.testsupport.transaction;

import jakarta.persistence.EntityManagerFactory;
import org.springframework.orm.jpa.JpaTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.DefaultTransactionStatus;

final class ObservingJpaTransactionManager extends JpaTransactionManager {

    private final TransactionManagerProbe transactionProbe;

    ObservingJpaTransactionManager(
        EntityManagerFactory entityManagerFactory,
        TransactionManagerProbe transactionProbe
    ) {
        super(entityManagerFactory);
        this.transactionProbe = transactionProbe;
    }

    @Override
    protected void doBegin(
        Object transaction,
        TransactionDefinition definition
    ) {
        super.doBegin(transaction, definition);
        transactionProbe.recordBegin(
            definition.getName(),
            definition.isReadOnly()
        );
    }

    @Override
    protected void doCommit(DefaultTransactionStatus status) {
        super.doCommit(status);
        transactionProbe.recordCommit();
    }

    @Override
    protected void doRollback(DefaultTransactionStatus status) {
        super.doRollback(status);
        transactionProbe.recordRollback();
    }
}
