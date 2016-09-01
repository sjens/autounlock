package net.simonjensen.autounlock;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;
import android.support.annotation.Nullable;
import android.util.Log;
import java.util.ArrayList;
import java.util.List;

public class DataProcessorService extends Service {
    private volatile boolean running = true;

    private DataProcessor dataProcessor;
    private Thread dataCollector;

    private static String TAG = "DataProcessorService";

    @Override
    public void onCreate() {
        dataProcessor = new DataProcessor();
        dataCollector = new Thread(dataProcessor);
        dataCollector.start();
    }

    @Override
    public void onDestroy() {
        dataProcessor.terminate();
        CoreService.recordedBluetooth = new ArrayList<BluetoothData>();
        CoreService.recordedLocation = new ArrayList<LocationData>();
        CoreService.recordedWifi = new ArrayList<WifiData>();
    }

    void sendDecisionIntent(List foundLocks) {
        Intent startDecision = new Intent("START_DECISION");
        sendBroadcast(startDecision);
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    private class DataProcessor implements Runnable {

        @Override
        public void run() {
            List<AccelerometerData> prevRecordedAccelerometer = new ArrayList<AccelerometerData>();
            List<BluetoothData> prevRecordedBluetooth = new ArrayList<BluetoothData>();
            List<LocationData> prevRecordedLocation = new ArrayList<LocationData>();
            List<WifiData> prevRecordedWifi = new ArrayList<WifiData>();

            List foundLocks = new ArrayList();

            while (running) {
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

                // Do not start decision making before we have at least one nearby Bluetooth device (the lock),
                // and adapter location. We cannot be sure any Wifi access points are nearby.
                if (!prevRecordedBluetooth.isEmpty() && !prevRecordedLocation.isEmpty()) {
                    for (int i = 0; i < CoreService.recordedBluetooth.size(); i++) {
                        if (CoreService.recordedBluetooth.get(i).getSource().equals(BluetoothService.MIBAND)) {
                            Log.e("Start Decision", "BeKey found");
                            //coreService.startHeuristicsDecision(CoreService.recordedBluetooth.get(i).getSource());
                            foundLocks.add(CoreService.recordedBluetooth.get(i).getSource());
                        }
                    }
                    if (!foundLocks.isEmpty()) {
                        sendDecisionIntent(foundLocks);
                    }
                }

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

                Log.v("StringOUT", stringBuilder.toString());
                try {
                    Thread.sleep(2000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }

        private void terminate() {
            running = false;
        }
    }
}
