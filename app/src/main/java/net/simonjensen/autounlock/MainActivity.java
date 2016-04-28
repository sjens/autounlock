package net.simonjensen.autounlock;

import android.Manifest;
import android.app.Activity;
import android.content.*;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.IBinder;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

public class MainActivity extends Activity {
    UnlockService unlockService;
    boolean bound = false;

    DataStore dataStore;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        dataStore = new DataStore(this);
    }

    @Override
    protected void onStart() {
        super.onStart();
        // Bind to LocalService
        ComponentName unlockService = startService(new Intent(this, UnlockService.class));
        bindService(new Intent(this, UnlockService.class), serviceConnection, Context.BIND_AUTO_CREATE);
        //Intent unlockIntent = new Intent(this, UnlockService.class);
        //startService(unlockIntent);
        //bindService(unlockIntent, serviceConnection, Context.BIND_AUTO_CREATE);


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
            // Call a method from the LocalService.
            // However, if this call were something that might hang, then this request should
            // occur in a separate thread to avoid slowing down the activity performance.
            //int num = unlockService.getRandomNumber();
            //Toast.makeText(this, "number: " + num, Toast.LENGTH_SHORT).show();
            unlockService.startAccelService();
            Toast.makeText(this, "AccelerometerService started", Toast.LENGTH_SHORT).show();
        }
    }

    public void onButtonClickNetwork(View v) {
        // Here, thisActivity is the current activity
        if (ContextCompat.checkSelfPermission(this,
                Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {

            // Should we show an explanation?
            if (ActivityCompat.shouldShowRequestPermissionRationale(this,
                    Manifest.permission.ACCESS_FINE_LOCATION)) {

                // Show an expanation to the user *asynchronously* -- don't block
                // this thread waiting for the user's response! After the user
                // sees the explanation, try again to request the permission.

            } else {

                // No explanation needed, we can request the permission.

                ActivityCompat.requestPermissions(this,
                        new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                        1);

                // MY_PERMISSIONS_REQUEST_READ_CONTACTS is an
                // app-defined int constant. The callback method gets the
                // result of the request.
            }
        } else {
            unlockService.startLoactionService();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String permissions[], int[] grantResults) {
        switch (requestCode) {
            case 1: {
                // If request is cancelled, the result arrays are empty.
                if (grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED) {

                    Log.v("yay", "yay");
                    //unlockService.startLoactionService();
                    // permission was granted, yay! Do the
                    // contacts-related task you need to do.

                } else {
                    Toast.makeText(this, "The app needs access to location in order to function.", Toast.LENGTH_SHORT).show();

                    // permission denied, boo! Disable the
                    // functionality that depends on this permission.
                }
                return;
            }

            // other 'case' lines to check for other
            // permissions this app might request
        }
    }

    public void onButtonClickWifi(View v) {
        if (bound) {
            unlockService.startWifiService();
            Toast.makeText(this, "WifiService started", Toast.LENGTH_SHORT).show();
        }
    }

    public void onButtonClickBluetooth(View v) {
        if (bound) {
            unlockService.startBluetoothService();
            Toast.makeText(this, "BluetoothService started", Toast.LENGTH_SHORT).show();
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
