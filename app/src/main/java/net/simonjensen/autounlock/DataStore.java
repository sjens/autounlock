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

    static final String BTLE_TABLE = "btle";
    static final String BTLE_RSSI = "RSSI";
    static final String BTLE_SOURCE = "Source";

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
    static final String LOCATION_LATITUDE = "latitude";
    static final String LOCATION_LONGITUDE = "longitude";
    static final String LOCATION_PRECISION = "precision";

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
    public void insertBtle(int id, String btleRSSI, String btleSource, String timestamp) {
        ContentValues contentValues = new ContentValues();
        contentValues.put(ID, id);
        contentValues.put(BTLE_RSSI, btleRSSI);
        contentValues.put(BTLE_SOURCE, btleSource);
        contentValues.put(TIMESTAMP, timestamp);
        database.replace(BTLE_TABLE, null, contentValues);
    }

    public void insertWifi(int id, String wifiSSID, String wifiMAC, String wifiRSSI, String timestamp) {
        ContentValues contentValues = new ContentValues();
        contentValues.put(ID, id);
        contentValues.put(WIFI_SSID, wifiSSID);
        contentValues.put(WIFI_MAC, wifiMAC);
        contentValues.put(WIFI_RSSI, wifiRSSI);
        contentValues.put(TIMESTAMP, timestamp);
        database.replace(WIFI_TABLE, null, contentValues);
    }

    public void insertAccelerometer(int id, String accelVector, String accelVelocity, String timestamp) {
        ContentValues contentValues = new ContentValues();
        contentValues.put(ID, id);
        contentValues.put(ACCEL_VECTOR, accelVector);
        contentValues.put(ACCEL_VELOCITY, accelVelocity);
        contentValues.put(TIMESTAMP, timestamp);
        database.replace(ACCEL_TABLE, null, contentValues);
    }

    public void insertMagnetometer(int id, String degree, String timestamp) {
        ContentValues contentValues = new ContentValues();
        contentValues.put(ID, id);
        contentValues.put(MAGNET_DEGREE, degree);
        contentValues.put(TIMESTAMP, timestamp);
        database.replace(MAGNET_TABLE, null, contentValues);
    }

    public void insertLocation(int id, String latitude, String longitude, String precision, String timestamp) {
        ContentValues contentValues = new ContentValues();
        contentValues.put(ID, id);
        contentValues.put(LOCATION_LATITUDE, latitude);
        contentValues.put(LOCATION_LONGITUDE, longitude);
        contentValues.put(LOCATION_PRECISION, precision);
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
        public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
            // Production-quality upgrade code should modify the tables when
            // the database version changes instead of dropping the tables and
            // re-creating them.
            if (newVersion != DATABASE_VERSION) {
                Log.w("Datastore", "Database upgrade from old: " + oldVersion + " to: " +
                        newVersion);
                db.execSQL("DROP TABLE IF EXISTS " + BTLE_TABLE);
                db.execSQL("DROP TABLE IF EXISTS " + WIFI_TABLE);
                db.execSQL("DROP TABLE IF EXISTS " + ACCEL_TABLE);
                db.execSQL("DROP TABLE IF EXISTS " + MAGNET_TABLE);
                db.execSQL("DROP TABLE IF EXISTS " + LOCATION_TABLE);
                createDatastore(db);
                return;
            }
        }

        private void createDatastore(SQLiteDatabase db) {
            db.execSQL("CREATE TABLE " + BTLE_TABLE + " ("
                    + ID + " INTEGER PRIMARY KEY, "
                    + BTLE_RSSI + " TEXT, "
                    + BTLE_SOURCE + " TEXT, "
                    + TIMESTAMP + " TEXT)");

            db.execSQL("CREATE TABLE " + WIFI_TABLE + " ("
                    + ID + " INTEGER PRIMARY KEY, "
                    + WIFI_SSID + " TEXT, "
                    + WIFI_MAC + " TEXT, "
                    + WIFI_RSSI + " TEXT, "
                    + TIMESTAMP + " TEXT)");

            db.execSQL("CREATE TABLE " + ACCEL_TABLE + " ("
                    + ID + " INTEGER PRIMARY KEY, "
                    + ACCEL_VECTOR + " TEXT, "
                    + ACCEL_VELOCITY + " TEXT, "
                    + TIMESTAMP + " TEXT)");

            db.execSQL("CREATE TABLE " + MAGNET_TABLE + " ("
                    + ID + " INTEGER PRIMARY KEY, "
                    + MAGNET_DEGREE + " TEXT, "
                    + TIMESTAMP + " TEXT)");

            db.execSQL("CREATE TABLE " + LOCATION_TABLE + " ("
                    + ID + " INTEGER PRIMARY KEY, "
                    + LOCATION_LATITUDE + " TEXT, "
                    + LOCATION_LONGITUDE + " TEXT, "
                    + LOCATION_PRECISION + " TEXT, "
                    + TIMESTAMP + " TEXT)");
        }
    }
}
