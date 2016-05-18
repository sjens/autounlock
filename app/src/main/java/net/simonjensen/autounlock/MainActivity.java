package net.simonjensen.autounlock;

import android.Manifest;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.Environment;
import android.os.IBinder;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.nio.channels.FileChannel;

public class MainActivity extends AppCompatActivity {
    UnlockService unlockService;
    boolean bound = false;
    static boolean geofencAdded = false;
    static int allButton = 0;

    DataStore dataStore;

    static Button startAccelerometer;
    static Button stopAccelerometer;
    static boolean startAccelerometerEnabled = true;
    static boolean stopAccelerometerEnabled = false;

    static Button startLocation;
    static Button stopLocation;
    static boolean startLocationEnabled = true;
    static boolean stopLocationEnabled = false;

    static Button startWifi;
    static Button stopWifi;
    static boolean startWifiEnabled = true;
    static boolean stopWifiEnabled = false;

    static Button startBluetooth;
    static Button stopBluetooth;
    static boolean startBluetoothEnabled = true;
    static boolean stopBluetoothEnabled = false;

    static Button startAll;
    static Button stopAll;
    static boolean startAllEnabled = true;
    static boolean stopAllEnabled = false;

    static Button startBuffer;
    static Button stopBuffer;
    static boolean startBufferEnabled = true;
    static boolean stopBufferEnabled = false;

    static Button registerGeofence;
    static Button unregisterGeofence;
    static boolean registerGeofenceEnabled = false;
    static boolean unregisterGeofenceEnabled = false;

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

        dataStore = new DataStore(this);
    }

    @Override
    protected void onStart() {
        super.onStart();
        // Bind to LocalService
        ComponentName unlockService = startService(new Intent(this, UnlockService.class));
        bindService(new Intent(this, UnlockService.class), serviceConnection, Context.BIND_AUTO_CREATE);

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

    void setStartAccelerometer() {
        startAccelerometerEnabled = false;
        startAccelerometer.setEnabled(startAccelerometerEnabled);
        stopAccelerometerEnabled = true;
        stopAccelerometer.setEnabled(stopAccelerometerEnabled);
    }

    /** Called when a button is clicked (the button in the layout file attaches to
     * this method with the android:onClick attribute) */
    public void onButtonClickAccel(View v) {
        if (bound) {
            unlockService.startAccelerometerService();
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
            unlockService.stopAccelerometerService();
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
            unlockService.startLoactionService();
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
            unlockService.stopLocationService();
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
            unlockService.startWifiService();
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
            unlockService.stopWifiService();
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
            unlockService.startBluetoothService();
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
            unlockService.stopBluetoothService();
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
            unlockService.startDataBufferCollection();
            unlockService.startAccelerometerService();
            unlockService.startLoactionService();
            unlockService.startWifiService();
            unlockService.startBluetoothService();

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
            unlockService.stopDataBufferCollection();
            unlockService.stopAccelerometerService();
            unlockService.stopLocationService();
            unlockService.stopWifiService();
            unlockService.stopBluetoothService();

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
            unlockService.startDataBufferCollection();
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
            unlockService.stopDataBufferCollection();
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
            unlockService.addGeofence();
            if (!geofencAdded) {
                registerGeofenceEnabled = true;
                registerGeofence.setEnabled(registerGeofenceEnabled);
            }
        }
    }

    public void onButtonClickRegisterGeofence(View v) {
        if (bound) {
            unlockService.registerGeofences();
            registerGeofenceEnabled = false;
            registerGeofence.setEnabled(registerGeofenceEnabled);
            unregisterGeofenceEnabled = true;
            unregisterGeofence.setEnabled(unregisterGeofenceEnabled);
        }
    }

    public void onButtonClickUnregisterGeofence(View v) {
        if (bound) {
            unlockService.unregisterGeofences();
            registerGeofenceEnabled = true;
            registerGeofence.setEnabled(registerGeofenceEnabled);
            unregisterGeofenceEnabled = false;
            unregisterGeofence.setEnabled(unregisterGeofenceEnabled);
        }
    }

    public void onButtonClickExportDatastore(View v) {
        try {
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
        }
    }

    public void onButtonClickNewDB(View v) {
        Log.v("New Datastore", "Deleting data in datastore");
        if (bound) {
            unlockService.newDatastore();
        }
    }

    /** Defines callbacks for service binding, passed to bindService() */
    private ServiceConnection serviceConnection = new ServiceConnection() {

        @Override
        public void onServiceConnected(ComponentName className,
                                       IBinder service) {
            // We've bound to LocalService, cast the IBinder and get LocalService instance
            UnlockService.LocalBinder binder = (UnlockService.LocalBinder) service;
            unlockService = binder.getService();
            bound = true;
        }

        @Override
        public void onServiceDisconnected(ComponentName arg0) {
            bound = false;
        }
    };
}
