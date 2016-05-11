package net.simonjensen.autounlock;

import android.app.Notification;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.os.*;
import android.os.Process;
import android.support.v7.app.NotificationCompat;
import android.util.Log;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.List;

public class UnlockService extends Service {
    private Looper serviceLooper;
    private ServiceHandler serviceHandler;
    UnlockLoop unlockLoop = new UnlockLoop();
    Thread dataCollect = new Thread(unlockLoop);

    static DataStore dataStore;

    static List<List<String>> foundBluetooth = new ArrayList<List<String>>();
    static List<List<String>> foundWifi = new ArrayList<List<String>>();
    static List<List<String>> foundLocation = new ArrayList<List<String>>();
    static List<List<String>> recordedAccelerometer = new ArrayList<List<String>>();

    public static DataBuffer<List> dataBuffer = new DataBuffer<List>(1000);

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
        dataCollect.start();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        //Toast.makeText(this, "service starting", Toast.LENGTH_SHORT).show();

        // For each start request, send a message to start a job and deliver the
        // start ID so we know which request we're stopping when we finish the job
        Message msg = serviceHandler.obtainMessage();
        msg.arg1 = startId;
        serviceHandler.sendMessage(msg);

        // If we get killed, after returning from here, restart
        return START_STICKY;
    }

    @Override
    public IBinder onBind(Intent intent) {
        // localBinder is used for bound services
        return localBinder;
    }

    public void startAccelService() {
        Intent accelIntent = new Intent(this, AccelerometerService.class);
        startService(accelIntent);
    }

    public void startLoactionService() {
        Intent locationIntent = new Intent(this, LocationService.class);
        startService(locationIntent);
    }

    public void startWifiService() {
        Intent wifiIntent = new Intent(this, WifiService.class);
        startService(wifiIntent);
    }

    public void startBluetoothService() {
        Intent bluetoothIntent = new Intent(this, BluetoothService.class);
        startService(bluetoothIntent);
    }

    public void startDecision() {
        Toast.makeText(this, "BeKey found", Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onDestroy() {
        Log.v("UnlockService", "Service destroyed");
        //dataStore.close();
        //Toast.makeText(this, "service done", Toast.LENGTH_SHORT).show();
    }
}
