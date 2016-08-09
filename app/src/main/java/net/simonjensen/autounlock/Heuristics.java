package net.simonjensen.autounlock;

public class Heuristics {

    boolean makeDecision(String foundLock) {
        CoreService.dataStore.getLockDetails(foundLock);
        return false;
    }
}
