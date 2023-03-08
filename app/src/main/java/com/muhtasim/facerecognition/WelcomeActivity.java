package com.muhtasim.facerecognition;

import android.annotation.SuppressLint;
import android.app.ProgressDialog;
import android.content.Intent;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.res.ResourcesCompat;

import com.bumptech.glide.Glide;
import com.muhtasim.facerecognition.utility.CommonUtility;
import com.muhtasim.facerecognition.utility.Constants;
import com.muhtasim.facerecognition.utility.WebService;

import org.json.JSONObject;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

@SuppressLint("CustomSplashScreen")
public class WelcomeActivity extends AppCompatActivity {

    private static final String TAG = WelcomeActivity.class.getSimpleName();
    String staffFullName, staffID, staffMatricNumber, staffCentre, staffPosition, convoSidang;

    ProgressDialog progressDialog;
    TextView tvClockingTime, tvStaffFullName, tvStaffMatricNumber, tvStaffPosition, tvStaffCentre, tvActivityName, tvClockingStatus;
    ImageView ivStaffPic;

    @SuppressLint("SetTextI18n")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_welcome);

        tvActivityName = findViewById(R.id.tv_activity_name);
        tvStaffFullName = findViewById(R.id.tv_staff_full_name);
        tvStaffMatricNumber = findViewById(R.id.tv_staff_matric_number);
        tvStaffPosition = findViewById(R.id.tv_staff_position);
        tvStaffCentre = findViewById(R.id.tv_staff_centre);
        tvClockingTime = findViewById(R.id.tv_clocking_time);
        tvClockingStatus = findViewById(R.id.tv_clocking_status);
        ivStaffPic = findViewById(R.id.iv_staff_pic);
        progressDialog = new ProgressDialog(WelcomeActivity.this, R.style.MyAlertDialogStyle);

        Intent intent = getIntent();
        staffFullName = intent.getStringExtra(Constants.EXTRA_STAFF_FULL_NAME);
        staffID = intent.getStringExtra(Constants.EXTRA_STAFF_ID);
        staffMatricNumber = intent.getStringExtra(Constants.EXTRA_STAFF_MATRIC_NUMBER);
        staffCentre = intent.getStringExtra(Constants.EXTRA_STAFF_CENTRE);
        staffPosition = intent.getStringExtra(Constants.EXTRA_STAFF_POSITION);

        Locale localeMalaysia = new Locale("ms", "MY", "MY");
        SimpleDateFormat malaysiaSdf = new SimpleDateFormat("dd MMMM yyyy hh:mm:ss a", localeMalaysia);

        int currentSidang;
        if (CommonUtility.currentSidang == 0) {
            currentSidang = 1;
        } else {
            currentSidang = CommonUtility.currentSidang;
        }

        convoSidang = getResources().getString(R.string.txt_convo_name_template);
        convoSidang = convoSidang.replace("[s]", String.valueOf(currentSidang));
        String textTarikhMasaMasukTemplate = getResources().getString(R.string.txt_tarikh_masa_masuk);

        if (staffMatricNumber.startsWith("k")) {
            /* Create visitation record */
            progressDialog.show();
            String urlRegisterLogPTM = Constants.API_LINK_CONVO_PTM + currentSidang + "&ukmper=" + staffMatricNumber;
            Log.e(TAG, "registerURLPTM: " + urlRegisterLogPTM);
            WebService.WebServiceResponseListener wsrl = resultResponse -> {
                /* Declare what to do once a result is obtained */
                try {
                    String status = resultResponse.getString(Constants.API_VAR_STATUS);
                    Log.e(TAG, status);
                    JSONObject dataObject = resultResponse.getJSONArray(Constants.API_VAR_DATA).getJSONObject(0);
                    String ukmper = dataObject.getString(Constants.FIELD_CONVO_LOG_UKMPER);
                    String namaPenuh = dataObject.getString(Constants.FIELD_CONVO_LOG_NAMA_PENUH);
                    String tarikhMasuk = dataObject.getString(Constants.FIELD_CONVO_LOG_TARIKH_MASUK);
                    String masaMasuk = dataObject.getString(Constants.FIELD_CONVO_LOG_MASA_MASUK);
                    String namaAktiviti = dataObject.getString(Constants.FIELD_CONVO_LOG_NAMA_AKTIVITI);
                    masaMasuk = masaMasuk.substring(0, 8);
                    String tarikhMasaMasuk = tarikhMasuk + " " + masaMasuk;
                    try {
                        SimpleDateFormat oriSdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US);
                        Date tarikhMasaMasukDateObj = oriSdf.parse(tarikhMasaMasuk);
                        assert tarikhMasaMasukDateObj != null;
                        String tarikhMasaMasukFinalString = malaysiaSdf.format(tarikhMasaMasukDateObj);
                        String textTarikhMasaMasuk = textTarikhMasaMasukTemplate.replace("[t]", tarikhMasaMasukFinalString.toUpperCase());
                        tvClockingTime.setText(textTarikhMasaMasuk);
                    } catch (ParseException e) {
                        e.printStackTrace();
                    }
                    tvActivityName.setText(namaAktiviti);
                    tvStaffFullName.setText(namaPenuh);
                    tvStaffMatricNumber.setText(ukmper);
                    tvClockingStatus.setText(status);
                    Glide.with(this).load(getImage(ukmper.toLowerCase())).fallback(R.drawable.user_fallback).error(R.drawable.user_fallback).centerCrop().into(ivStaffPic);
                    if (status.equalsIgnoreCase(Constants.VAL_CONVO_LOG_STATUS_BERJAYA)) {
                        tvClockingStatus.setBackground(ResourcesCompat.getDrawable(getResources(), R.drawable.bg_success, null));
                    } else if (status.equalsIgnoreCase(Constants.VAL_CONVO_LOG_STATUS_TELAH_WUJUD)) {
                        tvClockingStatus.setBackground(ResourcesCompat.getDrawable(getResources(), R.drawable.bg_warning, null));
                    }
                } catch (Exception e) {
                    Log.e(TAG, e.getLocalizedMessage());
                }
                progressDialog.dismiss();
            };
            WebService ws = new WebService(wsrl);
            ws.getRequest(WelcomeActivity.this, urlRegisterLogPTM);
        } else {
            tvStaffFullName.setText(staffFullName);
            tvActivityName.setText(convoSidang);
            Date currentDate = new Date();
            String currentDateString = malaysiaSdf.format(currentDate);
            String textTarikhMasaMasuk = textTarikhMasaMasukTemplate.replace("[t]", currentDateString.toUpperCase());
            tvClockingTime.setText(textTarikhMasaMasuk);
            tvStaffPosition.setVisibility(View.GONE);
            tvClockingStatus.setVisibility(View.GONE);
        }
        tvStaffCentre.setText(staffCentre);
        tvStaffPosition.setText(staffPosition);
        MediaPlayer mediaPlayer = MediaPlayer.create(WelcomeActivity.this, R.raw.welcome);
        mediaPlayer.start(); // no need to call prepare(); create() does that for you

        new Handler().postDelayed(() -> {
            Intent goToIntent = new Intent(WelcomeActivity.this, ConvoActivity.class);
            startActivity(goToIntent);
            finish();
        }, 8000);
    }

    @SuppressLint("DiscouragedApi")
    private int getImage(String imageName) {
        int drawableID = 0;
        try {
            drawableID = R.raw.class.getField(imageName).getInt(null);
        } catch (Exception e) {
            Log.e(TAG, e.getMessage());
        }
        return drawableID;
    }
}