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

    Button startAccelerometer;
    Button stopAccelerometer;

    Button startLocation;
    Button stopLocation;

    Button startWifi;
    Button stopWifi;

    Button startBluetooth;
    Button stopBluetooth;

    Button startAll;
    Button stopAll;

    Button startBuffer;
    Button stopBuffer;

    Button registerGeofence;
    Button unregisterGeofence;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        startAccelerometer = (Button) findViewById(R.id.accstart);
        stopAccelerometer = (Button) findViewById(R.id.accstop);
        stopAccelerometer.setEnabled(false);

        startLocation = (Button) findViewById(R.id.locationstart);
        stopLocation = (Button) findViewById(R.id.locationstop);
        stopLocation.setEnabled(false);

        startWifi = (Button) findViewById(R.id.wifistart);
        stopWifi = (Button) findViewById(R.id.wifistop);
        stopWifi.setEnabled(false);

        startBluetooth = (Button) findViewById(R.id.bluetoothstart);
        stopBluetooth = (Button) findViewById(R.id.bluetoothstop);
        stopBluetooth.setEnabled(false);

        startAll = (Button) findViewById(R.id.allstart);
        stopAll = (Button) findViewById(R.id.allstop);
        stopAll.setEnabled(false);

        startBuffer = (Button) findViewById(R.id.databufferstart);
        stopBuffer = (Button) findViewById(R.id.databufferstop);
        stopBuffer.setEnabled(false);

        registerGeofence = (Button) findViewById(R.id.registergeofence);
        unregisterGeofence = (Button) findViewById(R.id.unregistergeofence);
        registerGeofence.setEnabled(false);
        unregisterGeofence.setEnabled(false);

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

    /** Called when a button is clicked (the button in the layout file attaches to
     * this method with the android:onClick attribute) */
    public void onButtonClickAccel(View v) {
        if (bound) {
            unlockService.startAccelerometerService();
            startAccelerometer.setEnabled(false);
            stopAccelerometer.setEnabled(true);
            startAll.setEnabled(false);
            stopAll.setEnabled(true);
            allButton += 1;
        }
    }

    public void onButtonClickAccelStop(View v) {
        if (bound) {
            unlockService.stopAccelerometerService();
            startAccelerometer.setEnabled(true);
            stopAccelerometer.setEnabled(false);
            if (allButton == 1) {
                startAll.setEnabled(true);
                stopAll.setEnabled(false);
                allButton -= 1;
            } else {
                allButton -= 1;
            }
        }
    }

    public void onButtonClickLocation(View v) {
        if (bound) {
            unlockService.startLoactionService();
            startLocation.setEnabled(false);
            stopLocation.setEnabled(true);
            startAll.setEnabled(false);
            stopAll.setEnabled(true);
            allButton += 1;
        }
    }

    public void onButtonClickLocationStop(View v) {
        if (bound) {
            unlockService.stopLocationService();
            startLocation.setEnabled(true);
            stopLocation.setEnabled(false);
            if (allButton == 1) {
                startAll.setEnabled(true);
                stopAll.setEnabled(false);
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
                    Log.v("Permission", "Location permission grannted, yay");

                } else {
                    Toast.makeText(this, "The app needs access to location in order to function.", Toast.LENGTH_SHORT).show();
                }
                return;
            }
        }
    }

    public void onButtonClickWifi(View v) {
        if (bound) {
            unlockService.startWifiService();
            startWifi.setEnabled(false);
            stopWifi.setEnabled(true);
            startAll.setEnabled(false);
            stopAll.setEnabled(true);
            allButton += 1;
        }
    }

    public void onButtonClickWifiStop(View v) {
        if (bound) {
            unlockService.stopWifiService();
            startWifi.setEnabled(true);
            stopWifi.setEnabled(false);
            if (allButton == 1) {
                startAll.setEnabled(true);
                stopAll.setEnabled(false);
                allButton -= 1;
            } else {
                allButton -= 1;
            }
        }
    }

    public void onButtonClickBluetooth(View v) {
        if (bound) {
            unlockService.startBluetoothService();
            startBluetooth.setEnabled(false);
            stopBluetooth.setEnabled(true);
            startAll.setEnabled(false);
            stopAll.setEnabled(true);
            allButton += 1;
        }
    }

    public void onButtonClickBluetoothStop(View v) {
        if (bound) {
            unlockService.stopBluetoothService();
            startBluetooth.setEnabled(true);
            stopBluetooth.setEnabled(false);
            if (allButton == 1) {
                startAll.setEnabled(true);
                stopAll.setEnabled(false);
                allButton -= 1;
            } else {
                allButton -= 1;
            }
        }
    }


    public void onButtonClickAll(View v) {
        if (bound) {
            unlockService.startDataBufferCollection();
            unlockService.startAccelerometerService();
            unlockService.startLoactionService();
            unlockService.startWifiService();
            unlockService.startBluetoothService();
            startBuffer.setEnabled(false);
            stopBuffer.setEnabled(true);
            startAccelerometer.setEnabled(false);
            stopAccelerometer.setEnabled(true);
            startLocation.setEnabled(false);
            stopLocation.setEnabled(true);
            startWifi.setEnabled(false);
            stopWifi.setEnabled(true);
            startBluetooth.setEnabled(false);
            stopBluetooth.setEnabled(true);
            startAll.setEnabled(false);
            stopAll.setEnabled(true);
            allButton = 5;
        }
    }

    public void onButtonClickAllStop(View v) {
        if (bound) {
            unlockService.stopDataBufferCollection();
            unlockService.stopAccelerometerService();
            unlockService.stopLocationService();
            unlockService.stopWifiService();
            unlockService.stopBluetoothService();
            startBuffer.setEnabled(true);
            stopBuffer.setEnabled(false);
            startAccelerometer.setEnabled(true);
            stopAccelerometer.setEnabled(false);
            startLocation.setEnabled(true);
            stopLocation.setEnabled(false);
            startWifi.setEnabled(true);
            stopWifi.setEnabled(false);
            startBluetooth.setEnabled(true);
            stopBluetooth.setEnabled(false);
            startAll.setEnabled(true);
            stopAll.setEnabled(false);
            allButton = 0;
        }
    }

    public void onButtonClickDataBuffer(View v) {
        if (bound) {
            unlockService.startDataBufferCollection();
            startBuffer.setEnabled(false);
            stopBuffer.setEnabled(true);
            startAll.setEnabled(false);
            stopAll.setEnabled(true);
            allButton += 1;
        }
    }

    public void onButtonClickDataBufferStop(View v) {
        if (bound) {
            unlockService.stopDataBufferCollection();
            startBuffer.setEnabled(true);
            stopBuffer.setEnabled(false);
            if (allButton == 1) {
                startAll.setEnabled(true);
                stopAll.setEnabled(false);
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
                registerGeofence.setEnabled(true);
            }
        }
    }

    public void onButtonClickRegisterGeofence(View v) {
        if (bound) {
            unlockService.registerGeofences();
            registerGeofence.setEnabled(false);
            unregisterGeofence.setEnabled(true);
        }
    }

    public void onButtonClickUnregisterGeofence(View v) {
        if (bound) {
            unlockService.unregisterGeofences();
            registerGeofence.setEnabled(true);
            unregisterGeofence.setEnabled(false);
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
