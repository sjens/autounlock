package net.simonjensen.autounlock;

import android.app.Service;
import android.content.Intent;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.*;
import android.os.Process;
import android.util.Log;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.List;

public class AccelerometerService extends Service implements SensorEventListener {
    private Looper serviceLooper;
    private ServiceHandler serviceHandler;
    private Sensor accelerometer;
    private Sensor magnetometer;

    DataStore dataStore;

    private float[] previousAccelerometer = new float[3];
    private float[] previousMagnetometer = new float[3];
    private boolean previousAccelerometerSet = false;
    private boolean previousMagnetometerSet = false;
    private float[] rotation = new float[9];
    private float[] orientation = new float[3];
    private float currentDegree = 0f;

    @Override
    public void onSensorChanged(SensorEvent event) {
        if (event.sensor == accelerometer) {
            System.arraycopy(event.values, 0, previousAccelerometer, 0, event.values.length);
            previousAccelerometerSet = true;
        } else if (event.sensor == magnetometer) {
            System.arraycopy(event.values, 0, previousMagnetometer, 0, event.values.length);
            previousMagnetometerSet = true;
        }
        if (previousAccelerometerSet && previousMagnetometerSet) {
            SensorManager.getRotationMatrix(rotation, null, previousAccelerometer, previousMagnetometer);
            SensorManager.getOrientation(rotation, orientation);
            float azimuthInRadians = orientation[0];
            float azimuthInDegrees = (float)(Math.toDegrees(azimuthInRadians)+360)%360;

            long time = System.currentTimeMillis();

            currentDegree = -azimuthInDegrees;
            Log.v("ON SENSOR CHANGED: ", String.valueOf((float)(Math.toDegrees(orientation[0])+360)%360) + " "
                    + String.valueOf((float)(Math.toDegrees(orientation[1])+360)%360) + " "
                    + String.valueOf((float)(Math.toDegrees(orientation[2])+360)%360));

            String accelerometerX = String.valueOf(previousAccelerometer[0]);
            String accelerometerY = String.valueOf(previousAccelerometer[1]);
            String accelerometerZ = String.valueOf(previousAccelerometer[2]);
            String rotationX = String.valueOf((float)(Math.toDegrees(orientation[0])+360)%360);
            String rotationY = String.valueOf((float)(Math.toDegrees(orientation[1])+360)%360);
            String rotationZ = String.valueOf((float)(Math.toDegrees(orientation[2])+360)%360);

            List<String> anAccelerometerEvent = new ArrayList<String>();
            anAccelerometerEvent.add(accelerometerX);
            anAccelerometerEvent.add(accelerometerY);
            anAccelerometerEvent.add(accelerometerZ);
            anAccelerometerEvent.add(rotationX);
            anAccelerometerEvent.add(rotationY);
            anAccelerometerEvent.add(rotationZ);
            UnlockService.recordedAccelerometer.add(anAccelerometerEvent);

            dataStore = new DataStore(this);
            dataStore.insertAccelerometer(accelerometerX, accelerometerY, accelerometerZ, rotationX, rotationY, rotationZ, time);
            dataStore.close();
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {

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
            stopSelf(msg.arg1);
        }
    }

    @Override
    public void onCreate() {
        //Toast.makeText(this, "accservice onCreate", Toast.LENGTH_SHORT).show();
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

        SensorManager sensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);
        accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        magnetometer = sensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD);
        sensorManager.registerListener(this, accelerometer, SensorManager.SENSOR_DELAY_NORMAL);
        sensorManager.registerListener(this, magnetometer, SensorManager.SENSOR_DELAY_NORMAL);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        //Toast.makeText(this, "accservice starting", Toast.LENGTH_SHORT).show();

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
        // We don't provide binding, so return null
        return null;
    }

    @Override
    public void onDestroy() {
        dataStore.close();
        //Toast.makeText(this, "accservice done", Toast.LENGTH_SHORT).show();
    }
}
