package com.bamboo.bambooheli.activity;

import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.view.ContextThemeWrapper;
import android.support.v7.widget.PopupMenu;
import android.util.Log;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Button;
import android.widget.GridLayout;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ToggleButton;

import com.parrot.arsdk.arcommands.ARCOMMANDS_ARDRONE3_MEDIARECORDEVENT_PICTUREEVENTCHANGED_ERROR_ENUM;
import com.parrot.arsdk.arcommands.ARCOMMANDS_ARDRONE3_PILOTINGSTATE_FLYINGSTATECHANGED_STATE_ENUM;
import com.parrot.arsdk.arcontroller.ARCONTROLLER_DEVICE_STATE_ENUM;
import com.parrot.arsdk.arcontroller.ARCONTROLLER_STREAM_CODEC_TYPE_ENUM;
import com.parrot.arsdk.arcontroller.ARControllerCodec;
import com.parrot.arsdk.arcontroller.ARFrame;
import com.parrot.arsdk.ardiscovery.ARDiscoveryDeviceService;
import com.bamboo.bambooheli.R;
import com.bamboo.bambooheli.drone.BebopDrone;
import com.bamboo.bambooheli.drone.Beeper;
import com.bamboo.bambooheli.view.BebopVideoView;
import com.bamboo.bambooheli.view.FaceDetect;
import com.bamboo.bambooheli.view.WideShot;

import java.nio.ByteBuffer;

public class BebopActivity extends AppCompatActivity {
    public static final int LEVEL_LAND = 1;
    public static final int LEVEL_TAKEOFF = 0;

    private static final String TAG = "BebopActivity";
    private BebopDrone mBebopDrone;

    private ProgressDialog mConnectionProgressDialog;
    private ProgressDialog mDownloadProgressDialog;

    private BebopVideoView mVideoView;
    private FaceDetect mFaceDetect;
    private ImageView mImageView;

    private ImageButton mTakeOffLandBt;
    private ImageButton mAdditionalBt;
    private GridLayout mAddtionalItems;
    private ToggleButton mDetectBt;
    private ToggleButton mWideShotBt;
    private ToggleButton mTimerBt;

    private ImageButton mDownloadBt;

    private ImageView mBatteryIndicator;

    private ByteBuffer mSpsBuffer;
    private ByteBuffer mPpsBuffer;
    private int mNbMaxDownload;
    private int mCurrentDownloadIndex;
    private boolean isDetect = false;
    private boolean isFollow = false;
    private boolean isSmile = false;

    // variable for timer
    private boolean isTimerMode = false;
    private ToggleButton followBtn;
    private ToggleButton smileBtn;
    private ImageButton startBtn;
    private TextView timer;
    private SeekBar seekBar;
    private CountDownTimer mCountDown = null;
    private Beeper beep;
    private Beeper beepFinish;


    private boolean isWide = false;
    private WideShot mWideShot;
    private ImageButton wideStart;

    private  boolean isAdditional = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_bebop);

        initIHM();

        Intent intent = getIntent();
        ARDiscoveryDeviceService service = intent.getParcelableExtra(DeviceListActivity.EXTRA_DEVICE_SERVICE);
        mBebopDrone = new BebopDrone(this, service);
        mBebopDrone.addListener(mBebopListener);

    }

    @Override
    protected void onStart() {
        super.onStart();

        // show a loading view while the bebop drone is connecting
        if ((mBebopDrone != null) && !(ARCONTROLLER_DEVICE_STATE_ENUM.ARCONTROLLER_DEVICE_STATE_RUNNING.equals(mBebopDrone.getConnectionState())))
        {
            mConnectionProgressDialog = new ProgressDialog(this, R.style.AppCompatAlertDialogStyle);
            mConnectionProgressDialog.setIndeterminate(true);
            mConnectionProgressDialog.setMessage("Connecting ...");
            mConnectionProgressDialog.setCancelable(false);
            mConnectionProgressDialog.show();

            // if the connection to the Bebop fails, finish the activity
            if (!mBebopDrone.connect()) {
                finish();
            }
        }
    }

    @Override
    public void onBackPressed() {
        if (mBebopDrone != null)
        {
            mConnectionProgressDialog = new ProgressDialog(this, R.style.AppCompatAlertDialogStyle);
            mConnectionProgressDialog.setIndeterminate(true);
            mConnectionProgressDialog.setMessage("Disconnecting ...");
            mConnectionProgressDialog.setCancelable(false);
            mConnectionProgressDialog.show();

            if (!mBebopDrone.disconnect()) {
                finish();
            }
        }
    }
    @Override
    public void onDestroy()
    {
        mBebopDrone.dispose();
        super.onDestroy();
    }
    private void timerStart(){
        final int inputTime = Integer.parseInt(timer.getText().toString());
        if(inputTime != 0){
            mCountDown = new CountDownTimer((inputTime+1) * 1000, 1000) {
                int nowTime = inputTime + 1;
                @Override
                public void onTick(long millisUntilFinished) {
                    beep.play();
                    timer.setText(""+ (--nowTime));
                }
                @Override
                public void onFinish() {
                    timer.setText(""+ (--nowTime));
                    takePicture();
                    //download();

                    seekBar.setProgress(1);
                }
            }.start();
        }
    }

    private void takePicture(){
        beepFinish.play();
        mBebopDrone.takePicture();
    }

    private void download() {
        mBebopDrone.getLastFlightMedias();

        mDownloadProgressDialog = new ProgressDialog(BebopActivity.this, R.style.AppCompatAlertDialogStyle);
        mDownloadProgressDialog.setIndeterminate(true);
        mDownloadProgressDialog.setMessage("Fetching medias");
        mDownloadProgressDialog.setCancelable(false);
        mDownloadProgressDialog.setButton(DialogInterface.BUTTON_NEGATIVE, "Cancel", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                mBebopDrone.cancelGetLastFlightMedias();
            }
        });
        mDownloadProgressDialog.show();
    }

    private void initIHM() {
        mVideoView = (BebopVideoView) findViewById(R.id.videoView);
        mVideoView.setSurfaceTextureListener(mVideoView);

        mFaceDetect = (FaceDetect)findViewById(R.id.faceDetect);
        mImageView = (ImageView)findViewById(R.id.imageView);

        mWideShot = new WideShot();

        mBatteryIndicator = (ImageView) findViewById(R.id.battery_indicator);

        followBtn = (ToggleButton)findViewById(R.id.followBtn);
        followBtn.setEnabled(false);
        followBtn.setVisibility(View.INVISIBLE);
        followBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if(isFollow){
                    isFollow = false;
                    mFaceDetect.resetFollow();
                    smileBtn.setVisibility(View.INVISIBLE);
                    followBtn.setBackgroundDrawable(getResources().getDrawable(R.drawable.if_follow_off));
                    Toast toast = Toast.makeText(getApplicationContext(), "Follow off", Toast.LENGTH_LONG);
                    toast.show();
                }else{
                    isFollow = true;
                    mFaceDetect.setFollow();
                    smileBtn.setVisibility(View.VISIBLE);
                    followBtn.setBackgroundDrawable(getResources().getDrawable(R.drawable.if_follow_on));
                    Toast toast = Toast.makeText(getApplicationContext(), "Follow on", Toast.LENGTH_LONG);
                    toast.show();
                }
            }
        });

        timer = (TextView) findViewById(R.id.TimerText);
        timer.setEnabled(false);
        timer.setVisibility(View.INVISIBLE);

        seekBar = (SeekBar)findViewById(R.id.seekBar);
        seekBar.setEnabled(false);
        seekBar.setVisibility(View.INVISIBLE);
        seekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            public void onStopTrackingTouch(SeekBar seekBar) {
            }

            public void onStartTrackingTouch(SeekBar seekBar) {
            }

            public void onProgressChanged(SeekBar seekBar, int progress,
                                          boolean fromUser) {
                timer.setText("" + progress);
            }
        });

        startBtn = (ImageButton)findViewById(R.id.startBtn);
        startBtn.setEnabled(false);
        startBtn.setVisibility(View.INVISIBLE);
        startBtn.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View v) {
                timerStart();
            }
        });

        wideStart = (ImageButton)findViewById(R.id.wideStartBtn);
        wideStart.setEnabled(false);
        wideStart.setVisibility(View.INVISIBLE);
        wideStart.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View v) {
                Toast toast = Toast.makeText(getApplicationContext(), "WideShot Start", Toast.LENGTH_LONG);
                toast.show();
                mWideShot.resume(mBebopDrone, beep, beepFinish);
            }
        });

        smileBtn = (ToggleButton)findViewById(R.id.smileBtn);
        smileBtn.setEnabled(false);
        smileBtn.setVisibility(View.INVISIBLE);
        smileBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if(isSmile){
                    isSmile = false;
                    mFaceDetect.resetSmileShot();
                    smileBtn.setBackgroundDrawable(getResources().getDrawable(R.drawable.if_smile_off));
                    Toast toast = Toast.makeText(getApplicationContext(), "Smile off", Toast.LENGTH_LONG);
                    toast.show();
                }else{
                    isSmile = true;
                    mFaceDetect.setSmileShot();
                    smileBtn.setBackgroundDrawable(getResources().getDrawable(R.drawable.if_smile_on));
                    Toast toast = Toast.makeText(getApplicationContext(), "얼굴을 화면 중앙에 맞추고 찰칵 소리가 날 때까지 웃으세요!", Toast.LENGTH_LONG);
                    toast.show();
                }
            }
        });

        beep = new Beeper(this, R.raw.beep_repeat2);
        beepFinish = new Beeper(this, R.raw.beep_camera);

        findViewById(R.id.emergencyBt).setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                mBebopDrone.emergency();
            }
        });

        mDownloadBt = (ImageButton)findViewById(R.id.downloadBt);
        mDownloadBt.setEnabled(true);
        mDownloadBt.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                download();
            }
        });

        mAddtionalItems = (GridLayout)findViewById(R.id.additionalMenuItems);
        mAddtionalItems.setEnabled(false);
        mAddtionalItems.setVisibility(View.INVISIBLE);

        mDetectBt = (ToggleButton)findViewById(R.id.Detect);
        mDetectBt.setOnClickListener(new View.OnClickListener(){
            public void onClick(View v){
                if(isDetect){ // 얼굴인식을 안할때 즉, unfollow일때
                    isDetect = false;
                    mFaceDetect.pause();
                    mDetectBt.setBackgroundDrawable(getResources().getDrawable(R.drawable.if_detect_off));
                    isFollow = false;
                    followBtn.setBackgroundDrawable(getResources().getDrawable(R.drawable.if_follow_off));
                    smileBtn.setBackgroundDrawable(getResources().getDrawable(R.drawable.if_smile_off));
                }else{
                    isDetect = true;
                    mFaceDetect.resume(mVideoView, mImageView, mBebopDrone, followBtn, smileBtn, beepFinish);
                    mDetectBt.setBackgroundDrawable(getResources().getDrawable(R.drawable.if_detect_on));
                }
            }
        });

        mWideShotBt = (ToggleButton)findViewById(R.id.WideShot);
        mWideShotBt.setOnClickListener(new View.OnClickListener(){
            public void onClick(View v){
                if(isWide){
                    isWide = false;
                    wideStart.setVisibility(View.INVISIBLE);
                    wideStart.setEnabled(false);
                    mWideShotBt.setBackgroundDrawable(getResources().getDrawable(R.drawable.if_wideshot_off));
                }else{
                    isWide = true;
                    wideStart.setVisibility(View.VISIBLE);
                    wideStart.setEnabled(true);
                    mWideShotBt.setBackgroundDrawable(getResources().getDrawable(R.drawable.if_wideshot_on));
                }
            }
        });
        mTimerBt = (ToggleButton)findViewById(R.id.Timer);
        mTimerBt.setOnClickListener(new View.OnClickListener(){
            public void onClick(View v){
                if(isTimerMode){
                    isTimerMode = false;
                    startBtn.setVisibility(View.INVISIBLE);
                    startBtn.setEnabled(false);
                    timer.setVisibility(View.INVISIBLE);
                    timer.setEnabled(false);
                    seekBar.setVisibility(View.INVISIBLE);
                    seekBar.setEnabled(false);
                    mTimerBt.setBackgroundDrawable(getResources().getDrawable(R.drawable.if_timer_off));
                }else{
                    isTimerMode = true;
                    startBtn.setVisibility(View.VISIBLE);
                    startBtn.setEnabled(true);
                    timer.setVisibility(View.VISIBLE);
                    timer.setEnabled(true);
                    seekBar.setVisibility(View.VISIBLE);
                    seekBar.setEnabled(true);
                    mTimerBt.setBackgroundDrawable(getResources().getDrawable(R.drawable.if_timer_on));
                }
            }
        });

        mAdditionalBt = (ImageButton)findViewById(R.id.additionalMenu);
        mAdditionalBt.setOnClickListener(new View.OnClickListener(){
            public void onClick(View v){
                if(isAdditional){
                    isAdditional = false;
                    mAddtionalItems.setEnabled(false);
                    mAddtionalItems.setVisibility(View.INVISIBLE);
                }else{
                    isAdditional = true;
                    mAddtionalItems.setEnabled(true);
                    mAddtionalItems.setVisibility(View.VISIBLE);
                }
            }
        });

        mTakeOffLandBt = (ImageButton) findViewById(R.id.btn_takeoff_land);
        mTakeOffLandBt.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                switch (mBebopDrone.getFlyingState()) {
                    case ARCOMMANDS_ARDRONE3_PILOTINGSTATE_FLYINGSTATECHANGED_STATE_LANDED:
                        mBebopDrone.takeOff();
                        break;
                    case ARCOMMANDS_ARDRONE3_PILOTINGSTATE_FLYINGSTATECHANGED_STATE_FLYING:
                    case ARCOMMANDS_ARDRONE3_PILOTINGSTATE_FLYINGSTATECHANGED_STATE_HOVERING:
                        mBebopDrone.land();
                        break;
                    default:
                }
            }
        });

        findViewById(R.id.takePictureBt).setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                takePicture();
            }
        });

        findViewById(R.id.btn_gaz_up).setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                switch (event.getAction()) {
                    case MotionEvent.ACTION_DOWN:
                        v.setPressed(true);
                        mBebopDrone.setGaz((byte) 50);
                        break;

                    case MotionEvent.ACTION_UP:
                        v.setPressed(false);
                        mBebopDrone.setGaz((byte) 0);
                        break;

                    default:

                        break;
                }

                return true;
            }
        });

        findViewById(R.id.automove).setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                switch (event.getAction()) {
                    case MotionEvent.ACTION_DOWN:
                        v.setPressed(true);
                        mBebopDrone.setPitch((byte)200);
                        mBebopDrone.setPitch((byte)-200);
                        mBebopDrone.setPitch((byte)200);
                        mBebopDrone.setPitch((byte)-200);

                        break;

                    case MotionEvent.ACTION_UP:
                        v.setPressed(false);
                        mBebopDrone.setGaz((byte) 0);
                        break;

                    default:

                        break;
                }

                return true;
            }
        });



        findViewById(R.id.btn_gaz_down).setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                switch (event.getAction()) {
                    case MotionEvent.ACTION_DOWN:
                        v.setPressed(true);
                        mBebopDrone.setGaz((byte) -50);
                        break;

                    case MotionEvent.ACTION_UP:
                        v.setPressed(false);
                        mBebopDrone.setGaz((byte) 0);
                        break;

                    default:

                        break;
                }

                return true;
            }
        });

        findViewById(R.id.btn_yaw_left).setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                switch (event.getAction()) {
                    case MotionEvent.ACTION_DOWN:
                        v.setPressed(true);
                        mBebopDrone.setYaw((byte) -50);
                        break;

                    case MotionEvent.ACTION_UP:
                        v.setPressed(false);
                        mBebopDrone.setYaw((byte) 0);
                        break;

                    default:

                        break;
                }

                return true;
            }
        });

        findViewById(R.id.btn_yaw_right).setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                switch (event.getAction()) {
                    case MotionEvent.ACTION_DOWN:
                        v.setPressed(true);
                        mBebopDrone.setYaw((byte) 50);
                        break;

                    case MotionEvent.ACTION_UP:
                        v.setPressed(false);
                        mBebopDrone.setYaw((byte) 0);
                        break;

                    default:

                        break;
                }

                return true;
            }
        });

        findViewById(R.id.btn_forward).setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                switch (event.getAction()) {
                    case MotionEvent.ACTION_DOWN:
                        v.setPressed(true);
                        mBebopDrone.setPitch((byte) 50);
                        mBebopDrone.setFlag((byte) 1);
                        break;

                    case MotionEvent.ACTION_UP:
                        v.setPressed(false);
                        mBebopDrone.setPitch((byte) 0);
                        mBebopDrone.setFlag((byte) 0);
                        break;

                    default:

                        break;
                }

                return true;
            }
        });

        findViewById(R.id.btn_back).setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                switch (event.getAction()) {
                    case MotionEvent.ACTION_DOWN:
                        v.setPressed(true);
                        mBebopDrone.setPitch((byte) -50);
                        mBebopDrone.setFlag((byte) 1);
                        break;

                    case MotionEvent.ACTION_UP:
                        v.setPressed(false);
                        mBebopDrone.setPitch((byte) 0);
                        mBebopDrone.setFlag((byte) 0);
                        break;

                    default:

                        break;
                }

                return true;
            }
        });

        findViewById(R.id.btn_roll_left).setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                switch (event.getAction()) {
                    case MotionEvent.ACTION_DOWN:
                        v.setPressed(true);
                        mBebopDrone.setRoll((byte) -50);
                        mBebopDrone.setFlag((byte) 1);
                        break;

                    case MotionEvent.ACTION_UP:
                        v.setPressed(false);
                        mBebopDrone.setRoll((byte) 0);
                        mBebopDrone.setFlag((byte) 0);
                        break;

                    default:

                        break;
                }

                return true;
            }
        });

        findViewById(R.id.btn_roll_right).setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                switch (event.getAction()) {
                    case MotionEvent.ACTION_DOWN:
                        v.setPressed(true);
                        mBebopDrone.setRoll((byte) 50);
                        mBebopDrone.setFlag((byte) 1);
                        break;

                    case MotionEvent.ACTION_UP:
                        v.setPressed(false);
                        mBebopDrone.setRoll((byte) 0);
                        mBebopDrone.setFlag((byte) 0);
                        break;

                    default:

                        break;
                }

                return true;
            }
        });
    }

    private final BebopDrone.Listener mBebopListener = new BebopDrone.Listener() {
        @Override
        public void onDroneConnectionChanged(ARCONTROLLER_DEVICE_STATE_ENUM state) {
            switch (state)
            {
                case ARCONTROLLER_DEVICE_STATE_RUNNING:
                    mConnectionProgressDialog.dismiss();
                    break;

                case ARCONTROLLER_DEVICE_STATE_STOPPED:
                    // if the deviceController is stopped, go back to the previous activity
                    mConnectionProgressDialog.dismiss();
                    finish();
                    break;

                default:
                    break;
            }
        }

        @Override
        public void onBatteryChargeChanged(int batteryPercentage) {
            mBatteryIndicator.setImageLevel(batteryPercentage);
        }

        @Override
        public void onPilotingStateChanged(ARCOMMANDS_ARDRONE3_PILOTINGSTATE_FLYINGSTATECHANGED_STATE_ENUM state) {
            switch (state) {
                case ARCOMMANDS_ARDRONE3_PILOTINGSTATE_FLYINGSTATECHANGED_STATE_LANDED:
                    mTakeOffLandBt.setImageLevel(LEVEL_TAKEOFF);
                    mTakeOffLandBt.setEnabled(true);
                    break;
                case ARCOMMANDS_ARDRONE3_PILOTINGSTATE_FLYINGSTATECHANGED_STATE_FLYING:
                case ARCOMMANDS_ARDRONE3_PILOTINGSTATE_FLYINGSTATECHANGED_STATE_HOVERING:
                    mTakeOffLandBt.setImageLevel(LEVEL_LAND);
                    mTakeOffLandBt.setEnabled(true);
                    break;
                default:
                    mTakeOffLandBt.setEnabled(false);
            }
        }

        @Override
        public void onPictureTaken(ARCOMMANDS_ARDRONE3_MEDIARECORDEVENT_PICTUREEVENTCHANGED_ERROR_ENUM error) {
            Log.i(TAG, "Picture has been taken");
        }

        @Override
        public void configureDecoder(ARControllerCodec codec) {
            if (codec.getType() == ARCONTROLLER_STREAM_CODEC_TYPE_ENUM.ARCONTROLLER_STREAM_CODEC_TYPE_H264) {
                ARControllerCodec.H264 codecH264 = codec.getAsH264();

                mSpsBuffer = ByteBuffer.wrap(codecH264.getSps().getByteData());
                mPpsBuffer = ByteBuffer.wrap(codecH264.getPps().getByteData());
            }
        }

        @Override
        public void onFrameReceived(ARFrame frame) {
            mVideoView.displayFrame(mSpsBuffer, mPpsBuffer, frame);
        }

        @Override
        public void onMatchingMediasFound(int nbMedias) {
            mDownloadProgressDialog.dismiss();

            mNbMaxDownload = nbMedias;
            mCurrentDownloadIndex = 1;

            if (nbMedias > 0) {
                mDownloadProgressDialog = new ProgressDialog(BebopActivity.this, R.style.AppCompatAlertDialogStyle);
                mDownloadProgressDialog.setIndeterminate(false);
                mDownloadProgressDialog.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
                mDownloadProgressDialog.setMessage("Downloading medias");
                mDownloadProgressDialog.setMax(mNbMaxDownload * 100);
                mDownloadProgressDialog.setSecondaryProgress(mCurrentDownloadIndex * 100);
                mDownloadProgressDialog.setProgress(0);
                mDownloadProgressDialog.setCancelable(false);
                mDownloadProgressDialog.setButton(DialogInterface.BUTTON_NEGATIVE, "Cancel", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        mBebopDrone.cancelGetLastFlightMedias();
                    }
                });
                mDownloadProgressDialog.show();
            }
        }

        @Override
        public void onDownloadProgressed(String mediaName, int progress) {
            mDownloadProgressDialog.setProgress(((mCurrentDownloadIndex - 1) * 100) + progress);
        }

        @Override
        public void onDownloadComplete(String mediaName) {
            mCurrentDownloadIndex++;
            mDownloadProgressDialog.setSecondaryProgress(mCurrentDownloadIndex * 100);

            if (mCurrentDownloadIndex > mNbMaxDownload) {
                mDownloadProgressDialog.dismiss();
                mDownloadProgressDialog = null;
            }
        }
    };
}