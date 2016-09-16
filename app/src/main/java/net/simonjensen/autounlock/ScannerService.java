package net.simonjensen.autounlock;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;
import android.support.annotation.Nullable;
import android.util.Log;

import java.util.ArrayList;
import java.util.List;

public class ScannerService extends Service {
    private volatile boolean running = true;

    private Scanner scanner;
    private Thread scannerThread;

    private static final String TAG = "ScannerService";

    private Intent stopScan = new Intent("STOP_SCAN");

    @Override
    public void onCreate() {
        scanner = new Scanner();
        scannerThread = new Thread(scanner);
        scannerThread.start();
    }

    @Override
    public void onDestroy() {
        scanner.terminate();
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    private class Scanner implements Runnable {

        @Override
        public void run() {
            List<String> foundLocks = new ArrayList<String>();
            ArrayList<String> decisionLocks = new ArrayList<String>();
            long startTime = System.currentTimeMillis();

            while (running) {
                Log.i(TAG, "run: Scanning for locks " + CoreService.activeInnerGeofences.toString());
                for (BluetoothData bluetoothData : CoreService.recordedBluetooth) {
                    Log.e(TAG, "run: activeInnerGeofences " + bluetoothData.getSource() + CoreService.activeInnerGeofences.toString());
                    if (CoreService.activeInnerGeofences.contains(bluetoothData.getSource())) {
                        foundLocks.add(bluetoothData.getSource());
                    }
                }
                Log.i(TAG, "run: " + CoreService.currentOrientation + " " + CoreService.lastSignificantMovement);
                if (!foundLocks.isEmpty() && System.currentTimeMillis() - CoreService.lastSignificantMovement > 2000) {
                    for (String foundLock : foundLocks) {
                        LockData foundLockWithDetails = CoreService.dataStore.getLockDetails(foundLock);
                        if (foundLockWithDetails.getOrientation() == -1) {
                            NotificationUtility notification = new NotificationUtility();
                            notification.displayOrientationNotification(getApplicationContext(), foundLockWithDetails.getMAC(), CoreService.currentOrientation);
                            sendBroadcast(stopScan);
                            running = false;
                            stopSelf();
                        } else if (Math.min(Math.abs(CoreService.currentOrientation - foundLockWithDetails.getOrientation()),
                                Math.min(Math.abs((CoreService.currentOrientation - foundLockWithDetails.getOrientation()) + 360),
                                        Math.abs((CoreService.currentOrientation - foundLockWithDetails.getOrientation()) - 360)))
                                < 22.5 ) {
                            decisionLocks.add(foundLock);
                        }
                    }
                    if (!decisionLocks.isEmpty()) {
                        Intent startDecision = new Intent("START_DECISION");
                        startDecision.putStringArrayListExtra("Locks", decisionLocks);
                        sendBroadcast(startDecision);

                        sendBroadcast(stopScan);
                        running = false;
                        stopSelf();
                    }
                } else if (!foundLocks.isEmpty()) {
                    foundLocks = new ArrayList<>();
                    decisionLocks = new ArrayList<>();
                } else if (System.currentTimeMillis() - startTime > 60000) {
                    sendBroadcast(stopScan);
                    running = false;
                    stopSelf();
                }

                try {
                    Thread.sleep(300);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }

        private void terminate() {
            sendBroadcast(stopScan);
            running = false;
        }
    }
}
