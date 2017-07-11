package demo.unisys.com.rekoginitionfinal.ui;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Bundle;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

import android.os.*;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.provider.MediaStore;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONArray;

import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicHeader;

import com.amazonaws.auth.CognitoCachingCredentialsProvider;
import com.amazonaws.auth.CognitoCredentialsProvider;
import com.amazonaws.mobileconnectors.lex.interactionkit.Response;
import com.amazonaws.mobileconnectors.lex.interactionkit.config.InteractionConfig;
import com.amazonaws.mobileconnectors.lex.interactionkit.ui.InteractiveVoiceView;
import com.amazonaws.mobileconnectors.lex.interactionkit.ui.InteractiveVoiceViewAdapter;
import com.amazonaws.mobileconnectors.s3.transferutility.TransferListener;
import com.amazonaws.mobileconnectors.s3.transferutility.TransferObserver;
import com.amazonaws.mobileconnectors.s3.transferutility.TransferState;
import com.amazonaws.mobileconnectors.s3.transferutility.TransferUtility;
import com.amazonaws.regions.Regions;
import com.amazonaws.services.s3.AmazonS3Client;

import com.amazonaws.services.polly.AmazonPollyPresigningClient;
import com.amazonaws.services.polly.model.DescribeVoicesRequest;
import com.amazonaws.services.polly.model.DescribeVoicesResult;
import com.amazonaws.services.polly.model.OutputFormat;
import com.amazonaws.services.polly.model.SynthesizeSpeechPresignRequest;
import com.amazonaws.services.polly.model.Voice;


import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import demo.unisys.com.rekoginitionfinal.constant.Constants;
import demo.unisys.com.rekoginitionfinal.adapter.MessagesListAdapter;
import demo.unisys.com.rekoginitionfinal.R;
import demo.unisys.com.rekoginitionfinal.model.TextMessage;
import demo.unisys.com.rekoginitionfinal.utils.Util;

public class ImagePickActivity extends Activity implements InteractiveVoiceView.InteractiveVoiceListener {
    private static final String TAG = "ImagePickActivity";
    private ImageView imageView, resImageView, dashImageView;
    private TextView name, role, gender, city, age, smile, eyeGlass, emotion;
    private TransferUtility transferUtility;
    private AmazonS3Client s3Client;
    String mCurrentPhotoPath;
    static final int REQUEST_TAKE_PHOTO = 1;
    private static final int REQ_CODE_CAMERA = 1000; //Constant to uniquely identify the result
    private static final int REQ_CODE_FILEWRITE = 1001; //Constant to uniquely identify the result
    private static final int REQ_CODE_FILEREAD = 1002; //Constant to uniquely identify the result
    private static final int REQUEST_MICRO = 1003;
    private static final int RESULT_LOAD_IMAGE = 100;
    private static final int FINE_MEDIA_ACCESS_PERMISSION_REQUEST = 123;
    private List<Voice> voices;
    private AmazonPollyPresigningClient pollyClient;
    private String finalPersonName;
    private String personRoleImage;
    MessagesListAdapter messagesListAdapter;

    private Context appContext;
    private InteractiveVoiceView voiceView;
    private InteractiveVoiceViewAdapter voiceViewAdapter;

    @Override
    public void onBackPressed() {
        exit();
    }

    private void init() {
        appContext = getApplicationContext();
        voiceView = (InteractiveVoiceView) findViewById(R.id.voiceInterface);
        voiceView.setInteractiveVoiceListener(this);
        CognitoCredentialsProvider credentialsProvider = new CognitoCredentialsProvider(
                appContext.getResources().getString(R.string.identity_id_test),
                Regions.fromName(appContext.getResources().getString(R.string.aws_region)));
        voiceView.getViewAdapter().setCredentialProvider(credentialsProvider);
        voiceView.getViewAdapter().setInteractionConfig(
                new InteractionConfig(appContext.getString(R.string.bot_name),
                        appContext.getString(R.string.bot_alias)));
        voiceView.getViewAdapter().setAwsRegion(appContext.getString(R.string.aws_region));

    }

    private void exit() {
        finish();
    }

    @Override
    public void dialogReadyForFulfillment(final Map<String, String> slots, final String intent) {
        Log.i(TAG, "dialogReadyForFulfillment: " + intent);
        Log.d(TAG, String.format(
                Locale.US,
                "Dialog ready for fulfillment:\n\tIntent: %s\n\tSlots: %s",
                intent,
                slots.toString()));
    }

    @Override
    public void onResponse(Response response) {
        Log.d(TAG, "Bot response: " + response.getTextResponse());
        Log.i(TAG, "onResponse: " + response.getInputTranscript());
        String res = response.getTextResponse();
        String inputRes = response.getInputTranscript();
        String find = "alert";
        if (res != null && res.toLowerCase().indexOf(find.toLowerCase()) != -1) {
            System.out.println("inside dashboard");
            downloadDashBoard();
        } else if (inputRes.equals("Hello guest could you please help me with your picture?")) {
            openImageGallery();
        }
    }

    @Override
    public void onError(final String responseText, final Exception e) {
        Log.e(TAG, "Error: " + responseText, e);
    }

    public void downloadDashBoard() {
        System.out.println("gonna donwload from s3");
        try {
            System.out.println("download in" + getExternalFilesDir(Environment.DIRECTORY_PICTURES).toString());
            final File file = new File(getExternalFilesDir(Environment.DIRECTORY_PICTURES).toString() + "/" + personRoleImage + ".jpg");
            TransferObserver transferObserver = transferUtility.download("rekognition-dashboard", file.getName(),
                    file);
            transferObserver.setTransferListener(new TransferListener() {

                @Override
                public void onStateChanged(int id, TransferState state) {
                    // do something
                    System.out.println("download " + state.name());
                    System.out.println("download " + state.toString());
                    if (state.name() == "COMPLETED") {
                        System.out.println("completed downloading so showing up the image");
                        int targetW = dashImageView.getWidth();
                        int targetH = dashImageView.getHeight();
                        String resultantPath = file.getAbsolutePath();
                        // Get the dimensions of the bitmap
                        BitmapFactory.Options bmOptions = new BitmapFactory.Options();
                        bmOptions.inJustDecodeBounds = true;
                        BitmapFactory.decodeFile(resultantPath, bmOptions);
                        int photoW = bmOptions.outWidth;
                        int photoH = bmOptions.outHeight;

                        // Determine how much to scale down the image
                        int scaleFactor = Math.min(photoW / targetW, photoH / targetH);

                        // Decode the image file into a Bitmap sized to fill the View
                        bmOptions.inJustDecodeBounds = false;
                        bmOptions.inSampleSize = scaleFactor;
                        bmOptions.inPurgeable = true;
                        Bitmap bitmap = BitmapFactory.decodeFile(resultantPath, bmOptions);
                        // imageView.setImageBitmap(bitmap);
                        dashImageView.setImageBitmap(bitmap);
                        System.out.println("bitmap" + bitmap);

                    }
                }

                @Override
                public void onProgressChanged(int id, long bytesCurrent, long bytesTotal) {
                    if (bytesTotal != 0) {
                        int percentage = (int) (bytesCurrent / bytesTotal * 100);
                        System.out.println("per" + percentage);
                    }
                    System.out.println("byte total is zero");
                    //Display percentage transfered to user
                }

                @Override
                public void onError(int id, Exception ex) {
                    // do something
                    System.out.println("id" + id);
                    System.out.println("exception" + ex);
                }

            });


        } catch (Exception e) {
            System.out.println("inside download catch" + e);
        } finally {
            System.out.println("inside download finally");
        }
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        s3Client = Util.getS3Client(this);
        transferUtility = Util.getTransferUtility(this);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_image_pick);
        imageView = (ImageView) findViewById(R.id.result);
        resImageView = (ImageView) findViewById(R.id.s3result);
        dashImageView = (ImageView) findViewById(R.id.dashresult);
        name = (TextView) findViewById(R.id.name);
        age = (TextView) findViewById(R.id.age);
        gender = (TextView) findViewById(R.id.gender);
        eyeGlass = (TextView) findViewById(R.id.eyeGlass);
        emotion = (TextView) findViewById(R.id.emotion);
        init();
    }

    public void miceClick(View view) {
        init();
    }

    public void onBtnClick(View View) {
        System.out.println("starting");
        checkForMicroPhonePermission();
        //checkForCameraPermission();
    }

    public void galleryClick(View view) {
        openImageGallery();
    }

    private void openImageGallery() {
        if (ContextCompat.checkSelfPermission(this,
                Manifest.permission.READ_EXTERNAL_STORAGE)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.READ_EXTERNAL_STORAGE},
                    FINE_MEDIA_ACCESS_PERMISSION_REQUEST);
        } else {
            Intent i = new Intent(Intent.ACTION_PICK, android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
            startActivityForResult(i, RESULT_LOAD_IMAGE);
        }
    }

    private void dispatchTakePictureIntent() {
        Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        // Ensure that there's a camera activity to handle the intent
        if (takePictureIntent.resolveActivity(getPackageManager()) != null) {
            // Create the File where the photo should go
            File photoFile = null;
            try {
                photoFile = createImageFile();
            } catch (IOException ex) {
                // Error occurred while creating the File
                System.out.println("error creating file" + ex);
            }
            // Continue only if the File was successfully created
            if (photoFile != null) {
                System.out.println("path*** " + photoFile.getAbsolutePath());
                Uri photoURI = Uri.fromFile(photoFile);
                System.out.println("photouri " + photoURI + photoFile.getAbsolutePath());
                takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, photoURI);
                System.out.println("takePictureIntent " + takePictureIntent);
                startActivityForResult(takePictureIntent, REQUEST_TAKE_PHOTO);

            }
        }
    }

    private File createImageFile() throws IOException {
        // Create an image file name
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
        String imageFileName = "JPEG_" + timeStamp + "_";
        File storageDir = getExternalFilesDir(Environment.DIRECTORY_PICTURES);
        if (Environment.MEDIA_MOUNTED.equals(Environment.getExternalStorageState())) {
            System.out.println("external storage avail");
        } else {
            System.out.println("no external storage ");
        }
        System.out.println("storageDir  " + storageDir);
        System.out.println("storageDir  " + storageDir + imageFileName + ".jpg");
        File image = File.createTempFile(
                imageFileName,  /* prefix */
                ".jpg",         /* suffix */
                storageDir     /* directory */
        );

        // Save a file: path for use with ACTION_VIEW intents
        mCurrentPhotoPath = image.getAbsolutePath();
        System.out.println("mCurrentPhotoPath  " + mCurrentPhotoPath);
        return image;
    }

    private void checkForMicroPhonePermission() {
        int checkSendSmsPermission = ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO);
        if (checkSendSmsPermission == PackageManager.PERMISSION_GRANTED) {
            checkForCameraPermission();
        } else {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.RECORD_AUDIO}, REQUEST_MICRO);
        }
    }

    private void checkForCameraPermission() {
        int checkCameraPermission = ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA);
        if (checkCameraPermission == PackageManager.PERMISSION_GRANTED) {
            checkForFileWritePermission();
        } else {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.CAMERA}, REQ_CODE_CAMERA);
        }
    }

    private void checkForFileWritePermission() {
        int checkSendSmsPermission = ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE);
        if (checkSendSmsPermission == PackageManager.PERMISSION_GRANTED) {
            checkForFileReadPermission();
        } else {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, REQ_CODE_FILEWRITE);
        }
    }

    private void checkForFileReadPermission() {
        int checkSendSmsPermission = ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE);
        if (checkSendSmsPermission == PackageManager.PERMISSION_GRANTED) {
            dispatchTakePictureIntent();
        } else {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.READ_EXTERNAL_STORAGE}, REQ_CODE_FILEREAD);
        }
    }


    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        switch (requestCode) {
            case REQ_CODE_CAMERA:
                if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    dispatchTakePictureIntent();
                } else {
                    Toast.makeText(this, "Can't open camera", Toast.LENGTH_SHORT).show();
                }
                break;
            case REQ_CODE_FILEWRITE:
                if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    dispatchTakePictureIntent();
                } else {
                    Toast.makeText(this, "Can't Write to internal storage", Toast.LENGTH_SHORT).show();
                }
                break;
            case REQ_CODE_FILEREAD:
                if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    dispatchTakePictureIntent();
                } else {
                    Toast.makeText(this, "Can't read from internal storage", Toast.LENGTH_SHORT).show();
                }
                break;
            case REQUEST_MICRO:
                if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    dispatchTakePictureIntent();
                } else {
                    Toast.makeText(this, "Can't open Microphone", Toast.LENGTH_SHORT).show();
                }
                break;
            case FINE_MEDIA_ACCESS_PERMISSION_REQUEST: {
                if (grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    openImageGallery();
                } else if (ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.READ_EXTERNAL_STORAGE)) {
                    Log.e("denied", "shouldShowRequestPermissionRationale permission");
                } else {
                    Log.e("denied", "else permission");
                }
            }
            default:
                super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        }
    }

    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        System.out.println("on activity result");
        if (requestCode == REQUEST_TAKE_PHOTO && resultCode == Activity.RESULT_OK) {
            System.out.println("mCurrentPhotoPath" + mCurrentPhotoPath);
            int targetW = imageView.getWidth();
            int targetH = imageView.getHeight();

            // Get the dimensions of the bitmap
            BitmapFactory.Options bmOptions = new BitmapFactory.Options();
            bmOptions.inJustDecodeBounds = true;
            BitmapFactory.decodeFile(mCurrentPhotoPath, bmOptions);
            int photoW = bmOptions.outWidth;
            int photoH = bmOptions.outHeight;

            // Determine how much to scale down the image
            int scaleFactor = Math.min(photoW / targetW, photoH / targetH);

            // Decode the image file into a Bitmap sized to fill the View
            bmOptions.inJustDecodeBounds = false;
            bmOptions.inSampleSize = scaleFactor;
            bmOptions.inPurgeable = true;

            Bitmap bitmap = BitmapFactory.decodeFile(mCurrentPhotoPath, bmOptions);
            imageView.setImageBitmap(bitmap);
            uploadToS3();

        } else {
            getImage(data);
        }
    }

    private void getImage(Intent data) {
        Uri selectedImage = data.getData();
        String[] filePathColumn = {MediaStore.Images.Media.DATA};
        Cursor cursor = getContentResolver().query(selectedImage, filePathColumn, null, null, null);
        cursor.moveToFirst();
        int columnIndex = cursor.getColumnIndex(filePathColumn[0]);
        String picturePath = cursor.getString(columnIndex);
        mCurrentPhotoPath = cursor.getString(columnIndex);
        cursor.close();
        Bitmap bitmap = BitmapFactory.decodeFile(picturePath);

        imageView.setImageBitmap(bitmap);
        uploadToS3();
    }

    public void uploadToS3() {
        File file = new File(mCurrentPhotoPath);
        TransferObserver observer = transferUtility.upload(Constants.TEST_BUCKET, file.getName(),
                file);
        observer.setTransferListener(new TransferListener() {

            @Override
            public void onStateChanged(int id, TransferState state) {
                System.out.println("upload " + state.name());
                System.out.println("upload " + state.toString());
                if (state.name() == "COMPLETED") {
                    System.out.println("completed uploading so calling rest func");
                    new MyAsyncTask().execute("https://iev0kuqj21.execute-api.us-east-1.amazonaws.com/dev/facecomparison");
                    //  getFacialAnalysis();

                }
            }

            @Override
            public void onProgressChanged(int id, long bytesCurrent, long bytesTotal) {
                if (bytesTotal != 0) {
                    int percentage = (int) (bytesCurrent / bytesTotal * 100);
                    System.out.println("per" + percentage);
                }
            }

            @Override
            public void onError(int id, Exception ex) {
                System.out.println("id" + id);
                System.out.println("exception" + ex);
                // do something
            }

        });
    }

    public static String changeStringCase(String s) {

        final String DELIMITERS = " '-/";

        StringBuilder sb = new StringBuilder();
        boolean capNext = true;

        for (char c : s.toCharArray()) {
            c = (capNext)
                    ? Character.toUpperCase(c)
                    : Character.toLowerCase(c);
            sb.append(c);
            capNext = (DELIMITERS.indexOf((int) c) >= 0);
        }
        return sb.toString();
    }

    public class MyAsyncTask extends AsyncTask<String, Void, String> {
        JSONObject jsonObject;
        HttpResponse response;
        StringBuilder stringBuilder;
        String decisionMaker;

        @Override
        protected String doInBackground(String... params) {

            // TODO Auto-generated method stub
            //postData(params[0]);
            HttpClient httpclient = new DefaultHttpClient();
            HttpPost httppost = new HttpPost(params[0]);
            Log.d("img url ", params[0]);
            System.out.println("calling rest api");
            JSONObject resjsonObject;
            if (params[0].equals("https://iev0kuqj21.execute-api.us-east-1.amazonaws.com/dev/facecomparison")) {
                //  if(params[0].equals("https://wect0vssl0.execute-api.us-east-1.amazonaws.com/prod/forums")){
                System.out.println("calling rest api inside func call");
                try {
                    // Add your data
                    httppost.setHeader(new BasicHeader("Content-Type", "application/json"));
                    httppost.setHeader(new BasicHeader("Authorization", "Basic cGRzLWRlbW9AdW5pc3lzLmNvbTpVTklTWVNfMTIzNDU2"));
                    System.out.println("about to call");
                    response = httpclient.execute(httppost);
                    System.out.println(response);
                    System.out.println("about to call");
                    InputStream inputStream = response.getEntity().getContent();
                    System.out.println(inputStream);
                    System.out.println("inputStream" + inputStream);
                    InputStreamReader inputStreamReader = new InputStreamReader(inputStream);
                    BufferedReader bufferedReader = new BufferedReader(inputStreamReader);
                    stringBuilder = new StringBuilder();
                    String bufferedStrChunk = null;
                    while ((bufferedStrChunk = bufferedReader.readLine()) != null) {
                        stringBuilder.append(bufferedStrChunk);
                    }
                    System.out.println("output" + stringBuilder);
                    resjsonObject = new JSONObject(stringBuilder.toString());
                    System.out.println("calling download func");
                    return resjsonObject.toString();
                    //   download(resjsonObject);
                } catch (ClientProtocolException e) {
                    // TODO Auto-generated catch block
                    System.out.println(e);
                } catch (IOException e) {
                    System.out.println(e);
                    System.out.println(e.getMessage());
                    System.out.println(e.getLocalizedMessage());
                    // TODO Auto-generated catch block
                } catch (JSONException e) {
                    System.out.println("json conversion exception " + e);
                    System.out.println(e.getMessage());
                    System.out.println(e.getLocalizedMessage());
                    // TODO Auto-generated catch block
                } finally {

                }
            }
            return "";
        }

        @Override
        protected void onPostExecute(final String result) {
            super.onPostExecute(result);
            Log.d("Response : ", result);
            download(result);
        }
    }

    public void download(String res) {
        System.out.println("gonna donwload from s3");
        try {
            JSONObject jsonObject = new JSONObject(res);
            System.out.println("download in" + getExternalFilesDir(Environment.DIRECTORY_PICTURES).toString());
            if (jsonObject.get("statusMsg").toString().equals("NotMatched")) {
                Toast.makeText(this, "We are seeing you for the first time. So we don't have any details about you.", Toast.LENGTH_SHORT).show();
                name.setText("We are seeing you for the first time.\nSo we don't have any details about you.");
            } else {
                final File file = new File(getExternalFilesDir(Environment.DIRECTORY_PICTURES).toString() + "/" + jsonObject.get("matchedImage"));
                System.out.println("key is" + file.getName());
                if (jsonObject.get("personName") != null && jsonObject.get("role") != null) {
                    finalPersonName = jsonObject.get("personName").toString();
                    personRoleImage = jsonObject.get("role").toString();
                    name.setText("Name :" + jsonObject.get("personName").toString() + "   Role :" + jsonObject.get("role").toString());
                }

                if (jsonObject.get("gender") != null && jsonObject.get("city") != null)
                    gender.setText("Gender :" + jsonObject.get("gender").toString() + "                City :" + jsonObject.get("city").toString());


                if (jsonObject.get("smile") != null && jsonObject.get("ageRange") != null) {
                    JSONObject smileJson = (JSONObject) jsonObject.get("smile");
                    System.out.println(smileJson.get("confidence"));
                    String smile = smileJson.get("confidence").toString();
                    char[] smileArr = smile.toCharArray();
                    JSONObject ageJson = (JSONObject) jsonObject.get("ageRange");
                    System.out.println(ageJson.get("low").toString() + "to" + ageJson.get("high").toString());
                    age.setText("Age :" + ageJson.get("low").toString() + " to " + ageJson.get("high").toString() + "                    Smile  :" + smileArr[0] + smileArr[1] + "%");
                }
                if (jsonObject.get("eyeglasses") != null && jsonObject.get("emotions") != null) {
                    JSONObject eyeGlassJson = (JSONObject) jsonObject.get("eyeglasses");
                    String eye = eyeGlassJson.get("confidence").toString();
                    char[] eyeArr = eye.toCharArray();

                    JSONArray emotionJson = (JSONArray) jsonObject.get("emotions");
                    String emotionStr = "";
                   /* for (int n = 0; n < emotionJson.length(); n++) {
                        JSONObject object = emotionJson.getJSONObject(n);
                        String emotion=object.get("confidence").toString();
                        char[] emotionArr = emotion.toCharArray();
                        String changeEmotion=changeStringCase(object.get("type").toString());
                        emotionStr += "\n" + changeEmotion+ " " +emotionArr[0]+emotionArr[1]+ "% ";
                    }*/
                    JSONObject object = emotionJson.getJSONObject(0);
                    String e = object.get("confidence").toString();
                    char[] emotionArr = e.toCharArray();
                    String changeEmotion = changeStringCase(object.get("type").toString());

                    eyeGlass.setText("EyeGlass : " + eyeGlassJson.get("value").toString() + "  " + eyeArr[0] + eyeArr[1] + "%" + "       Emotions : " + changeEmotion + " " + emotionArr[0] + emotionArr[1] + "% ");
                    //  emotion.setText("Emotions : " +emotionStr);
                }
                System.out.println("directory is" + Environment.getExternalStorageDirectory().toString());
                System.out.println("directory is" + Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS).toString());
                System.out.println("directory is" + Environment.getExternalStorageDirectory().getAbsolutePath());

                TransferObserver transferObserver = transferUtility.download(Constants.SAMPLE_BUCKET, file.getName(),
                        file);
                transferObserver.setTransferListener(new TransferListener() {

                    @Override
                    public void onStateChanged(int id, TransferState state) {
                        // do something
                        System.out.println("download " + state.name());
                        System.out.println("download " + state.toString());
                        if (state.name() == "COMPLETED") {
                            invokePolly();
                            System.out.println("completed downloading so showing up the image");
                          /*  Bitmap b = null;
                            try {
                                b = BitmapFactory.decodeStream(new FileInputStream(file));
                            } catch (FileNotFoundException e) {
                                System.out.println("showing up the image" + b);
                                e.printStackTrace();
                            }*/
                            int targetW = resImageView.getWidth();
                            int targetH = resImageView.getHeight();
                            String resultantPath = file.getAbsolutePath();
                            // Get the dimensions of the bitmap
                            BitmapFactory.Options bmOptions = new BitmapFactory.Options();
                            bmOptions.inJustDecodeBounds = true;
                            BitmapFactory.decodeFile(resultantPath, bmOptions);
                            int photoW = bmOptions.outWidth;
                            int photoH = bmOptions.outHeight;

                            // Determine how much to scale down the image
                            int scaleFactor = Math.min(photoW / targetW, photoH / targetH);

                            // Decode the image file into a Bitmap sized to fill the View
                            bmOptions.inJustDecodeBounds = false;
                            bmOptions.inSampleSize = scaleFactor;
                            bmOptions.inPurgeable = true;
                            Bitmap bitmap = BitmapFactory.decodeFile(resultantPath, bmOptions);
                            // imageView.setImageBitmap(bitmap);
                            resImageView.setImageBitmap(bitmap);

                            System.out.println("bitmap" + bitmap);
                          /*  try{
                            if(jsonObject.get("personName")!=null){
                                 String guestName=jsonObject.get("personName").toString();

                            }}
                            catch(Exception e)
                            {
                                System.out.println("json conversion errror "+e);
                            }*/
                        }
                    }

                    @Override
                    public void onProgressChanged(int id, long bytesCurrent, long bytesTotal) {
                        if (bytesTotal != 0) {
                            int percentage = (int) (bytesCurrent / bytesTotal * 100);
                            System.out.println("percent :" + percentage);
                        }
                        System.out.println("byte total is zero");
                        //Display percentage transfered to user
                    }

                    @Override
                    public void onError(int id, Exception ex) {
                        // do something
                        System.out.println("id" + id);
                        System.out.println("exception" + ex);
                    }

                });
            }

        } catch (Exception e) {
            System.out.println("inside download catch" + e);
        } finally {
            System.out.println("inside download finally");
        }
    }

    public void invokePolly() {

        System.out.println("inside polly");
        CognitoCachingCredentialsProvider credentialsProvider = new CognitoCachingCredentialsProvider(
                getApplicationContext(),
                Constants.COGNITO_POOL_ID,
                Regions.US_EAST_1
        );

        pollyClient = new AmazonPollyPresigningClient(credentialsProvider);
        System.out.println("client" + pollyClient);
        // Asynchronously get available Polly voices.
        new GetPollyVoices().execute();
    }

    public void onPollyPlay() {
        SynthesizeSpeechPresignRequest synthesizeSpeechPresignRequest =
                new SynthesizeSpeechPresignRequest()
                        // Set the text to synthesize.
                        .withText("Hello " + finalPersonName + "My Name is Sally. How may I help you today?")
                        // Select voice for synthesis.
                        .withVoiceId(voices.get(0).getId()) // "Joanna"
                        // Set format to MP3.
                        .withOutputFormat(OutputFormat.Mp3);

// Get the presigned URL for synthesized speech audio stream.
        URL presignedSynthesizeSpeechUrl =
                pollyClient.getPresignedSynthesizeSpeechUrl(synthesizeSpeechPresignRequest);
        System.out.println("presignedSynthesizeSpeechUrl" + presignedSynthesizeSpeechUrl.toString());
        MediaPlayer mediaPlayer = new MediaPlayer();
        mediaPlayer.setAudioStreamType(AudioManager.STREAM_MUSIC);
        System.out.println("mediaPlayer " + mediaPlayer);

        try {
            // Set media player's data source to previously obtained URL.
            mediaPlayer.setDataSource(presignedSynthesizeSpeechUrl.toString());
        } catch (IOException e) {
            Log.e(TAG, "Unable to set data source for the media player! " + e.getMessage());
        }

// Prepare the MediaPlayer asynchronously (since the data source is a network stream).
        mediaPlayer.prepareAsync();

// Set the callback to start the MediaPlayer when it's prepared.
        mediaPlayer.setOnPreparedListener(new MediaPlayer.OnPreparedListener() {
            @Override
            public void onPrepared(MediaPlayer mp) {
                System.out.println("onPrepared" + mp);
                System.out.println("is playing " + mp.isPlaying());
                mp.start();
            }
        });

// Set the callback to release the MediaPlayer after playback is completed.
        mediaPlayer.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
            @Override
            public void onCompletion(MediaPlayer mp) {
                System.out.println("onCompletion" + mp);
                mp.release();
            }
        });

    }

    private class GetPollyVoices extends AsyncTask<Void, Void, Void> {
        @Override
        protected Void doInBackground(Void... params) {

            // Create describe voices request.
            DescribeVoicesRequest describeVoicesRequest = new DescribeVoicesRequest();

            DescribeVoicesResult describeVoicesResult;
            try {
                // Synchronously ask the Polly Service to describe available TTS voices.
                describeVoicesResult = pollyClient.describeVoices(describeVoicesRequest);
            } catch (RuntimeException e) {
                Log.e(TAG, "Unable to get available voices. " + e.getMessage());
                return null;
            }

            // Get list of voices from the result.
            voices = describeVoicesResult.getVoices();

            // Log a message with a list of available TTS voices.
            Log.i(TAG, "Available Polly voices: " + voices);

            return null;
        }

        @Override
        protected void onPostExecute(Void aVoid) {
            System.out.println("calling polly to execute");
            onPollyPlay();
        }

    }
}