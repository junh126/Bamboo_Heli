package com.bamboo.bambooheli.activity;
import android.content.res.AssetManager;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;


import com.bamboo.bambooheli.R;
import com.googlecode.tesseract.android.TessBaseAPI;

import org.opencv.android.Utils;
import org.opencv.core.Mat;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

public class ManualActivity extends AppCompatActivity {
    public native void car_plate(long a1,long a2);
    static{
        System.loadLibrary("native-lib");
        //System.loadLibrary("tess");
    }
    private ImageView mImageView;
    private BitmapDrawable mdrawable;
    private Bitmap mbitmap;
    private Bitmap mbitmap2;
    private Button button1;
    private Mat img_input;
    private Mat img_output;

    private TessBaseAPI mTess;
    String datapath = "";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_manual);


        img_input = new Mat();
        img_output = new Mat();

        mdrawable = (BitmapDrawable) getResources().getDrawable(R.drawable.test2);
        mbitmap = mdrawable.getBitmap();
        button1 = (Button)findViewById(R.id.button1);
        mImageView = (ImageView)findViewById(R.id.ManualView);
        mImageView.setImageResource(R.drawable.test2);
        button1.setOnClickListener(new Button.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (mbitmap != null) {
                    Utils.bitmapToMat(mbitmap, img_input);

                    car_plate(img_input.getNativeObjAddr(), img_output.getNativeObjAddr());

                    mbitmap2 = Bitmap.createBitmap(img_output.cols(), img_output.rows(), Bitmap.Config.ARGB_8888);
                    //Log.i("imgoutput는 아무 죄가 없다 : " ,img_output.dump());

                    Utils.matToBitmap(img_output, mbitmap2);
                    mImageView.setImageBitmap(mbitmap2);

                    //mbitmap2
                    datapath = getFilesDir() + "/tesseract/";

                    checkFile(new File(datapath + "tessdata/"));
                    String lang = "kor";

                    mTess = new TessBaseAPI();
                    mTess.init(datapath, lang);

                    String OCRresult = null;
                    mTess.setImage(mbitmap2);
                    OCRresult = mTess.getUTF8Text();
                    button1.setText(OCRresult);
                }
            }
        });
    }

//    copy file to device
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

}