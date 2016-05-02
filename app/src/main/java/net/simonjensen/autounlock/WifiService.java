package net.simonjensen.autounlock;

import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiManager;
import android.os.IBinder;
import android.util.Log;

import java.util.ArrayList;
import java.util.List;

public class WifiService extends Service {
    int mStartMode;       // indicates how to behave if the service is killed
    IBinder mBinder;      // interface for clients that bind
    boolean mAllowRebind = false; // indicates whether onRebind should be used

    WifiManager wifiManager;

    private final BroadcastReceiver broadcastReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent.getAction().equals(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION)) {
                List<ScanResult> scanResults = wifiManager.getScanResults();
                long time = System.currentTimeMillis();

                for (int i = 0; i < scanResults.size(); i++) {
                    Log.v("Wifi", String.valueOf(scanResults.get(i)));
                    String SSID = scanResults.get(i).SSID;
                    String MAC = scanResults.get(i).BSSID;
                    String RSSI = String.valueOf(scanResults.get(i).level);

                    List<String> aWifiDevice = new ArrayList<String>();
                    aWifiDevice.add(SSID);
                    aWifiDevice.add(MAC);
                    aWifiDevice.add(RSSI);
                    aWifiDevice.add(String.valueOf(time));
                    UnlockService.foundWifi.add(aWifiDevice);
                    //Log.v("TEST", UnlockService.foundWifi.toString());

                    UnlockService.dataStore.insertWifi(SSID, MAC, RSSI, time);
                }
            }
        }
    };

    @Override
    public void onCreate() {
        // The service is being created
        wifiManager = (WifiManager) getSystemService(Context.WIFI_SERVICE);
        registerReceiver(broadcastReceiver, new IntentFilter(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION));
        wifiManager.startScan();
    }
    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        // The service is starting, due to a call to startService()
        return mStartMode;
    }
    @Override
    public IBinder onBind(Intent intent) {
        // A client is binding to the service with bindService()
        return mBinder;
    }
    @Override
    public boolean onUnbind(Intent intent) {
        // All clients have unbound with unbindService()
        return mAllowRebind;
    }
    @Override
    public void onRebind(Intent intent) {
        // A client is binding to the service with bindService(),
        // after onUnbind() has already been called
    }
    @Override
    public void onDestroy() {
        // The service is no longer used and is being destroyed
    }
}