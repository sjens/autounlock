package net.simonjensen.autounlock;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

import java.util.ArrayList;

class DataStore {
    private static final String DATABASE_NAME = "datastore.db";
    private static final int DATABASE_VERSION = 1;

    private static final String ID = "ID";
    private static final String TIMESTAMP = "TIMESTAMP";

    private static final String LOCK_TABLE = "lock";
    private static final String LOCK_MAC = "MAC";
    private static final String LOCK_PASSPHRASE = "passphrase";
    private static final String LOCK_LATITUDE = "latitude";
    private static final String LOCK_LONGITUDE = "longitude";
    private static final String LOCK_INNER_GEOFENCE = "inner_geofence";
    private static final String LOCK_OUTER_GEOFENCE = "outer_geofence";

    private static final String BLUETOOTH_TABLE = "bluetooth";
    private static final String BLUETOOTH_NAME = "name";
    private static final String BLUETOOTH_SOURCE = "source";
    private static final String BLUETOOTH_RSSI = "RSSI";
    private static final String BLUETOOTH_NEARBY_LOCK = "nearby_lock";

    private static final String WIFI_TABLE = "wifi";
    private static final String WIFI_SSID = "SSID";
    private static final String WIFI_MAC = "MAC";
    private static final String WIFI_RSSI = "RSSI";
    private static final String WIFI_NEARBY_LOCK = "nearby_lock";

    private static final String ACCELEROMETER_TABLE = "accelerometer";
    private static final String ACCELERATION_X = "acceleration_x";
    private static final String ACCELERATION_Y = "acceleration_y";
    private static final String ACCELERATION_Z = "acceleration_z";
    private static final String SPEED_X = "speed_x";
    private static final String SPEED_Y = "speed_y";
    private static final String SPEED_Z = "speed_z";

    private static final String LOCATION_TABLE = "location";
    private static final String LOCATION_PROVIDER = "provider";
    private static final String LOCATION_LATITUDE = "latitude";
    private static final String LOCATION_LONGITUDE = "longitude";
    private static final String LOCATION_ACCURACY = "accuracy";

    private static final String DECISION_TABLE = "decision";
    private static final String DECISION_DECISION = "decision";

    private static final String BUFFER_TABLE = "buffer";
    private static final String DATA = "data";

    private SQLiteDatabase database;
    private DatabaseHelper databaseHelper;

    DataStore(Context context) {
        databaseHelper = new DatabaseHelper(context);
    }

    void deleteDatastore() {
        database = databaseHelper.getWritableDatabase();
        database.delete(BLUETOOTH_TABLE, null, null);
        database.delete(WIFI_TABLE, null, null);
        database.delete(ACCELEROMETER_TABLE, null, null);
        database.delete(LOCATION_TABLE, null, null);
        database.delete(DECISION_TABLE, null, null);
        database.delete(BUFFER_TABLE, null, null);
        database.close();
    }

    void insertLockDetails(String lockMAC, String lockPassphrase, double lockLatitude, double lockLongitude,
                                  float lockInnerGeofence, float lockOuterGeofence, long timestamp) {
        ContentValues contentValues = new ContentValues();
        contentValues.put(LOCK_MAC, lockMAC);
        contentValues.put(LOCK_PASSPHRASE, lockPassphrase);
        contentValues.put(LOCK_LATITUDE, lockLatitude);
        contentValues.put(LOCK_LONGITUDE, lockLongitude);
        contentValues.put(LOCK_INNER_GEOFENCE, lockInnerGeofence);
        contentValues.put(LOCK_OUTER_GEOFENCE, lockOuterGeofence);
        contentValues.put(TIMESTAMP, timestamp);

        try {
            database = databaseHelper.getWritableDatabase();
            database.beginTransaction();
            database.replace(LOCK_TABLE, null, contentValues);
            database.setTransactionSuccessful();
        } finally {
            database.endTransaction();
        }
    }

    ArrayList<LockData> getKnownLocks() {
        ArrayList<LockData> lockDataArrayList = new ArrayList<>();

        try {
            database = databaseHelper.getReadableDatabase();
            database.beginTransaction();

            String lockQuery = "SELECT * FROM " + LOCK_TABLE + ";";
            Cursor lockCursor = database.rawQuery(lockQuery, null);

            lockCursor.moveToFirst();
            if (!lockCursor.isAfterLast()) {
                for (int i = 0; i < lockCursor.getCount(); i++) {
                    String lockMac = lockCursor.getString(lockCursor.getColumnIndex(LOCK_MAC));
                    double lockLatitude = lockCursor.getDouble(lockCursor.getColumnIndex(LOCK_LATITUDE));
                    double lockLongitude = lockCursor.getDouble(lockCursor.getColumnIndex(LOCK_LONGITUDE));
                    float innerGeofence = lockCursor.getInt(lockCursor.getColumnIndex(LOCK_INNER_GEOFENCE));
                    float outerGeofence = lockCursor.getInt(lockCursor.getColumnIndex(LOCK_OUTER_GEOFENCE));
                    LockData lockData = new LockData(
                            lockMac,
                            new LocationData(lockLatitude, lockLongitude),
                            innerGeofence,
                            outerGeofence
                    );
                    lockDataArrayList.add(lockData);
                }
            }
            lockCursor.close();
        } finally {
            database.endTransaction();
        }
        return lockDataArrayList;
    }

    LockData getLockDetails(String foundLock) {
        LockData lockData;
        LocationData locationData;
        BluetoothData bluetoothData;
        WifiData wifiData;

        String lockMac;
        String lockPassphrase;
        double lockLatitude;
        double lockLongitude;
        float innerGeofence;
        float outerGeofence;

        ArrayList<BluetoothData> nearbyBluetoothDevices = new ArrayList<>();
        ArrayList<WifiData> nearbyWifiAccessPoints = new ArrayList<>();

        try {
            database = databaseHelper.getReadableDatabase();
            database.beginTransaction();

            String lockQuery = "SELECT * FROM " + LOCK_TABLE + " WHERE " + LOCK_MAC + "='" + foundLock + "';";
            Cursor lockCursor = database.rawQuery(lockQuery, null);

            lockCursor.moveToFirst();
            if (lockCursor.isAfterLast()) {
                // We have not found any locks and return null.
                lockCursor.close();
                return null;
            } else {
                lockMac = lockCursor.getString(lockCursor.getColumnIndex(LOCK_MAC));
                lockPassphrase = lockCursor.getString(lockCursor.getColumnIndex(LOCK_PASSPHRASE));
                lockLatitude = lockCursor.getDouble(lockCursor.getColumnIndex(LOCK_LATITUDE));
                lockLongitude = lockCursor.getDouble(lockCursor.getColumnIndex(LOCK_LONGITUDE));
                innerGeofence = lockCursor.getInt(lockCursor.getColumnIndex(LOCK_INNER_GEOFENCE));
                outerGeofence = lockCursor.getInt(lockCursor.getColumnIndex(LOCK_OUTER_GEOFENCE));
                lockCursor.close();
            }

            String bluetoothQuery = "SELECT * FROM " + BLUETOOTH_TABLE + " WHERE "
                    + BLUETOOTH_NEARBY_LOCK + "='" + foundLock + "';";
            Cursor bluetoothCursor = database.rawQuery(bluetoothQuery, null);
            if (bluetoothCursor.getColumnCount() != 0) {
                bluetoothCursor.moveToFirst();
                for (int i = 0; i <= bluetoothCursor.getColumnCount(); i++) {
                    String bluetoothName = bluetoothCursor.getString(bluetoothCursor.getColumnIndex(BLUETOOTH_NAME));
                    String bluetoothSource = bluetoothCursor.getString(bluetoothCursor.getColumnIndex(BLUETOOTH_SOURCE));
                    int bluetoothRSSI = bluetoothCursor.getInt(bluetoothCursor.getColumnIndex(BLUETOOTH_RSSI));
                    long bluetoothTimestamp = bluetoothCursor.getLong(bluetoothCursor.getColumnIndex(TIMESTAMP));

                    bluetoothData = new BluetoothData(bluetoothName, bluetoothSource, bluetoothRSSI, bluetoothTimestamp);
                    nearbyBluetoothDevices.add(bluetoothData);

                    if (!(bluetoothCursor.isLast() || bluetoothCursor.isAfterLast())) {
                        bluetoothCursor.moveToNext();
                    }
                }
            }
            bluetoothCursor.close();

            String wifiQuery = "SELECT * FROM " + WIFI_TABLE + " WHERE "
                    + WIFI_NEARBY_LOCK + "='" + foundLock + "';";
            Cursor wifiCursor = database.rawQuery(wifiQuery, null);
            if (wifiCursor.getColumnCount() != 0) {
                wifiCursor.moveToFirst();
                for (int i = 0; i <= wifiCursor.getColumnCount(); i++) {
                    String wifiSSID = wifiCursor.getString(wifiCursor.getColumnIndex(WIFI_SSID));
                    String wifiMAC = wifiCursor.getString(wifiCursor.getColumnIndex(WIFI_MAC));
                    int wifiRSSI = wifiCursor.getInt(wifiCursor.getColumnIndex(WIFI_RSSI));
                    long wifiTimestamp = wifiCursor.getLong(wifiCursor.getColumnIndex(TIMESTAMP));

                    wifiData = new WifiData(wifiSSID, wifiMAC, wifiRSSI, wifiTimestamp);
                    nearbyWifiAccessPoints.add(wifiData);

                    if (!(wifiCursor.isLast() || wifiCursor.isAfterLast())) {
                        wifiCursor.moveToNext();
                    }
                }
            }
            wifiCursor.close();

            locationData = new LocationData(lockLatitude, lockLongitude);
            lockData = new LockData(lockMac, lockPassphrase, locationData,
                    innerGeofence, outerGeofence, nearbyBluetoothDevices, nearbyWifiAccessPoints);
            return lockData;
        } finally {
            database.endTransaction();
        }
    }

    void insertBtle(String name, String btleSource, int btleRSSI, String nearbyLock, long timestamp) {
        ContentValues contentValues = new ContentValues();
        contentValues.put(BLUETOOTH_NAME, name);
        contentValues.put(BLUETOOTH_SOURCE, btleSource);
        contentValues.put(BLUETOOTH_RSSI, btleRSSI);
        contentValues.put(BLUETOOTH_NEARBY_LOCK, nearbyLock);
        contentValues.put(TIMESTAMP, timestamp);

        try {
            database = databaseHelper.getWritableDatabase();
            database.beginTransaction();
            database.replace(BLUETOOTH_TABLE, null, contentValues);
            database.setTransactionSuccessful();
        } finally {
            database.endTransaction();
        }
    }

    void insertWifi(String wifiSSID, String wifiMAC, int wifiRSSI, String nearbyLock, long timestamp) {
        ContentValues contentValues = new ContentValues();
        contentValues.put(WIFI_SSID, wifiSSID);
        contentValues.put(WIFI_MAC, wifiMAC);
        contentValues.put(WIFI_RSSI, wifiRSSI);
        contentValues.put(WIFI_NEARBY_LOCK, nearbyLock);
        contentValues.put(TIMESTAMP, timestamp);

        try {
            database = databaseHelper.getWritableDatabase();
            database.beginTransaction();
            database.replace(WIFI_TABLE, null, contentValues);
            database.setTransactionSuccessful();
        } finally {
            database.endTransaction();
        }
    }

    void insertAccelerometer(float accelerometerX, float accelerometerY, float accelerometerZ,
                                    float speedX, float speedY, float speedZ, long timestamp) {
        ContentValues contentValues = new ContentValues();
        contentValues.put(ACCELERATION_X, accelerometerX);
        contentValues.put(ACCELERATION_Y, accelerometerY);
        contentValues.put(ACCELERATION_Z, accelerometerZ);
        contentValues.put(SPEED_X, speedX);
        contentValues.put(SPEED_Y, speedY);
        contentValues.put(SPEED_Z, speedZ);
        contentValues.put(TIMESTAMP, timestamp);

        try {
            database = databaseHelper.getWritableDatabase();
            database.beginTransaction();
            database.replace(ACCELEROMETER_TABLE, null, contentValues);
            database.setTransactionSuccessful();
        } finally {
            database.endTransaction();
        }
    }

    void insertLocation(String provider, double latitude, double longitude, float accuracy, long timestamp) {
        ContentValues contentValues = new ContentValues();
        contentValues.put(LOCATION_PROVIDER, provider);
        contentValues.put(LOCATION_LATITUDE, latitude);
        contentValues.put(LOCATION_LONGITUDE, longitude);
        contentValues.put(LOCATION_ACCURACY, accuracy);
        contentValues.put(TIMESTAMP, timestamp);

        try {
            database = databaseHelper.getWritableDatabase();
            database.beginTransaction();
            database.replace(LOCATION_TABLE, null, contentValues);
            database.setTransactionSuccessful();
        } finally {
            database.endTransaction();
        }
    }

    void insertDecision(int decision, long timestamp) {
        ContentValues contentValues = new ContentValues();
        contentValues.put(DECISION_DECISION, decision);
        contentValues.put(TIMESTAMP, timestamp);

        try {
            database = databaseHelper.getWritableDatabase();
            database.beginTransaction();
            database.replace(DECISION_TABLE, null, contentValues);
            database.setTransactionSuccessful();
        } finally {
            database.endTransaction();
        }
    }

    void insertBuffer(long timestamp, String data) {
        ContentValues contentValues = new ContentValues();
        contentValues.put(TIMESTAMP, timestamp);
        contentValues.put(DATA, data);

        try {
            database = databaseHelper.getWritableDatabase();
            database.beginTransaction();
            database.replace(BUFFER_TABLE, null, contentValues);
            database.setTransactionSuccessful();
        } finally {
            database.endTransaction();
        }
    }

    private class DatabaseHelper extends SQLiteOpenHelper {
        DatabaseHelper(Context context) {
            super(context, DATABASE_NAME, null, DATABASE_VERSION);
        }

        @Override
        public void onCreate(SQLiteDatabase db) {
            createDatastore(db);
        }

        @Override
        public void onUpgrade(SQLiteDatabase database, int oldVersion, int newVersion) {
            // Production-quality upgrade code should modify the tables when
            // the database version changes instead of dropping the tables and
            // re-creating them.
            if (newVersion != DATABASE_VERSION) {
                Log.w("Datastore", "Database upgrade from old: " + oldVersion + " to: " +
                        newVersion);
                database = databaseHelper.getWritableDatabase();
                dropDatastore();
                createDatastore(database);
                database.close();
            }
        }

        private void createDatastore(SQLiteDatabase database) {
            database.execSQL("CREATE TABLE " + LOCK_TABLE + " ("
                    + LOCK_MAC + " TEXT PRIMARY KEY, "
                    + LOCK_PASSPHRASE + " TEXT, "
                    + LOCK_LATITUDE + " DOUBLE, "
                    + LOCK_LONGITUDE + " DOUBLE, "
                    + LOCK_INNER_GEOFENCE + " FLOAT, "
                    + LOCK_OUTER_GEOFENCE + " FLOAT, "
                    + TIMESTAMP + " LONG)");

            database.execSQL("CREATE TABLE " + BLUETOOTH_TABLE + " ("
                    + BLUETOOTH_NAME + " TEXT, "
                    + BLUETOOTH_SOURCE + " TEXT, "
                    + BLUETOOTH_RSSI + " INTEGER, "
                    + BLUETOOTH_NEARBY_LOCK + " FOREIGNKEY REFERENCES " + LOCK_TABLE + "(" + LOCK_MAC + "), "
                    + TIMESTAMP + " LONG, "
                    + "PRIMARY KEY (" + BLUETOOTH_SOURCE + ", " + BLUETOOTH_NEARBY_LOCK + "))");

            database.execSQL("CREATE TABLE " + WIFI_TABLE + " ("
                    + WIFI_SSID + " TEXT, "
                    + WIFI_MAC + " TEXT, "
                    + WIFI_RSSI + " INTEGER, "
                    + WIFI_NEARBY_LOCK + " FOREIGNKEY REFERENCES " + LOCK_TABLE + "(" + LOCK_MAC + "), "
                    + TIMESTAMP + " LONG, "
                    + "PRIMARY KEY (" + WIFI_MAC + ", " + WIFI_NEARBY_LOCK + "))");

            database.execSQL("CREATE TABLE " + ACCELEROMETER_TABLE + " ("
                    + ID + " INTEGER PRIMARY KEY AUTOINCREMENT, "
                    + ACCELERATION_X + " FLOAT, "
                    + ACCELERATION_Y + " FLOAT, "
                    + ACCELERATION_Z + " FLOAT, "
                    + SPEED_X + " FLOAT, "
                    + SPEED_Y + " FLOAT, "
                    + SPEED_Z + " FLOAT, "
                    + TIMESTAMP + " LONG)");
        }

        private void dropDatastore() {
            database.execSQL("DROP TABLE IF EXISTS " + LOCK_TABLE);
            database.execSQL("DROP TABLE IF EXISTS " + BLUETOOTH_TABLE);
            database.execSQL("DROP TABLE IF EXISTS " + WIFI_TABLE);
            database.execSQL("DROP TABLE IF EXISTS " + ACCELEROMETER_TABLE);
            database.execSQL("DROP TABLE IF EXISTS " + LOCATION_TABLE);
            database.execSQL("DROP TABLE IF EXISTS " + DECISION_TABLE);
            database.execSQL("DROP TABLE IF EXISTS " + BUFFER_TABLE);
        }
    }
}
