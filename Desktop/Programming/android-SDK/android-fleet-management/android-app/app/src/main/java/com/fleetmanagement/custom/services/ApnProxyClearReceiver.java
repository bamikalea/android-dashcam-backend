package com.fleetmanagement.custom.services;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

/**
 * Broadcast receiver to clear APN proxy settings on boot and SIM changes
 * This ensures direct connections regardless of SIM card
 */
public class ApnProxyClearReceiver extends BroadcastReceiver {
    private static final String TAG = "ApnProxyClearReceiver";

    @Override
    public void onReceive(Context context, Intent intent) {
        String action = intent.getAction();
        Log.i(TAG, "Received broadcast: " + action);

        if (android.content.Intent.ACTION_BOOT_COMPLETED.equals(action) ||
                "android.intent.action.SIM_STATE_CHANGED".equals(action)) {

            Log.i(TAG, "Clearing APN proxy settings...");

            // Clear APN proxy settings
            try {
                android.database.Cursor cursor = context.getContentResolver().query(
                        android.provider.Telephony.Carriers.CONTENT_URI,
                        new String[] { "_id", "name", "proxy", "port" },
                        android.provider.Telephony.Carriers.CURRENT + "=1",
                        null, null);

                if (cursor != null && cursor.moveToFirst()) {
                    int idColumn = cursor.getColumnIndex("_id");
                    int nameColumn = cursor.getColumnIndex("name");
                    int proxyColumn = cursor.getColumnIndex("proxy");

                    if (idColumn >= 0) {
                        long apnId = cursor.getLong(idColumn);
                        String apnName = cursor.getString(nameColumn);
                        String currentProxy = cursor.getString(proxyColumn);

                        Log.i(TAG, "Current APN: " + apnName + " (ID: " + apnId + ")");
                        Log.i(TAG, "Current proxy: " + currentProxy);

                        if (currentProxy != null && !currentProxy.isEmpty()) {
                            android.content.ContentValues values = new android.content.ContentValues();
                            values.put("proxy", "");
                            values.put("port", "");

                            int updated = context.getContentResolver().update(
                                    android.provider.Telephony.Carriers.CONTENT_URI,
                                    values,
                                    "_id=?",
                                    new String[] { String.valueOf(apnId) });

                            if (updated > 0) {
                                Log.i(TAG, "=== APN PROXY CLEARED SUCCESSFULLY ===");
                                Log.i(TAG, "Removed proxy from APN: " + apnName);
                            } else {
                                Log.w(TAG, "Failed to clear APN proxy");
                            }
                        } else {
                            Log.i(TAG, "APN already has no proxy configured");
                        }
                    }
                    cursor.close();
                } else {
                    Log.w(TAG, "No current APN found");
                }
            } catch (Exception e) {
                Log.e(TAG, "Error clearing APN proxy: " + e.getMessage(), e);
            }
        }
    }
}