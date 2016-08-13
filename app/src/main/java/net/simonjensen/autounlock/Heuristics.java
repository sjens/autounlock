package net.simonjensen.autounlock;

import android.util.Log;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

class Heuristics {

    private static final String TAG = "Heuristics";

    void makeDecision(List<String> foundLocks) {
        Map<String, Double> lockScores = new HashMap<>();
        Map.Entry<String, Double> maxEntry = null;

        List<List> recentRecordedData;
        recentRecordedData = CoreService.dataBuffer.get();
        List<BluetoothData> recentBluetoothList = recentRecordedData.get(1);
        List<WifiData> recentWifiList = recentRecordedData.get(3);
        List<LocationData> recentLocationList = recentRecordedData.get(2);
        Log.d(TAG, recentBluetoothList.toString());
        Log.d(TAG, recentWifiList.toString());
        Log.d(TAG, recentLocationList.toString());

        // For each lock nearby, compare the recently recorded data with the stored data and give a score.
        for (String foundLock : foundLocks) {
            double lockScore = 0;
            int validWifi = 0;
            int validBluetooth = 0;
            LockData storedLockData;
            storedLockData = CoreService.dataStore.getLockDetails(foundLock);

            if (!recentWifiList.isEmpty()) {
                for (WifiData storedWifi : storedLockData.getNearbyWifiAccessPoints()) {
                    for (WifiData recentWifi : recentWifiList ) {
                        if (storedWifi.getMac().equals(recentWifi.getMac())) {
                            validWifi++;
                        }
                    }
                }
                if (validWifi != 0) {
                    Log.i(TAG, "validWifi " + validWifi + " total wifi " + storedLockData.getNearbyWifiAccessPoints().size());
                    lockScore += ((double)validWifi / (double)storedLockData.getNearbyWifiAccessPoints().size()) * 100;
                }
            }

            if (!recentBluetoothList.isEmpty()) {
                for (BluetoothData storedBluetooth : storedLockData.getNearbyBluetoothDevices()) {
                    for (BluetoothData recentBluetooth : recentBluetoothList) {
                        if (storedBluetooth.getSource().equals(recentBluetooth.getSource())) {
                            validBluetooth++;
                        }
                    }
                }
                if (validBluetooth != 0) {
                    lockScore += ((double)validBluetooth / (double)storedLockData.getNearbyBluetoothDevices().size()) * 100;
                }
            }

            if (CoreService.nearbyLocks.contains(foundLock)) {
                lockScore += 50;
            }
            Log.i(TAG, "lockScore " + lockScore);
            lockScores.put(foundLock, lockScore);
        }

        // Find lock with highest score, if multiple use first one found.
        for (Map.Entry<String, Double> entry : lockScores.entrySet()) {
            if (maxEntry == null || entry.getValue().compareTo(maxEntry.getValue()) > 0) {
                maxEntry = entry;
            }
        }
    }
}
