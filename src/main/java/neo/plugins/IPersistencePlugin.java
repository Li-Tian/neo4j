package neo.plugins;

import java.util.ArrayList;

import neo.persistence.Snapshot;
import neo.ledger.Blockchain.ApplicationExecuted;

public interface IPersistencePlugin {
    void onPersist(Snapshot snapshot, ArrayList<ApplicationExecuted> applicationExecutedList);

    void onCommit(Snapshot snapshot);

    boolean shouldThrowExceptionFromCommit(Exception ex);
}