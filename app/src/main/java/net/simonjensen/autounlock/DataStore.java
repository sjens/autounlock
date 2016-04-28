package net.simonjensen.autounlock;

import android.content.ContentValues;
import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

public class DataStore {
    static final String DATABASE_NAME = "datastore.db";
    static final int DATABASE_VERSION = 1;

    static final String ID = "ID";
    static final String TIMESTAMP = "TIMESTAMP";

    static final String BLUETOOTH_TABLE = "bluetooth";
    static final String BLUETOOTH_NAME = "name";
    static final String BLUETOOTH_SOURCE = "Source";
    static final String BLUETOOTH_RSSI = "RSSI";


    static final String WIFI_TABLE = "wifi";
    static final String WIFI_SSID = "SSID";
    static final String WIFI_MAC = "MAC";
    static final String WIFI_RSSI = "RSSI";

    static final String ACCEL_TABLE = "accelerometer";
    static final String ACCEL_VECTOR = "vector";
    static final String ACCEL_VELOCITY = "velocity";

    static final String MAGNET_TABLE = "magnetometer";
    static final String MAGNET_DEGREE = "degree";

    static final String LOCATION_TABLE = "location";
    static final String LOCATION_PROVIDER = "provider";
    static final String LOCATION_LATITUDE = "latitude";
    static final String LOCATION_LONGITUDE = "longitude";
    static final String LOCATION_ACCURACY = "accuracy";

    private SQLiteDatabase database;
    private DatabaseHelper databaseHelper;

    public DataStore(Context context) {
        databaseHelper = new DatabaseHelper(context);
        database = databaseHelper.getWritableDatabase();
    }

    public void close() {
        database.close();
    }

    //: I cannot see why it should not work.
    public void insertBtle(String name, String btleSource, int btleRSSI, long timestamp) {
        ContentValues contentValues = new ContentValues();
        contentValues.put(BLUETOOTH_NAME, name);
        contentValues.put(BLUETOOTH_SOURCE, btleSource);
        contentValues.put(BLUETOOTH_RSSI, btleRSSI);
        contentValues.put(TIMESTAMP, timestamp);
        database.replace(BLUETOOTH_TABLE, null, contentValues);
    }

    public void insertWifi(String wifiSSID, String wifiMAC, String wifiRSSI, long timestamp) {
        ContentValues contentValues = new ContentValues();
        //contentValues.put(ID, id);
        contentValues.put(WIFI_SSID, wifiSSID);
        contentValues.put(WIFI_MAC, wifiMAC);
        contentValues.put(WIFI_RSSI, wifiRSSI);
        contentValues.put(TIMESTAMP, timestamp);
        database.replace(WIFI_TABLE, null, contentValues);
    }

    public void insertAccelerometer(String accelVector, String accelVelocity, long timestamp) {
        ContentValues contentValues = new ContentValues();
        contentValues.put(ACCEL_VECTOR, accelVector);
        contentValues.put(ACCEL_VELOCITY, accelVelocity);
        contentValues.put(TIMESTAMP, timestamp);
        database.replace(ACCEL_TABLE, null, contentValues);
    }

    public void insertMagnetometer(String degree, long timestamp) {
        ContentValues contentValues = new ContentValues();
        contentValues.put(MAGNET_DEGREE, degree);
        contentValues.put(TIMESTAMP, timestamp);
        database.replace(MAGNET_TABLE, null, contentValues);
    }

    public void insertLocation(String provider, double latitude, double longitude, float accuracy, long timestamp) {
        ContentValues contentValues = new ContentValues();
        contentValues.put(LOCATION_PROVIDER, provider);
        contentValues.put(LOCATION_LATITUDE, latitude);
        contentValues.put(LOCATION_LONGITUDE, longitude);
        contentValues.put(LOCATION_ACCURACY, accuracy);
        contentValues.put(TIMESTAMP, timestamp);
        database.replace(LOCATION_TABLE, null, contentValues);
    }

    private class DatabaseHelper extends SQLiteOpenHelper {
        public DatabaseHelper(Context context) {
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
                database.execSQL("DROP TABLE IF EXISTS " + BLUETOOTH_TABLE);
                database.execSQL("DROP TABLE IF EXISTS " + WIFI_TABLE);
                database.execSQL("DROP TABLE IF EXISTS " + ACCEL_TABLE);
                database.execSQL("DROP TABLE IF EXISTS " + MAGNET_TABLE);
                database.execSQL("DROP TABLE IF EXISTS " + LOCATION_TABLE);
                createDatastore(database);
                return;
            }
        }

        private void createDatastore(SQLiteDatabase database) {
            database.execSQL("CREATE TABLE " + BLUETOOTH_TABLE + " ("
                    + ID + " INTEGER PRIMARY KEY AUTOINCREMENT, "
                    + BLUETOOTH_NAME + " TEXT, "
                    + BLUETOOTH_SOURCE + " TEXT, "
                    + BLUETOOTH_RSSI + " INTEGER, "
                    + TIMESTAMP + " LONG)");

            database.execSQL("CREATE TABLE " + WIFI_TABLE + " ("
                    + ID + " INTEGER PRIMARY KEY AUTOINCREMENT, "
                    + WIFI_SSID + " TEXT, "
                    + WIFI_MAC + " TEXT, "
                    + WIFI_RSSI + " TEXT, "
                    + TIMESTAMP + " LONG)");

            database.execSQL("CREATE TABLE " + ACCEL_TABLE + " ("
                    + ID + " INTEGER PRIMARY KEY AUTOINCREMENT, "
                    + ACCEL_VECTOR + " TEXT, "
                    + ACCEL_VELOCITY + " TEXT, "
                    + TIMESTAMP + " LONG)");

            database.execSQL("CREATE TABLE " + MAGNET_TABLE + " ("
                    + ID + " INTEGER PRIMARY KEY AUTOINCREMENT, "
                    + MAGNET_DEGREE + " TEXT, "
                    + TIMESTAMP + " LONG)");

            database.execSQL("CREATE TABLE " + LOCATION_TABLE + " ("
                    + ID + " INTEGER PRIMARY KEY AUTOINCREMENT, "
                    + LOCATION_PROVIDER + " TEXT, "
                    + LOCATION_LATITUDE + " FLOAT, "
                    + LOCATION_LONGITUDE + " FLOAT, "
                    + LOCATION_ACCURACY + " FLOAT, "
                    + TIMESTAMP + " LONG)");
        }
    }
}
