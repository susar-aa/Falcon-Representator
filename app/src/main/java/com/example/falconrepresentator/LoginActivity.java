package com.example.falconrepresentator;

import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;

import org.json.JSONException;
import org.json.JSONObject;

public class LoginActivity extends AppCompatActivity {

    private static final String TAG = "LoginActivity";
    private EditText etUsername, etPassword;
    private Button btnLogin;
    private ProgressBar progressBar;
    private RequestQueue requestQueue;
    private SessionManager sessionManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        etUsername = findViewById(R.id.etUsername);
        etPassword = findViewById(R.id.etPassword);
        btnLogin = findViewById(R.id.btnLogin);
        progressBar = findViewById(R.id.loginProgressBar);

        requestQueue = Volley.newRequestQueue(this);
        sessionManager = new SessionManager(getApplicationContext());

        btnLogin.setOnClickListener(v -> {
            String username = etUsername.getText().toString().trim();
            String password = etPassword.getText().toString().trim();

            if (username.isEmpty() || password.isEmpty()) {
                Toast.makeText(this, "Please enter both username and password", Toast.LENGTH_SHORT).show();
            } else {
                performLogin(username, password);
            }
        });
    }

    // NEW: Helper method to check for an active internet connection.
    private boolean isNetworkAvailable() {
        ConnectivityManager connectivityManager
                = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        if (connectivityManager == null) {
            Log.w(TAG, "Couldn't get ConnectivityManager");
            return false;
        }
        NetworkInfo activeNetworkInfo = connectivityManager.getActiveNetworkInfo();
        return activeNetworkInfo != null && activeNetworkInfo.isConnected();
    }

    private void performLogin(String username, String password) {
        // NEW: Check for network before doing anything else.
        if (!isNetworkAvailable()) {
            Toast.makeText(this, "No Internet Connection. Please check your settings.", Toast.LENGTH_LONG).show();
            Log.e(TAG, "Login attempt failed: No network connection.");
            return;
        }

        progressBar.setVisibility(View.VISIBLE);
        btnLogin.setEnabled(false);

        String url = "https://representator.falconstationery.com/Api/login.php";

        JSONObject requestBody = new JSONObject();
        try {
            requestBody.put("username", username);
            requestBody.put("password", password);
        } catch (JSONException e) {
            Log.e(TAG, "Failed to create JSON body for login", e);
            progressBar.setVisibility(View.GONE);
            btnLogin.setEnabled(true);
            return;
        }

        // NEW: Log the request details before sending.
        Log.d(TAG, "Attempting to login...");
        Log.d(TAG, "URL: " + url);
        Log.d(TAG, "Request Body: " + requestBody.toString());


        JsonObjectRequest jsonObjectRequest = new JsonObjectRequest(Request.Method.POST, url, requestBody,
                response -> {
                    progressBar.setVisibility(View.GONE);
                    btnLogin.setEnabled(true);
                    // NEW: Log the raw server response.
                    Log.d(TAG, "Server Response: " + response.toString());
                    try {
                        String status = response.getString("status");
                        if ("success".equals(status)) {
                            int repId = response.getInt("rep_id");
                            String user = response.getString("username");
                            String fullName = response.getString("full_name");

                            sessionManager.createLoginSession(repId, user, fullName);

                            Toast.makeText(this, "Login Successful!", Toast.LENGTH_SHORT).show();
                            Intent intent = new Intent(LoginActivity.this, SplashScreenActivity.class);
                            startActivity(intent);
                            finish();
                        } else {
                            String message = response.getString("message");
                            Log.w(TAG, "Login failed on server: " + message);
                            Toast.makeText(this, message, Toast.LENGTH_LONG).show();
                        }
                    } catch (JSONException e) {
                        Log.e(TAG, "JSON parsing error: " + e.getMessage());
                        Toast.makeText(this, "An unexpected error occurred parsing the response.", Toast.LENGTH_LONG).show();
                    }
                },
                error -> {
                    progressBar.setVisibility(View.GONE);
                    btnLogin.setEnabled(true);
                    // NEW: More detailed error logging.
                    Log.e(TAG, "Volley network error during login", error);
                    Toast.makeText(this, "Login failed. Could not connect to the server.", Toast.LENGTH_LONG).show();
                });

        requestQueue.add(jsonObjectRequest);
    }
}
