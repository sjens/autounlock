package net.simonjensen.autounlock;

import android.util.Log;

import java.util.ArrayList;
import java.util.List;

public class DataProcessor implements Runnable {
    private volatile boolean running = true;

    @Override
    public void run() {
        List<AccelerometerData> prevRecordedAccelerometer = new ArrayList<AccelerometerData>();
        List<BluetoothData> prevRecordedBluetooth = new ArrayList<BluetoothData>();
        List<LocationData> prevRecordedLocation = new ArrayList<LocationData>();
        List<WifiData> prevRecordedWifi = new ArrayList<WifiData>();

        while (running) {
            for (int i = 0; i < CoreService.recordedBluetooth.size(); i++) {
                if (CoreService.recordedBluetooth.get(i).getSource().equals(BluetoothService.RASMUS_BEKEY)) {
                    Log.e("Start Decision", "BeKey found");
                }
            }

            // In order to not have empty lists in the DataBuffer, previous data will be used if no new data has been found.
            if (!CoreService.recordedAccelerometer.isEmpty() || prevRecordedAccelerometer.isEmpty()) {
                prevRecordedAccelerometer = CoreService.recordedAccelerometer;
                CoreService.recordedAccelerometer = new ArrayList<AccelerometerData>();
            }

            if (!CoreService.recordedBluetooth.isEmpty() || prevRecordedBluetooth.isEmpty()) {
                prevRecordedBluetooth = CoreService.recordedBluetooth;
                CoreService.recordedBluetooth = new ArrayList<BluetoothData>();
            }

            if (!CoreService.recordedLocation.isEmpty() || prevRecordedLocation.isEmpty()) {
                prevRecordedLocation = CoreService.recordedLocation;
                CoreService.recordedLocation = new ArrayList<LocationData>();
            }

            if (!CoreService.recordedWifi.isEmpty() || prevRecordedWifi.isEmpty()) {
                prevRecordedWifi = CoreService.recordedWifi;
                CoreService.recordedWifi = new ArrayList<WifiData>();
            }

            List<List> dataBlob = new ArrayList<List>();
            dataBlob.add(prevRecordedAccelerometer);
            dataBlob.add(prevRecordedBluetooth);
            dataBlob.add(prevRecordedLocation);
            dataBlob.add(prevRecordedWifi);

            CoreService.dataBuffer.add(dataBlob);

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

            CoreService.dataStore.insertBuffer(System.currentTimeMillis(), stringBuilder.toString());

            Log.v("dataBuffer", CoreService.dataBuffer.toString());

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
