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

public class AccelerometerService extends Service implements SensorEventListener {
    static String TAG = "AccelerometerService";

    int startMode;       // indicates how to behave if the service is killed
    IBinder binder;      // interface for clients that bind
    boolean allowRebind; // indicates whether onRebind should be used

    SensorManager sensorManager;
    private Sensor rotationVector;
    private Sensor linearAcceleration;

    PowerManager powerManager;
    PowerManager.WakeLock wakeLock;

    private float[] previousRotationVector = new float[5];
    private float[] previousLinearAcceleration = new float[3];
    private boolean previousRotationVectorSet = false;
    private boolean previousLinearAccelerationSet = false;
    private float[] rotationMatrix = new float[9];
    private float[] result = new float[9];
    private float[] speed =new float[3];

    int i = 0;

    @Override
    public void onSensorChanged(SensorEvent event) {
        if (event.sensor == rotationVector) {
            System.arraycopy(event.values, 0, previousRotationVector, 0, event.values.length);
            previousRotationVectorSet = true;
        } else if (event.sensor == linearAcceleration) {
            System.arraycopy(event.values, 0, previousLinearAcceleration, 0, event.values.length);
            previousLinearAccelerationSet = true;
        }

        // Only process data if all needed data is available.
        if (previousRotationVectorSet && previousLinearAccelerationSet) {
            processSensorData();
        }
    }

    public void calculateAngles(float[] result, float[] rotationVector){
        //caculate rotationMatrix matrix from rotationMatrix vector first
        SensorManager.getRotationMatrixFromVector(rotationMatrix, rotationVector);

        //calculate Euler angles now
        SensorManager.getOrientation(rotationMatrix, result);

        //The results are in radians, need to convert it to degrees
        convertToDegrees(result);
    }

    private void convertToDegrees(float[] vector){
        for (int i = 0; i < vector.length; i++){
            vector[i] = Math.round(Math.toDegrees(vector[i]));
        }
    }

    private void processSensorData() {
        //SensorManager.getRotationMatrix(rotationMatrix, null, previousLinearAcceleration, previousMagnetometer);

        calculateAngles(result, previousLinearAcceleration);

        //Log.v(TAG, result[0] + " " + result[1] + " " + result[2]);

        long time = System.currentTimeMillis();

        float movementVector[] = new float[3];

        movementVector[0] = rotationMatrix[0] * previousLinearAcceleration[0]
                + rotationMatrix[1] * previousLinearAcceleration[1]
                + rotationMatrix[2] * previousLinearAcceleration[2];

        movementVector[1] = rotationMatrix[3] * previousLinearAcceleration[0]
                + rotationMatrix[4] * previousLinearAcceleration[1]
                + rotationMatrix[5] * previousLinearAcceleration[2];

        movementVector[2] = rotationMatrix[6] * previousLinearAcceleration[0]
                + rotationMatrix[7] * previousLinearAcceleration[1]
                + rotationMatrix[8] * previousLinearAcceleration[2];

/*        for (int i = 0; i < movementVector.length; i++) {
            // High-pass filter in attempt to remove noise from sensors.
            if (movementVector[i] < 0.15) {
                movementVector[i] = (float) 0;
            }
        }*/

        float accelerationX = previousLinearAcceleration[0];
        float accelerationY = previousLinearAcceleration[1];
        float accelerationZ = previousLinearAcceleration[2];
        speed[0] = speed[0] + previousLinearAcceleration[0];
        speed[1] = speed[1] + previousLinearAcceleration[1];
        speed[2] = speed[2] + previousLinearAcceleration[2];

        AccelerometerData anAccelerometerEvent = new AccelerometerData(
                accelerationX, accelerationY, accelerationZ,
                speed[0], speed[1], speed[2], time);

        Log.e(TAG, "new observation");
        Log.v(TAG, "LEFT/RIGHT: " + previousLinearAcceleration[0] + " UP/DOWN: " + previousLinearAcceleration[1] + " FORWARD/BACK: " + previousLinearAcceleration[2]);
        Log.v(TAG, "SLR: " + speed[0] + " SUD: " + speed[1] + " SFB: " + speed[2]);

        UnlockService.recordedAccelerometer.add(anAccelerometerEvent);

        UnlockService.dataStore.insertAccelerometer(
                accelerationX, accelerationY, accelerationZ,
                speed[0], speed[1], speed[2], time);
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
        rotationVector = sensorManager.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR);
        linearAcceleration = sensorManager.getDefaultSensor(Sensor.TYPE_LINEAR_ACCELERATION);
        sensorManager.registerListener(this, rotationVector, SensorManager.SENSOR_DELAY_NORMAL);
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
        sensorManager.unregisterListener(this, rotationVector);
        sensorManager.unregisterListener(this, linearAcceleration);
        wakeLock.release();
    }
}