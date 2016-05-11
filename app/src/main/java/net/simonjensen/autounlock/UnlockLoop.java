package net.simonjensen.autounlock;

import android.util.Log;

import java.util.ArrayList;
import java.util.List;

public class UnlockLoop implements Runnable {
    @Override
    public void run() {
        while (true) {
            Log.v("klj", "threading");
            Log.v("recordedWifi", UnlockService.recordedWifi.toString());

            List<List<String>> prevRecordedBluetooth = new ArrayList<List<String>>();
            List<List<String>> prevRecordedWifi = new ArrayList<List<String>>();
            List<List<String>> prevRecordedLocation = new ArrayList<List<String>>();
            List<List<String>> prevRecordedAccelerometer = new ArrayList<List<String>>();

            prevRecordedBluetooth = UnlockService.recordedBluetooth;
            UnlockService.recordedBluetooth = new ArrayList<List<String>>();

            prevRecordedWifi = UnlockService.recordedWifi;
            UnlockService.recordedWifi = new ArrayList<List<String>>();

            prevRecordedLocation = UnlockService.recordedLocation;
            UnlockService.recordedLocation = new ArrayList<List<String>>();

            prevRecordedAccelerometer = UnlockService.recordedAccelerometer;
            UnlockService.recordedAccelerometer = new ArrayList<List<String>>();

            List<List> dataBlob = new ArrayList<List>();
            dataBlob.add(prevRecordedBluetooth);
            dataBlob.add(prevRecordedWifi);
            dataBlob.add(prevRecordedLocation);
            dataBlob.add(prevRecordedAccelerometer);
            UnlockService.dataBuffer.add(dataBlob);

            for (int i = 0; i < prevRecordedBluetooth.size(); i++) {
                if (prevRecordedBluetooth.get(i).get(1).equals(BluetoothService.SIMON_BEKEY)) {
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
}
