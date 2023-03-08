package com.muhtasim.facerecognition.utility;

import android.annotation.SuppressLint;
import android.content.Context;
import android.text.Editable;
import android.text.TextWatcher;
import android.widget.EditText;
import android.widget.TextView;

public class DynamicTextWatcher implements TextWatcher {

    private final EditText inputEditText;
    private final TextView outputTextView;
    private final Context context;

    public DynamicTextWatcher(Context con, EditText iet, TextView otv) {
        this.context = con;
        this.inputEditText = iet;
        this.outputTextView = otv;
    }

    @Override
    public void beforeTextChanged(CharSequence s, int start, int count, int after) {

    }

    @SuppressLint("NonConstantResourceId")
    @Override
    public void onTextChanged(CharSequence s, int start, int before, int count) {
        int inputEtId = inputEditText.getId();
//        if (inputEtId == R.id.et_case_title) {
//            outputTextView.setText(context.getResources().getString(R.string.txt_counter_120_zero).replace("n", String.valueOf(s.length())));
//        } else if (inputEtId == R.id.et_case_description || inputEtId == R.id.et_community_name || inputEtId == R.id.et_community_email || inputEtId == R.id.et_community_address) {
//            outputTextView.setText(context.getResources().getString(R.string.txt_counter_240_zero).replace("n", String.valueOf(s.length())));
//        } else if (inputEtId == R.id.et_community_phone_number) {
//            outputTextView.setText(context.getResources().getString(R.string.txt_counter_20_zero).replace("n", String.valueOf(s.length())));
//        }
    }

    @Override
    public void afterTextChanged(Editable s) {

    }

}
