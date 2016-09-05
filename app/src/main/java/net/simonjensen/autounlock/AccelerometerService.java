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
    private static final boolean ADAPTIVE_ACCEL_FILTER = true;

    int startMode;       // indicates how to behave if the service is killed
    IBinder binder;      // interface for clients that bind
    boolean allowRebind; // indicates whether onRebind should be used

    private SensorManager sensorManager;
    private Sensor gravitySensor;
    private Sensor magneticFieldSensor;
    private Sensor linearAccelerationSensor;
    private Sensor rotationVectorSensor;

    private float[] gravity = new float[3];
    private float[] magneticField = new float[3];
    private float[] linearAcceleration = new float[3];
    private float[] previousAcceleration = new float[3];
    private float[] accelerationFilter = new float[3];
    private float[] rotationVector = new float[5];

    PowerManager powerManager;
    PowerManager.WakeLock wakeLock;

    @Override
    public void onSensorChanged(SensorEvent event) {
        if (event.sensor == gravitySensor) {
            System.arraycopy(event.values, 0, gravity, 0, event.values.length);
        } else if (event.sensor == magneticFieldSensor) {
            System.arraycopy(event.values, 0, magneticField, 0, event.values.length);
            processMagneticField(magneticField, gravity);
        } else if (event.sensor == linearAccelerationSensor) {
            System.arraycopy(event.values, 0, linearAcceleration, 0, event.values.length);
            processAccelerometer(linearAcceleration);
            accelerometerFilter(linearAcceleration[0], linearAcceleration[1], linearAcceleration[2]);
        } else if (event.sensor == rotationVectorSensor) {
            System.arraycopy(event.values, 0, rotationVector, 0, event.values.length);
            Log.i(TAG, "onSensorChanged: " + rotationVector[0] + " " + rotationVector[1]);
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {
        //We do not take accuracy into account
    }

    // https://stackoverflow.com/questions/1638864/filtering-accelerometer-data-noise
    // && https://developer.apple.com/library/ios/samplecode/AccelerometerGraph/Listings/AccelerometerGraph_AccelerometerFilter_m.html
    private void accelerometerFilter(float accelerometerX, float accelerometerY, float accelerometerZ) {
        // high pass filter
        float updateFreq = 30; // match this to your update speed
        float cutOffFreq = 0.9f;
        float RC = 1.0f / cutOffFreq;
        float dt = 1.0f / updateFreq;
        float filterConstant = RC / (dt + RC);
        float alpha = filterConstant;
        float kAccelerometerMinStep = 0.033f;
        float kAccelerometerNoiseAttenuation = 3.0f;

        if(ADAPTIVE_ACCEL_FILTER)
        {

            float d = (float) clamp(Math.abs(norm(accelerationFilter[0], accelerationFilter[1], accelerationFilter[2]) - norm(accelerometerX, accelerometerY, accelerometerZ)) / kAccelerometerMinStep - 1.0f, 0.0f, 1.0f);
            alpha = d * filterConstant / kAccelerometerNoiseAttenuation + (1.0f - d) * filterConstant;
        }

        accelerationFilter[0] = (float) (alpha * (accelerationFilter[0] + accelerometerX - previousAcceleration[0]));
        accelerationFilter[1] = (float) (alpha * (accelerationFilter[1] + accelerometerY - previousAcceleration[1]));
        accelerationFilter[2] = (float) (alpha * (accelerationFilter[2] + accelerometerZ - previousAcceleration[2]));

        previousAcceleration[0] = accelerometerX;
        previousAcceleration[1] = accelerometerY;
        previousAcceleration[2] = accelerometerZ;
        //onFilteredAccelerometerChanged(accelerationFilter[0], accelerationFilter[1], accelerationFilter[2]);
        Log.e(TAG, "accelerationFilter: new reading");
        Log.d(TAG, "accelerationFilter: unfiltered " + previousAcceleration[0] + " " + previousAcceleration[1] + " " + previousAcceleration[2]);
        Log.d(TAG, "accelerationFilter: filtered" + accelerationFilter[0] + " " + accelerationFilter[1] + " " + accelerationFilter[2]);
    }

    private double norm(double x, double y, double z) {
        return Math.sqrt(x * x + y * y + z * z);
    }

    private double clamp(double v, double min, double max) {
        if (v > max) {
            return max;
        } else if (v < min) {
            return min;
        } else {
            return v;
        }
    }

    private void processAccelerometer(float[] linearAcceleration) {
        if (linearAcceleration[0] > 0.5 || linearAcceleration[0] < -0.5
                || linearAcceleration[1] > 0.5 || linearAcceleration[1] < -0.5
                || linearAcceleration[2] > 0.5 || linearAcceleration[2] < -0.5) {
            CoreService.lastSignificantMovement = System.currentTimeMillis();
        }
    }

    private void processMagneticField(float[] magneticField, float[] gravity) {
        float R[] = new float[9];
        float I[] = new float[9];
        boolean success = SensorManager.getRotationMatrix(R, I, gravity, magneticField);
        if (success) {
            float orientation[] = new float[3];
            SensorManager.getOrientation(R, orientation);
            // Log.d(TAG, "azimuth (rad): " + azimuth);
            float azimuth = (float) Math.toDegrees(orientation[0]);
            azimuth = (azimuth + 360) % 360;
            //Log.v(TAG, "azimuth (deg): " + azimuth);
            CoreService.currentOrientation = azimuth;
        }
    }

    private void processRotationVector() {

    }

    @Override
    public void onCreate() {
        // The service is being created
        powerManager = (PowerManager) getApplicationContext().getSystemService(Context.POWER_SERVICE);
        wakeLock = powerManager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "AccelerometerService");
        wakeLock.acquire();

        sensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);
        gravitySensor = sensorManager.getDefaultSensor(Sensor.TYPE_GRAVITY);
        magneticFieldSensor = sensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD);
        linearAccelerationSensor = sensorManager.getDefaultSensor(Sensor.TYPE_LINEAR_ACCELERATION);
        rotationVectorSensor = sensorManager.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR);
        sensorManager.registerListener(this, gravitySensor, SensorManager.SENSOR_DELAY_NORMAL);
        sensorManager.registerListener(this, magneticFieldSensor, SensorManager.SENSOR_DELAY_NORMAL);
        sensorManager.registerListener(this, linearAccelerationSensor, SensorManager.SENSOR_DELAY_NORMAL);
        sensorManager.registerListener(this, rotationVectorSensor, SensorManager.SENSOR_DELAY_NORMAL);
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
        sensorManager.unregisterListener(this, gravitySensor);
        sensorManager.unregisterListener(this, magneticFieldSensor);
        sensorManager.unregisterListener(this, linearAccelerationSensor);
        sensorManager.unregisterListener(this, rotationVectorSensor);
        wakeLock.release();
    }
}