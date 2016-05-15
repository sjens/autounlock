// Geofencing from https://github.com/googlesamples/android-play-location/tree/master/Geofencing

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

public class UnlockService extends Service implements
        GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener, ResultCallback<Status> {

    private static final String TAG = "UnlockService";

    private Looper serviceLooper;
    private ServiceHandler serviceHandler;

    private Intent accelerometerIntent;
    private Intent locationIntent;
    private Intent wifiIntent;
    private Intent bluetoothIntent;

    private UnlockServiceLooper unlockServiceLooper;
    private Thread dataCollect;

    static List<List<String>> recordedBluetooth = new ArrayList<List<String>>();
    static List<List<String>> recordedWifi = new ArrayList<List<String>>();
    static List<List<String>> recordedLocation = new ArrayList<List<String>>();
    static List<List<String>> recordedAccelerometer = new ArrayList<List<String>>();

    static DataBuffer<List> dataBuffer;
    static DataStore dataStore;

    LocationManager locationManager;
    LocationListener locationListener;
    /**
     * The list of geofences used in this sample.
     */
    protected ArrayList<Geofence> geofences;

    /**
     * Used to keep track of whether geofences were added.
     */
    private boolean geofencesAdded;

    /**
     * Used when requesting to add or remove geofences.
     */
    private PendingIntent geofencePendingIntent;

    /**
     * Used to persist application state about whether geofences were added.
     */
    private SharedPreferences sharedPreferences;

    /**
     * Provides the entry point to Google Play services.
     */
    protected GoogleApiClient googleApiClient;

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

        // Empty list for storing geofences.
        geofences = new ArrayList<Geofence>();

        // Initially set the PendingIntent used in addGeofences() and removeGeofences() to null.
        geofencePendingIntent = null;

        // Retrieve an instance of the SharedPreferences object.
        sharedPreferences = getSharedPreferences(Constants.SHARED_PREFERENCES_NAME,
                MODE_PRIVATE);

        // Get the value of mGeofencesAdded from SharedPreferences. Set to false as a default.
        geofencesAdded = sharedPreferences.getBoolean(Constants.GEOFENCES_ADDED_KEY, false);

        // Kick off the request to build GoogleApiClient.
        buildGoogleApiClient();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        //Toast.makeText(this, "service starting", Toast.LENGTH_SHORT).show();

        // For each start request, send a message to start a job and deliver the
        // start ID so we know which request we're stopping when we finish the job
        Message msg = serviceHandler.obtainMessage();
        msg.arg1 = startId;
        serviceHandler.sendMessage(msg);

        googleApiClient.connect();

        // If we get killed, after returning from here, restart
        return START_STICKY;
    }

    @Override
    public void onDestroy() {
        googleApiClient.disconnect();
        Log.v("UnlockService", "Service destroyed");
    }

    @Override
    public IBinder onBind(Intent intent) {
        // localBinder is used for bound services
        return localBinder;
    }

    /**
     * Builds a GoogleApiClient. Uses the {@code #addApi} method to request the LocationServices API.
     */
    protected synchronized void buildGoogleApiClient() {
        googleApiClient = new GoogleApiClient.Builder(this)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .addApi(LocationServices.API)
                .build();
    }

    /**
     * Runs when a GoogleApiClient object successfully connects.
     */
    @Override
    public void onConnected(Bundle connectionHint) {
        Log.i(TAG, "Connected to GoogleApiClient");
    }

    @Override
    public void onConnectionFailed(ConnectionResult result) {
        // Refer to the javadoc for ConnectionResult to see what error codes might be returned in
        // onConnectionFailed.
        Log.i(TAG, "Connection failed: ConnectionResult.getErrorCode() = " + result.getErrorCode());
    }

    @Override
    public void onConnectionSuspended(int cause) {
        // The connection to Google Play services was lost for some reason.
        Log.i(TAG, "Connection suspended");

        // onConnected() will be called again automatically when the service reconnects
    }

    /**
     * Builds and returns a GeofencingRequest. Specifies the list of geofences to be monitored.
     * Also specifies how the geofence notifications are initially triggered.
     */
    private GeofencingRequest getGeofencingRequest() {
        GeofencingRequest.Builder builder = new GeofencingRequest.Builder();

        // The INITIAL_TRIGGER_ENTER flag indicates that geofencing service should trigger a
        // GEOFENCE_TRANSITION_ENTER notification when the geofence is added and if the device
        // is already inside that geofence.
        builder.setInitialTrigger(GeofencingRequest.INITIAL_TRIGGER_ENTER);

        // Add the geofences to be monitored by geofencing service.
        builder.addGeofences(geofences);

        // Return a GeofencingRequest.
        return builder.build();
    }

    private void logSecurityException(SecurityException securityException) {
        Log.e(TAG, "Invalid location permission. " +
                "You need to use ACCESS_FINE_LOCATION with geofences", securityException);
    }

    /**
     * Runs when the result of calling addGeofences() and removeGeofences() becomes available.
     * Either method can complete successfully or with an error.
     * <p>
     * Since this activity implements the {@link ResultCallback} interface, we are required to
     * define this method.
     *
     * @param status The Status returned through a PendingIntent when addGeofences() or
     *               removeGeofences() get called.
     */
    public void onResult(Status status) {
        if (status.isSuccess()) {
            // Update state and save in shared preferences.
            geofencesAdded = !geofencesAdded;
            SharedPreferences.Editor editor = sharedPreferences.edit();
            editor.putBoolean(Constants.GEOFENCES_ADDED_KEY, geofencesAdded);
            editor.apply();

            // Update the UI. Adding geofences enables the Remove Geofences button, and removing
            // geofences enables the Add Geofences button.
            //setButtonsEnabledState();

            Toast.makeText(
                    this,
                    getString(geofencesAdded ? R.string.geofences_added :
                            R.string.geofences_removed),
                    Toast.LENGTH_SHORT
            ).show();
        } else {
            // Get the status code for the error and log it using a user-friendly message.
            String errorMessage = GeofenceErrorMessages.getErrorString(this,
                    status.getStatusCode());
            Log.e(TAG, errorMessage);
        }
    }

    /**
     * Gets a PendingIntent to send with the request to add or remove Geofences. Location Services
     * issues the Intent inside this PendingIntent whenever a geofence transition occurs for the
     * current list of geofences.
     *
     * @return A PendingIntent for the IntentService that handles geofence transitions.
     */
    private PendingIntent getGeofencePendingIntent() {
        // Reuse the PendingIntent if we already have it.
        if (geofencePendingIntent != null) {
            return geofencePendingIntent;
        }
        Intent intent = new Intent(this, GeofenceTransitionsIntentService.class);
        // We use FLAG_UPDATE_CURRENT so that we get the same pending intent back when calling
        // addGeofences() and removeGeofences().
        return PendingIntent.getService(this, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT);
    }

    /**
     * This sample hard codes geofence data. A real app might dynamically create geofences based on
     * the user's location.
     */
    public void populateGeofenceList(String name, LatLng location, Float radius) {

        Log.v("Populate geofence list", "Name: " + name + " Location: " + location);

        geofences.add(new Geofence.Builder()
                // Set the request ID of the geofence. This is a string to identify this
                // geofence.
                .setRequestId(name)

                // Set the circular region of this geofence.
                // radius in meters
                .setCircularRegion(
                        location.latitude,
                        location.longitude,
                        radius
                )

                // Set the expiration duration of the geofence. This geofence gets automatically
                // removed after this period of time.
                .setExpirationDuration(Constants.GEOFENCE_EXPIRATION_IN_MILLISECONDS)

                // Set the transition types of interest. Alerts are only generated for these
                // transition. We track entry and exit transitions in this sample.
                .setTransitionTypes(Geofence.GEOFENCE_TRANSITION_ENTER |
                        Geofence.GEOFENCE_TRANSITION_EXIT)

                // Create the geofence.
                .build());
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
        // Necessary permission check
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED
                && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
            return;
        }

        locationManager = (LocationManager) this.getSystemService(Context.LOCATION_SERVICE);
        Criteria criteria = new Criteria();
        criteria.setAccuracy(Criteria.ACCURACY_FINE);
        locationManager.requestSingleUpdate(criteria, new LocationListener() {
            @Override
            public void onLocationChanged(Location location) {
                LatLng currentLocation = new LatLng(location.getLatitude(), location.getLongitude());
                // Name should be BeKey MAC address
                populateGeofenceList("test", currentLocation, 50f);
            }

            @Override
            public void onStatusChanged(String provider, int status, Bundle extras) {

            }

            @Override
            public void onProviderEnabled(String provider) {

            }

            @Override
            public void onProviderDisabled(String provider) {

            }
        }, null);
    }

    public void registerGeofences() {
        Log.v("geofences", geofences.toString());
        if (!googleApiClient.isConnected()) {
            Toast.makeText(this, getString(R.string.not_connected), Toast.LENGTH_SHORT).show();
            return;
        }

        try {
            LocationServices.GeofencingApi.addGeofences(
                    googleApiClient,
                    // The GeofenceRequest object.
                    getGeofencingRequest(),
                    // A pending intent that that is reused when calling removeGeofences(). This
                    // pending intent is used to generate an intent when a matched geofence
                    // transition is observed.
                    getGeofencePendingIntent()
            ).setResultCallback(this); // Result processed in onResult().
        } catch (SecurityException securityException) {
            // Catch exception generated if the app does not use ACCESS_FINE_LOCATION permission.
            logSecurityException(securityException);
        }
    }

    public void unregisterGeofences() {
        if (!googleApiClient.isConnected()) {
            Toast.makeText(this, getString(R.string.not_connected), Toast.LENGTH_SHORT).show();
            return;
        }
        try {
            // Remove geofences.
            LocationServices.GeofencingApi.removeGeofences(
                    googleApiClient,
                    // This is the same pending intent that was used in addGeofences().
                    getGeofencePendingIntent()
            ).setResultCallback(this); // Result processed in onResult().
        } catch (SecurityException securityException) {
            // Catch exception generated if the app does not use ACCESS_FINE_LOCATION permission.
            logSecurityException(securityException);
        }
    }
}
