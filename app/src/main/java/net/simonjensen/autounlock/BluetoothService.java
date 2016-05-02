package net.simonjensen.autounlock;

import android.app.Service;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.*;
import android.os.*;
import android.os.Process;
import android.util.Log;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class BluetoothService extends Service {
    private Looper serviceLooper;
    private ServiceHandler serviceHandler;

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
                UnlockService.foundBluetooth.add(aBluetoothDevice);

                UnlockService.dataStore.insertBtle(name, source, RSSI, time);
            }
        }
    };

    // Handler that receives messages from the thread
    private final class ServiceHandler extends Handler {
        public ServiceHandler(Looper looper) {
            super(looper);
        }
        @Override
        public void handleMessage(Message msg) {
            // Normally we would do some work here, like download a file.
            // For our sample, we just sleep for 5 seconds.
            try {
                Thread.sleep(5000);
            } catch (InterruptedException e) {
                // Restore interrupt status.
                Thread.currentThread().interrupt();
            }
            // Stop the service using the startId, so that we don't stop
            // the service in the middle of handling another job
            stopSelf(msg.arg1);
        }
    }

    @Override
    public void onCreate() {
        //Toast.makeText(this, "bluetooth service onCreate", Toast.LENGTH_SHORT).show();
        // Start up the thread running the service.  Note that we create a
        // separate thread because the service normally runs in the process's
        // main thread, which we don't want to block.  We also make it
        // background priority so CPU-intensive work will not disrupt our UI.
        HandlerThread thread = new HandlerThread("ServiceStartArguments",
                Process.THREAD_PRIORITY_BACKGROUND);
        thread.start();

        // Get the HandlerThread's Looper and use it for our Handler
        serviceLooper = thread.getLooper();
        serviceHandler = new ServiceHandler(serviceLooper);

        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        bluetoothAdapter.startDiscovery();

        startBluetoothDiscovery();
    }

    private void startBluetoothDiscovery() {
        IntentFilter filter = new IntentFilter(BluetoothDevice.ACTION_FOUND);
        registerReceiver(broadcastReceiver, filter); // Don't forget to unregister during onDestroy
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        //Toast.makeText(this, "bluetooth service starting", Toast.LENGTH_SHORT).show();

        // For each start request, send a message to start a job and deliver the
        // start ID so we know which request we're stopping when we finish the job
        Message msg = serviceHandler.obtainMessage();
        msg.arg1 = startId;
        serviceHandler.sendMessage(msg);

        // If we get killed, after returning from here, restart
        return START_STICKY;
    }

    @Override
    public IBinder onBind(Intent intent) {
        // We don't provide binding, so return null
        return null;
    }

    @Override
    public void onDestroy() {
        unregisterReceiver(broadcastReceiver);
    }
}
