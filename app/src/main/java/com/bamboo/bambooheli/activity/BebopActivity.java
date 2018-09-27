package com.bamboo.bambooheli.activity;

import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.res.AssetManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.os.Environment;
import android.os.Handler;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
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

import com.googlecode.tesseract.android.TessBaseAPI;
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


import org.opencv.android.Utils;
import org.opencv.core.Mat;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.nio.ByteBuffer;
import java.util.HashSet;


public class BebopActivity extends AppCompatActivity {


    public static final int LEVEL_LAND = 1;
    public static final int LEVEL_TAKEOFF = 0;

    private static final String TAG = "BebopActivity";
    private BebopDrone mBebopDrone;

    private ProgressDialog mConnectionProgressDialog;
    private ProgressDialog mDownloadProgressDialog;

    private BebopVideoView mVideoView;

    private ImageView mImageView;

    private ImageButton mTakeOffLandBt;
    private ImageButton mAdditionalBt;
    private GridLayout mAddtionalItems;
    private ImageButton mAutoBt;
    private ToggleButton mWideShotBt;
    private ToggleButton mTimerBt;
    private int tmpcount = 0;
    private ImageButton mDownloadBt;

    private TextView mBatteryIndicator;

    private Button mCompBtn;
    private ByteBuffer mSpsBuffer;
    private ByteBuffer mPpsBuffer;
    private int mNbMaxDownload;
    private HashSet<String> Plate_set;
    private int mCurrentDownloadIndex;
    private boolean isDetect = true;
    private int mynum = 0;
    private ImageButton mManualBt;
    // variable for timer
    private boolean isTimerMode = false;
    private ImageButton startBtn;
    private TextView timer;
    private SeekBar seekBar;
    private CountDownTimer mCountDown = null;
    private Beeper beep;
    private Beeper beepFinish;

    private Button mSaveBtn;

    private boolean isToggle = false;
    private  boolean isAdditional = false;


    private ToggleButton mtoggleBtn;
    private Bitmap mbitmap;
    private Bitmap mbitmap2;
    private File imgFile;
    private File copy_File;
    private File list1;
    private File txt_File;
    private String path;
    private String txt_path;
    private String copy_path;
    private String datapath = "";
    private TessBaseAPI mTess;
    private String[] imgList;
    private Mat img_input = new Mat();
    private Mat img_output = new Mat();

    public native void car_plate(long a1,long a2);
    static{
        System.loadLibrary("native-lib");
        //System.loadLibrary("tess");
    }
    public void WriteTextFile(String foldername, String filename, String contents){
        try{
            File dir = new File (foldername);
            //디렉토리 폴더가 없으면 생성함
            if(!dir.exists()){
                dir.mkdir();
            }
            //파일 output stream 생성
            FileOutputStream fos = new FileOutputStream(foldername+"/"+filename, true);
            //파일쓰기
            BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(fos));
            writer.write(contents);
            writer.flush();

            writer.close();
            fos.close();
        }catch (IOException e){
            e.printStackTrace();
        }
    }


    public String ReadTextFile(String path1){
        StringBuffer strBuffer = new StringBuffer();
        try{
            InputStream is = new FileInputStream(path1);
            BufferedReader reader = new BufferedReader(new InputStreamReader(is));
            String line="";
            while((line=reader.readLine())!=null){
                strBuffer.append(line+"\n");
            }

            reader.close();
            is.close();
        }catch (IOException e){
            e.printStackTrace();
            return "";
        }
        return strBuffer.toString();
    }


    public boolean isExternalStorageWritable() {
        String state = Environment.getExternalStorageState();
        if (Environment.MEDIA_MOUNTED.equals(state)) {
            return true;
        }
        return false;
    }

    public boolean isExternalStorageReadable() {
        String state = Environment.getExternalStorageState();
        if (Environment.MEDIA_MOUNTED.equals(state) ||
                Environment.MEDIA_MOUNTED_READ_ONLY.equals(state)) {
            return true;
        }
        return false;
    }

    private void copyFiles() {
        try{
            String filepath = datapath + "/tessdata/kor.traineddata";
            AssetManager assetManager = getAssets();
            InputStream instream = assetManager.open("tessdata/kor.traineddata");
            OutputStream outstream = new FileOutputStream(filepath);
            byte[] buffer = new byte[1024];
            int read;
            while ((read = instream.read(buffer)) != -1) {
                outstream.write(buffer, 0, read);
            }
            outstream.flush();
            outstream.close();
            instream.close();

        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    //check file on the device
    private void checkFile(File dir) {
        //디렉토리가 없으면 디렉토리를 만들고 그후에 파일을 카피
        if(!dir.exists()&& dir.mkdirs()) {
            copyFiles();
        }
        //디렉토리가 있지만 파일이 없으면 파일카피 진행
        if(dir.exists()) {
            String datafilepath = datapath+ "/tessdata/kor.traineddata";
            File datafile = new File(datafilepath);
            if(!datafile.exists()) {
                copyFiles();
            }
        }
    }

    private void findpath() {


        if(!isExternalStorageReadable()){
            //Toast.makeText(getApplicationContext(),"Can't Access",Toast.LENGTH_SHORT).show();
            return;
        }
        try{
            list1 = new File(path);
            imgList = list1.list(new FilenameFilter() {
                public boolean accept(File dir, String filename) {
                    boolean bOK = false;
                    if(filename.toLowerCase().endsWith(".jpg")) bOK = true;
                    //if(bOK) Toast.makeText(getApplicationContext(),"file name : " + filename + " is true",Toast.LENGTH_LONG).show();
                    return bOK;
                }
            });
            if(imgList != null){
                //path + imgList[0];
                imgFile = new File(path + imgList[0]);
                //uri = Uri.parse(path + imgList[0]);
                //Toast.makeText(getApplicationContext(),"path : " + path + imgList[0],Toast.LENGTH_LONG).show();
                //mImageView.setImageURI(uri);
            }
            else{
                //Toast.makeText(getApplicationContext(),"imgList is empty",Toast.LENGTH_LONG).show();
            }
        }
        catch (ArrayIndexOutOfBoundsException e){
            e.printStackTrace();
            Toast.makeText(getApplicationContext(),"Invalid path",Toast.LENGTH_LONG).show();
        }

    }

    private Bitmap imgRotate(Bitmap bmp){
        int width = bmp.getWidth();
        int height = bmp.getHeight();

        Matrix matrix = new Matrix();
        matrix.postRotate(270);

        Bitmap resizedBitmap = Bitmap.createBitmap(bmp, 0, 0, width, height, matrix, true);
        bmp.recycle();

        return resizedBitmap;
    }

    private void process() {
        findpath();
        try{
            if(imgFile.exists()){
                mbitmap = BitmapFactory.decodeFile(imgFile.getAbsolutePath());
                mbitmap = imgRotate(mbitmap);
                mbitmap = Bitmap.createBitmap(mbitmap,1024,825,2048,1500);

            }


        }
        catch (NullPointerException e){
            e.printStackTrace();
            Toast.makeText(getApplicationContext(),"Invalid path",Toast.LENGTH_LONG).show();
        }

        if (mbitmap != null) {
            Utils.bitmapToMat(mbitmap, img_input);

            car_plate(img_input.getNativeObjAddr(), img_output.getNativeObjAddr());

            mbitmap2 = Bitmap.createBitmap(img_output.cols(), img_output.rows(), Bitmap.Config.ARGB_8888);
            //Log.i("imgoutput는 아무 죄가 없다 : " ,img_output.dump());

            Utils.matToBitmap(img_output, mbitmap2);

            //mbitmap2
            datapath = getFilesDir() + "/tesseract/";

            checkFile(new File(datapath + "tessdata/"));
            String lang = "kor";

            mTess = new TessBaseAPI();
            mTess.init(datapath, lang);

            String OCRresult = null;
            String temp = "Invalid number";
            mTess.setImage(mbitmap2);
            temp = mTess.getUTF8Text();
            char[] c = temp.toCharArray();
            if(c != null){
                for(int i = 0;i <= c.length - 4 ; i++){
                    if( (c[i] >= 48) && (c[i] <= 57 ) &&
                            (c[i+1] >= 48) && (c[i+1] <= 57 ) &&
                            (c[i+2] >= 48) && (c[i+2] <= 57 ) &&
                            (c[i+3] >= 48) && (c[i+3] <= 57 )  )
                    {
                        char[] tmp1 = {c[i],c[i+1],c[i+2],c[i+3]};
                        OCRresult = new String(tmp1);
                        break;
                    }
                }
            }
            if(!OCRresult.equals("Invalid number") ){
                Plate_set.add(OCRresult);
                Toast.makeText(getApplicationContext(),"Plate num : " + OCRresult,Toast.LENGTH_SHORT).show();
            }
            else{
                //검출안됨
                Toast.makeText(getApplicationContext(),"Invalid Number",Toast.LENGTH_SHORT).show();
            }
        }
    }




    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_bebop);
        //path는 액티비티 시작하면 찾아줘야함.
         path= Environment.getExternalStorageDirectory().getAbsolutePath() + File.separator
                + "ARSDKMedias" + File.separator;
        Plate_set = new HashSet<String>();
        //txt_File = new File(txt_path);
        initIHM();

        Intent intent = getIntent();
        ARDiscoveryDeviceService service = intent.getParcelableExtra(DeviceListActivity.EXTRA_DEVICE_SERVICE);
        mBebopDrone = new BebopDrone(this, service);
        mBebopDrone.addListener(mBebopListener);

        mManualBt = (ImageButton) findViewById(R.id.btn_result);
        mManualBt.setOnClickListener(new View.OnClickListener(){
            public void onClick(View v){
                Intent intent = new Intent(BebopActivity.this, VerificationActivity.class);
                startActivity(intent);
            }
        });


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
                    //takePicture();
                    download();

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

    private void turn180() {
        mBebopDrone.setYaw((byte) -100);
        try {
            Thread.sleep(5500);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        mBebopDrone.setYaw((byte) 0);
    }

    private void initIHM() {
        mVideoView = (BebopVideoView) findViewById(R.id.videoView);
        mVideoView.setSurfaceTextureListener(mVideoView);

        mCompBtn = (Button) findViewById(R.id.compbutton);
        mCompBtn.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(BebopActivity.this, CompActivity.class);
                startActivity(intent);

            }
        });
        mImageView = (ImageView)findViewById(R.id.imageView);



        mSaveBtn = (Button)findViewById(R.id.savebutton);
        mSaveBtn.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View view) {

                if(isToggle){//두번째 비행
                    try{
                        if(!Plate_set.isEmpty()){
                            String ERER = "";
                            for(String item: Plate_set){
                                ERER += item + " ";
                            }

                            WriteTextFile(path,"result_2.txt", ERER);
                            Toast.makeText(getApplicationContext(),"결과를 result_2.txt에 저장하였습니다.",Toast.LENGTH_SHORT).show();
                            //Plate_set 초기화
                            Plate_set = new HashSet<String>();
                        }
                        else{
                            Toast.makeText(getApplicationContext(),"Plate_set is empty",Toast.LENGTH_SHORT).show();
                        }
                    }
                    catch (NullPointerException e){
                        e.printStackTrace();
                        Toast.makeText(getApplicationContext(),"Null pointer exception22",Toast.LENGTH_SHORT).show();
                    }
                }
                else{//첫번째 비행
                    try{
                        if(!Plate_set.isEmpty()){
                            String ERER = "";
                            for(String item: Plate_set){
                                ERER += item + " ";
                            }

                            WriteTextFile(path,"result_1.txt", ERER);
                            Toast.makeText(getApplicationContext(),"결과를 result_1.txt에 저장하였습니다.",Toast.LENGTH_SHORT).show();
                            //Plate_set 초기화
                            Plate_set = new HashSet<String>();
                        }
                        else{
                            Toast.makeText(getApplicationContext(),"Plate_set is empty",Toast.LENGTH_SHORT).show();
                        }
                    }
                    catch (NullPointerException e){
                        e.printStackTrace();
                        Toast.makeText(getApplicationContext(),"Null pointer exception11",Toast.LENGTH_SHORT).show();
                    }

                }
            }
        });

        mtoggleBtn = (ToggleButton) findViewById(R.id.toggleButton);
        mtoggleBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if(mtoggleBtn.isChecked()){
                    isToggle = true;
                }
                else{
                    isToggle = false;
                }
            }
        });
        mBatteryIndicator = (TextView) findViewById(R.id.battery_indicator);


        beep = new Beeper(this, R.raw.beep_repeat2);
        beepFinish = new Beeper(this, R.raw.beep_camera);


        mDownloadBt = (ImageButton)findViewById(R.id.downloadBt);
        mDownloadBt.setEnabled(true);
        mDownloadBt.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                download();
            }
        });

        mAutoBt = (ImageButton)findViewById(R.id.automove);
        mAutoBt.setOnClickListener(new View.OnClickListener(){
            public void onClick(View v){


                if(isDetect) {
                    isDetect = false;
                    mynum = 0;

                    final Handler mHandler = new Handler();
                    final Handler H2 = new Handler();
                    mHandler.postDelayed(new Runnable() {
                        @Override
                        public void run() {
                            mynum++;
                            takePicture();
                            H2.postDelayed(new Runnable() {
                                @Override
                                public void run() {
                                    mBebopDrone.setPitch((byte) 0);
                                    mBebopDrone.setFlag((byte) 0);
                                    process();
                                    //검출 완료된 이미지 파일 이동시키기.
                                    tmpcount++;
                                    copy_path = path + "After" + File.separator;
                                    try{
                                        if(isToggle){//두번째 비행
                                            copy_File = new File(copy_path +"2_"+ tmpcount +".jpg");
                                            imgFile.renameTo(copy_File);
                                        }
                                        else{//첫번째비행
                                            copy_File = new File(copy_path +"1_"+ tmpcount +".jpg");
                                            imgFile.renameTo(copy_File);
                                        }

                                    }
                                    catch (NullPointerException e){
                                        e.printStackTrace();
                                        Toast.makeText(getApplicationContext(),"No image",Toast.LENGTH_LONG).show();
                                    }

                                }
                            }, 3000);

                            mBebopDrone.setPitch((byte) 20);
                            mBebopDrone.setFlag((byte) 1);
                            download();

                            if(mynum == 4) {
                                mBebopDrone.setPitch((byte) 0);
                                mBebopDrone.setFlag((byte) 0);
                                turn180();
                                mBebopDrone.setPitch((byte) 20);
                                mBebopDrone.setFlag((byte) 1);
                                try {
                                    Thread.sleep(5000);
                                } catch (InterruptedException e) {
                                    e.printStackTrace();
                                }
                                mBebopDrone.setPitch((byte) 0);
                                mBebopDrone.setFlag((byte) 0);
                                mHandler.removeCallbacks(this);
                            }
                            else {
                                mHandler.postDelayed(this, 7000);
                            }
                        }
                    }, 7000);

                }
                else {
                    isDetect = true;
                    //mAutoBt.setBackgroundDrawable(getResources().getDrawable(R.drawable.if_detect_on));
                    mBebopDrone.setPitch((byte) 0);
                    mBebopDrone.setFlag((byte) 0);
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
            mBatteryIndicator.setText(Integer.toString(batteryPercentage));

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