package net.simonjensen.autounlock;

import android.annotation.TargetApi;
import android.app.Service;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothManager;
import android.bluetooth.le.ScanCallback;
import android.bluetooth.le.ScanResult;
import android.bluetooth.le.ScanSettings;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.IBinder;
import android.os.PowerManager;
import android.util.Log;

public class BluetoothService extends Service {
    static String TAG = "BluetoothService";

    int startMode;       // indicates how to behave if the service is killed
    IBinder binder;      // interface for clients that bind
    boolean allowRebind; // indicates whether onRebind should be used

    static final String SIMON_BEKEY = "7C:09:2B:EF:04:04";
    static final String RASMUS_BEKEY = "7C:09:2B:EF:03:FB";
    static final String MIBAND = "C8:0F:10:1F:C1:23";

    BluetoothAdapter bluetoothAdapter;
    ScanSettings scanSettings;

    PowerManager powerManager;
    PowerManager.WakeLock wakeLock;

    //Scan call back function
    private ScanCallback scanCallback = new ScanCallback() {
        @Override
        public void onScanResult(int callbackType, ScanResult result) {
            super.onScanResult(callbackType, result);
            Log.v("Bluetooth", result.getDevice().getName()
                    + " " + result.getDevice().getAddress()
                    + " " + result.getRssi()
                    + " " + result.getTimestampNanos());

            String name = result.getDevice().getName();
            String source = result.getDevice().getAddress();
            int RSSI = result.getRssi();
            long time = System.currentTimeMillis();

            BluetoothData aBluetoothDevice;
            aBluetoothDevice = new BluetoothData(name, source, RSSI, time);
            CoreService.recordedBluetooth.add(aBluetoothDevice);

            CoreService.dataStore.insertBtle(name, source, RSSI, time);

            Intent i = new Intent("BTLE_CONN");
            i.putExtra("mac", source);
            i.putExtra("rssi", RSSI);
            sendBroadcast(i);
        }
    };

    private void setScanSettings() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            scanSettings = new ScanSettings.Builder()
                    .setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY)
                    // The following require Marshmallow.
                    .setCallbackType(ScanSettings.CALLBACK_TYPE_ALL_MATCHES)
                    .setMatchMode(ScanSettings.MATCH_MODE_AGGRESSIVE)
                    .setNumOfMatches(ScanSettings.MATCH_NUM_MAX_ADVERTISEMENT)
                    .setReportDelay(0)
                    .build();
        } else {
            scanSettings = new ScanSettings.Builder()
                    .setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY)
                    .setReportDelay(0)
                    .build();
        }
    }

    @TargetApi(Build.VERSION_CODES.M)
    @Override
    public void onCreate() {
        // The service is being created
        powerManager = (PowerManager) getApplicationContext().getSystemService(Context.POWER_SERVICE);
        wakeLock = powerManager.newWakeLock(PowerManager.FULL_WAKE_LOCK, "BluetoothService");
        wakeLock.acquire();

        final BluetoothManager bluetoothManager = (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
        bluetoothAdapter = bluetoothManager.getAdapter();

        // Enabling bluetooth if not enabled.
        if (!bluetoothAdapter.isEnabled()) {
            Log.v(TAG, "Bluetooth is off, turning on");
            bluetoothAdapter.enable();

            // Waiting for bluetooth adapter to turn on.
            while (true) {
                if (bluetoothAdapter.isEnabled()) {
                    break;
                }
            }
        }

        setScanSettings();

        bluetoothAdapter.getBluetoothLeScanner().startScan(null, scanSettings, scanCallback);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        // The service is starting, due to a call to startService()
        return startMode;
    }

    @Override
    public IBinder onBind(Intent intent) {
        // A client is binding to the service with bindService()
        return binder;
    }

    @Override
    public boolean onUnbind(Intent intent) {
        // All clients have unbound with unbindService()
        return allowRebind;
    }

    @Override
    public void onRebind(Intent intent) {
        // A client is binding to the service with bindService(),
        // after onUnbind() has already been called
    }

    @Override
    public void onDestroy() {
        // The service is no longer used and is being destroyed
        Log.v("BluetoothService", "Stopping");
        bluetoothAdapter.getBluetoothLeScanner().stopScan(scanCallback);
        wakeLock.release();
    }
}
