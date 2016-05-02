package net.simonjensen.autounlock;

import android.app.Service;
import android.content.Intent;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.IBinder;
import android.util.Log;

import java.util.ArrayList;
import java.util.List;

public class AccelerometerService extends Service implements SensorEventListener {
    int mStartMode;       // indicates how to behave if the service is killed
    IBinder mBinder;      // interface for clients that bind
    boolean mAllowRebind; // indicates whether onRebind should be used

    private Sensor accelerometer;
    private Sensor magnetometer;

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

            UnlockService.dataStore.insertAccelerometer(accelerometerX, accelerometerY, accelerometerZ, rotationX, rotationY, rotationZ, time);
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {

    }

    @Override
    public void onCreate() {
        // The service is being created
        SensorManager sensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);
        accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        magnetometer = sensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD);
        sensorManager.registerListener(this, accelerometer, SensorManager.SENSOR_DELAY_NORMAL);
        sensorManager.registerListener(this, magnetometer, SensorManager.SENSOR_DELAY_NORMAL);
    }
    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        // The service is starting, due to a call to startService()
        return mStartMode;
    }
    @Override
    public IBinder onBind(Intent intent) {
        // A client is binding to the service with bindService()
        return mBinder;
    }
    @Override
    public boolean onUnbind(Intent intent) {
        // All clients have unbound with unbindService()
        return mAllowRebind;
    }
    @Override
    public void onRebind(Intent intent) {
        // A client is binding to the service with bindService(),
        // after onUnbind() has already been called
    }
    @Override
    public void onDestroy() {
        // The service is no longer used and is being destroyed
    }
}