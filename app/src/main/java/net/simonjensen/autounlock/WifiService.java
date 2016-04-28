package net.simonjensen.autounlock;

import android.app.Service;
import android.content.*;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiManager;
import android.os.*;
import android.os.Process;
import android.util.Log;
import android.widget.Toast;

import java.util.List;

public class WifiService extends Service {
    private Looper serviceLooper;
    private ServiceHandler serviceHandler;

    WifiManager wifiManager;
    DataStore dataStore;

    private final BroadcastReceiver broadcastReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent.getAction().equals(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION)) {
                List<ScanResult> scanResults = wifiManager.getScanResults();
                long time = System.currentTimeMillis();
                String stringTime = String.valueOf(time);

                for (int i = 0; i < scanResults.size(); i++) {
                    Log.v("Wifi", String.valueOf(scanResults.get(i)));
                    String SSID = scanResults.get(i).SSID;
                    String MAC = scanResults.get(i).BSSID;
                    String RSSI = String.valueOf(scanResults.get(i).level);
                    dataStore.insertWifi(SSID, MAC, RSSI, time);
                }
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
        Toast.makeText(this, "wifi service onCreate", Toast.LENGTH_SHORT).show();
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

        Log.v("HERE?", "");

        dataStore = new DataStore(this);

        wifiManager = (WifiManager) getSystemService(Context.WIFI_SERVICE);
        registerReceiver(broadcastReceiver,
                new IntentFilter(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION));
        wifiManager.startScan();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Toast.makeText(this, "wifi service starting", Toast.LENGTH_SHORT).show();

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
        dataStore.close();
        unregisterReceiver(broadcastReceiver);
        Toast.makeText(this, "wifi service done", Toast.LENGTH_SHORT).show();
    }
}
