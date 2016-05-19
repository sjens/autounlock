package net.simonjensen.autounlock;

import android.util.Log;

import java.util.ArrayList;
import java.util.List;

public class UnlockServiceLooper implements Runnable {
    private volatile boolean running = true;

    @Override
    public void run() {
        List<AccelerometerData> prevRecordedAccelerometer = new ArrayList<AccelerometerData>();
        List<BluetoothData> prevRecordedBluetooth = new ArrayList<BluetoothData>();
        List<LocationData> prevRecordedLocation = new ArrayList<LocationData>();
        List<WifiData> prevRecordedWifi = new ArrayList<WifiData>();

        while (running) {
            for (int i = 0; i < UnlockService.recordedBluetooth.size(); i++) {
                if (UnlockService.recordedBluetooth.get(i).getSource().equals(BluetoothService.RASMUS_BEKEY)) {
                    Log.e("Start Decision", "BeKey found");
                }
            }

            // In order to not have empty lists in the DataBuffer, previous data will be used if no new data has been found.
            if (!UnlockService.recordedAccelerometer.isEmpty() || prevRecordedAccelerometer.isEmpty()) {
                prevRecordedAccelerometer = UnlockService.recordedAccelerometer;
                UnlockService.recordedAccelerometer = new ArrayList<AccelerometerData>();
            }

            if (!UnlockService.recordedBluetooth.isEmpty() || prevRecordedBluetooth.isEmpty()) {
                prevRecordedBluetooth = UnlockService.recordedBluetooth;
                UnlockService.recordedBluetooth = new ArrayList<BluetoothData>();
            }

            if (!UnlockService.recordedLocation.isEmpty() || prevRecordedLocation.isEmpty()) {
                prevRecordedLocation = UnlockService.recordedLocation;
                UnlockService.recordedLocation = new ArrayList<LocationData>();
            }

            if (!UnlockService.recordedWifi.isEmpty() || prevRecordedWifi.isEmpty()) {
                prevRecordedWifi = UnlockService.recordedWifi;
                UnlockService.recordedWifi = new ArrayList<WifiData>();
            }

            List<List> dataBlob = new ArrayList<List>();
            dataBlob.add(prevRecordedAccelerometer);
            dataBlob.add(prevRecordedBluetooth);
            dataBlob.add(prevRecordedLocation);
            dataBlob.add(prevRecordedWifi);

            UnlockService.dataBuffer.add(dataBlob);

            StringBuilder stringBuilder = new StringBuilder();
            for (int i = 0; i < prevRecordedAccelerometer.size(); i++) {
                stringBuilder.append(prevRecordedAccelerometer.get(i).toString());
            }

            for (int i = 0; i < prevRecordedBluetooth.size(); i++) {
                stringBuilder.append(prevRecordedBluetooth.get(i).toString());
            }

            for (int i = 0; i < prevRecordedLocation.size(); i++) {
                stringBuilder.append(prevRecordedLocation.get(i).toString());
            }

            for (int i = 0; i < prevRecordedWifi.size(); i++) {
                stringBuilder.append(prevRecordedWifi.get(i).toString());
            }

            UnlockService.dataStore.insertBuffer(System.currentTimeMillis(), stringBuilder.toString());

            Log.v("dataBuffer", UnlockService.dataBuffer.toString());

            Log.v("StringOUT", stringBuilder.toString());
            try {
                Thread.sleep(2000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    public void terminate() {
        running = false;
    }
}
