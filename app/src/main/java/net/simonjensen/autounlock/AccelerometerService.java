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
    private Sensor gravity;
    private Sensor magneticField;
    private Sensor linearAcceleration;

    PowerManager powerManager;
    PowerManager.WakeLock wakeLock;

    private float[] previousGravity = new float[3];
    private float[] previousMagneticField = new float[3];
    private float[] previousLinearAcceleration = new float[4];
    private boolean previousGravitySet = false;
    private boolean previousMagneticFieldSet = false;
    private boolean previousLinearAccelerationSet = false;

    private float[] rotationMatrixInverted = new float[16];
    private float[] rotationMatrix = new float[16];
    private float[] speed =new float[3];

    int i = 0;

    @Override
    public void onSensorChanged(SensorEvent event) {
        if (event.sensor == gravity) {
            System.arraycopy(event.values, 0, previousGravity, 0, event.values.length);
            previousGravitySet = true;
        } else if (event.sensor == magneticField) {
            System.arraycopy(event.values, 0, previousMagneticField, 0, event.values.length);
            previousMagneticFieldSet = true;
        } else if (event.sensor == linearAcceleration) {
            System.arraycopy(event.values, 0, previousLinearAcceleration, 0, event.values.length);
            previousLinearAccelerationSet = true;
        }

        // Only process data if all needed data is available.
        if (previousGravitySet && previousMagneticFieldSet && previousLinearAccelerationSet) {
            rotateCoordinates();
        }
    }

    private void rotateCoordinates() {
        // Get the rotation matrix based on gravity and magnetic field.
        SensorManager.getRotationMatrix(rotationMatrix, null, previousGravity, previousMagneticField);

        // Invert the rotation matrix.
        android.opengl.Matrix.invertM(rotationMatrixInverted, 0, rotationMatrix, 0);

        // Multiply the linear acceleration onto the inverted rotation matrix to get linear acceleration in
        // earth coordinates.
        android.opengl.Matrix.multiplyMV(previousLinearAcceleration, 0, rotationMatrixInverted, 0, previousLinearAcceleration, 0);

        //Attempt to filter noise when there is no movement.
        for (int i = 0; i < previousLinearAcceleration.length; i++) {
            if (previousLinearAcceleration[i] < 0.5 && previousLinearAcceleration[i] > -0.5) {
                previousLinearAcceleration[i] = 0f;
            }
        }

        processSensorData();
    }

    private void processSensorData() {
        long time = System.currentTimeMillis();

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
        Log.v(TAG, "EAST/WEST: " + previousLinearAcceleration[0] + " NORTH/SOUTH: " + previousLinearAcceleration[1] + " UP/DOWN: " + previousLinearAcceleration[2]);
        Log.v(TAG, "SEW: " + speed[0] + " SNS: " + speed[1] + " SUD: " + speed[2]);

        CoreService.recordedAccelerometer.add(anAccelerometerEvent);
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
        gravity = sensorManager.getDefaultSensor(Sensor.TYPE_GRAVITY);
        magneticField = sensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD);
        linearAcceleration = sensorManager.getDefaultSensor(Sensor.TYPE_LINEAR_ACCELERATION);
        sensorManager.registerListener(this, gravity, SensorManager.SENSOR_DELAY_NORMAL);
        sensorManager.registerListener(this, magneticField, SensorManager.SENSOR_DELAY_NORMAL);
        sensorManager.registerListener(this, linearAcceleration, SensorManager.SENSOR_DELAY_NORMAL);
    }
    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        // The service is starting, due to adapter call to startService()
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
        sensorManager.unregisterListener(this, gravity);
        sensorManager.unregisterListener(this, magneticField);
        sensorManager.unregisterListener(this, linearAcceleration);
        wakeLock.release();
    }
}