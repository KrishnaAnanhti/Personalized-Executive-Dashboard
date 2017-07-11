package demo.unisys.com.rekoginitionfinal.ui;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.Drawable;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.os.AsyncTask;
import android.os.CountDownTimer;
import android.os.Handler;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.amazonaws.auth.CognitoCachingCredentialsProvider;
import com.amazonaws.auth.CognitoCredentialsProvider;
import com.amazonaws.mobileconnectors.lex.interactionkit.Response;
import com.amazonaws.mobileconnectors.lex.interactionkit.config.InteractionConfig;
import com.amazonaws.mobileconnectors.s3.transferutility.TransferUtility;
import com.amazonaws.regions.Regions;
import com.amazonaws.services.polly.AmazonPollyPresigningClient;
import com.amazonaws.services.polly.model.DescribeVoicesRequest;
import com.amazonaws.services.polly.model.DescribeVoicesResult;
import com.amazonaws.services.polly.model.OutputFormat;
import com.amazonaws.services.polly.model.SynthesizeSpeechPresignRequest;
import com.amazonaws.services.polly.model.Voice;
import com.amazonaws.services.s3.AmazonS3Client;

import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import demo.unisys.com.rekoginitionfinal.*;
import demo.unisys.com.rekoginitionfinal.adapter.MessagesListAdapter;
import demo.unisys.com.rekoginitionfinal.constant.Constants;
import demo.unisys.com.rekoginitionfinal.model.ResponseData;
import demo.unisys.com.rekoginitionfinal.model.TextMessage;
import demo.unisys.com.rekoginitionfinal.utils.Util;

public class DashBoardActivity extends AppCompatActivity implements InteractiveVoiceView.InteractiveVoiceListener, View.OnClickListener {
    private ResponseData mResponseData;
    private ImageView mUserImage;
    private TextView mPersonName;
    private TextView mPersonRole;
    private static final String TAG = "DashBoardActivity";
    private TransferUtility transferUtility;
    private AmazonS3Client s3Client;
    private List<Voice> voices;
    private AmazonPollyPresigningClient pollyClient;
    private Context appContext;
    private demo.unisys.com.rekoginitionfinal.InteractiveVoiceView voiceView;
    private com.amazonaws.mobileconnectors.lex.interactionkit.ui.InteractiveVoiceViewAdapter voiceViewAdapter;
    private ImageView mDashBoardImageView;
    private TextView mTxtTicketView;
    private TextView mTxtUserMessage;
    private Animation animFadeOut;
    private TextView mTxtBotMessage;
    private CountDownTimer countDownTimer = null;
    private ArrayList<TextMessage> mTextMessageArrayList;
    private ListView mMessageListView;
    private Boolean mFlagChatIconClick = true;
    private MessagesListAdapter mMessageListAdapter = null;
    private Boolean mIsBackButtonPressed = true;
    private RelativeLayout mGifWeatherRelativeLayout;
    private ImageView mGifDashBoardImageView;
    private TextView mTicketViewClick;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        s3Client = Util.getS3Client(this);
        transferUtility = Util.getTransferUtility(this);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_dash_board);
        mResponseData = getIntent().getParcelableExtra(Constants.RESPONSE_DATA);
        initView();
        init();
        mTextMessageArrayList = new ArrayList<>();
        mMessageListAdapter = new MessagesListAdapter(this, mTextMessageArrayList);
        mMessageListView.setAdapter(mMessageListAdapter);
        mMessageListView.setDividerHeight(0);
        setListenerOnView();
    }

    private void setListenerOnView() {
        mTicketViewClick.setOnClickListener(this);
    }

    @Override
    protected void onResume() {
        super.onResume();
    }

    private void init() {
        appContext = getApplicationContext();
        voiceView = (demo.unisys.com.rekoginitionfinal.InteractiveVoiceView) findViewById(R.id.voiceInterface);
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

    private void initView() {
        mTicketViewClick = (TextView) findViewById(R.id.txtTicketClickView);
        mGifWeatherRelativeLayout = (RelativeLayout) findViewById(R.id.gifFramelayout);
        mGifDashBoardImageView = (ImageView) findViewById(R.id.dashBoardImageView);

        mMessageListView = (ListView) findViewById(R.id.chatListView);
        mTxtBotMessage = (TextView) findViewById(R.id.txtMessageTwo);
        mTxtUserMessage = (TextView) findViewById(R.id.txtMessageOne);
        mTxtTicketView = (TextView) findViewById(R.id.txtClickView);
        mDashBoardImageView = (ImageView) findViewById(R.id.imgBodyContent);
        mUserImage = (ImageView) findViewById(R.id.imageProfile);
        mPersonName = (TextView) findViewById(R.id.txtPersonName);
        mPersonRole = (TextView) findViewById(R.id.txtPersonRole);
        ImageView imgSmiley = (ImageView) findViewById(R.id.imgSmiley);
        TextView txtPersonPlace = (TextView) findViewById(R.id.txtPersonPlace);

        if (mResponseData != null) {
            Log.i("TAG", "initView: " + mResponseData.getDownloadImagePath());
            if (!TextUtils.isEmpty(mResponseData.getDownloadImagePath())) {
                Bitmap myBitmap = BitmapFactory.decodeFile(mResponseData.getDownloadImagePath());
                mUserImage.setImageBitmap(myBitmap);
            }
            mPersonName.setText(mResponseData.getPersonName());
            mPersonRole.setText(mResponseData.getRole());

            this.setEmotionBadge(mResponseData.getEmotions(), imgSmiley);
            txtPersonPlace.setText(mResponseData.getCity());
        }
        mTxtTicketView.setOnClickListener(this);
        animFadeOut = AnimationUtils.loadAnimation(getApplicationContext(),
                R.anim.fade_out);
        findViewById(R.id.imgChatHistory).setOnClickListener(this);
    }

    private void visibilityTicketClickView(boolean flag) {
        if (flag) {
            mTicketViewClick.setVisibility(View.VISIBLE);
        } else {
            mTicketViewClick.setVisibility(View.GONE);
        }
    }

    private void setEmotionBadge(final String emotion, ImageView emotionImageview) {
        int emotionImage = R.drawable.happy;
        switch (emotion) {
            case "CONFUSED": {
                emotionImage = R.drawable.confused;
                break;
            }
            case "CALM":
            case "UNKNOWN": {
                emotionImage = R.drawable.calm;
                break;
            }
            case "ANGRY": {
                emotionImage = R.drawable.angry;
                break;
            }
            case "HAPPY": {
                emotionImage = R.drawable.happy;
                break;
            }
            case "SAD": {
                emotionImage = R.drawable.sad;
                break;
            }
            case "DISGUSTED": {
                emotionImage = R.drawable.disgusted;
                break;
            }
            case "SURPRISED": {
                emotionImage = R.drawable.surprise;
                break;
            }
        }
        emotionImageview.setImageDrawable(ActivityCompat.getDrawable(this, emotionImage));
    }

    @Override
    public void dialogReadyForFulfillment(Map<String, String> slots, String intent) {
        Log.i(TAG, "dialogReadyForFulfillment: " + intent);
        Log.d(TAG, String.format(
                Locale.US,
                "Dialog ready for fulfillment:\n\tIntent: %s\n\tSlots: %s",
                intent,
                slots.toString()));

        if (intent.contains("UserViewIntent")) {
            System.out.println("Show UX report");
            mDashBoardImageView.setImageDrawable(ActivityCompat.getDrawable(this, R.drawable.dashboard2uxview));

            final Drawable UxDashboardWithTexasEvent = ActivityCompat.getDrawable(this, R.drawable.dashboard3mapview);

            new Handler().postDelayed(new Runnable() {
                @Override
                public void run() {
                    invokePolly();
                    mDashBoardImageView.setImageDrawable(UxDashboardWithTexasEvent);
                    String personName = "";
                    if (mResponseData != null && !TextUtils.isEmpty(mResponseData.getPersonName())) {
                        personName = mResponseData.getPersonName();
                    }
                    addDataToArrayList("Bot", "Excuse me," + personName);
                }
            }, 10000);

        } else if (intent.contains("WeatherViewIntent")) {
            System.out.println("Show weather report.");
            showWeatherViewAndHideDashBoardImageView();
        } else if (intent.contains("CreateTicketIntent")) {
            System.out.println("Show weather dashboard7_ticket.");
            mDashBoardImageView.setImageDrawable(ActivityCompat.getDrawable(this, R.drawable.dashboard6));
            visibilityOfTotalTicketClickView(false);
            visibilityTicketClickView(true);
        }

    }

    private void showWeatherViewAndHideDashBoardImageView() {
        visibilityOfDashBoardImageView(false);
        visibilityOfGifImageViewFrameLayout(true);
        mGifDashBoardImageView.setImageDrawable(ActivityCompat.getDrawable(this, R.drawable.dashboard4_weather_map));
    }


    private void visibilityOfGifImageViewFrameLayout(boolean b) {
        if (b) {
            mGifWeatherRelativeLayout.setVisibility(View.VISIBLE);
        } else {
            mGifWeatherRelativeLayout.setVisibility(View.GONE);
        }
    }

    private void visibilityOfDashBoardImageView(boolean b) {
        if (b) {
            mDashBoardImageView.setVisibility(View.VISIBLE);
        } else {
            mDashBoardImageView.setVisibility(View.GONE);
        }
    }

    @Override
    public void onBackPressed() {
        if (mIsBackButtonPressed) {
            backButtonPressed();
        } else {
            super.onBackPressed();
        }
    }

    private void backButtonPressed() {
        mDashBoardImageView.setImageDrawable(ActivityCompat.getDrawable(this,
                R.drawable.dashboard3mapview));
        mIsBackButtonPressed = false;
        visibilityOfMicView(true);
    }

    @Override
    public void onResponse(final Response response) {
        Log.d(TAG, "Bot response: " + response.getTextResponse());
        Log.i(TAG, "onResponse: " + response.getInputTranscript());

        visibilityOfDashBoardImageView(true);
        visibilityOfGifImageViewFrameLayout(false);

        String res = response.getTextResponse();
        visibilityOfTotalTicketClickView(false);
       if (!TextUtils.isEmpty(res) && res.contains("send alerts to the application service teams and create a watch ticket related to this alert")) {
            Log.i(TAG, "onResponse: ");
            showWeatherViewAndHideDashBoardImageView();
        } else if (!TextUtils.isEmpty(res) && res.contains("Thank you. I have created the watch ticket for you")) {
            mDashBoardImageView.setImageDrawable(ActivityCompat.getDrawable(this, R.drawable.dashboard5));
            visibilityOfTotalTicketClickView(true);
            visibilityOfMicView(false);
        }
        if (!TextUtils.isEmpty(response.getInputTranscript())) {
            visibilityOfUserMessageView(true);
            mTxtUserMessage.setText(response.getInputTranscript());
            mTxtUserMessage.setBackgroundResource(R.drawable.green_chat_rectangle);
            addDataToArrayList("User", response.getInputTranscript());
        } else {
            visibilityOfUserMessageView(false);
        }
        if (!TextUtils.isEmpty(response.getTextResponse())) {
            visibilityOfBotMessageView(true);
            mTxtBotMessage.setText(response.getTextResponse());
            mTxtBotMessage.setBackgroundResource(R.drawable.red_chat_rectangle);
            addDataToArrayList("Bot", response.getTextResponse());
        } else {
            visibilityOfBotMessageView(false);
        }

        if (mMessageListView.getVisibility() == View.VISIBLE) {
            mMessageListAdapter.notifyDataSetChanged();
            mMessageListView.post(new Runnable() {
                @Override
                public void run() {
                    // Select the last row so it will scroll into view...
                    mMessageListView.setSelection(mMessageListAdapter.getCount() - 1);
                }
            });
        }
    }

    private void visibilityOfMicView(boolean b) {
        if (b) {
            voiceView.setVisibility(View.VISIBLE);
        } else {
            voiceView.setVisibility(View.GONE);
        }
    }

    private void addDataToArrayList(String from, String message) {
        TextMessage textMessage = new TextMessage();
        textMessage.setFrom(from);
        textMessage.setMessage(message);
        mTextMessageArrayList.add(textMessage);
    }

    private void visibilityOfBotMessageView(boolean b) {
        if (b) {
            mTxtBotMessage.setVisibility(View.VISIBLE);
            invalidateFadeOutAnimation();
        } else {
            mTxtBotMessage.setVisibility(View.GONE);
        }
    }

    private void visibilityOfUserMessageView(boolean b) {
        if (b) {
            mTxtUserMessage.setVisibility(View.VISIBLE);
            invalidateFadeOutAnimation();
        } else {
            mTxtUserMessage.setVisibility(View.GONE);
        }
    }

    private void visibilityOfTotalTicketClickView(boolean b) {
        if (b) {
            mTxtTicketView.setVisibility(View.VISIBLE);
        } else {
            mTxtTicketView.setVisibility(View.GONE);
        }
    }

    private void invalidateFadeOutAnimation() {
        if (countDownTimer != null) {
            countDownTimer.cancel();
            countDownTimer = null;
        }
    }

        private void fadeOutAnimation(int duration) {
        Log.i(TAG, "fadeOutAnimation: ");
        if (countDownTimer != null) {
            countDownTimer.cancel();
            countDownTimer = null;
        }

        countDownTimer = new CountDownTimer(duration, duration) {
            public void onTick(long millisUntilFinished) {
                //called every 300 milliseconds, which could be used to
                //send messages or some other action

            }

            public void onFinish() {
                //After 60000 milliseconds (60 sec) finish current
                //if you would like to execute something when time finishes
                mTxtUserMessage.startAnimation(animFadeOut);
                mTxtBotMessage.startAnimation(animFadeOut);

            }
        }.start();
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
        String speechLine = null;
        final Boolean flag;
        speechLine = "Excuse me, " + (mResponseData != null ? mResponseData.getPersonName() : null + "?");
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
        mediaPlayer.setOnPreparedListener(new MediaPlayer.OnPreparedListener() {
            @Override
            public void onPrepared(MediaPlayer mp) {
                System.out.println("onPrepared" + mp);
                System.out.println("is playing " + mp.isPlaying());
                mp.start();
                mTxtBotMessage.setText("Excuse me, " + (mResponseData != null ? mResponseData.getPersonName() : null + "?"));

            }
        });

// Set the callback to release the MediaPlayer after playback is completed.
        mediaPlayer.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
            @Override
            public void onCompletion(MediaPlayer mp) {
                System.out.println("onCompletion" + mp);
                mp.release();
                //fadeOutAnimation(5000);
            }
        });

    }

    @Override
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.txtTicketClickView:
                mDashBoardImageView.setImageDrawable(ActivityCompat.getDrawable(this, R.drawable.dashboard7_ticket));
                break;
            case R.id.txtClickView:
                mDashBoardImageView.setImageDrawable(ActivityCompat.getDrawable(DashBoardActivity.this,
                        R.drawable.dashboard6));
                visibilityTicketClickView(true);
                break;
            case R.id.imgChatHistory:
               chatHistory();
                break;

        }
    }

    private void chatHistory() {
        if (mFlagChatIconClick) {
            if (mTextMessageArrayList.size() > 0) {
                mFlagChatIconClick = false;
                RelativeLayout.LayoutParams layoutParams = (RelativeLayout.LayoutParams) mMessageListView.getLayoutParams();
                layoutParams.height = RelativeLayout.LayoutParams.WRAP_CONTENT;
                mMessageListView.setLayoutParams(layoutParams);
            }
        } else {
            mFlagChatIconClick = true;
            RelativeLayout.LayoutParams layoutParams = (RelativeLayout.LayoutParams) mMessageListView.getLayoutParams();
            layoutParams.height = 0;
            mMessageListView.setLayoutParams(layoutParams);
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


    @Override
    public void onAudioPlayBackCompleted() {
        Log.i(TAG, "onAudioPlayBackCompleted: ");
        fadeOutAnimation(5000);
    }

    @Override
    public void onAudioPlaybackError(Exception e) {
        // Audio playback failed.
        Log.i(TAG, "onAudioPlaybackError: ");
        fadeOutAnimation(5000);

    }

    @Override
    public void onAudioPlaybackStarted() {
        Log.i(TAG, "onAudioPlaybackStarted: ");

    }
}
