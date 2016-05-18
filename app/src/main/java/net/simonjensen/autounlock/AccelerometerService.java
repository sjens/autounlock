package net.simonjensen.autounlock;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.IBinder;
import android.os.PowerManager;
import android.util.Log;

import java.util.ArrayList;
import java.util.List;

public class AccelerometerService extends Service implements SensorEventListener {
    static String TAG = "AccelerometerService";

    int startMode;       // indicates how to behave if the service is killed
    IBinder binder;      // interface for clients that bind
    boolean allowRebind; // indicates whether onRebind should be used

    SensorManager sensorManager;
    private Sensor accelerometer;
    private Sensor magnetometer;
    private Sensor linearAcceleration;

    PowerManager powerManager;
    PowerManager.WakeLock wakeLock;

    private float[] previousAccelerometer = new float[3];
    private float[] previousMagnetometer = new float[3];
    private float[] previousLinearAcceleration = new float[3];
    private boolean previousAccelerometerSet = false;
    private boolean previousMagnetometerSet = false;
    private boolean previousLinearAccelerationSet = false;
    private float[] rotation = new float[9];
    private float[] orientation = new float[3];
    private float currentDegree = 0f;

    int i = 0;

    @Override
    public void onSensorChanged(SensorEvent event) {
        if (event.sensor == accelerometer) {
            System.arraycopy(event.values, 0, previousAccelerometer, 0, event.values.length);
            previousAccelerometerSet = true;
        } else if (event.sensor == magnetometer) {
            System.arraycopy(event.values, 0, previousMagnetometer, 0, event.values.length);
            previousMagnetometerSet = true;
        } else if (event.sensor == linearAcceleration) {
            System.arraycopy(event.values, 0, previousLinearAcceleration, 0, event.values.length);
            previousLinearAccelerationSet = true;
        }
        if (previousAccelerometerSet && previousMagnetometerSet && previousLinearAccelerationSet) {
            SensorManager.getRotationMatrix(rotation, null, previousAccelerometer, previousMagnetometer);
            SensorManager.getOrientation(rotation, orientation);
            float azimuthInRadians = orientation[0];
            float azimuthInDegrees = (float)(Math.toDegrees(azimuthInRadians)+360)%360;

            long time = System.currentTimeMillis();

            currentDegree = -azimuthInDegrees;

            float movementVector[] = new float[3];

            movementVector[0] = rotation[0] * previousLinearAcceleration[0]
                    + rotation[1] * previousLinearAcceleration[1]
                    + rotation[2] * previousLinearAcceleration[2];

            movementVector[1] = rotation[3] * previousLinearAcceleration[0]
                    + rotation[4] * previousLinearAcceleration[1]
                    + rotation[5] * previousLinearAcceleration[2];

            movementVector[2] = rotation[6] * previousLinearAcceleration[0]
                    + rotation[7] * previousLinearAcceleration[1]
                    + rotation[8] * previousLinearAcceleration[2];

            for (int i = 0; i < movementVector.length; i++) {
                if (movementVector[i] < 0.1) {
                    movementVector[i] = (float) 0;
                } else if (previousLinearAcceleration[i] < 0) {
                    movementVector[i] = movementVector[i] * (float) -1;
                }
            }

            if (i >= 30) {
                //Log.v("ON SENSOR CHANGED: ", String.valueOf((float)(Math.toDegrees(orientation[0])+360)%360) + " "
                //        + String.valueOf((float)(Math.toDegrees(orientation[1])+360)%360) + " "
                //        + String.valueOf((float)(Math.toDegrees(orientation[2])+360)%360));
                i = 0;
            }

            //Log.v(TAG, "NEW");
            //Log.v(TAG, "accelerometer" + previousAccelerometer[0] + " " + previousAccelerometer[1] + " " + previousAccelerometer[2]);
            //Log.v(TAG, rotation[0] + " " + rotation[1] + " " + rotation[2]);
            //Log.v(TAG, rotation[3] + " " + rotation[4] + " " + rotation[5]);
            //Log.v(TAG, rotation[6] + " " + rotation[7] + " " + rotation[8]);
            Log.v(TAG, "resultX " + movementVector[0] + " resultY " + movementVector[1] + " resultZ " + movementVector[2]);
            //Log.v(TAG, "linearAccelerationX " + previousLinearAcceleration[0] + " linearAccelerationY " + previousLinearAcceleration[1] + " linearAccelerationZ " + previousLinearAcceleration[2]);

            float accelerometerX = previousAccelerometer[0];
            float accelerometerY = previousAccelerometer[1];
            float accelerometerZ = previousAccelerometer[2];
            float rotationX = (float)(Math.toDegrees(orientation[0])+360)%360;
            float rotationY = (float)(Math.toDegrees(orientation[1])+360)%360;
            float rotationZ = (float)(Math.toDegrees(orientation[2])+360)%360;

            AccelerometerData anAccelerometerEvent = new AccelerometerData(
                    accelerometerX, accelerometerY, accelerometerZ,
                    rotationX, rotationY, rotationZ, time);

            UnlockService.recordedAccelerometer.add(anAccelerometerEvent);

            UnlockService.dataStore.insertAccelerometer(
                    accelerometerX, accelerometerY, accelerometerZ,
                    rotationX, rotationY, rotationZ, time);
            i++;
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {
        //We do not take accuracy into account
    }

    @Override
    public void onCreate() {
        // The service is being created
        powerManager = (PowerManager) getApplicationContext().getSystemService(Context.POWER_SERVICE);
        wakeLock = powerManager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "AccelerometerService");
        wakeLock.acquire();

        sensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);
        accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        magnetometer = sensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD);
        linearAcceleration = sensorManager.getDefaultSensor(Sensor.TYPE_LINEAR_ACCELERATION);
        sensorManager.registerListener(this, accelerometer, SensorManager.SENSOR_DELAY_NORMAL);
        sensorManager.registerListener(this, magnetometer, SensorManager.SENSOR_DELAY_NORMAL);
        sensorManager.registerListener(this, linearAcceleration, SensorManager.SENSOR_DELAY_NORMAL);
    }
    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        // The service is starting, due to a call to startService()
        return startMode;
    }
    @Override
    public IBinder onBind(Intent intent) {
        // A client is binding to the service with bindService()
        return binder;
    }
    @Override
    public boolean onUnbind(Intent intent) {
        // All clients have unbound with unbindService()
        return allowRebind;
    }
    @Override
    public void onRebind(Intent intent) {
        // A client is binding to the service with bindService(),
        // after onUnbind() has already been called
    }
    @Override
    public void onDestroy() {
        // The service is no longer used and is being destroyed
        Log.v("AccelerometerService", "Stopping");
        sensorManager.unregisterListener(this, accelerometer);
        sensorManager.unregisterListener(this, magnetometer);
        sensorManager.unregisterListener(this, linearAcceleration);
        wakeLock.release();
    }
}