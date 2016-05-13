package net.simonjensen.autounlock;

import android.app.Service;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.IBinder;
import android.util.Log;

import java.util.ArrayList;
import java.util.List;

public class BluetoothService extends Service {
    int startMode;       // indicates how to behave if the service is killed
    IBinder binder;      // interface for clients that bind
    boolean allowRebind; // indicates whether onRebind should be used

    static final String SIMON_BEKEY = "7C:09:2B:EF:04:04";

    BluetoothAdapter bluetoothAdapter;

    private final BroadcastReceiver broadcastReceiver = new BroadcastReceiver() {
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            long time = System.currentTimeMillis();
            // When discovery finds a device
            if (BluetoothDevice.ACTION_FOUND.equals(action)) {
                // Get the BluetoothDevice object from the Intent
                BluetoothDevice bluetoothDevice = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                String name = bluetoothDevice.getName();
                String source = bluetoothDevice.getAddress();
                int RSSI = intent.getShortExtra(BluetoothDevice.EXTRA_RSSI, Short.MIN_VALUE);
                Log.v("Bluetooth:", bluetoothDevice.getName() + bluetoothDevice.getAddress() + bluetoothDevice.getUuids() + RSSI);

                List<String> aBluetoothDevice = new ArrayList<String>();
                aBluetoothDevice.add(name);
                aBluetoothDevice.add(source);
                aBluetoothDevice.add(String.valueOf(RSSI));
                aBluetoothDevice.add(String.valueOf(time));
                UnlockService.recordedBluetooth.add(aBluetoothDevice);

                UnlockService.dataStore.insertBtle(name, source, RSSI, time);
            }
        }
    };

    @Override
    public void onCreate() {
        // The service is being created
        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        bluetoothAdapter.startDiscovery();

        IntentFilter filter = new IntentFilter(BluetoothDevice.ACTION_FOUND);
        registerReceiver(broadcastReceiver, filter); // Don't forget to unregister during onDestroy
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
        unregisterReceiver(broadcastReceiver);
    }
}
