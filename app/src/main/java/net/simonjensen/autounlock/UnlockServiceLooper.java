package net.simonjensen.autounlock;

import android.util.Log;

import java.util.ArrayList;
import java.util.List;

public class UnlockServiceLooper implements Runnable {
    private volatile boolean running = true;

    @Override
    public void run() {
        List<BluetoothData> prevRecordedBluetooth = new ArrayList<BluetoothData>();
        List<WifiData> prevRecordedWifi = new ArrayList<WifiData>();
        List<LocationData> prevRecordedLocation = new ArrayList<LocationData>();
        List<AccelerometerData> prevRecordedAccelerometer = new ArrayList<AccelerometerData>();

        while (running) {
            Log.v("klj", "threading");
            Log.v("recordedWifi", prevRecordedWifi.toString());

            // In order to not have empty lists in the DataBuffer, previous data will be used if no new data has been found.
            if (!UnlockService.recordedBluetooth.isEmpty() || prevRecordedBluetooth.isEmpty()) {
                prevRecordedBluetooth = UnlockService.recordedBluetooth;
                UnlockService.recordedBluetooth = new ArrayList<BluetoothData>();
            }

            if (!UnlockService.recordedWifi.isEmpty() || prevRecordedWifi.isEmpty()) {
                prevRecordedWifi = UnlockService.recordedWifi;
                UnlockService.recordedWifi = new ArrayList<WifiData>();
            }

            if (!UnlockService.recordedLocation.isEmpty() || prevRecordedLocation.isEmpty()) {
                prevRecordedLocation = UnlockService.recordedLocation;
                UnlockService.recordedLocation = new ArrayList<LocationData>();
            }

            if (!UnlockService.recordedAccelerometer.isEmpty() || prevRecordedAccelerometer.isEmpty()) {
                prevRecordedAccelerometer = UnlockService.recordedAccelerometer;
                UnlockService.recordedAccelerometer = new ArrayList<AccelerometerData>();
            }

            List<List> dataBlob = new ArrayList<List>();
            dataBlob.add(prevRecordedBluetooth);
            dataBlob.add(prevRecordedWifi);
            dataBlob.add(prevRecordedLocation);
            dataBlob.add(prevRecordedAccelerometer);
            UnlockService.dataBuffer.add(dataBlob);

            for (int i = 0; i < UnlockService.recordedBluetooth.size(); i++) {
                if (UnlockService.recordedBluetooth.get(i).getName().equals(BluetoothService.SIMON_BEKEY)) {
                    Log.v("Start Decision", "BeKey found");
                }
            }

            Log.v("dataBuffer", UnlockService.dataBuffer.toString());
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
