package net.simonjensen.autounlock;

import android.Manifest;
import android.app.Notification;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.location.Criteria;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.*;
import android.os.Process;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.NotificationCompat;
import android.util.Log;
import android.widget.Toast;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.common.api.Status;
import com.google.android.gms.location.Geofence;
import com.google.android.gms.location.GeofencingRequest;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.model.LatLng;

import java.util.ArrayList;
import java.util.List;

public class UnlockService extends Service {

    private static final String TAG = "UnlockService";

    private Looper serviceLooper;
    private ServiceHandler serviceHandler;

    private Intent accelerometerIntent;
    private Intent locationIntent;
    private Intent wifiIntent;
    private Intent bluetoothIntent;

    private UnlockServiceLooper unlockServiceLooper;
    private Thread dataCollect;

    static List<BluetoothData> recordedBluetooth = new ArrayList<BluetoothData>();
    static List<WifiData> recordedWifi = new ArrayList<WifiData>();
    static List<LocationData> recordedLocation = new ArrayList<LocationData>();
    static List<AccelerometerData> recordedAccelerometer = new ArrayList<AccelerometerData>();

    static DataBuffer<List> dataBuffer;
    static DataStore dataStore;

    private net.simonjensen.autounlock.Geofence geofence;

    // Binder given to clients
    private final IBinder localBinder = new LocalBinder();

    /**
     * Class used for the client Binder.  Because we know this service always
     * runs in the same process as its clients, we don't need to deal with IPC.
     */
    public class LocalBinder extends Binder {
        UnlockService getService() {
            // Return this instance of LocalService so clients can call public methods
            return UnlockService.this;
        }
    }

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
            //stopSelf(msg.arg1);
        }
    }

    @Override
    public void onCreate() {
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

        // Running the service in the foreground by creating a notification
        Intent notificationIntent = new Intent(this, MainActivity.class);

        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0,
                notificationIntent, 0);

        Notification notification = new NotificationCompat.Builder(this)
                .setSmallIcon(R.mipmap.ic_launcher)
                .setContentTitle("AutoUnlock")
                .setContentText("Service running in the background")
                .setContentIntent(pendingIntent).build();

        startForeground(1337, notification);

        dataStore = new DataStore(this);

        Log.v("UnlockService", "Service created");

        accelerometerIntent = new Intent(this, AccelerometerService.class);
        locationIntent = new Intent(this, LocationService.class);
        wifiIntent = new Intent(this, WifiService.class);
        bluetoothIntent = new Intent(this, BluetoothService.class);

        geofence = new net.simonjensen.autounlock.Geofence(this);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        //Toast.makeText(this, "service starting", Toast.LENGTH_SHORT).show();

        // For each start request, send a message to start a job and deliver the
        // start ID so we know which request we're stopping when we finish the job
        Message msg = serviceHandler.obtainMessage();
        msg.arg1 = startId;
        serviceHandler.sendMessage(msg);

        geofence.connect();

        // If we get killed, after returning from here, restart
        return START_STICKY;
    }

    @Override
    public void onDestroy() {
        geofence.disconnect();
        Log.v("UnlockService", "Service destroyed");
    }

    @Override
    public IBinder onBind(Intent intent) {
        // localBinder is used for bound services
        return localBinder;
    }

    public void startAccelerometerService() {
        Log.v(TAG, "Starting AccelerometerService");
        Thread accelerometerServiceThread = new Thread() {
            public void run() {
                startService(accelerometerIntent);
            }
        };
        accelerometerServiceThread.start();
    }

    public void stopAccelerometerService() {
        stopService(accelerometerIntent);
    }

    public void startLoactionService() {
        Log.v(TAG, "Starting LocationService");
        Thread locationServiceThread = new Thread() {
            public void run() {
                startService(locationIntent);
            }
        };
        locationServiceThread.start();
    }

    public void stopLocationService() {
        stopService(locationIntent);
    }

    public void startWifiService() {
        Log.v(TAG, "Starting WifiService");
        Thread wifiServiceThread = new Thread() {
            public void run() {
                startService(wifiIntent);
            }
        };
        wifiServiceThread.start();
    }

    public void stopWifiService() {
        stopService(wifiIntent);
    }

    public void startBluetoothService() {
        Log.v(TAG, "Starting BluetoothService");
        Thread bluetoothServiceThread = new Thread() {
            public void run() {
                startService(bluetoothIntent);
            }
        };
        bluetoothServiceThread.start();
    }

    public void stopBluetoothService() {
        stopService(bluetoothIntent);
    }

    public void startDecision() {
        Toast.makeText(this, "BeKey found", Toast.LENGTH_SHORT).show();
    }

    public void startDataBufferCollection() {
        dataBuffer = new DataBuffer<List>(1000);
        unlockServiceLooper = new UnlockServiceLooper();
        dataCollect = new Thread(unlockServiceLooper);
        dataCollect.start();
    }

    public void stopDataBufferCollection() {
        Log.v("UnlockService", "Trying to stop unlockServiceLooper");
        if (dataCollect != null) {
            unlockServiceLooper.terminate();
        }
    }

    public void addGeofence() {
        geofence.addGeofence(this);
    }

    public void registerGeofences() {
        geofence.registerGeofences(this);
    }

    public void unregisterGeofences() {
        geofence.unregisterGeofences(this);
    }

    public void newDatastore() {
        dataStore.newDatastore();
    }
}
