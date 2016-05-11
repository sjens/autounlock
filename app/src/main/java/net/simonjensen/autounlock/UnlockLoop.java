package net.simonjensen.autounlock;

import android.util.Log;

import java.util.ArrayList;
import java.util.List;

public class UnlockLoop implements Runnable {
    @Override
    public void run() {
        while (true) {
            Log.v("klj", "threading");
            Log.v("foundWifi", UnlockService.foundWifi.toString());

            List<List<String>> prevFoundBluetooth = new ArrayList<List<String>>();
            List<List<String>> prevFoundWifi = new ArrayList<List<String>>();
            List<List<String>> prevFoundLocation = new ArrayList<List<String>>();
            List<List<String>> prevRecordedAccelerometer = new ArrayList<List<String>>();

            prevFoundBluetooth = UnlockService.foundBluetooth;
            UnlockService.foundBluetooth = new ArrayList<List<String>>();

            prevFoundWifi = UnlockService.foundWifi;
            UnlockService.foundWifi = new ArrayList<List<String>>();

            prevFoundLocation = UnlockService.foundLocation;
            UnlockService.foundLocation = new ArrayList<List<String>>();

            prevRecordedAccelerometer = UnlockService.recordedAccelerometer;
            UnlockService.recordedAccelerometer = new ArrayList<List<String>>();

            List<List> dataBlob = new ArrayList<List>();
            dataBlob.add(prevFoundBluetooth);
            dataBlob.add(prevFoundWifi);
            dataBlob.add(prevFoundLocation);
            dataBlob.add(prevRecordedAccelerometer);
            UnlockService.dataBuffer.add(dataBlob);

            Log.v("dataBuffer", UnlockService.dataBuffer.toString());
            try {
                Thread.sleep(2000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }
}
