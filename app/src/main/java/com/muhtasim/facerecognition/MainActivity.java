package com.muhtasim.facerecognition;

import android.content.Intent;
import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;

import com.muhtasim.facerecognition.utility.Constants;

public class MainActivity extends AppCompatActivity {

    CardView cvStaff, cvGuest;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        cvStaff = findViewById(R.id.cv_staff);
        cvGuest = findViewById(R.id.cv_guest);

        cvStaff.setOnClickListener(view -> scanFace(Constants.VAL_TYPE_STAFF));
        cvGuest.setOnClickListener(view -> scanFace(Constants.VAL_TYPE_GUEST));
    }

    private void scanFace(String userType) {
        Intent scanIntent = new Intent(MainActivity.this, ScanActivity.class);
        scanIntent.putExtra(Constants.EXTRA_USER_TYPE, userType);
        startActivity(scanIntent);
    }

}

