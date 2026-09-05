package io.teampulse.testsupport.transaction;

public final class TransactionManagerProbe {

    public record TransactionObservation(
        String name,
        boolean readOnly,
        boolean committed,
        boolean rolledBack
    ) { }

    private TransactionObservation observation;

    synchronized void recordBegin(String name, boolean readOnly) {
        observation = new TransactionObservation(
            name,
            readOnly,
            false,
            false
        );
    }

    synchronized void recordCommit() {
        TransactionObservation current = requireObservation();
        observation = new TransactionObservation(
            current.name(),
            current.readOnly(),
            true,
            false
        );
    }

    synchronized void recordRollback() {
        TransactionObservation current = requireObservation();
        observation = new TransactionObservation(
            current.name(),
            current.readOnly(),
            false,
            true
        );
    }

    public synchronized TransactionObservation observation() {
        return requireObservation();
    }

    public synchronized void reset() {
        observation = null;
    }

    private TransactionObservation requireObservation() {
        if (observation == null) {
            throw new IllegalStateException("No transaction was observed");
        }
        return observation;
    }
}
