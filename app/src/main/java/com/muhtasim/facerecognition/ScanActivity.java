package com.muhtasim.facerecognition;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.res.AssetFileDescriptor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.ImageFormat;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.Typeface;
import android.graphics.YuvImage;
import android.media.Image;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.ParcelFileDescriptor;
import android.util.Log;
import android.util.Pair;
import android.util.Size;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.camera.core.CameraSelector;
import androidx.camera.core.ImageAnalysis;
import androidx.camera.core.Preview;
import androidx.camera.lifecycle.ProcessCameraProvider;
import androidx.camera.view.PreviewView;
import androidx.core.content.ContextCompat;

import com.google.common.util.concurrent.ListenableFuture;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.google.mlkit.vision.common.InputImage;
import com.google.mlkit.vision.face.Face;
import com.google.mlkit.vision.face.FaceDetection;
import com.google.mlkit.vision.face.FaceDetector;
import com.google.mlkit.vision.face.FaceDetectorOptions;
import com.muhtasim.facerecognition.utility.Constants;
import com.muhtasim.facerecognition.utility.DelayClass;
import com.muhtasim.facerecognition.utility.Staff;
import com.muhtasim.facerecognition.utility.WebService;
import com.muhtasim.facerecognition.utility.users;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.tensorflow.lite.Interpreter;

import java.io.ByteArrayOutputStream;
import java.io.FileDescriptor;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.MappedByteBuffer;
import java.nio.ReadOnlyBufferException;
import java.nio.channels.FileChannel;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

public class ScanActivity extends AppCompatActivity {

    private static final String TAG = ScanActivity.class.getSimpleName();
    private static final int MY_CAMERA_REQUEST_CODE = 100;
    private static final int SELECT_PICTURE = 1;
    ArrayList<Staff> staffWithFaceArrayList, staffWithoutFaceArrayList;
    ArrayList<String> userSpinnerListName;
    boolean start = true, flipX = false;
    int cam_face = CameraSelector.LENS_FACING_FRONT; /* Front facing camera */
    int[] intValues;
    int inputSize = 112;  //Input size for model
    boolean isModelQuantized = false;
    float[][] embeddings;
    float IMAGE_MEAN = 128.0f;
    float IMAGE_STD = 128.0f;
    int OUTPUT_SIZE = 192; //Output size of model
    String modelFile = "mobile_face_net.tflite"; //model name
    Staff selectedStaff;
    ProgressDialog progressDialog;
    FirebaseFirestore db;
    FaceDetector detector;
    PreviewView pvPreview;
    LinearLayout lLRecogniseFace, llTrainingFace;
    ImageView ivFacePreview;
    Interpreter tfLite;
    TextView tvResult, tvInstruction, tvStaffCentre, tvStaffPosition;
    Button btnAction;
    ImageButton btnAddFace, btnSwitchCamera, btnActions;
    CameraSelector cameraSelector;
    Context context = ScanActivity.this;
    ProcessCameraProvider cameraProvider;
    private ListenableFuture<ProcessCameraProvider> cameraProviderFuture;
    private HashMap<String, SimilarityClassifier.Recognition> registered = new HashMap<>(); //saved temporarily

    @SuppressLint("SimpleDateFormat")
    public static String getCurrentTimestamp() {
        return new SimpleDateFormat("dd/MM/yyyy hh:mm:ss aa").format(Calendar.getInstance().getTime());
    }

    private static Bitmap getCropBitmapByCPU(Bitmap source, RectF cropRectF) {
        Bitmap resultBitmap = Bitmap.createBitmap((int) cropRectF.width(), (int) cropRectF.height(), Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(resultBitmap);

        // draw background
        Paint paint = new Paint(Paint.FILTER_BITMAP_FLAG);
        paint.setColor(Color.WHITE);
        canvas.drawRect(//from  w w  w. ja v  a  2s. c  om
                new RectF(0, 0, cropRectF.width(), cropRectF.height()),
                paint);

        Matrix matrix = new Matrix();
        matrix.postTranslate(-cropRectF.left, -cropRectF.top);

        canvas.drawBitmap(source, matrix, paint);

        if (source != null && !source.isRecycled()) {
            source.recycle();
        }

        return resultBitmap;
    }

    private static Bitmap rotateBitmap(Bitmap bitmap, int rotationDegrees, boolean flipX) {
        Matrix matrix = new Matrix();

        // Rotate the image back to straight.
        matrix.postRotate(rotationDegrees);

        // Mirror the image along the X or Y axis.
        matrix.postScale(flipX ? -1.0f : 1.0f, 1.0f);
        Bitmap rotatedBitmap = Bitmap.createBitmap(bitmap, 0, 0, bitmap.getWidth(), bitmap.getHeight(), matrix, true);

        // Recycle the old bitmap if it has changed.
        if (rotatedBitmap != bitmap) {
            bitmap.recycle();
        }

        return rotatedBitmap;
    }

    //IMPORTANT. If conversion not done ,the toBitmap conversion does not work on some devices.
    private static byte[] YUV_420_888toNV21(Image image) {

        int width = image.getWidth();
        int height = image.getHeight();
        int ySize = width * height;
        int uvSize = width * height / 4;

        byte[] nv21 = new byte[ySize + uvSize * 2];

        ByteBuffer yBuffer = image.getPlanes()[0].getBuffer(); // Y
        ByteBuffer uBuffer = image.getPlanes()[1].getBuffer(); // U
        ByteBuffer vBuffer = image.getPlanes()[2].getBuffer(); // V

        int rowStride = image.getPlanes()[0].getRowStride();
        assert (image.getPlanes()[0].getPixelStride() == 1);

        int pos = 0;

        if (rowStride == width) { // likely
            yBuffer.get(nv21, 0, ySize);
            pos += ySize;
        } else {
            long yBufferPos = -rowStride; // not an actual position
            for (; pos < ySize; pos += width) {
                yBufferPos += rowStride;
                yBuffer.position((int) yBufferPos);
                yBuffer.get(nv21, pos, width);
            }
        }

        rowStride = image.getPlanes()[2].getRowStride();
        int pixelStride = image.getPlanes()[2].getPixelStride();

        assert (rowStride == image.getPlanes()[1].getRowStride());
        assert (pixelStride == image.getPlanes()[1].getPixelStride());

        if (pixelStride == 2 && rowStride == width && uBuffer.get(0) == vBuffer.get(1)) {
            // maybe V an U planes overlap as per NV21, which means vBuffer[1] is alias of uBuffer[0]
            byte savePixel = vBuffer.get(1);
            try {
                vBuffer.put(1, (byte) ~savePixel);
                if (uBuffer.get(0) == (byte) ~savePixel) {
                    vBuffer.put(1, savePixel);
                    vBuffer.position(0);
                    uBuffer.position(0);
                    vBuffer.get(nv21, ySize, 1);
                    uBuffer.get(nv21, ySize + 1, uBuffer.remaining());

                    return nv21; // shortcut
                }
            } catch (ReadOnlyBufferException ex) {
                // unfortunately, we cannot check if vBuffer and uBuffer overlap
            }

            // unfortunately, the check failed. We must save U and V pixel by pixel
            vBuffer.put(1, savePixel);
        }

        // other optimizations could check if (pixelStride == 1) or (pixelStride == 2),
        // but performance gain would be less significant

        for (int row = 0; row < height / 2; row++) {
            for (int col = 0; col < width / 2; col++) {
                int vuPos = col * pixelStride + row * rowStride;
                nv21[pos++] = vBuffer.get(vuPos);
                nv21[pos++] = uBuffer.get(vuPos);
            }
        }

        return nv21;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_scan);

        /* Retrieve staff on first load */
        retrieveStaffList(true);

        registered = readFromSP(); //Load saved faces from memory when app starts
        db = FirebaseFirestore.getInstance();

        lLRecogniseFace = findViewById(R.id.ll_recognise_face);
        llTrainingFace = findViewById(R.id.ll_training_face);
        ivFacePreview = findViewById(R.id.iv_face_preview);
        tvResult = findViewById(R.id.tv_result);
        tvInstruction = findViewById(R.id.tv_instruction);
        tvStaffCentre = findViewById(R.id.tv_staff_centre);
        tvStaffPosition = findViewById(R.id.tv_staff_position);
        btnAddFace = findViewById(R.id.btn_add_face);
        btnAction = findViewById(R.id.btn_action);
        btnSwitchCamera = findViewById(R.id.btn_switch_camera);
        btnActions = findViewById(R.id.btn_actions);
        progressDialog = new ProgressDialog(context, R.style.MyAlertDialogStyle);

        //Camera Permission
        if (checkSelfPermission(Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{Manifest.permission.CAMERA}, MY_CAMERA_REQUEST_CODE);
        }
        //On-screen Action Button
        btnActions.setOnClickListener(v -> {
            AlertDialog.Builder builder = new AlertDialog.Builder(context);
            builder.setTitle("Select Action:");

            // add a checkbox list
            String[] names = {"View Recognition List", "Update Recognition List", "Save Recognitions", "Load Recognitions", "Clear All Recognitions", "Import Photo (Beta)"};

            builder.setItems(names, (dialog, which) -> {

                switch (which) {
                    case 0:
                        displayNameListView();
                        break;
                    case 1:
                        updateNameListView();
                        break;
                    case 2:
                        insertToSP(registered, false);
                        break;
                    case 3:
                        registered.putAll(readFromSP());
                        break;
                    case 4:
                        clearNameList();
                        break;
                    case 5:
                        loadPhoto();
                        break;
                }

            });


            builder.setPositiveButton("OK", (dialog, which) -> {

            });
            builder.setNegativeButton("Cancel", null);

            // create and show the alert dialog
            AlertDialog dialog = builder.create();
            dialog.show();
        });

        //On-screen switch to toggle between Cameras.
        btnSwitchCamera.setOnClickListener(v -> {
            if (cam_face == CameraSelector.LENS_FACING_BACK) {
                cam_face = CameraSelector.LENS_FACING_FRONT;
                flipX = true;
            } else {
                cam_face = CameraSelector.LENS_FACING_BACK;
                flipX = false;
            }
            cameraProvider.unbindAll();
            cameraBind();
        });

        btnAddFace.setOnClickListener((v -> addFace()));

        btnAction.setOnClickListener(v -> {
            if (btnAction.getText().toString().equals(getResources().getString(R.string.txt_recognize))) {
                start = true;
                btnAction.setText(getResources().getString(R.string.txt_register));
                lLRecogniseFace.setVisibility(View.VISIBLE);
                llTrainingFace.setVisibility(View.GONE);
            } else {
                btnAction.setText(getResources().getString(R.string.txt_recognize));
                lLRecogniseFace.setVisibility(View.GONE);
                llTrainingFace.setVisibility(View.VISIBLE);
            }
        });

        //Load model
        try {
            tfLite = new Interpreter(loadModelFile(ScanActivity.this, modelFile));
        } catch (IOException e) {
            e.printStackTrace();
        }
        //Initialize Face Detector
        FaceDetectorOptions highAccuracyOpts =
                new FaceDetectorOptions.Builder()
                        .setPerformanceMode(FaceDetectorOptions.PERFORMANCE_MODE_ACCURATE)
                        .build();
        detector = FaceDetection.getClient(highAccuracyOpts);

        cameraBind();


    }

    private void addFace() {
        start = false;

        AlertDialog.Builder mBuilder = new AlertDialog.Builder(context);
        View mView = getLayoutInflater().inflate(R.layout.dialog_spinner, null);
        mBuilder.setTitle("Please Select Your Name");
        Spinner mSpinner = mView.findViewById(R.id.spinner);

        ArrayAdapter<String> adapter = new ArrayAdapter<>(context, android.R.layout.simple_spinner_item, userSpinnerListName);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        mSpinner.setAdapter(adapter);
        mSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> adapterView, View view, int position, long l) {
                selectedStaff = staffWithoutFaceArrayList.get(position);
                Log.e(TAG, selectedStaff.getStaffID() + " " + position);
            }

            @Override
            public void onNothingSelected(AdapterView<?> adapterView) {
                selectedStaff = null;
            }
        });

        mBuilder.setPositiveButton("Ok", (dialog, which) -> {
            Log.e(TAG, "Selected staff ID: " + selectedStaff.getStaffID() + ", matric number: " + selectedStaff.getStaffMatricNumber() + ", name: " + selectedStaff.getStaffName());

            Toast.makeText(context, "Training on progress " + selectedStaff.getStaffName(), Toast.LENGTH_SHORT).show();
            Toast.makeText(context, "Added " + selectedStaff.getStaffName(), Toast.LENGTH_SHORT).show();
            String urlRegisterFace = Constants.API_LINK + "convostaff/regface.php?staffID=" + selectedStaff.getStaffID();
            Log.e(TAG, "Register face URL: " + urlRegisterFace);

            WebService.WebServiceResponseListener wsrlVH = resultResponseVH -> {
                /* Declare what to do once a result is obtained */
                try {
                    boolean success = resultResponseVH.getBoolean(Constants.API_VAR_SUCCESS);
                    if (success) {
                        /* If success, we display the message, and then go back to LoginActivity */
                        Toast.makeText(ScanActivity.this, "Success", Toast.LENGTH_SHORT).show();
                        retrieveStaffList(false);
                    } else {
                        /* If fail, we just log the message for developer to look. For user just show an error has occurred */
                        Toast.makeText(ScanActivity.this, "Failed", Toast.LENGTH_SHORT).show();
                        String errorCode = resultResponseVH.getString(Constants.API_VAR_ERROR);
                        Log.e(TAG, errorCode);
                    }
                } catch (Exception e) {
                    Log.e(TAG, e.getLocalizedMessage());
                }
                progressDialog.dismiss();
            };
            WebService ws = new WebService(wsrlVH);
            ws.getRequest(ScanActivity.this, urlRegisterFace);

            //Create and Initialize new object with Face embeddings and Name.
            SimilarityClassifier.Recognition result = new SimilarityClassifier.Recognition(
                    "0", "", -1f);
            result.setExtra(embeddings);

            users user = new users("0", "", -1f);
            user.setExtra(Arrays.deepToString(embeddings));
            //get timestamp
            String time = getCurrentTimestamp();
            user.setTime(time);
            Log.e(TAG, "currentTIme" + time);

            Log.e(TAG, "embeddings" + Arrays.deepToString(embeddings));
            user.setTitle(mSpinner.getSelectedItem().toString());
            db.collection("users")
                    .add(user)
                    .addOnSuccessListener(documentReference -> Log.e(TAG, "DocumentSnapshot written with ID: " + documentReference.getId()))
                    .addOnFailureListener(e -> Log.w(TAG, "Error adding document", e));


            registered.put(mSpinner.getSelectedItem().toString() + " [" + selectedStaff.getStaffID() + "] ", result);
            insertToSP(registered, false);
            registered.putAll(readFromSP());
            start = true;
            dialog.cancel();

            /* Generate UID */
            db.collection("users")
                    .whereEqualTo("id", selectedStaff.getStaffID())
                    .get()
                    .addOnCompleteListener(task -> {
                        if (task.isSuccessful()) {
                            for (QueryDocumentSnapshot document : task.getResult()) {
                                Log.e(TAG, document.getId() + " => " + document.getData());
                                Log.e(TAG, "FireStoreID " + document.getId());
                            }
                        } else {
                            Log.e(TAG, "Error getting documents: ", task.getException());
                        }
                    });
        });
        mBuilder.setNegativeButton("Cancel", (dialog, which) -> dialog.cancel());
        mBuilder.setView(mView);
        AlertDialog dialog = mBuilder.create();
        dialog.show();
    }

    private void clearNameList() {
        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        builder.setTitle("Do you want to delete all Recognitions?");
        builder.setPositiveButton("Delete All", (dialog, which) -> {
            registered.clear();
            Toast.makeText(context, "Recognitions Cleared", Toast.LENGTH_SHORT).show();
        });
        insertToSP(registered, true);
        builder.setNegativeButton("Cancel", null);
        AlertDialog dialog = builder.create();
        dialog.show();
    }

    private void updateNameListView() {
        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        if (registered.isEmpty()) {
            builder.setTitle("No Faces Added!!");
            builder.setPositiveButton("OK", null);
        } else {
            builder.setTitle("Select Recognition to delete:");

            // add a checkbox list
            String[] names = new String[registered.size()];
            boolean[] checkedItems = new boolean[registered.size()];
            int i = 0;
            for (Map.Entry<String, SimilarityClassifier.Recognition> entry : registered.entrySet()) {
                Log.e(TAG, "NAMESelected" + entry.getKey());
                names[i] = entry.getKey();
                checkedItems[i] = true;
                i = i + 1;

            }

            builder.setMultiChoiceItems(names, checkedItems, (dialog, which, isChecked) -> checkedItems[which] = isChecked);


            builder.setPositiveButton("OK", (dialog, which) -> {

                // Log.e(TAG, "status:"+ Arrays.toString(checkedItems));
                for (int i1 = 0; i1 < checkedItems.length; i1++) {
                    Log.e(TAG, "statusCheckItem:" + checkedItems[i1]);
                    if (checkedItems[i1]) {
                        Toast.makeText(context, names[i1], Toast.LENGTH_SHORT).show();
                        registered.remove(names[i1]);
                    }

                }
                Toast.makeText(context, "Recognitions Updated", Toast.LENGTH_SHORT).show();
            });
            builder.setNegativeButton("Cancel", null);

            // create and show the alert dialog
            AlertDialog dialog = builder.create();
            dialog.show();
        }
    }

    private void retrieveStaffList(boolean isFirstLoad) {
        /* Get Staff Names */
        String url = Constants.API_LINK + "convostaff/read.php";
        Log.e(TAG, "retrieveStaffList call: " + url + " " + isFirstLoad);
        if (isFirstLoad) {
            staffWithFaceArrayList = new ArrayList<>();
            staffWithoutFaceArrayList = new ArrayList<>();
        } else {
            staffWithFaceArrayList.clear();
            staffWithoutFaceArrayList.clear();
        }
        userSpinnerListName = new ArrayList<>();
        WebService.WebServiceResponseListener wsrl = resultResponse -> {
            try {
                boolean success = resultResponse.getBoolean(Constants.API_VAR_SUCCESS);
                if (success) {
                    JSONArray staffJSONArray = resultResponse.getJSONArray(Constants.API_VAR_DATA);
                    for (int i = 0; i < staffJSONArray.length(); i++) {
                        JSONObject currentStaffJSONObject = staffJSONArray.getJSONObject(i);
                        String currentStaffID, currentStaffIsRegisteredWithFace, currentStaffFullName, currentStaffMatricNumber, currentStaffCentre, currentStaffPosition;
                        /* Get the staffID, full name and matric number */
                        currentStaffID = currentStaffJSONObject.getString(Constants.FIELD_STAFF_ID);
                        currentStaffFullName = currentStaffJSONObject.getString(Constants.FIELD_STAFF_FULL_NAME);
                        currentStaffIsRegisteredWithFace = currentStaffJSONObject.getString(Constants.FIELD_STAFF_IS_REGISTERED_WITH_FACE);
                        currentStaffMatricNumber = currentStaffJSONObject.getString(Constants.FIELD_STAFF_MATRIC_NUMBER);
                        currentStaffCentre = currentStaffJSONObject.getString(Constants.FIELD_STAFF_CENTRE);
                        currentStaffPosition = currentStaffJSONObject.getString(Constants.FIELD_STAFF_POSITION);
                        Staff currentStaff = new Staff(currentStaffID, currentStaffFullName, currentStaffMatricNumber, currentStaffCentre, currentStaffPosition);
                        if (currentStaffIsRegisteredWithFace.equalsIgnoreCase("Y")) {
                            staffWithFaceArrayList.add(currentStaff);
                        } else {
                            staffWithoutFaceArrayList.add(currentStaff);
                            userSpinnerListName.add(currentStaffFullName);
                        }
                    }
                } else {
                    /* Show error */
                    String failedToLoadMessage = getResources().getString(R.string.txt_failed_to_load);
                    failedToLoadMessage = failedToLoadMessage.replace("[o]", "staff");
                    /* Show no data */
                    Toast.makeText(context, failedToLoadMessage, Toast.LENGTH_SHORT).show();
                }
            } catch (JSONException jsonException) {
                Toast.makeText(context, getResources().getString(R.string.txt_error), Toast.LENGTH_SHORT).show();
                jsonException.printStackTrace();
            }
        };
        WebService ws = new WebService(wsrl);
        ws.getRequest(context, url);
    }

    private void getUsersDetailsForVisiting(String recognizedFace) {
        Log.e(TAG, "recognizedFaceName: " + recognizedFace);
        int indexOfStupidBracket = recognizedFace.indexOf("[");
        int indexOfStupidEndingBracket = recognizedFace.indexOf("]");
        String recognizedFaceStaffID = recognizedFace.substring(indexOfStupidBracket + 1, indexOfStupidEndingBracket);
        Log.e(TAG, "recognizedFaceStaffID: " + recognizedFaceStaffID);
        for (int j = 0; j < staffWithFaceArrayList.size(); j++) {
            Staff currentStaff = staffWithFaceArrayList.get(j);
            if (currentStaff.getStaffID().equals(recognizedFaceStaffID)) {
                Log.e(TAG, "Found staff: " + currentStaff.getStaffName() + " (ID: " + currentStaff.getStaffID() + "; Matric: " + currentStaff.getStaffMatricNumber() + ")");

                users user = new users(currentStaff.getStaffID(), currentStaff.getStaffName(), -1f);
                String time = getCurrentTimestamp();
                user.setTime(time);

                db.collection("Visiting_history")
                        .add(user)
                        .addOnSuccessListener(documentReference -> Log.e(TAG, "DocumentSnapshot written with ID: " + documentReference.getId()))
                        .addOnFailureListener(e -> Log.w(TAG, "Error adding document", e));

                tvInstruction.setTypeface(tvInstruction.getTypeface(), Typeface.BOLD);
                tvStaffCentre.setVisibility(View.VISIBLE);
                tvStaffPosition.setVisibility(View.VISIBLE);
                tvStaffCentre.setText(currentStaff.getStaffCentre());
                tvStaffPosition.setText(currentStaff.getStaffPosition());

                // Do something after delay
                new Handler().postDelayed(() -> {
                    Intent intent = new Intent(context, WelcomeActivity.class);
                    intent.putExtra(Constants.EXTRA_STAFF_FULL_NAME, currentStaff.getStaffName());
                    intent.putExtra(Constants.EXTRA_STAFF_ID, currentStaff.getStaffID());
                    intent.putExtra(Constants.EXTRA_STAFF_MATRIC_NUMBER, currentStaff.getStaffMatricNumber());
                    intent.putExtra(Constants.EXTRA_STAFF_CENTRE, currentStaff.getStaffCentre());
                    intent.putExtra(Constants.EXTRA_STAFF_POSITION, currentStaff.getStaffPosition());
                    startActivity(intent);
                    finish();
                }, 1000);
            }
        }
    }

    private void displayNameListView() {
        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        // Log.e(TAG, "Registered"+registered);
        if (registered.isEmpty())
            builder.setTitle("No Faces Added!!");
        else
            builder.setTitle("Recognitions:");

        // add a checkbox list
        String[] names = new String[registered.size()];
        boolean[] checkedItems = new boolean[registered.size()];
        int i = 0;
        for (Map.Entry<String, SimilarityClassifier.Recognition> entry : registered.entrySet()) {
            names[i] = entry.getKey();
            checkedItems[i] = true;
            i = i + 1;
        }
        builder.setItems(names, null);
        builder.setPositiveButton("OK", (dialog, which) -> {

        });

        Log.e(TAG, Arrays.toString(checkedItems));

        // create and show the alert dialog
        AlertDialog dialog = builder.create();
        dialog.show();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == MY_CAMERA_REQUEST_CODE) {
            if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(this, "camera permission granted", Toast.LENGTH_LONG).show();
            } else {
                Toast.makeText(this, "camera permission denied", Toast.LENGTH_LONG).show();
            }
        }
    }

    private MappedByteBuffer loadModelFile(Activity activity, String MODEL_FILE) throws IOException {
        AssetFileDescriptor fileDescriptor = activity.getAssets().openFd(MODEL_FILE);
        FileInputStream inputStream = new FileInputStream(fileDescriptor.getFileDescriptor());
        FileChannel fileChannel = inputStream.getChannel();
        long startOffset = fileDescriptor.getStartOffset();
        long declaredLength = fileDescriptor.getDeclaredLength();
        return fileChannel.map(FileChannel.MapMode.READ_ONLY, startOffset, declaredLength);
    }

    //Bind camera and preview view
    private void cameraBind() {
        cameraProviderFuture = ProcessCameraProvider.getInstance(this);

        pvPreview = findViewById(R.id.pv_preview);
        cameraProviderFuture.addListener(() -> {
            try {
                cameraProvider = cameraProviderFuture.get();

                bindPreview(cameraProvider);
            } catch (ExecutionException | InterruptedException e) {
                // No errors need to be handled for this in Future.
                // This should never be reached.
            }
        }, ContextCompat.getMainExecutor(this));
    }

    void bindPreview(@NonNull ProcessCameraProvider cameraProvider) {
        Preview preview = new Preview.Builder()
                .build();

        cameraSelector = new CameraSelector.Builder()
                .requireLensFacing(cam_face)
                .build();

        preview.setSurfaceProvider(pvPreview.getSurfaceProvider());
        ImageAnalysis imageAnalysis =
                new ImageAnalysis.Builder()
                        .setTargetResolution(new Size(640, 480))
                        .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST) //Latest frame is shown
                        .build();

        Executor executor = Executors.newSingleThreadExecutor();


        imageAnalysis.setAnalyzer(executor, imageProxy -> {

            InputImage image = null;


            @SuppressLint({"UnsafeExperimentalUsageError", "UnsafeOptInUsageError"})
            // Camera Feed-->Analyzer-->ImageProxy-->mediaImage-->InputImage(needed for ML kit face detection)

            Image mediaImage = imageProxy.getImage();

            if (mediaImage != null) {
                image = InputImage.fromMediaImage(mediaImage, imageProxy.getImageInfo().getRotationDegrees());
                Log.e(TAG, "Rotation " + imageProxy.getImageInfo().getRotationDegrees());
            }

            Log.e(TAG, "ANALYSIS");

            //Process acquired image to detect faces
            assert image != null;
            detector.process(image)
                    .addOnSuccessListener(
                            faces -> {

                                if (faces.size() != 0) {
                                    Face face = faces.get(0); //Get first face from detected faces
                                    Log.e(TAG, "face" + face);

                                    //mediaImage to Bitmap
                                    Bitmap frame_bmp = toBitmap(mediaImage);

                                    int rot = imageProxy.getImageInfo().getRotationDegrees();

                                    //Adjust orientation of Face
                                    Bitmap frame_bmp1 = rotateBitmap(frame_bmp, rot, false);


                                    //Get bounding box of face
                                    RectF boundingBox = new RectF(face.getBoundingBox());

                                    //Crop out bounding box from whole Bitmap(image)
                                    Bitmap cropped_face = getCropBitmapByCPU(frame_bmp1, boundingBox);

                                    if (flipX)
                                        cropped_face = rotateBitmap(cropped_face, 0, true);
                                    //Scale the acquired Face to 112*112 which is required input for model
                                    Bitmap scaled = getResizedBitmap(cropped_face, 112, 112);

                                    if (start)
                                        recognizeImage(scaled); //Send scaled bitmap to create face embeddings.
                                    Log.e(TAG, "boundingBox" + boundingBox);
                                    try {
                                        Thread.sleep(10);  //Camera preview refreshed every 10 milliseconds (adjust as required)
                                    } catch (InterruptedException e) {
                                        e.printStackTrace();
                                    }
                                } else {
                                    if (registered.isEmpty())
                                        tvResult.setText(getResources().getString(R.string.txt_no_faces_trained));
                                    else
                                        tvResult.setText(getResources().getString(R.string.txt_no_face_detected));
                                }
                            })
                    .addOnFailureListener(
                            e -> Log.e(TAG, e.getMessage()))
                    .addOnCompleteListener(task -> {

                        int secs = 7; // Delay in seconds

                        // Do something after delay
                        DelayClass.delay(secs, imageProxy::close);

                        //v.important to acquire next frame for analysis
                    });


        });


        cameraProvider.bindToLifecycle(this, cameraSelector, imageAnalysis, preview);
    }

    public void recognizeImage(final Bitmap bitmap) {

        // set Face to Preview
        ivFacePreview.setImageBitmap(bitmap);

        //Create ByteBuffer to store normalized image

        ByteBuffer imgData = ByteBuffer.allocateDirect(inputSize * inputSize * 3 * 4);


        imgData.order(ByteOrder.nativeOrder());

        intValues = new int[inputSize * inputSize];

        //get pixel values from Bitmap to normalize
        bitmap.getPixels(intValues, 0, bitmap.getWidth(), 0, 0, bitmap.getWidth(), bitmap.getHeight());

        imgData.rewind();

        for (int i = 0; i < inputSize; ++i) {
            for (int j = 0; j < inputSize; ++j) {
                int pixelValue = intValues[i * inputSize + j];
                if (isModelQuantized) {
                    // Quantized model
                    imgData.put((byte) ((pixelValue >> 16) & 0xFF));
                    imgData.put((byte) ((pixelValue >> 8) & 0xFF));
                    imgData.put((byte) (pixelValue & 0xFF));
                } else { // Float model
                    imgData.putFloat((((pixelValue >> 16) & 0xFF) - IMAGE_MEAN) / IMAGE_STD);
                    imgData.putFloat((((pixelValue >> 8) & 0xFF) - IMAGE_MEAN) / IMAGE_STD);
                    imgData.putFloat(((pixelValue & 0xFF) - IMAGE_MEAN) / IMAGE_STD);

                }
            }
        }
        //imgData is input to our model
        Object[] inputArray = {imgData};


        Map<Integer, Object> outputMap = new HashMap<>();


        embeddings = new float[1][OUTPUT_SIZE]; //output of model will be stored in this variable


        Log.e(TAG, "embeddings" + Arrays.deepToString(embeddings));

        outputMap.put(0, embeddings);

        tfLite.runForMultipleInputsOutputs(inputArray, outputMap); //Run model


        float distance;

        //Compare new face with saved Faces.

        if (registered.size() > 0) {

            final Pair<String, Float> nearest = findNearest(embeddings[0]);//Find closest matching face

            if (nearest != null) {

                final String name = nearest.first;
                distance = nearest.second;
                if (distance < 1.000f) {//If distance between Closest found face is more than 1.000 ,then output UNKNOWN face.
                    int secs = 4; // Delay in seconds

                    DelayClass.delay(secs, () -> {
                        users user = new users("0", "", -1f);
                        String time = getCurrentTimestamp();
                        user.setTime(time);
                        user.setTitle(name);

                        btnAction.setVisibility(View.GONE);
                        llTrainingFace.setVisibility(View.GONE);
                        tvInstruction.setText(getResources().getString(R.string.txt_recognized_face));
                        int indexOfStupidBracket = name.indexOf("[");
                        String trimmedName = name.substring(0, indexOfStupidBracket - 1);
                        tvResult.setText(trimmedName);
                        Toast.makeText(context, "Face Detected", Toast.LENGTH_SHORT).show();
                        Toast.makeText(context, "Done", Toast.LENGTH_SHORT).show();

                        // Call this method directly from java file
                        String recognizedFace = user.getTitle();
                        getUsersDetailsForVisiting(recognizedFace);
                    });
                } else {
                    tvResult.setText(getResources().getString(R.string.txt_face_unable_to_recognize));
                }
                Log.e(TAG, "nearest: " + name + " - distance: " + distance);
            }
        }

    }

    //Compare Faces by distance between face embeddings
    private Pair<String, Float> findNearest(float[] emb) {

        Pair<String, Float> ret = null;
        for (Map.Entry<String, SimilarityClassifier.Recognition> entry : registered.entrySet()) {

            final String name = entry.getKey();
            final float[] knownEmb = ((float[][]) entry.getValue().getExtra())[0];

            float distance = 0;
            for (int i = 0; i < emb.length; i++) {
                float diff = emb[i] - knownEmb[i];
                distance += diff * diff;
            }
            distance = (float) Math.sqrt(distance);
            if (ret == null || distance < ret.second) {
                ret = new Pair<>(name, distance);
            }
        }

        return ret;

    }

    public Bitmap getResizedBitmap(Bitmap bm, int newWidth, int newHeight) {
        int width = bm.getWidth();
        int height = bm.getHeight();
        float scaleWidth = ((float) newWidth) / width;
        float scaleHeight = ((float) newHeight) / height;
        // CREATE A MATRIX FOR THE MANIPULATION
        Matrix matrix = new Matrix();
        // RESIZE THE BIT MAP
        matrix.postScale(scaleWidth, scaleHeight);

        // "RECREATE" THE NEW BITMAP
        Bitmap resizedBitmap = Bitmap.createBitmap(
                bm, 0, 0, width, height, matrix, false);
        bm.recycle();
        return resizedBitmap;
    }

    private Bitmap toBitmap(Image image) {

        byte[] nv21 = YUV_420_888toNV21(image);


        YuvImage yuvImage = new YuvImage(nv21, ImageFormat.NV21, image.getWidth(), image.getHeight(), null);

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        yuvImage.compressToJpeg(new Rect(0, 0, yuvImage.getWidth(), yuvImage.getHeight()), 75, out);

        byte[] imageBytes = out.toByteArray();

        return BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.length);
    }

    //Save Faces to Shared Preferences.Conversion of Recognition objects to json string
    private void insertToSP(HashMap<String, SimilarityClassifier.Recognition> jsonMap, boolean clear) {
        if (clear)
            jsonMap.clear();
        else
            jsonMap.putAll(readFromSP());
        String jsonString = new Gson().toJson(jsonMap);
        Log.e(TAG, "Input json" + jsonString);

        SharedPreferences sharedPreferences = getSharedPreferences("HashMap", MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putString("map", jsonString);
        editor.apply();
        Toast.makeText(context, "Recognitions Saved", Toast.LENGTH_SHORT).show();
    }

    //Load Faces from Shared Preferences.Json String to Recognition object
    private HashMap<String, SimilarityClassifier.Recognition> readFromSP() {
        SharedPreferences sharedPreferences = getSharedPreferences("HashMap", MODE_PRIVATE);
        String defValue = new Gson().toJson(new HashMap<String, SimilarityClassifier.Recognition>());
        String json = sharedPreferences.getString("map", defValue);
        TypeToken<HashMap<String, SimilarityClassifier.Recognition>> token = new TypeToken<HashMap<String, SimilarityClassifier.Recognition>>() {
        };
        HashMap<String, SimilarityClassifier.Recognition> retrievedMap = new Gson().fromJson(json, token.getType());
        Log.e(TAG, "Output map" + retrievedMap.toString());

        //During type conversion and save/load procedure,format changes(eg float converted to double).
        //So embeddings need to be extracted from it in required format(eg.double to float).
        for (Map.Entry<String, SimilarityClassifier.Recognition> entry : retrievedMap.entrySet()) {
            float[][] output = new float[1][OUTPUT_SIZE];
            ArrayList arrayList = (ArrayList) entry.getValue().getExtra();
            arrayList = (ArrayList) arrayList.get(0);
            for (int counter = 0; counter < arrayList.size(); counter++) {
                output[0][counter] = ((Double) arrayList.get(counter)).floatValue();
            }
            entry.getValue().setExtra(output);

        }
        Toast.makeText(context, "Recognitions Loaded", Toast.LENGTH_SHORT).show();
        return retrievedMap;
    }

    //Load Photo from phone storage
    private void loadPhoto() {

        start = false;
        Intent intent = new Intent();
        intent.setType("image/*");
        intent.setAction(Intent.ACTION_GET_CONTENT);
        startActivityForResult(Intent.createChooser(intent, "Select Picture"), SELECT_PICTURE);
    }

    //Similar Analyzing Procedure
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == RESULT_OK) {
            if (requestCode == SELECT_PICTURE) {
                Uri selectedImageUri = data.getData();
                try {
                    Thread.sleep(10);
                    InputImage inputImage = InputImage.fromBitmap(getBitmapFromUri(selectedImageUri), 0);
                    detector.process(inputImage).addOnSuccessListener(faces -> {

                        if (faces.size() != 0) {
                            btnAction.setText(getResources().getString(R.string.txt_recognize));
                            llTrainingFace.setVisibility(View.VISIBLE);
                            lLRecogniseFace.setVisibility(View.GONE);
                            Face face = faces.get(0);
                            Log.e(TAG, face.toString());

                            Bitmap frame_bmp = null;
                            try {
                                frame_bmp = getBitmapFromUri(selectedImageUri);
                            } catch (IOException e) {
                                e.printStackTrace();
                            }
                            Bitmap frame_bmp1 = rotateBitmap(frame_bmp, 0, flipX);

                            RectF boundingBox = new RectF(face.getBoundingBox());

                            Bitmap cropped_face = getCropBitmapByCPU(frame_bmp1, boundingBox);

                            Bitmap scaled = getResizedBitmap(cropped_face, 112, 112);

                            float distance;
                            if (registered.size() > 0) {
                                final Pair<String, Float> nearest = findNearest(embeddings[0]);//Find closest matching face

                                if (nearest != null) {
                                    distance = nearest.second;
                                    if (distance > 1.000f) {
                                        addFace();
                                    } else {
                                        Toast.makeText(context, "Already registered", Toast.LENGTH_SHORT).show();
                                        recognizeImage(scaled);

                                    }

                                }
                            }
                            Log.e(TAG, boundingBox.toString());
                            try {
                                Thread.sleep(10);
                            } catch (InterruptedException e) {
                                e.printStackTrace();
                            }
                        }
                    }).addOnFailureListener(e -> {
                        start = true;
                        Toast.makeText(context, "Failed to add", Toast.LENGTH_SHORT).show();
                    });
                    ivFacePreview.setImageBitmap(getBitmapFromUri(selectedImageUri));
                } catch (IOException | InterruptedException e) {
                    e.printStackTrace();
                }


            }
        }
    }

    private Bitmap getBitmapFromUri(Uri uri) throws IOException {
        ParcelFileDescriptor parcelFileDescriptor =
                getContentResolver().openFileDescriptor(uri, "r");
        FileDescriptor fileDescriptor = parcelFileDescriptor.getFileDescriptor();
        Bitmap image = BitmapFactory.decodeFileDescriptor(fileDescriptor);
        parcelFileDescriptor.close();
        return image;
    }

    @Override
    public void onBackPressed() {
        Intent backIntent = new Intent(context, ConvoActivity.class);
        startActivity(backIntent);
        finish();
    }
}

