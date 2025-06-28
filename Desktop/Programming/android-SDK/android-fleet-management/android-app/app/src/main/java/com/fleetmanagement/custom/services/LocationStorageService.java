package com.fleetmanagement.custom.services;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.location.Location;
import android.util.Log;

import java.util.ArrayList;
import java.util.List;

/**
 * Location Storage Service for local database storage
 * Provides offline location data persistence
 */
public class LocationStorageService extends SQLiteOpenHelper {

    private static final String TAG = "LocationStorageService";
    private static final String DATABASE_NAME = "fleet_location.db";
    private static final int DATABASE_VERSION = 1;

    // Table name
    private static final String TABLE_LOCATIONS = "locations";

    // Column names
    private static final String COLUMN_ID = "id";
    private static final String COLUMN_LATITUDE = "latitude";
    private static final String COLUMN_LONGITUDE = "longitude";
    private static final String COLUMN_ALTITUDE = "altitude";
    private static final String COLUMN_SPEED = "speed";
    private static final String COLUMN_ACCURACY = "accuracy";
    private static final String COLUMN_PROVIDER = "provider";
    private static final String COLUMN_TIMESTAMP = "timestamp";
    private static final String COLUMN_SYNCED = "synced";

    // Create table SQL
    private static final String CREATE_TABLE_LOCATIONS = "CREATE TABLE " + TABLE_LOCATIONS + " (" +
            COLUMN_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
            COLUMN_LATITUDE + " REAL NOT NULL, " +
            COLUMN_LONGITUDE + " REAL NOT NULL, " +
            COLUMN_ALTITUDE + " REAL, " +
            COLUMN_SPEED + " REAL, " +
            COLUMN_ACCURACY + " REAL, " +
            COLUMN_PROVIDER + " TEXT, " +
            COLUMN_TIMESTAMP + " INTEGER NOT NULL, " +
            COLUMN_SYNCED + " INTEGER DEFAULT 0" +
            ")";

    // Index for faster queries
    private static final String CREATE_INDEX_TIMESTAMP = "CREATE INDEX idx_timestamp ON " + TABLE_LOCATIONS + " ("
            + COLUMN_TIMESTAMP + ")";

    private static final String CREATE_INDEX_SYNCED = "CREATE INDEX idx_synced ON " + TABLE_LOCATIONS + " ("
            + COLUMN_SYNCED + ")";

    public LocationStorageService(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        try {
            db.execSQL(CREATE_TABLE_LOCATIONS);
            db.execSQL(CREATE_INDEX_TIMESTAMP);
            db.execSQL(CREATE_INDEX_SYNCED);
            Log.i(TAG, "Location database created successfully");
        } catch (Exception e) {
            Log.e(TAG, "Error creating database: " + e.getMessage(), e);
        }
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        // For future database schema updates
        if (oldVersion < newVersion) {
            db.execSQL("DROP TABLE IF EXISTS " + TABLE_LOCATIONS);
            onCreate(db);
        }
    }

    /**
     * Save location to local database
     */
    public long saveLocation(Location location) {
        SQLiteDatabase db = this.getWritableDatabase();
        long id = -1;

        try {
            ContentValues values = new ContentValues();
            values.put(COLUMN_LATITUDE, location.getLatitude());
            values.put(COLUMN_LONGITUDE, location.getLongitude());
            values.put(COLUMN_ALTITUDE, location.getAltitude());
            values.put(COLUMN_SPEED, location.getSpeed());
            values.put(COLUMN_ACCURACY, location.getAccuracy());
            values.put(COLUMN_PROVIDER, location.getProvider());
            values.put(COLUMN_TIMESTAMP, location.getTime());
            values.put(COLUMN_SYNCED, 0); // Not synced yet

            id = db.insert(TABLE_LOCATIONS, null, values);

            if (id != -1) {
                Log.d(TAG, "Location saved to database with ID: " + id);
            } else {
                Log.e(TAG, "Failed to save location to database");
            }

        } catch (Exception e) {
            Log.e(TAG, "Error saving location: " + e.getMessage(), e);
        } finally {
            db.close();
        }

        return id;
    }

    /**
     * Get last known location from database
     */
    public Location getLastKnownLocation() {
        SQLiteDatabase db = this.getReadableDatabase();
        Location location = null;

        try {
            String query = "SELECT * FROM " + TABLE_LOCATIONS +
                    " ORDER BY " + COLUMN_TIMESTAMP + " DESC LIMIT 1";

            Cursor cursor = db.rawQuery(query, null);

            if (cursor.moveToFirst()) {
                location = cursorToLocation(cursor);
                Log.d(TAG, "Retrieved last known location from database");
            }

            cursor.close();

        } catch (Exception e) {
            Log.e(TAG, "Error getting last known location: " + e.getMessage(), e);
        } finally {
            db.close();
        }

        return location;
    }

    /**
     * Get unsynced locations (for offline sync)
     */
    public List<Location> getUnsyncedLocations() {
        SQLiteDatabase db = this.getReadableDatabase();
        List<Location> locations = new ArrayList<>();

        try {
            String query = "SELECT * FROM " + TABLE_LOCATIONS +
                    " WHERE " + COLUMN_SYNCED + " = 0" +
                    " ORDER BY " + COLUMN_TIMESTAMP + " ASC";

            Cursor cursor = db.rawQuery(query, null);

            while (cursor.moveToNext()) {
                Location location = cursorToLocation(cursor);
                locations.add(location);
            }

            cursor.close();
            Log.d(TAG, "Retrieved " + locations.size() + " unsynced locations");

        } catch (Exception e) {
            Log.e(TAG, "Error getting unsynced locations: " + e.getMessage(), e);
        } finally {
            db.close();
        }

        return locations;
    }

    /**
     * Mark location as synced
     */
    public boolean markLocationAsSynced(long locationId) {
        SQLiteDatabase db = this.getWritableDatabase();
        boolean success = false;

        try {
            ContentValues values = new ContentValues();
            values.put(COLUMN_SYNCED, 1);

            int rowsAffected = db.update(TABLE_LOCATIONS, values,
                    COLUMN_ID + " = ?",
                    new String[] { String.valueOf(locationId) });

            success = rowsAffected > 0;

            if (success) {
                Log.d(TAG, "Location " + locationId + " marked as synced");
            }

        } catch (Exception e) {
            Log.e(TAG, "Error marking location as synced: " + e.getMessage(), e);
        } finally {
            db.close();
        }

        return success;
    }

    /**
     * Get location history (last N locations)
     */
    public List<Location> getLocationHistory(int limit) {
        SQLiteDatabase db = this.getReadableDatabase();
        List<Location> locations = new ArrayList<>();

        try {
            String query = "SELECT * FROM " + TABLE_LOCATIONS +
                    " ORDER BY " + COLUMN_TIMESTAMP + " DESC LIMIT " + limit;

            Cursor cursor = db.rawQuery(query, null);

            while (cursor.moveToNext()) {
                Location location = cursorToLocation(cursor);
                locations.add(location);
            }

            cursor.close();
            Log.d(TAG, "Retrieved " + locations.size() + " locations from history");

        } catch (Exception e) {
            Log.e(TAG, "Error getting location history: " + e.getMessage(), e);
        } finally {
            db.close();
        }

        return locations;
    }

    /**
     * Clean old location data (keep last 7 days)
     */
    public int cleanOldLocations() {
        SQLiteDatabase db = this.getWritableDatabase();
        int deletedRows = 0;

        try {
            long sevenDaysAgo = System.currentTimeMillis() - (7 * 24 * 60 * 60 * 1000L);

            deletedRows = db.delete(TABLE_LOCATIONS,
                    COLUMN_TIMESTAMP + " < ?",
                    new String[] { String.valueOf(sevenDaysAgo) });

            Log.i(TAG, "Cleaned " + deletedRows + " old location records");

        } catch (Exception e) {
            Log.e(TAG, "Error cleaning old locations: " + e.getMessage(), e);
        } finally {
            db.close();
        }

        return deletedRows;
    }

    /**
     * Convert cursor to Location object
     */
    private Location cursorToLocation(Cursor cursor) {
        Location location = new Location("database");

        location.setLatitude(cursor.getDouble(cursor.getColumnIndex(COLUMN_LATITUDE)));
        location.setLongitude(cursor.getDouble(cursor.getColumnIndex(COLUMN_LONGITUDE)));
        location.setAltitude(cursor.getDouble(cursor.getColumnIndex(COLUMN_ALTITUDE)));
        location.setSpeed(cursor.getFloat(cursor.getColumnIndex(COLUMN_SPEED)));
        location.setAccuracy(cursor.getFloat(cursor.getColumnIndex(COLUMN_ACCURACY)));
        location.setProvider(cursor.getString(cursor.getColumnIndex(COLUMN_PROVIDER)));
        location.setTime(cursor.getLong(cursor.getColumnIndex(COLUMN_TIMESTAMP)));

        return location;
    }

    /**
     * Get database statistics
     */
    public String getDatabaseStats() {
        SQLiteDatabase db = this.getReadableDatabase();
        String stats = "No data";

        try {
            // Total locations
            Cursor totalCursor = db.rawQuery("SELECT COUNT(*) FROM " + TABLE_LOCATIONS, null);
            int totalLocations = 0;
            if (totalCursor.moveToFirst()) {
                totalLocations = totalCursor.getInt(0);
            }
            totalCursor.close();

            // Unsynced locations
            Cursor unsyncedCursor = db.rawQuery("SELECT COUNT(*) FROM " + TABLE_LOCATIONS +
                    " WHERE " + COLUMN_SYNCED + " = 0", null);
            int unsyncedLocations = 0;
            if (unsyncedCursor.moveToFirst()) {
                unsyncedLocations = unsyncedCursor.getInt(0);
            }
            unsyncedCursor.close();

            stats = "Total: " + totalLocations + ", Unsynced: " + unsyncedLocations;

        } catch (Exception e) {
            Log.e(TAG, "Error getting database stats: " + e.getMessage(), e);
        } finally {
            db.close();
        }

        return stats;
    }
}