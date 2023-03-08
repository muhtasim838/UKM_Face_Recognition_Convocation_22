package com.muhtasim.facerecognition;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;

import com.muhtasim.facerecognition.utility.CommonUtility;
import com.muhtasim.facerecognition.utility.Constants;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class ConvoActivity extends AppCompatActivity {

    private static final String TAG = ConvoActivity.class.getSimpleName();
    List<Date> convoSidangSlots = new ArrayList<>();
    long backPressedTime;

    TextView tvConvoSidang;
    CardView cvScan;
    Toast backToast;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_convo);

        /* Generate the sidang slot and pass it to the rest of the activity */
        try {
            for (int k = 0; k < Constants.UKM_CONVO50_DATETIME_SLOTS.length; k++) {
                convoSidangSlots.add(new SimpleDateFormat("yyyy-MM-dd hh:mm:ss", Locale.US).parse(Constants.UKM_CONVO50_DATETIME_SLOTS[k]));
            }
        } catch (Exception e) {
            Log.e(TAG, "Error: " + e.getMessage());
            Log.e(TAG, "Cause of error: " + e.getCause());
        }

        /* Obtain the current sidang */
        Date currentDate = new Date();
        if (currentDate.after(convoSidangSlots.get(0)) && currentDate.before(convoSidangSlots.get(1))) {
            CommonUtility.currentSidang = 1;
        } else if (currentDate.after(convoSidangSlots.get(1)) && currentDate.before(convoSidangSlots.get(2))) {
            CommonUtility.currentSidang = 2;
        } else if (currentDate.after(convoSidangSlots.get(3)) && currentDate.before(convoSidangSlots.get(4))) {
            CommonUtility.currentSidang = 3;
        } else if (currentDate.after(convoSidangSlots.get(4)) && currentDate.before(convoSidangSlots.get(5))) {
            CommonUtility.currentSidang = 4;
        } else if (currentDate.after(convoSidangSlots.get(6)) && currentDate.before(convoSidangSlots.get(7))) {
            CommonUtility.currentSidang = 5;
        } else if (currentDate.after(convoSidangSlots.get(7)) && currentDate.before(convoSidangSlots.get(8))) {
            CommonUtility.currentSidang = 6;
        } else if (currentDate.after(convoSidangSlots.get(9)) && currentDate.before(convoSidangSlots.get(10))) {
            CommonUtility.currentSidang = 7;
        } else if (currentDate.after(convoSidangSlots.get(10)) && currentDate.before(convoSidangSlots.get(11))) {
            CommonUtility.currentSidang = 8;
        } else if (currentDate.after(convoSidangSlots.get(12)) && currentDate.before(convoSidangSlots.get(13))) {
            CommonUtility.currentSidang = 9;
        } else if (currentDate.after(convoSidangSlots.get(13)) && currentDate.before(convoSidangSlots.get(14))) {
            CommonUtility.currentSidang = 10;
        } else if (currentDate.after(convoSidangSlots.get(15)) && currentDate.before(convoSidangSlots.get(16))) {
            CommonUtility.currentSidang = 11;
        }

        cvScan = findViewById(R.id.cv_scan);
        tvConvoSidang = findViewById(R.id.tv_convo_sidang);

        if (CommonUtility.currentSidang <= 1) {
            /* Sidang Canselor */
            tvConvoSidang.setText(getResources().getString(R.string.txt_sidang_template).replace("[s]", getResources().getString(R.string.txt_canselor)));
        } else {
            tvConvoSidang.setText(getResources().getString(R.string.txt_sidang_template).replace("[s]", String.valueOf(CommonUtility.currentSidang)));
        }

        cvScan.setOnClickListener(view -> {
            Intent scanIntent = new Intent(ConvoActivity.this, ScanActivity.class);
            startActivity(scanIntent);
            finish();
        });
    }

    @Override
    public void onBackPressed() {
        if (backPressedTime + 3000 > System.currentTimeMillis()) {
            backToast.cancel();
            super.onBackPressed();
            return;
        } else {
            backToast = Toast.makeText(ConvoActivity.this, getResources().getString(R.string.txt_press_back_to_exit), Toast.LENGTH_SHORT);
            backToast.show();
        }
        backPressedTime = System.currentTimeMillis();
    }

}