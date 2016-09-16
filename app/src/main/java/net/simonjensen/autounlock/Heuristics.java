package net.simonjensen.autounlock;

import android.content.Context;
import android.util.Log;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

class Heuristics {
    private static final String TAG = "Heuristics";

    private List<BluetoothData> recentBluetoothList;
    private List<WifiData> recentWifiList;
    private List<LocationData> recentLocationList;

    public void setRecentBluetoothList(List<BluetoothData> recentBluetoothList) {
        this.recentBluetoothList = recentBluetoothList;
    }

    public void setRecentWifiList(List<WifiData> recentWifiList) {
        this.recentWifiList = recentWifiList;
    }

    public void setRecentLocationList(List<LocationData> recentLocationList) {
        this.recentLocationList = recentLocationList;
    }

    boolean makeDecision(Context context, ArrayList<String> foundLocks) {
        Map<String, Double> lockScores = new HashMap<>();
        Map.Entry<String, Double> maxEntry = null;

        // For each lock nearby, compare the recently recorded data with the stored data and give adapter score.
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

            if (CoreService.activeInnerGeofences.contains(foundLock)) {
                lockScore += 50;
            } else {
                lockScore -= 1000;
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

        if (maxEntry.getValue() > 200) {
            NotificationUtility notification = new NotificationUtility();
            notification.displayUnlockNotification(context, maxEntry.getKey(), recentBluetoothList, recentWifiList, recentLocationList);
            return true;
        } else {
            return false;
        }
    }

    void updateGeofenceSize(String lock, String type, String direction) {
        if (type.equals("Inner")) {
            if (direction.equals("Larger")) {

            } else if (direction.equals("Smaller")) {

            }
        } else if (type.equals("Outer")) {
            if (direction.equals("Larger")) {

            } else if (direction.equals("Smaller")) {

            }
        }
    }
}
