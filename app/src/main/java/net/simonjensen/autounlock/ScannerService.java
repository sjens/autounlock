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
            List<String> foundLocks = new ArrayList<>();
            List<String> decisionLocks = new ArrayList<>();
            long startTime = System.currentTimeMillis();

            while (running) {
                Log.i(TAG, "run: Scanning for locks " + CoreService.activeInnerGeofences.toString());
                for (BluetoothData bluetoothData : CoreService.recordedBluetooth) {
                    Log.e(TAG, "run: activeInnerGeofences " + bluetoothData.getSource() + CoreService.activeInnerGeofences.toString());
                    if (CoreService.activeInnerGeofences.contains(bluetoothData.getSource())) {
                        foundLocks.add(bluetoothData.getSource());
                    }
                }
                if (!foundLocks.isEmpty() && System.currentTimeMillis() - CoreService.lastSignificantMovement > 2000) {
                    for (String foundLock : foundLocks) {
                        Log.d(TAG, "run: here? --");
                        LockData foundLockWithDetails = CoreService.dataStore.getLockDetails(foundLock);
                        if (foundLockWithDetails.getOrientation() == -1) {
                            NotificationUtils notification = new NotificationUtils();
                            notification.displayOrientationNotification(getApplicationContext(), foundLockWithDetails.getMAC(), CoreService.currentOrientation);
                            CoreService.isScanningForLocks = false;
                        } else if (Math.min(Math.abs(CoreService.currentOrientation - foundLockWithDetails.getOrientation()),
                                Math.min(Math.abs((CoreService.currentOrientation - foundLockWithDetails.getOrientation()) + 360),
                                        Math.abs((CoreService.currentOrientation - foundLockWithDetails.getOrientation()) - 360)))
                                < 22.5 ) {
                            decisionLocks.add(foundLock);
                            Log.w(TAG, "run: decisionLocks " + decisionLocks.toString());
                        } else {
                            Log.e(TAG, "run: we shouldn't get this far");
                        }
                    }
                    if (!decisionLocks.isEmpty()) {
                        Log.e(TAG, "run: here?");
                        //startHeuristicsDecision(decisionLocks);
                        CoreService.isScanningForLocks = false;
                        running = false;
                    }
                } else if (!foundLocks.isEmpty()) {
                    foundLocks = new ArrayList<>();
                    decisionLocks = new ArrayList<>();
                } else if (System.currentTimeMillis() - startTime > 6) {
                    Log.e(TAG, "run: stopping");
                    CoreService.isScanningForLocks = false;
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
            CoreService.isScanningForLocks = false;
            running = false;
        }
    }
}
