package net.simonjensen.autounlock;

import android.app.Notification;
import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.*;
import android.os.Process;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.NotificationCompat;
import android.util.Log;
import android.widget.Toast;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.common.api.Status;
import com.google.android.gms.location.LocationServices;

import java.util.ArrayList;
import java.util.List;

public class CoreService extends Service implements
        GoogleApiClient.ConnectionCallbacks,
        GoogleApiClient.OnConnectionFailedListener,
        ResultCallback<Status> {

    private static final String TAG = "CoreService";

    private Looper serviceLooper;
    private ServiceHandler serviceHandler;
    private ScannerService lockScanner;

    private Intent accelerometerIntent;
    private Intent bluetoothIntent;
    private Intent wifiIntent;
    private Intent locationIntent;
    private Intent dataProcessorIntent;
    private Intent scannerIntent;

    private GoogleApiClient mGoogleApiClient;
    private net.simonjensen.autounlock.Geofence geofence;

    static ArrayList<String> export = new ArrayList<>();
    static ArrayList<String> velocity = new ArrayList<>();
    static List<BluetoothData> recordedBluetooth = new ArrayList<BluetoothData>();
    static List<WifiData> recordedWifi = new ArrayList<WifiData>();
    static List<LocationData> recordedLocation = new ArrayList<LocationData>();
    static List<AccelerometerData> recordedAccelerometer = new ArrayList<AccelerometerData>();
    static volatile ArrayList<String> activeInnerGeofences = new ArrayList<>();
    static ArrayList<String> activeOuterGeofences = new ArrayList<>();

    static long lastSignificantMovement = 0;
    static float currentOrientation = -1f;

    static boolean isLocationDataCollectionStarted = false;
    static boolean isDetailedDataCollectionStarted = false;
    static volatile boolean isScanningForLocks = false;

    static DataBuffer<List> dataBuffer;
    static DataStore dataStore;

    // Binder given to clients
    private final IBinder localBinder = new LocalBinder();

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
            // Normally we would do some work here, like download adapter file.
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
        // Start up the thread running the service.  Note that we create adapter
        // separate thread because the service normally runs in the process's
        // main thread, which we don't want to block.  We also make it
        // background priority so CPU-intensive work will not disrupt our UI.
        HandlerThread thread = new HandlerThread("ServiceStartArguments",
                Process.THREAD_PRIORITY_BACKGROUND);
        thread.start();

        // Get the HandlerThread's Looper and use it for our Handler
        serviceLooper = thread.getLooper();
        serviceHandler = new ServiceHandler(serviceLooper);

        // Running the service in the foreground by creating adapter notification
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
        geofence = new net.simonjensen.autounlock.Geofence();
        //heuristics = new Heuristics();

        accelerometerIntent = new Intent(this, AccelerometerService.class);
        locationIntent = new Intent(this, LocationService.class);
        wifiIntent = new Intent(this, WifiService.class);
        bluetoothIntent = new Intent(this, BluetoothService.class);
        dataProcessorIntent = new Intent(this, DataProcessorService.class);
        scannerIntent = new Intent(this, ScannerService.class);

        buildGoogleApiClient();

        IntentFilter geofencesFilter = new IntentFilter();
        geofencesFilter.addAction("GEOFENCES_ENTERED");
        geofencesFilter.addAction("GEOFENCES_EXITED");
        registerReceiver(geofencesReceiver, geofencesFilter);

        IntentFilter startDecisionFilter = new IntentFilter();
        startDecisionFilter.addAction("START_DECISION");
        registerReceiver(startDecisionReceiver, startDecisionFilter);

        IntentFilter heuristicsTunerFilter = new IntentFilter();
        heuristicsTunerFilter.addAction("HEURISTICS_TUNER");
        heuristicsTunerFilter.addAction("ADD_ORIENTATION");
        heuristicsTunerFilter.addAction("STOP_SCAN");
        heuristicsTunerFilter.addAction("START_SCAN");
        registerReceiver(heuristicsReceiver, heuristicsTunerFilter);

        Log.v("CoreService", "Service created");
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        // For each start request, send adapter message to start adapter job and deliver the
        // start ID so we know which request we're stopping when we finish the job
        Message msg = serviceHandler.obtainMessage();
        msg.arg1 = startId;
        serviceHandler.sendMessage(msg);

        googleConnect();

        // If we get killed, after returning from here, restart
        return START_STICKY;
    }

    @Override
    public void onDestroy() {
        googleDisconnect();
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

    void googleConnect() {
        if (!mGoogleApiClient.isConnecting() || !mGoogleApiClient.isConnected()) {
            mGoogleApiClient.connect();
        }
    }

    void googleDisconnect() {
        if (mGoogleApiClient.isConnecting() || mGoogleApiClient.isConnected()) {
            mGoogleApiClient.disconnect();
        }
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

    private BroadcastReceiver geofencesReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            Bundle extras = intent.getExtras();
            List<String> triggeringGeofencesList = extras.getStringArrayList("Geofences");

            Log.i(TAG, "onReceive: " + extras.getStringArrayList("Geofences"));

            if ("GEOFENCES_ENTERED".equals(action)) {
                for (String geofence : triggeringGeofencesList) {
                    if (geofence.contains("inner")) {
                        activeInnerGeofences.add(geofence.substring(5));
                        if (!isDetailedDataCollectionStarted) {
                            Log.i(TAG, "onReceive: starting detailed data collection");
                            isDetailedDataCollectionStarted = true;
                            isScanningForLocks = true;
                            startAccelerometerService();
                            startBluetoothService();
                            startWifiService();
                            scanForLocks();
                        }
                    } else if (geofence.contains("outer")) {
                        activeOuterGeofences.add(geofence.substring(5));
                        if (!isLocationDataCollectionStarted) {
                            isLocationDataCollectionStarted = true;
                            startLocationService();
                        }
                    }
                }
            } else if ("GEOFENCES_EXITED".equals(action)) {
                for (String geofence : triggeringGeofencesList) {
                    if (geofence.contains("inner")) {
                        if (isDetailedDataCollectionStarted && activeInnerGeofences.isEmpty()) {
                            isDetailedDataCollectionStarted = false;
                            isScanningForLocks = false;
                            stopAccelerometerService();
                            stopBluetoothService();
                            stopWifiService();
                        }
                    } else if (geofence.contains("outer")) {
                        if (isLocationDataCollectionStarted && activeOuterGeofences.isEmpty()) {
                            isLocationDataCollectionStarted = false;
                            stopLocationService();
                        }
                    }
                }
            }
        }
    };

    private BroadcastReceiver startDecisionReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            Bundle extras = intent.getExtras();
            Log.e(TAG, "StartDecision");
            if ("START_DECISION".equals(action)) {
                Log.i(TAG, "onReceive: arraylist " + extras.getStringArrayList("Locks"));
                if (!startHeuristicsDecision(extras.getStringArrayList("Locks"))) {
                    isLocationDataCollectionStarted = true;
                    isDetailedDataCollectionStarted = true;
                    isScanningForLocks = true;
                    startLocationService();
                    startAccelerometerService();
                    startBluetoothService();
                    startWifiService();
                    scanForLocks();
                }
            }
        }
    };

    private BroadcastReceiver heuristicsReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            Log.i(TAG, "onReceive: tuning heuristics");
            String action = intent.getAction();
            Bundle extras = intent.getExtras();
            if ("ADD_ORIENTATION".equals(action)) {
                Log.w(TAG, "onReceive: add orientation, lock: " + intent.getExtras().getString("lock") + " orientation: " + intent.getExtras().getFloat("orientation"));
                dataStore.updateLockOrientation(
                        intent.getExtras().getString("lock"),
                        intent.getExtras().getFloat("orientation"));
                if (isDetailedDataCollectionStarted && !isScanningForLocks) {
                    isScanningForLocks = true;
                    scanForLocks();
                }
            } else if ("STOP_SCAN".equals(action)) {
                stopAccelerometerService();
                stopBluetoothService();
                stopWifiService();
                stopLocationService();
                isScanningForLocks = false;
                isDetailedDataCollectionStarted = false;
                isLocationDataCollectionStarted = false;
            } else if ("START_SCAN".equals(action)) {
                scanForLocks();
            } else if ("HEURISTICS_TUNER".equals(action)) {
                switch (extras.getInt("Position")) {
                    case 0: updateGeofenceSize(extras.getString("Lock"), "Inner", "Smaller");
                        break;
                    case 1: updateGeofenceSize(extras.getString("Lock"), "Inner", "Larger");
                        break;
                    case 2: updateGeofenceSize(extras.getString("Lock"), "Outer", "Smaller");
                        break;
                    case 3: updateGeofenceSize(extras.getString("Lock"), "Outer", "Larger");
                        break;
                    case 4: redoDataCollection(extras.getString("Lock"));
                        break;
                    case 5: redoOrientation(extras.getString("Lock"));
                }
            }
        }
    };

    void startAccelerometerService() {
        export = new ArrayList<>();
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

    void startLocationService() {
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

    boolean startHeuristicsDecision(ArrayList<String> foundLocks) {
        Log.d(TAG, foundLocks.toString());
        Toast.makeText(this, "BeKey found", Toast.LENGTH_SHORT).show();

        Heuristics heuristics = new Heuristics();
        heuristics.setRecentBluetoothList(recordedBluetooth);
        heuristics.setRecentWifiList(recordedWifi);
        heuristics.setRecentLocationList(recordedLocation);
        return heuristics.makeDecision(this, foundLocks);
    }

    void updateGeofenceSize(String lock, String type, String direction) {
        if (type.equals("Inner")) {
            if (direction.equals("Larger")) {
                LockData lockData = dataStore.getLockDetails(lock);
                String size = String.valueOf(lockData.getInnerGeofence() * 1.25);
                dataStore.updateGeofence(lock, "inner_geofence", size);
            } else if (direction.equals("Smaller")) {
                LockData lockData = dataStore.getLockDetails(lock);
                String size = String.valueOf(lockData.getInnerGeofence() * 0.75);
                dataStore.updateGeofence(lock, "inner_geofence", size);
            }
        } else if (type.equals("Outer")) {
            if (direction.equals("Larger")) {
                LockData lockData = dataStore.getLockDetails(lock);
                String size = String.valueOf(lockData.getOuterGeofence() * 1.25);
                dataStore.updateGeofence(lock, "outer_geofence", size);
            } else if (direction.equals("Smaller")) {
                LockData lockData = dataStore.getLockDetails(lock);
                String size = String.valueOf(lockData.getOuterGeofence() * 0.75);
                dataStore.updateGeofence(lock, "outer_geofence", size);
            }
        }
    }


    void redoDataCollection(String lock) {
        dataStore.deleteLockData(lock);
        manualUnlock(lock);
    }

    void redoOrientation(String lock) {
        dataStore.updateLockOrientation(lock, -1f);
    }

    void startDataBuffer() {
        Log.d(TAG, "Starting data processing");
        dataBuffer = new DataBuffer<List>(1000);
        Thread dataProcessorThread = new Thread() {
            public void run() {
                startService(dataProcessorIntent);
            }
        };
        dataProcessorThread.start();
    }

    void stopDataBuffer() {
        Log.d("CoreService", "Trying to stop dataProcessor");
        stopService(dataProcessorIntent);
    }

    void startDataCollection() {
        startBluetoothService();
        startWifiService();
        startLocationService();
        startDataBuffer();
    }

    void stopDataCollection() {
        stopBluetoothService();
        stopWifiService();
        stopLocationService();
        stopDataBuffer();
    }

    void addGeofences() {
        ArrayList<LockData> lockDataArrayList = dataStore.getKnownLocks();
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

    void manualUnlock(final String lockMAC) {
        new Thread(new Runnable() {
            public void run() {
                boolean success = true;
                String passphrase = "";

                startAccelerometerService();
                startBluetoothService();
                startWifiService();
                startLocationService();

                try {
                    Thread.sleep(20000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }

                stopAccelerometerService();
                stopBluetoothService();
                stopWifiService();
                stopLocationService();

                if (success && recordedLocation.size() != 0) {
                    LocationData currentLocation = recordedLocation.get(recordedLocation.size() - 1);

                    LockData lockData = new LockData(
                            lockMAC,
                            passphrase,
                            currentLocation,
                            30,
                            100,
                            -1f,
                            recordedBluetooth,
                            recordedWifi
                    );
                    Log.d(TAG, lockData.toString());
                    newLock(lockData);
                } else {
                    Log.e(TAG, "No location found, cannot add lock");
                }
            }
        }).start();
    }

    private boolean newLock(LockData lockData) {
        Log.d(TAG, "Inserting lock into db");
        dataStore.insertLockDetails(
                lockData.getMAC(),
                lockData.getPassphrase(),
                lockData.getLocation().getLatitude(),
                lockData.getLocation().getLongitude(),
                lockData.getInnerGeofence(),
                lockData.getOuterGeofence(),
                lockData.getOrientation(),
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

        unregisterGeofences();
        addGeofences();
        registerGeofences();
        return true;
    }

    private void scanForLocks() {
        Log.e(TAG, "scanForLocks: " + activeInnerGeofences);
        Thread scannerServiceThread = new Thread() {
            public void run() {
                startService(scannerIntent);
            }
        };
        scannerServiceThread.start();
    }

    void getLock(String lockMAC) {
        LockData lock = dataStore.getLockDetails(lockMAC);
        if (lock == null) {
            Log.d(TAG, "no lock found");
        } else {
            Log.e(TAG, lock.toString());
        }
    }
}
