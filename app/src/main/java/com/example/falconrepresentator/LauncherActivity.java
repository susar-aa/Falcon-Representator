package com.example.falconrepresentator;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;

import org.json.JSONException;
import org.json.JSONObject;

public class LauncherActivity extends AppCompatActivity {

    private static final String TAG = "LauncherActivity";
    private SessionManager sessionManager;
    private RequestQueue requestQueue;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // No setContentView is needed for this logic-only activity

        sessionManager = new SessionManager(getApplicationContext());
        requestQueue = Volley.newRequestQueue(getApplicationContext());

        // Use a handler to avoid a blank screen flash
        new Handler(Looper.getMainLooper()).postDelayed(this::decideNextActivity, 200);
    }

    private void decideNextActivity() {
        if (sessionManager.isLoggedIn()) {
            validateUserSession(sessionManager.getRepId());
        } else {
            goToLogin();
        }
    }

    private void validateUserSession(int repId) {
        String url = "https://representator.falconstationery.com/Api/validate_session.php";

        JSONObject requestBody = new JSONObject();
        try {
            requestBody.put("rep_id", repId);
        } catch (JSONException e) {
            Log.e(TAG, "Failed to create JSON for validation", e);
            goToLogin(); // Fail safe
            return;
        }

        JsonObjectRequest request = new JsonObjectRequest(Request.Method.POST, url, requestBody,
                response -> {
                    try {
                        String status = response.getString("status");
                        if ("success".equals(status)) {
                            // User is valid, proceed to splash screen
                            goToSplash();
                        } else {
                            // User is invalid (deleted from DB), log them out
                            Toast.makeText(this, "Your session has expired. Please log in again.", Toast.LENGTH_LONG).show();
                            sessionManager.logoutUser();
                            finish(); // Finish this activity
                        }
                    } catch (JSONException e) {
                        Log.e(TAG, "Validation response parsing error", e);
                        goToLogin(); // Fail safe
                    }
                },
                error -> {
                    // Network error. Can't validate. Let user proceed with cached data.
                    Log.w(TAG, "Could not validate session. Proceeding offline. Error: " + error.toString());
                    goToSplash();
                });

        requestQueue.add(request);
    }

    private void goToLogin() {
        startActivity(new Intent(this, LoginActivity.class));
        finish();
    }

    private void goToSplash() {
        startActivity(new Intent(this, SplashScreenActivity.class));
        finish();
    }
}