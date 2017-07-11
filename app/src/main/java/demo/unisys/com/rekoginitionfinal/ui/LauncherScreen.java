package demo.unisys.com.rekoginitionfinal.ui;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Environment;
import android.os.Handler;
import android.provider.MediaStore;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v4.content.FileProvider;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.widget.ListView;

import com.crashlytics.android.Crashlytics;

import demo.unisys.com.rekoginitionfinal.utils.InternetConnectivityUtil;
import demo.unisys.com.rekoginitionfinal.utils.MarshMallowPermission;
import demo.unisys.com.rekoginitionfinal.utils.Preferences;
import io.fabric.sdk.android.Fabric;

import com.amazonaws.auth.CognitoCachingCredentialsProvider;
import com.amazonaws.auth.CognitoCredentialsProvider;
import com.amazonaws.mobileconnectors.lex.interactionkit.Response;
import com.amazonaws.mobileconnectors.lex.interactionkit.config.InteractionConfig;

import com.amazonaws.mobileconnectors.lex.interactionkit.ui.InteractiveVoiceViewAdapter;
import com.amazonaws.mobileconnectors.s3.transferutility.TransferListener;
import com.amazonaws.mobileconnectors.s3.transferutility.TransferObserver;
import com.amazonaws.mobileconnectors.s3.transferutility.TransferState;
import com.amazonaws.mobileconnectors.s3.transferutility.TransferUtility;
import com.amazonaws.regions.Regions;
import com.amazonaws.services.polly.AmazonPollyPresigningClient;
import com.amazonaws.services.polly.model.DescribeVoicesRequest;
import com.amazonaws.services.polly.model.DescribeVoicesResult;
import com.amazonaws.services.polly.model.OutputFormat;
import com.amazonaws.services.polly.model.SynthesizeSpeechPresignRequest;
import com.amazonaws.services.polly.model.Voice;
import com.amazonaws.services.s3.AmazonS3Client;
import com.crashlytics.android.answers.Answers;

import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicHeader;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import demo.unisys.com.rekoginitionfinal.InteractiveVoiceView;
import demo.unisys.com.rekoginitionfinal.constant.Constants;
import demo.unisys.com.rekoginitionfinal.adapter.MessagesListAdapter;
import demo.unisys.com.rekoginitionfinal.R;
import demo.unisys.com.rekoginitionfinal.model.ResponseData;
import demo.unisys.com.rekoginitionfinal.model.TextMessage;
import demo.unisys.com.rekoginitionfinal.utils.Util;

public class LauncherScreen extends AppCompatActivity implements InteractiveVoiceView.InteractiveVoiceListener {
    private static final String TAG = "LauncherScreen";
    private TransferUtility transferUtility;
    private AmazonS3Client s3Client;
    String mCurrentPhotoPath;
    static final int REQUEST_TAKE_PHOTO = 1;
    private static final int RESULT_LOAD_IMAGE = 100;
    private static final int FINE_MEDIA_ACCESS_PERMISSION_REQUEST = 123;
    private List<Voice> voices;
    private AmazonPollyPresigningClient pollyClient;
    private String finalPersonName;
    private String personRoleImage;
    private MessagesListAdapter messagesListAdapter;
    private ArrayList<TextMessage> mMessageArrayList;
    private Context appContext;
    private InteractiveVoiceView mVoiceView;
    private InteractiveVoiceViewAdapter voiceViewAdapter;
    private ListView mListView;
    private ResponseData mResponseData;
    private Boolean isItFirstTime;
    private String mFaceNotRecog = null;
    private String messageContent = null;
    public static String liveImage;
    private Boolean randomPerson = false;

    public Boolean getItFirstTime() {
        return isItFirstTime;
    }

    public void setItFirstTime(Boolean itFirstTime) {
        isItFirstTime = itFirstTime;
    }

    @Override
    public void onBackPressed() {
        exit();
    }

    private void init() {
        appContext = getApplicationContext();
        mVoiceView = (InteractiveVoiceView) findViewById(R.id.voiceInterface);
        mVoiceView.setInteractiveVoiceListener(LauncherScreen.this);
        CognitoCredentialsProvider credentialsProvider = new CognitoCredentialsProvider(
                appContext.getResources().getString(R.string.identity_id_test),
                Regions.fromName(appContext.getResources().getString(R.string.aws_region)));
        mVoiceView.getViewAdapter().setCredentialProvider(credentialsProvider);
        mVoiceView.getViewAdapter().setInteractionConfig(
                new InteractionConfig(appContext.getString(R.string.bot_name),
                        appContext.getString(R.string.bot_alias)));
        mVoiceView.getViewAdapter().setAwsRegion(appContext.getString(R.string.aws_region));
    }

    private void exit() {
        finish();
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        outState.putParcelableArrayList("savedListValue", mMessageArrayList);
        super.onSaveInstanceState(outState);
    }

    @Override
    protected void onRestoreInstanceState(Bundle savedInstanceState) {
        super.onRestoreInstanceState(savedInstanceState);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        s3Client = Util.getS3Client(this);
        transferUtility = Util.getTransferUtility(this);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_launcher);
        mMessageArrayList = new ArrayList<>();
        checkInternetConnection();
        Fabric.with(this, new Crashlytics());
        Fabric.with(this, new Answers());
        mListView = (ListView) findViewById(R.id.chatListView);
        if (savedInstanceState == null) {
            init();
            setItFirstTime(true);
        } else {
            mMessageArrayList = savedInstanceState.getParcelableArrayList("savedListValue");
            setItFirstTime(false);
        }
        messagesListAdapter = new MessagesListAdapter(this, mMessageArrayList);
        mListView.setAdapter(messagesListAdapter);
        mListView.setDividerHeight(0);


    }

    private void checkInternetConnection() {
        if (!InternetConnectivityUtil.isNetworkAvailable(this)) {
            AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(this);
            alertDialogBuilder.setMessage("Please Check internet connection");
            alertDialogBuilder.setPositiveButton("Ok",
                    new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialogInterface, int i) {
                            finish();
                        }
                    }).setNegativeButton("Cancel",
                    new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialogInterface, int i) {
                            finish();
                        }
                    });
            alertDialogBuilder.show();
        }
    }

    @Override
    protected void onStart() {
        super.onStart();
        Log.i(TAG, "onStart: ");
    }


    @Override
    protected void onResume() {
        super.onResume();
        //checkForMicroPhonePermission();
        checkPermission();

    }

    private void checkPermission() {
        MarshMallowPermission marshMallowPermission = new MarshMallowPermission(this);
        if (marshMallowPermission.checkForMicroPhonePermission()) {
            if (marshMallowPermission.checkPermissionForCamera()) {
                if (marshMallowPermission.checkPermissionForExternalReadStorage()) {
                    if (marshMallowPermission.checkPermissionForExternalWriteStorage()) {
                        if (InternetConnectivityUtil.isNetworkAvailable(this)) {
                            if (getItFirstTime()) {
                                invokePolly();
                            }
                        }
                    } else {
                        marshMallowPermission.requestPermission(REQUEST_TAKE_PHOTO);
                    }
                } else {
                    marshMallowPermission.requestPermission(REQUEST_TAKE_PHOTO);
                }
            } else {
                marshMallowPermission.requestPermission(REQUEST_TAKE_PHOTO);
            }
        } else {
            marshMallowPermission.requestPermission(REQUEST_TAKE_PHOTO);
        }
    }

    @Override
    protected void onRestart() {
        super.onRestart();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        switch (requestCode) {
            case REQUEST_TAKE_PHOTO:
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
                //Uri photoURI = Uri.fromFile(photoFile);
                Uri photoURI = FileProvider.getUriForFile(this, getApplicationContext().getPackageName() + ".provider", photoFile);
                System.out.println("photouri " + photoURI + photoFile.getAbsolutePath());
                mCurrentPhotoPath = photoFile.getAbsolutePath();
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

        Preferences.writeString(this, Preferences.PHOTO_PATH, mCurrentPhotoPath);

        System.out.println("mCurrentPhotoPath  " + mCurrentPhotoPath);
        return image;
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        System.out.println("on activity result");
        // Bitmap resizedbitmap = null;
        if (requestCode == REQUEST_TAKE_PHOTO && resultCode == Activity.RESULT_OK) {
            System.out.println("mCurrentPhotoPath" + mCurrentPhotoPath);

            // Get the dimensions of the bitmap
            BitmapFactory.Options bmOptions = new BitmapFactory.Options();
            bmOptions.inJustDecodeBounds = true;
            BitmapFactory.decodeFile(mCurrentPhotoPath, bmOptions);

            // Decode the image file into a Bitmap sized to fill the View
            bmOptions.inJustDecodeBounds = false;
//            bmOptions.inSampleSize = scaleFactor;
            bmOptions.inPurgeable = true;

            //Bitmap bitmap = BitmapFactory.decodeFile(Preferences.readString(this,Preferences.PHOTO_PATH, ""), bmOptions);
            /*File file = new File(mCurrentPhotoPath);
            int file_size = Integer.parseInt(String.valueOf(file.length() / 1024));
            Log.i(TAG, "uploadToS3: file size" + file_size);
            FileOutputStream fos = null;
            try {
                fos = new FileOutputStream(file);
                bitmap.compress(Bitmap.CompressFormat.JPEG, 100, fos);
                fos.close();
            } catch (Exception e) {
                Log.e("SAVE_IMAGE", e.getMessage(), e);
            }*/


            //Bitmap photo = (Bitmap)data.getExtras().get("data");
            //imageView.setImageBitmap(photo);
            TextMessage textMessage = new TextMessage("", "imageType");
            textMessage.setImagePath(Preferences.readString(this, Preferences.PHOTO_PATH, ""));
            mMessageArrayList.add(textMessage);
            if (messagesListAdapter != null) {
                Log.i(TAG, "onActivityResult: Size " + mMessageArrayList.size());
                messagesListAdapter.notifyDataSetChanged();
                mListView.post(new Runnable() {
                    @Override
                    public void run() {
                        // Select the last row so it will scroll into view...
                        mListView.setSelection(messagesListAdapter.getCount() - 1);
                    }
                });
            }
            uploadToS3();
        } else if (requestCode == RESULT_LOAD_IMAGE && resultCode == Activity.RESULT_OK) {
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
        Log.i(TAG, "getImage: " + mCurrentPhotoPath);
        cursor.close();
        Bitmap bitmap = BitmapFactory.decodeFile(picturePath);
        TextMessage textMessage = new TextMessage("", "imageType");
        textMessage.setImagePath(mCurrentPhotoPath);
        mMessageArrayList.add(textMessage);
        if (messagesListAdapter != null) {
            Log.i(TAG, "onActivityResult: Size " + mMessageArrayList.size());
            messagesListAdapter.notifyDataSetChanged();
            mListView.post(new Runnable() {
                @Override
                public void run() {
                    // Select the last row so it will scroll into view...
                    mListView.setSelection(messagesListAdapter.getCount() - 1);
                }
            });
        }
//        imageView.setImageBitmap(bitmap);
        //loadImageFromAsset();
        uploadToS3();
    }

    private File loadImageFromAsset() {
        File f = null;
        try {
            InputStream ims = this.getAssets().open("unisys_image.jpg");
            Bitmap bitmap = BitmapFactory.decodeStream(ims);
            //create a file to write bitmap data
            f = new File(this.getCacheDir(), "unisys.jpg");
            f.createNewFile();

            //Convert bitmap to byte array
            //Bitmap bitmap = your bitmap;
            ByteArrayOutputStream bos = new ByteArrayOutputStream();
            bitmap.compress(Bitmap.CompressFormat.JPEG, 0, bos);
            byte[] bitmapdata = bos.toByteArray();
            //write the bytes in file
            FileOutputStream fos = new FileOutputStream(f);
            fos.write(bitmapdata);
            fos.flush();
            fos.close();/**/
        } catch (IOException e) {
            e.printStackTrace();
        }
        return f;
    }


    public File getBitmapFile(String fileName) {
        mCurrentPhotoPath = Environment.getExternalStorageDirectory().getAbsolutePath() + File.separator + Constants.USNISYS + File.separator + fileName;
        return new File(mCurrentPhotoPath);
    }

    public File saveBitmapToFile(File file) {
        try {

            // BitmapFactory options to downsize the image
            BitmapFactory.Options o = new BitmapFactory.Options();
            o.inJustDecodeBounds = true;
            o.inSampleSize = 6;
            // factor of downsizing the image

            FileInputStream inputStream = new FileInputStream(file);
            //Bitmap selectedBitmap = null;
            BitmapFactory.decodeStream(inputStream, null, o);
            inputStream.close();

            // The new size we want to scale to
            final int REQUIRED_SIZE = 200;

            // Find the correct scale value. It should be the power of 2.
            int scale = 1;
            while (o.outWidth / scale / 2 >= REQUIRED_SIZE &&
                    o.outHeight / scale / 2 >= REQUIRED_SIZE) {
                scale *= 2;
            }

            BitmapFactory.Options o2 = new BitmapFactory.Options();
            o2.inSampleSize = scale;
            inputStream = new FileInputStream(file);

            Bitmap selectedBitmap = BitmapFactory.decodeStream(inputStream, null, o2);
            inputStream.close();

            // here i override the original image file
            file.createNewFile();
            FileOutputStream outputStream = new FileOutputStream(file);

            selectedBitmap.compress(Bitmap.CompressFormat.JPEG, 100, outputStream);

            return file;
        } catch (Exception e) {
            return null;
        }
    }

    public void uploadToS3() {
        File file = new File(Preferences.readString(this, Preferences.PHOTO_PATH, ""));
        liveImage = file.getName();

        //File file = loadImageFromAsset();//getBitmapFile(Constants.USER_IMAGE);
        int file_size = Integer.parseInt(String.valueOf(file.length() / 1024));
        Log.i(TAG, "uploadToS3: file size" + file_size);
//        file = saveBitmapToFile(file);
//        file_size = Integer.parseInt(String.valueOf(file.length() / 1024));
//        Log.i(TAG, "uploadToS3: file size" + file_size);
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
                    // getFacialAnalysis();

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
        Boolean recognizationSuccess = false;
        System.out.println("gonna donwload from s3");
        mResponseData = new ResponseData();
        try {
            JSONObject jsonObject = new JSONObject(res);
            System.out.println("download in" + getExternalFilesDir(Environment.DIRECTORY_PICTURES).toString());
            TextMessage textMessage = new TextMessage();
            textMessage.setMessage("");
            textMessage.setFrom("Bot");
            mMessageArrayList.add(textMessage);
            if (jsonObject.get("statusMsg").toString().equals("NotMatched") || jsonObject.get("statusMsg").toString().equals("RandomImage")) {
                //Toast.makeText(this, "We are seeing you for the first time. So we don't have any details about you.", Toast.LENGTH_SHORT).show();
               // messageContent = "We are seeing you for the first time. So we don't have any details about you.";
               // finalPersonName = null;
                randomPerson = true;
                invokePolly();
                if (messagesListAdapter != null) {
                    Log.i(TAG, "onActivityResult: Size " + mMessageArrayList.size());
                    messagesListAdapter.setLastRecognizationStatus(true, recognizationSuccess);
                    messagesListAdapter.notifyDataSetChanged();
                    mListView.post(new Runnable() {
                        @Override
                        public void run() {
                            // Select the last row so it will scroll into view...
                            mListView.setSelection(messagesListAdapter.getCount() - 1);
                        }
                    });
                }
            } else {
                final File file = new File(getExternalFilesDir(Environment.DIRECTORY_PICTURES).toString() + "/" + jsonObject.get("matchedImage"));
                System.out.println("key is" + file.getName());
                recognizationSuccess = true;
                if (jsonObject.get("personFirstName") != null && jsonObject.get("role") != null) {
                    finalPersonName = jsonObject.get("personFirstName").toString();
                    personRoleImage = jsonObject.get("role").toString();
                    //name.setText("Name :" + jsonObject.get("personName").toString() + "   Role :" + jsonObject.get("role").toString());
                    mResponseData.setPersonName(finalPersonName);
                    mResponseData.setRole(personRoleImage);
                }

                if (jsonObject.get("gender") != null && jsonObject.get("city") != null) {
                    //gender.setText("Gender :" + jsonObject.get("gender").toString() + "                City :" + jsonObject.get("city").toString());
                    mResponseData.setGender(jsonObject.get("gender").toString());
                    mResponseData.setCity(jsonObject.get("city").toString());
                }
                if (jsonObject.get("smile") != null && jsonObject.get("ageRange") != null) {
                    JSONObject smileJson = (JSONObject) jsonObject.get("smile");
                    System.out.println(smileJson.get("confidence"));
                    String smile = smileJson.get("confidence").toString();
                    char[] smileArr = smile.toCharArray();
                    JSONObject ageJson = (JSONObject) jsonObject.get("ageRange");
                    System.out.println(ageJson.get("low").toString() + "to" + ageJson.get("high").toString());
                    //age.setText("Age :" + ageJson.get("low").toString() + " to " + ageJson.get("high").toString() + "                    Smile  :" + smileArr[0] + smileArr[1] + "%");
                }
                if (jsonObject.get("eyeglasses") != null && jsonObject.get("emotions") != null) {
                    JSONObject eyeGlassJson = (JSONObject) jsonObject.get("eyeglasses");
                    String eye = eyeGlassJson.get("confidence").toString();
                    char[] eyeArr = eye.toCharArray();
                    mResponseData.setConfidence(eye);
                    JSONArray emotionJson = (JSONArray) jsonObject.get("emotions");
                    String emotionStr = "";
                    JSONObject object = emotionJson.getJSONObject(0);
                    String e = object.get("confidence").toString();
                    char[] emotionArr = e.toCharArray();
                    String changeEmotion = object.get("type").toString();
                    mResponseData.setEmotions(changeEmotion);

                    //eyeGlass.setText("EyeGlass : " + eyeGlassJson.get("value").toString() + "  " + eyeArr[0] + eyeArr[1] + "%" + "       Emotions : " + changeEmotion + " " + emotionArr[0] + emotionArr[1] + "% ");
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
                            Log.i(TAG, "onStateChanged: completed downloading so showing up the image");
                            String resultantPath = file.getAbsolutePath();
                            mResponseData.setDownloadImagePath(resultantPath);
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
            if (messagesListAdapter != null) {
                messagesListAdapter.mDownloadCompleted = true;
                messagesListAdapter.setLastRecognizationStatus(true, recognizationSuccess);
                messagesListAdapter.notifyDataSetChanged();
            }
        } catch (Exception e) {
            System.out.println("inside download catch" + e);
        } finally {
            System.out.println("inside download finally");
        }
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

    @Override
    public void dialogReadyForFulfillment(Map<String, String> slots, String intent) {
        Log.i(TAG, "dialogReadyForFulfillment: " + intent);
        Log.d(TAG, String.format(
                Locale.US,
                "Dialog ready for fulfillment:\n\tIntent: %s\n\tSlots: %s",
                intent,
                slots.toString()));
        if (intent.contains("TriggerIntent")) {
            System.out.println("inside camera");
            dispatchTakePictureIntent();
        }
        else if (intent.contains("DailyDashboardIntent")) {
            System.out.println("Go to dashboard.");
            Intent dashboardIntent = new Intent(LauncherScreen.this, DashBoardActivity.class);
            dashboardIntent.putExtra(Constants.RESPONSE_DATA, mResponseData);
            startActivity(dashboardIntent);
        }
    }

    @Override
    public void onResponse(Response response) {
        Log.d(TAG, "Bot response: " + response.getTextResponse());
        Log.i(TAG, "onResponse: " + response.getInputTranscript());
        TextMessage textMessage;
        if (!TextUtils.isEmpty(response.getInputTranscript()) && !response.getInputTranscript().contains("unisysstarone")) {
            textMessage = new TextMessage(response.getInputTranscript(), "user");
            mMessageArrayList.add(textMessage);
        }else {
            textMessage = new TextMessage(response.getInputTranscript(), "user");
            textMessage.setIsItPasswordField(true);
            mMessageArrayList.add(textMessage);
        }
        textMessage = new TextMessage(response.getTextResponse(), "Bot");
        mMessageArrayList.add(textMessage);
        if (messagesListAdapter != null) {
            messagesListAdapter.notifyDataSetChanged();
            mListView.post(new Runnable() {
                @Override
                public void run() {
                    // Select the last row so it will scroll into view...
                    mListView.setSelection(messagesListAdapter.getCount() - 1);
                }
            });
        }

        Log.i(TAG, "onResponse: " + mMessageArrayList.size());
        String res = response.getTextResponse();
        String inputRes = response.getInputTranscript();
        String find = "alert";
        if (res != null && res.toLowerCase().indexOf(find.toLowerCase()) != -1) {
            System.out.println("inside dashboard");
        } else if (TextUtils.isEmpty(res) && (inputRes.equalsIgnoreCase("yes") || inputRes.equalsIgnoreCase("ok"))) { // can you help me with your picture?")) {
            //openCamera();
            //checkForCameraPermission();
            //dispatchTakePictureIntent();
            Log.i(TAG, "onResponse: ");/*Please step in front of the camera so I can see who I am speaking with*/
        } else if (!TextUtils.isEmpty(res) && (res.contains("Please step in front of the camera. so I can see who I am speaking with") || res.contains("Please step in front of the camera so I can see who I am speaking with"))) {
            final Handler handler = new Handler();
            handler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    //checkForCameraPermission();
//                    dispatchTakePictureIntent();
                }
            }, 5000);/*Thank you, lets bring up your. daily operational dashboard .for you to review*/
        } else if (!TextUtils.isEmpty(res) && (res.contains("Thank you, lets bring up your daily operational dashboard for you to review") || res.contains("Thank you, lets bring up your.daily operational dashboard .for you to review"))) {
//            Intent intent = new Intent(LauncherScreen.this, DashBoardActivity.class);
//            intent.putExtra(Constants.RESPONSE_DATA, mResponseData);
//            startActivity(intent);
        } else if (!TextUtils.isEmpty(res) && (res.contains("Thank you. Here you go"))) {

        }

    }

    @Override
    public void onError(String responseText, Exception e) {
        Log.e(TAG, "Error: " + responseText, e);
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
        String speechLine = "";
        if (mFaceNotRecog != null) {
            speechLine = mFaceNotRecog;
            mFaceNotRecog = null;
        } else if (randomPerson == true) {
            speechLine = getString(R.string.randomPerson);
            //mVoiceView.getViewAdapter().onClick(mVoiceView);
        } else if (getItFirstTime()) {
            speechLine = getString(R.string.welcomeMessage);
        } else if (!TextUtils.isEmpty(finalPersonName)) {
            speechLine = "Hello " + finalPersonName + " to be sure it is you, please provide me your secret passcode";
        } else if (!TextUtils.isEmpty(messageContent)) {
            speechLine = getString(R.string.userFaceNotMatchingMess);
        }
        Log.i(TAG, "onPollyPlay: " + speechLine);
        SynthesizeSpeechPresignRequest synthesizeSpeechPresignRequest =
                new SynthesizeSpeechPresignRequest()
                        // Set the text to synthesize.
                        .withText(speechLine)
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
        final String finalSpeechLine = speechLine;
        mediaPlayer.setOnPreparedListener(new MediaPlayer.OnPreparedListener() {
            @Override
            public void onPrepared(MediaPlayer mp) {
                System.out.println("onPrepared" + mp);
                System.out.println("is playing " + mp.isPlaying());
                mp.start();
                if (randomPerson) {
                    Intent intent = new Intent(LauncherScreen.this, PhotoAnimate.class);
                    intent.putExtra(liveImage, liveImage);
                    startActivity(intent);

                }
                //update adapter first time at launching time
                if (getItFirstTime()) {
                    setItFirstTime(false);
                    addTextMessageInListAndRefreshListView("Bot", getString(R.string.welcomeMessage));
                } else if (!TextUtils.isEmpty(finalSpeechLine) && finalSpeechLine.contains("Hello")) {
                    addTextMessageInListAndRefreshListView("Bot", finalSpeechLine);
                } else if (!TextUtils.isEmpty(messageContent) && messageContent.contains(getString(R.string.userFaceNotMatchingMess))) {
                    addTextMessageInListAndRefreshListView("Bot", getString(R.string.userFaceNotMatchingMess));
                } else if (randomPerson) {
                    addTextMessageInListAndRefreshListView("Bot", getString(R.string.randomPerson));
                }
            }
        });

        // Set the callback to release the MediaPlayer after playback is completed.
        mediaPlayer.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
            @Override
            public void onCompletion(MediaPlayer mp) {
                System.out.println("onCompletion" + mp);
                mp.release();
                Log.i(TAG, "onCompletion: getItFirstTime" + getItFirstTime());
                //if(getItFirstTime()){
                Log.i(TAG, "onCompletion: View Dispatch");
            }
        });

    }

    private void addTextMessageInListAndRefreshListView(String from, String message) {
        TextMessage textMessage = new TextMessage();
        textMessage.setFrom(from);
        textMessage.setMessage(message);
        mMessageArrayList.add(textMessage);
        if (messagesListAdapter != null) {
            messagesListAdapter.notifyDataSetChanged();
            mListView.post(new Runnable() {
                @Override
                public void run() {
                    // Select the last row so it will scroll into view...
                    mListView.setSelection(messagesListAdapter.getCount() - 1);
                }
            });
        }
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
//                        int targetW = dashImageView.getWidth();
//                        int targetH = dashImageView.getHeight();
                        String resultantPath = file.getAbsolutePath();
                        Log.i(TAG, "onStateChanged: DownloadPath" + resultantPath);
                        // Get the dimensions of the bitmap
                        BitmapFactory.Options bmOptions = new BitmapFactory.Options();
                        bmOptions.inJustDecodeBounds = true;
                        BitmapFactory.decodeFile(resultantPath, bmOptions);
                        int photoW = bmOptions.outWidth;
                        int photoH = bmOptions.outHeight;

                        // Determine how much to scale down the image
                        //int scaleFactor = Math.min(photoW / targetW, photoH / targetH);

                        // Decode the image file into a Bitmap sized to fill the View
                        bmOptions.inJustDecodeBounds = false;
                        //bmOptions.inSampleSize = scaleFactor;
                        bmOptions.inPurgeable = true;
                        Bitmap bitmap = BitmapFactory.decodeFile(resultantPath, bmOptions);
                        // imageView.setImageBitmap(bitmap);
                        //dashImageView.setImageBitmap(bitmap);
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
    public void onAudioPlayBackCompleted()
    {
        Log.i(TAG, "onAudioPlayBackCompleted: " );
    }

    @Override
    public void onAudioPlaybackError(Exception e) {
        // Audio playback failed.
        Log.i(TAG, "onAudioPlaybackError: " );

    }

    @Override
    public void onAudioPlaybackStarted()
    {
        Log.i(TAG, "onAudioPlaybackStarted: " );

    }


}
