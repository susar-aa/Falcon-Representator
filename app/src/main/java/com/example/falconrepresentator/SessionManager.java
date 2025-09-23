package com.example.falconrepresentator;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;

public class SessionManager {
    private final SharedPreferences pref;
    private final SharedPreferences.Editor editor;
    private final Context _context;

    private static final String PREF_NAME = "FalconRepLoginPref";
    private static final String IS_LOGIN = "IsLoggedIn";
    public static final String KEY_REP_ID = "rep_id";
    public static final String KEY_USERNAME = "username";
    public static final String KEY_FULL_NAME = "full_name";
    public static final String KEY_LAST_SYNC = "last_sync_timestamp";

    // Keys for Daily Route State
    private static final String IS_DAY_STARTED = "IsDayStarted";
    private static final String KEY_START_METER = "start_meter";
    private static final String KEY_END_METER = "end_meter";
    private static final String KEY_ROUTE_DATE = "route_date";
    // NEW: Keys for selected route
    private static final String KEY_SELECTED_ROUTE_ID = "selected_route_id";
    private static final String KEY_SELECTED_ROUTE_NAME = "selected_route_name";


    public SessionManager(Context context) {
        this._context = context;
        int PRIVATE_MODE = 0;
        pref = _context.getSharedPreferences(PREF_NAME, PRIVATE_MODE);
        editor = pref.edit();
    }

    public void createLoginSession(int repId, String username, String fullName) {
        editor.putBoolean(IS_LOGIN, true);
        editor.putInt(KEY_REP_ID, repId);
        editor.putString(KEY_USERNAME, username);
        editor.putString(KEY_FULL_NAME, fullName);
        editor.apply();
    }

    public HashMap<String, String> getUserDetails() {
        HashMap<String, String> user = new HashMap<>();
        user.put(KEY_REP_ID, String.valueOf(pref.getInt(KEY_REP_ID, 0)));
        user.put(KEY_USERNAME, pref.getString(KEY_USERNAME, null));
        user.put(KEY_FULL_NAME, pref.getString(KEY_FULL_NAME, null));
        return user;
    }

    public void logoutUser() {
        editor.clear();
        editor.apply();

        Intent i = new Intent(_context, LoginActivity.class);
        i.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        i.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        _context.startActivity(i);
    }

    public boolean isLoggedIn() {
        return pref.getBoolean(IS_LOGIN, false);
    }

    public int getRepId() {
        return pref.getInt(KEY_REP_ID, -1);
    }

    public void updateLastSyncTimestamp() {
        editor.putLong(KEY_LAST_SYNC, System.currentTimeMillis());
        editor.apply();
    }

    public long getLastSyncTimestamp() {
        return pref.getLong(KEY_LAST_SYNC, 0);
    }

    // --- Daily Route Methods ---

    public void startDay(int startMeter) {
        editor.putBoolean(IS_DAY_STARTED, true);
        editor.putInt(KEY_START_METER, startMeter);
        editor.putString(KEY_ROUTE_DATE, new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(new Date()));
        editor.apply();
    }

    public void endDay() {
        editor.putBoolean(IS_DAY_STARTED, false);
        editor.apply();
    }

    public boolean isDayStarted() {
        return pref.getBoolean(IS_DAY_STARTED, false);
    }

    public int getStartMeter() {
        return pref.getInt(KEY_START_METER, 0);
    }

    public void setEndMeter(int endMeter) {
        editor.putInt(KEY_END_METER, endMeter);
        editor.apply();
    }

    public int getEndMeter() {
        return pref.getInt(KEY_END_METER, 0);
    }

    public String getRouteDate() {
        return pref.getString(KEY_ROUTE_DATE, "");
    }

    public void clearDailyRouteData() {
        editor.remove(IS_DAY_STARTED);
        editor.remove(KEY_START_METER);
        editor.remove(KEY_END_METER);
        editor.remove(KEY_ROUTE_DATE);
        editor.remove(KEY_SELECTED_ROUTE_ID);
        editor.remove(KEY_SELECTED_ROUTE_NAME);
        editor.apply();
    }

    // --- NEW: Methods for storing selected route ---
    public void setSelectedRoute(int routeId, String routeName) {
        editor.putInt(KEY_SELECTED_ROUTE_ID, routeId);
        editor.putString(KEY_SELECTED_ROUTE_NAME, routeName);
        editor.apply();
    }

    public int getSelectedRouteId() {
        return pref.getInt(KEY_SELECTED_ROUTE_ID, 0);
    }

    public String getSelectedRouteName() {
        return pref.getString(KEY_SELECTED_ROUTE_NAME, "N/A");
    }
}

