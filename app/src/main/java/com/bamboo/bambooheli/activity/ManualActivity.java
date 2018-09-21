package com.bamboo.bambooheli.activity;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;


import com.bamboo.bambooheli.R;

import org.opencv.android.Utils;
import org.opencv.core.Mat;

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

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_manual);


        img_input = new Mat();
        img_output = new Mat();

        mdrawable = (BitmapDrawable) getResources().getDrawable(R.drawable.test5);
        mbitmap = mdrawable.getBitmap();
        button1 = (Button)findViewById(R.id.button1);
        mImageView = (ImageView)findViewById(R.id.ManualView);
        mImageView.setImageResource(R.drawable.test5);
        button1.setOnClickListener(new Button.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (mbitmap != null) {
                    Utils.bitmapToMat(mbitmap, img_input);

                    car_plate(img_input.getNativeObjAddr(), img_output.getNativeObjAddr());

                    mbitmap2 = Bitmap.createBitmap(img_output.cols(), img_output.rows(), Bitmap.Config.ARGB_8888);
                    Log.i("imgoutput는 아무 죄가 없다 : " ,img_output.dump());

                    Utils.matToBitmap(img_output, mbitmap2);
                    mImageView.setImageBitmap(mbitmap2);

                }
            }
        });
    }
}