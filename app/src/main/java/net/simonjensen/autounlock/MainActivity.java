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
import android.widget.Toast;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.nio.channels.FileChannel;

public class MainActivity extends AppCompatActivity {
    UnlockService unlockService;
    boolean bound = false;

    DataStore dataStore;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

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
        }
    }

    public void onButtonClickAccelStop(View v) {
        if (bound) {
            unlockService.stopAccelerometerService();
        }
    }

    public void onButtonClickLocation(View v) {
        if (bound) {
            unlockService.startLoactionService();
        }
    }

    public void onButtonClickLocationStop(View v) {
        if (bound) {
            unlockService.stopLocationService();
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
        }
    }

    public void onButtonClickWifiStop(View v) {
        if (bound) {
            unlockService.stopWifiService();
        }
    }

    public void onButtonClickBluetooth(View v) {
        if (bound) {
            unlockService.startBluetoothService();
        }
    }

    public void onButtonClickBluetoothStop(View v) {
        if (bound) {
            unlockService.stopBluetoothService();
        }
    }


    public void onButtonClickAll(View v) {
        if (bound) {
            unlockService.startDataBufferCollection();
            unlockService.startAccelerometerService();
            unlockService.startLoactionService();
            unlockService.startWifiService();
            unlockService.startBluetoothService();
        }
    }

    public void onButtonClickAllStop(View v) {
        if (bound) {
            unlockService.stopDataBufferCollection();
            unlockService.stopAccelerometerService();
            unlockService.stopLocationService();
            unlockService.stopWifiService();
            unlockService.stopBluetoothService();
        }
    }

    public void onButtonClickDataBuffer(View v) {
        if (bound) {
            unlockService.startDataBufferCollection();
        }
    }

    public void onButtonClickDataBufferStop(View v) {
        if (bound) {
            unlockService.stopDataBufferCollection();
        }
    }

    public void onButtonClickAddGeofence(View v) {
        if (bound) {
            unlockService.addGeofence();
        }
    }

    public void onButtonClickRegisterGeofence(View v) {
        if (bound) {
            unlockService.registerGeofences();
        }
    }

    public void onButtonClickUnregisterGeofence(View v) {
        if (bound) {
            unlockService.unregisterGeofences();
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
