package net.simonjensen.autounlock;

import android.Manifest;
import android.app.Notification;
import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.location.Criteria;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.*;
import android.os.Process;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.NotificationCompat;
import android.util.Log;
import android.view.View;
import android.widget.Toast;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.common.api.Status;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.model.LatLng;

import java.util.ArrayList;
import java.util.List;

public class CoreService extends Service implements
        GoogleApiClient.ConnectionCallbacks,
        GoogleApiClient.OnConnectionFailedListener,
        ResultCallback<Status> {

    private static final String TAG = "CoreService";

    private Looper serviceLooper;
    private ServiceHandler serviceHandler;

    private Intent accelerometerIntent;
    private Intent bluetoothIntent;
    private Intent wifiIntent;
    private Intent locationIntent;

    private DataProcessor dataProcessor;
    private Thread dataCollector;
    private Heuristics heuristics;

    static List<BluetoothData> recordedBluetooth = new ArrayList<BluetoothData>();
    static List<WifiData> recordedWifi = new ArrayList<WifiData>();
    static List<LocationData> recordedLocation = new ArrayList<LocationData>();
    static List<AccelerometerData> recordedAccelerometer = new ArrayList<AccelerometerData>();
    static ArrayList nearbyLocks = new ArrayList();

    static DataBuffer<List> dataBuffer;
    static DataStore dataStore;

    private net.simonjensen.autounlock.Geofence geofence;

    // Binder given to clients
    private final IBinder localBinder = new LocalBinder();
    private GoogleApiClient mGoogleApiClient;

    /**
     * Class used for the client Binder.  Because we know this service always
     * runs in the same process as its clients, we don't need to deal with IPC.
     */
    class LocalBinder extends Binder {
        CoreService getService() {
            // Return this instance of LocalService so clients can call public methods
            return CoreService.this;
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

        Log.v("CoreService", "Service created");

        accelerometerIntent = new Intent(this, AccelerometerService.class);
        locationIntent = new Intent(this, LocationService.class);
        wifiIntent = new Intent(this, WifiService.class);
        bluetoothIntent = new Intent(this, BluetoothService.class);

        buildGoogleApiClient();

        geofence = new net.simonjensen.autounlock.Geofence();

        IntentFilter enteredGeofencesFilter = new IntentFilter();
        enteredGeofencesFilter.addAction("GEOFENCES_ENTERED");
        registerReceiver(geofencesEntered, enteredGeofencesFilter);

        IntentFilter exitedGeofencesFilter = new IntentFilter();
        exitedGeofencesFilter.addAction("GEOFENCES_EXITED");
        registerReceiver(geofencesExited, exitedGeofencesFilter);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        //Toast.makeText(this, "service starting", Toast.LENGTH_SHORT).show();

        // For each start request, send a message to start a job and deliver the
        // start ID so we know which request we're stopping when we finish the job
        Message msg = serviceHandler.obtainMessage();
        msg.arg1 = startId;
        serviceHandler.sendMessage(msg);

        if (!mGoogleApiClient.isConnecting() || !mGoogleApiClient.isConnected()) {
            mGoogleApiClient.connect();
        }

        // If we get killed, after returning from here, restart
        return START_STICKY;
    }

    @Override
    public void onDestroy() {
        if (mGoogleApiClient.isConnecting() || mGoogleApiClient.isConnected()) {
            mGoogleApiClient.disconnect();
        }
        Log.v("CoreService", "Service destroyed");
    }

    @Override
    public IBinder onBind(Intent intent) {
        // localBinder is used for bound services
        return localBinder;
    }

    private synchronized void buildGoogleApiClient() {
        mGoogleApiClient = new GoogleApiClient.Builder(this)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .addApi(LocationServices.API)
                .build();
    }

    @Override
    public void onConnected(@Nullable Bundle bundle) {
        Log.v(TAG, "Connected ");
        addGeofences();
    }

    @Override
    public void onConnectionSuspended(int i) {

    }

    @Override
    public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {

    }

    @Override
    public void onResult(@NonNull Status status) {

    }

    private BroadcastReceiver geofencesEntered = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            Bundle extras = intent.getExtras();
            Log.i(TAG, String.valueOf(extras.getStringArrayList("Geofences")));
            for (String lockMAC : extras.getStringArrayList("Geofences")) {
                if (!nearbyLocks.contains(lockMAC)) {
                    nearbyLocks.add(lockMAC);
                }
            }
            if (!nearbyLocks.isEmpty()) {
                startDataCollection();
            }
        }
    };

    private BroadcastReceiver geofencesExited = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            //TODO: Stop all data collection
        }
    };

    void startAccelerometerService() {
        Log.v(TAG, "Starting AccelerometerService");
        Thread accelerometerServiceThread = new Thread() {
            public void run() {
                startService(accelerometerIntent);
            }
        };
        accelerometerServiceThread.start();
    }

    void stopAccelerometerService() {
        stopService(accelerometerIntent);
    }

    void startLoactionService() {
        Log.v(TAG, "Starting LocationService");
        Thread locationServiceThread = new Thread() {
            public void run() {
                startService(locationIntent);
            }
        };
        locationServiceThread.start();
    }

    void stopLocationService() {
        stopService(locationIntent);
    }

    void startWifiService() {
        Log.v(TAG, "Starting WifiService");
        Thread wifiServiceThread = new Thread() {
            public void run() {
                startService(wifiIntent);
            }
        };
        wifiServiceThread.start();
    }

    void stopWifiService() {
        stopService(wifiIntent);
    }

    void startBluetoothService() {
        Log.v(TAG, "Starting BluetoothService");
        Thread bluetoothServiceThread = new Thread() {
            public void run() {
                startService(bluetoothIntent);
            }
        };
        bluetoothServiceThread.start();
    }

    void stopBluetoothService() {
        stopService(bluetoothIntent);
    }

    void startDecision(String foundLock) {
        Toast.makeText(this, "BeKey found", Toast.LENGTH_SHORT).show();
        heuristics.makeDecision(foundLock);
    }

    void notifyDecision() {

    }

    void startDataBuffer() {
        dataBuffer = new DataBuffer<List>(1000);
        dataProcessor = new DataProcessor();
        dataCollector = new Thread(dataProcessor);
        dataCollector.start();
    }

    void stopDataBuffer() {
        Log.d("CoreService", "Trying to stop dataProcessor");
        if (dataCollector != null) {
            dataProcessor.terminate();
        }
    }

    void startDataCollection() {
        startBluetoothService();
        startWifiService();
        startLoactionService();
        startDataBuffer();
    }

    void addGeofences() {
        ArrayList<LockData> lockDataArrayList = dataStore.getKnownLocks();
        Log.v(TAG, lockDataArrayList.get(0).toString());
        if (!lockDataArrayList.isEmpty()) {
            for (int i = 0; i < lockDataArrayList.size(); i++) {
                geofence.addGeofence(lockDataArrayList.get(i));
            }
            registerGeofences();
        }
    }

    void registerGeofences() {
        geofence.registerGeofences(this, mGoogleApiClient);
    }

    void unregisterGeofences() {
        geofence.unregisterGeofences(this, mGoogleApiClient);
    }

    void newDatastore() {
        dataStore.deleteDatastore();
    }

    void newTruePositive() { long time = System.currentTimeMillis(); dataStore.insertDecision(1, time); }

    void newFalsePositive() { long time = System.currentTimeMillis(); dataStore.insertDecision(0, time); }

    void manualUnlock(String lockMAC) {
        boolean success = true;
        String passphrase = "";

        LocationData currentLocation = recordedLocation.get(recordedLocation.size() - 1);

        if (success && recordedLocation.size() != 0) {
            LockData lockData = new LockData(
                    lockMAC,
                    passphrase,
                    currentLocation,
                    10,
                    100,
                    recordedBluetooth,
                    recordedWifi
            );
            Log.d(TAG, lockData.toString());
            newLock(lockData);
        }
    }

    boolean newLock(LockData lockData) {
        Log.d(TAG, "Inserting lock into db");
        dataStore.insertLockDetails(
                lockData.getMAC(),
                lockData.getPassphrase(),
                lockData.getLocation().getLatitude(),
                lockData.getLocation().getLongitude(),
                lockData.getInnerGeofence(),
                lockData.getOuterGeofence(),
                System.currentTimeMillis()
        );

        for (int i = 0; i < lockData.getNearbyBluetoothDevices().size(); i++) {
            dataStore.insertBtle(
                    lockData.getNearbyBluetoothDevices().get(i).getName(),
                    lockData.getNearbyBluetoothDevices().get(i).getSource(),
                    lockData.getNearbyBluetoothDevices().get(i).getRssi(),
                    lockData.getMAC(),
                    lockData.getNearbyBluetoothDevices().get(i).getTime()
            );
        }

        for (int i = 0; i < lockData.getNearbyWifiAccessPoints().size(); i++) {
            dataStore.insertWifi(
                    lockData.getNearbyWifiAccessPoints().get(i).getSsid(),
                    lockData.getNearbyWifiAccessPoints().get(i).getMac(),
                    lockData.getNearbyWifiAccessPoints().get(i).getRssi(),
                    lockData.getMAC(),
                    lockData.getNearbyWifiAccessPoints().get(i).getTime()
            );
        }
        return true;
    }

    void getLock(String lockMAC) {
        LockData lock = dataStore.getLockDetails(lockMAC);
        if (lock == null) {
            Log.e(TAG, "no lock found");
        } else {
            Log.e(TAG, lock.toString());
        }
    }
}
