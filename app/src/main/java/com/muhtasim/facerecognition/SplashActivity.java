package com.muhtasim.facerecognition;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.splashscreen.SplashScreen;

@SuppressLint("CustomSplashScreen")
public class SplashActivity extends AppCompatActivity {

    int intentDecision;
    ConnectivityManager connectivityManager;
    NetworkInfo networkInfo;
    private boolean keepSplashOnScreen = true;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        /* Handle the splash screen transition */
        SplashScreen splashScreen = SplashScreen.installSplashScreen(this);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash);

        /* setKeepOnScreenCondition - Retrieve information until all information needed */
        splashScreen.setKeepOnScreenCondition(() -> keepSplashOnScreen);

        /* Check internet connection */
        connectivityManager = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        networkInfo = connectivityManager.getActiveNetworkInfo();
        if (networkInfo.isAvailable()) {
            intentDecision = 1;
        } else {
            intentDecision = 2;
        }
        moveToIntent();
    }

    protected void moveToIntent() {
        Intent goToIntent = null;
        if (intentDecision == 1) {
            /* Move to scanActivity for convocation */
            goToIntent = new Intent(SplashActivity.this, ConvoActivity.class);
        } else if (intentDecision == 2) {
            goToIntent = new Intent(SplashActivity.this, NoConnectionActivity.class);
        }
        keepSplashOnScreen = false;
        startActivity(goToIntent);
        finish();
    }
}