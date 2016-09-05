package net.simonjensen.autounlock;

import android.Manifest;
import android.app.ActivityManager;
import android.content.*;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.IBinder;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity {
    private static final String TAG = "MainActivity";
    private CoreService coreService;
    private boolean bound = false;
    private static boolean geofencAdded = false;
    private static int allButton = 0;

    private View trainingView;
    private TextView trainingBtleMacValue;
    private TextView trainingBtleRssiValue;

    private static Button startAccelerometer;
    private static Button stopAccelerometer;
    private static boolean startAccelerometerEnabled = true;
    private static boolean stopAccelerometerEnabled = false;

    private static Button startLocation;
    private static Button stopLocation;
    private static boolean startLocationEnabled = true;
    private static boolean stopLocationEnabled = false;

    private static Button startWifi;
    private static Button stopWifi;
    private static boolean startWifiEnabled = true;
    private static boolean stopWifiEnabled = false;

    private static Button startBluetooth;
    private static Button stopBluetooth;
    private static boolean startBluetoothEnabled = true;
    private static boolean stopBluetoothEnabled = false;

    private static Button startAll;
    private static Button stopAll;
    private static boolean startAllEnabled = true;
    private static boolean stopAllEnabled = false;

    private static Button startBuffer;
    private static Button stopBuffer;
    private static boolean startBufferEnabled = true;
    private static boolean stopBufferEnabled = false;

    private static Button registerGeofence;
    private static Button unregisterGeofence;
    private static boolean registerGeofenceEnabled = false;
    private static boolean unregisterGeofenceEnabled = false;

    private BroadcastReceiver receiver;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        startAccelerometer = (Button) findViewById(R.id.accstart);
        stopAccelerometer = (Button) findViewById(R.id.accstop);
        startAccelerometer.setEnabled(startAccelerometerEnabled);
        stopAccelerometer.setEnabled(stopAccelerometerEnabled);

        startLocation = (Button) findViewById(R.id.locationstart);
        stopLocation = (Button) findViewById(R.id.locationstop);
        startLocation.setEnabled(startLocationEnabled);
        stopLocation.setEnabled(stopLocationEnabled);

        startWifi = (Button) findViewById(R.id.wifistart);
        stopWifi = (Button) findViewById(R.id.wifistop);
        startWifi.setEnabled(startWifiEnabled);
        stopWifi.setEnabled(stopWifiEnabled);

        startBluetooth = (Button) findViewById(R.id.bluetoothstart);
        stopBluetooth = (Button) findViewById(R.id.bluetoothstop);
        startBluetooth.setEnabled(startBluetoothEnabled);
        stopBluetooth.setEnabled(stopBluetoothEnabled);

        startAll = (Button) findViewById(R.id.allstart);
        stopAll = (Button) findViewById(R.id.allstop);
        startAll.setEnabled(startAllEnabled);
        stopAll.setEnabled(stopAllEnabled);

        startBuffer = (Button) findViewById(R.id.databufferstart);
        stopBuffer = (Button) findViewById(R.id.databufferstop);
        startBuffer.setEnabled(startBufferEnabled);
        stopBuffer.setEnabled(stopBufferEnabled);

        registerGeofence = (Button) findViewById(R.id.registergeofence);
        unregisterGeofence = (Button) findViewById(R.id.unregistergeofence);
        registerGeofence.setEnabled(registerGeofenceEnabled);
        unregisterGeofence.setEnabled(unregisterGeofenceEnabled);

        DataStore dataStore = new DataStore(this);

        trainingView = findViewById(R.id.feedbackControlsContainer);
        trainingBtleMacValue = (TextView) findViewById(R.id.btleMacValue);
        trainingBtleRssiValue = (TextView) findViewById(R.id.btleRssiValue);

        IntentFilter filter = new IntentFilter();
        filter.addAction("BTLE_CONN");

        receiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                Bundle extras = intent.getExtras();
                String extraMac = extras.getString("mac");
                int extraRSSI = extras.getInt("rssi");
                trainingBtleMacValue.setText(extraMac);
                trainingBtleRssiValue.setText(Integer.toString(extraRSSI));
                trainingView.setVisibility(View.VISIBLE);
                Log.v("IntentBroadcast", "MAC: " + extraMac);
                Log.v("IntentBroadcast", "RSSI: " + extraRSSI);
            }
        };
        registerReceiver(receiver, filter);
    }

    @Override
    protected void onDestroy() {
        if (receiver != null) {
            unregisterReceiver(receiver);
            receiver = null;
        }
        super.onDestroy();
    }

    @Override
    protected void onStart() {
        super.onStart();

        if (isMyServiceRunning(CoreService.class)) {
            bindService(new Intent(this, CoreService.class), serviceConnection, Context.BIND_AUTO_CREATE);
        } else {
            ComponentName coreService = startService(new Intent(this, CoreService.class));
            bindService(new Intent(this, CoreService.class), serviceConnection, Context.BIND_AUTO_CREATE);
        }

        // Check for location permission on startup if not granted.
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.WRITE_EXTERNAL_STORAGE}, 1);
        } else

        // Check for permission to write to external storage.
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE, Manifest.permission.ACCESS_FINE_LOCATION}, 1);
        }
    }

    @Override
    protected void onStop() {
        super.onStop();
        // Unbind from the service
        if (bound) {
            unbindService(serviceConnection);
            bound = false;
        }
    }

    private boolean isMyServiceRunning(Class<?> serviceClass) {
        ActivityManager manager = (ActivityManager) getSystemService(Context.ACTIVITY_SERVICE);
        for (ActivityManager.RunningServiceInfo service : manager.getRunningServices(Integer.MAX_VALUE)) {
            if (serviceClass.getName().equals(service.service.getClassName())) {
                return true;
            }
        }
        return false;
    }

    public void onButtonClickTruePositive(View v) {
        if (bound) {
            coreService.newTruePositive();
            Log.d("Manual Decision", "True Positive");
        }
    }

    public void onButtonClickFalsePositive(View v) {
        if (bound) {
            coreService.newFalsePositive();
            Log.d("Manual Decision", "False Positive");
        }
    }

    void setStartAccelerometer() {
        startAccelerometerEnabled = false;
        startAccelerometer.setEnabled(startAccelerometerEnabled);
        stopAccelerometerEnabled = true;
        stopAccelerometer.setEnabled(stopAccelerometerEnabled);
    }

    /** Called when adapter button is clicked (the button in the layout file attaches to
     * this method with the android:onClick attribute) */
    public void onButtonClickAccel(View v) {
        if (bound) {
            coreService.startAccelerometerService();
            setStartAccelerometer();

            startAllEnabled = false;
            startAll.setEnabled(startAllEnabled);
            stopAllEnabled = true;
            stopAll.setEnabled(stopAllEnabled);
            allButton += 1;
        }
    }

    void setStopAccelerometer() {
        startAccelerometerEnabled = true;
        startAccelerometer.setEnabled(startAccelerometerEnabled);
        stopAccelerometerEnabled = false;
        stopAccelerometer.setEnabled(stopAccelerometerEnabled);
    }

    public void onButtonClickAccelStop(View v) {
        if (bound) {
            coreService.stopAccelerometerService();
            setStopAccelerometer();

            if (allButton == 1) {
                startAllEnabled = true;
                startAll.setEnabled(startAllEnabled);
                stopAllEnabled = false;
                stopAll.setEnabled(stopAllEnabled);
                allButton -= 1;
            } else {
                allButton -= 1;
            }
        }
    }

    void setStartLocation() {
        startLocationEnabled = false;
        startLocation.setEnabled(startLocationEnabled);
        stopLocationEnabled = true;
        stopLocation.setEnabled(stopLocationEnabled);
    }

    public void onButtonClickLocation(View v) {
        if (bound) {
            coreService.startLocationService();
            setStartLocation();

            startAllEnabled = false;
            startAll.setEnabled(startAllEnabled);
            stopAllEnabled = true;
            stopAll.setEnabled(stopAllEnabled);
            allButton += 1;
        }
    }

    void setStopLocation() {
        startLocationEnabled = true;
        startLocation.setEnabled(startLocationEnabled);
        stopLocationEnabled = false;
        stopLocation.setEnabled(stopLocationEnabled);
    }

    public void onButtonClickLocationStop(View v) {
        if (bound) {
            coreService.stopLocationService();
            setStopLocation();

            if (allButton == 1) {
                startAllEnabled = true;
                startAll.setEnabled(startAllEnabled);
                stopAllEnabled = false;
                stopAll.setEnabled(stopAllEnabled);
                allButton -= 1;
            } else {
                allButton -= 1;
            }
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String permissions[], int[] grantResults) {
        switch (requestCode) {
            case 1: {
                if (grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    Log.v("Permission", "Permission grannted, yay");
                    if (bound) {
                        coreService.googleConnect();
                    }
                } else {
                    Toast.makeText(this, "The app needs access to location in order to function.", Toast.LENGTH_SHORT).show();
                }
                return;
            }
        }
    }

    void setStartWifi() {
        startWifiEnabled = false;
        startWifi.setEnabled(startWifiEnabled);
        stopWifiEnabled = true;
        stopWifi.setEnabled(stopWifiEnabled);
    }

    public void onButtonClickWifi(View v) {
        if (bound) {
            coreService.startWifiService();
            setStartWifi();

            startAllEnabled = false;
            startAll.setEnabled(startAllEnabled);
            stopAllEnabled = true;
            stopAll.setEnabled(stopAllEnabled);
            allButton += 1;
        }
    }

    void setStopWifi() {
        startWifiEnabled = true;
        startWifi.setEnabled(startWifiEnabled);
        stopWifiEnabled = false;
        stopWifi.setEnabled(stopWifiEnabled);
    }

    public void onButtonClickWifiStop(View v) {
        if (bound) {
            coreService.stopWifiService();
            setStopWifi();

            if (allButton == 1) {
                startAllEnabled = true;
                startAll.setEnabled(startAllEnabled);
                stopAllEnabled = false;
                stopAll.setEnabled(stopAllEnabled);
                allButton -= 1;
            } else {
                allButton -= 1;
            }
        }
    }

    void setStartBluetooth() {
        startBluetoothEnabled = false;
        startBluetooth.setEnabled(startBluetoothEnabled);
        stopBluetoothEnabled = true;
        stopBluetooth.setEnabled(stopBluetoothEnabled);
    }

    public void onButtonClickBluetooth(View v) {
        if (bound) {
            coreService.startBluetoothService();
            setStartBluetooth();

            startAllEnabled = false;
            startAll.setEnabled(startAllEnabled);
            stopAllEnabled = true;
            stopAll.setEnabled(stopAllEnabled);
            allButton += 1;
        }
    }

    void setStopBluetooth() {
        startBluetoothEnabled = true;
        startBluetooth.setEnabled(startBluetoothEnabled);
        stopBluetoothEnabled = false;
        stopBluetooth.setEnabled(stopBluetoothEnabled);
    }

    public void onButtonClickBluetoothStop(View v) {
        if (bound) {
            coreService.stopBluetoothService();
            setStopBluetooth();

            if (allButton == 1) {
                startAllEnabled = true;
                startAll.setEnabled(startAllEnabled);
                stopAllEnabled = false;
                stopAll.setEnabled(stopAllEnabled);
                allButton -= 1;
            } else {
                allButton -= 1;
            }
        }
    }

    void setStartAll() {
        setStartBuffer();
        setStartAccelerometer();
        setStartBluetooth();
        setStartLocation();
        setStartWifi();

        startAllEnabled = false;
        startAll.setEnabled(startAllEnabled);
        stopAllEnabled = true;
        stopAll.setEnabled(stopAllEnabled);
    }

    public void onButtonClickAll(View v) {
        if (bound) {
            coreService.startDataBuffer();
            coreService.startAccelerometerService();
            coreService.startLocationService();
            coreService.startWifiService();
            coreService.startBluetoothService();

            setStartAll();

            allButton = 5;
        }
    }

    void setStopAll() {
        setStopBuffer();
        setStopAccelerometer();
        setStopBluetooth();
        setStopLocation();
        setStopWifi();

        startAllEnabled = true;
        startAll.setEnabled(startAllEnabled);
        stopAllEnabled = false;
        stopAll.setEnabled(stopAllEnabled);
    }

    public void onButtonClickAllStop(View v) {
        if (bound) {
            coreService.stopDataBuffer();
            coreService.stopAccelerometerService();
            coreService.stopLocationService();
            coreService.stopWifiService();
            coreService.stopBluetoothService();

            setStopAll();

            allButton = 0;
        }
    }

    void setStartBuffer() {
        startBufferEnabled = false;
        startBuffer.setEnabled(startBufferEnabled);
        stopBufferEnabled = true;
        stopBuffer.setEnabled(stopBufferEnabled);
    }

    public void onButtonClickDataBuffer(View v) {
        if (bound) {
            coreService.startDataBuffer();
            setStartBuffer();

            startAllEnabled = false;
            startAll.setEnabled(startAllEnabled);
            stopAllEnabled = true;
            stopAll.setEnabled(stopAllEnabled);
            allButton += 1;
        }
    }

    void setStopBuffer() {
        startBufferEnabled = true;
        startBuffer.setEnabled(startBufferEnabled);
        stopBufferEnabled = false;
        stopBuffer.setEnabled(stopBufferEnabled);
    }

    public void onButtonClickDataBufferStop(View v) {
        if (bound) {
            coreService.stopDataBuffer();
            setStopBuffer();

            if (allButton == 1) {
                startAllEnabled = true;
                startAll.setEnabled(startAllEnabled);
                stopAllEnabled = false;
                stopAll.setEnabled(stopAllEnabled);
                allButton -= 1;
            } else {
                allButton -= 1;
            }
        }
    }

    public void onButtonClickAddGeofence(View v) {
        if (bound) {
            coreService.addGeofences();
            if (!geofencAdded) {
                registerGeofenceEnabled = true;
                registerGeofence.setEnabled(registerGeofenceEnabled);
            }
        }
    }

    public void onButtonClickRegisterGeofence(View v) {
        if (bound) {
            coreService.registerGeofences();
            registerGeofenceEnabled = false;
            registerGeofence.setEnabled(registerGeofenceEnabled);
            unregisterGeofenceEnabled = true;
            unregisterGeofence.setEnabled(unregisterGeofenceEnabled);
        }
    }

    public void onButtonClickUnregisterGeofence(View v) {
        if (bound) {
            coreService.unregisterGeofences();
            registerGeofenceEnabled = true;
            registerGeofence.setEnabled(registerGeofenceEnabled);
            unregisterGeofenceEnabled = false;
            unregisterGeofence.setEnabled(unregisterGeofenceEnabled);
        }
    }

    public void onButtonClickExportDatastore(View v) {
/*        try {
            File data = Environment.getDataDirectory();

            try {
                String datastorePath = "//data//net.simonjensen.autounlock//databases//datastore.db";
                String exportPath = String.valueOf(System.currentTimeMillis()) + ".db";

                File outputDirectory = new File("/sdcard/AutoUnlock/");
                outputDirectory.mkdirs();

                File datastore = new File(data, datastorePath);
                File export = new File(outputDirectory, exportPath);

                FileChannel source = new FileInputStream(datastore).getChannel();
                FileChannel destination = new FileOutputStream(export).getChannel();

                destination.transferFrom(source, 0, source.size());
                source.close();
                destination.close();

                Log.v("Export Datastore", "Datastore exported to " + exportPath);
            } catch (Exception e) {
                e.printStackTrace();
            }
        } catch (Exception e) {
            // do something
        }*/
        if (bound) {
            List<String> foundLocks = new ArrayList<>();
            foundLocks.add(BluetoothService.MIBAND);
            Log.d(TAG, foundLocks.toString());
            //coreService.startHeuristicsDecision(foundLocks);
            //coreService.notifyDecision();
            NotificationUtils notificationUtils = new NotificationUtils();
            notificationUtils.displayNotification(this,
                    CoreService.recordedBluetooth,
                    CoreService.recordedWifi,
                    CoreService.recordedLocation);
        }
    }

    public void onButtonClickNewDB(View v) {
        Log.v("New Datastore", "Deleting data in datastore");
        if (bound) {
            //coreService.newDatastore();
            coreService.manualUnlock(BluetoothService.SIMON_BEKEY);
        }
    }

    /** Defines callbacks for service binding, passed to bindService() */
    private ServiceConnection serviceConnection = new ServiceConnection() {

        @Override
        public void onServiceConnected(ComponentName className,
                                       IBinder service) {
            // We've bound to LocalService, cast the IBinder and get LocalService instance
            CoreService.LocalBinder binder = (CoreService.LocalBinder) service;
            coreService = binder.getService();
            bound = true;
        }

        @Override
        public void onServiceDisconnected(ComponentName arg0) {
            bound = false;
        }
    };
}
