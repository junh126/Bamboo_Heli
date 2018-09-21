package com.bamboo.bambooheli.activity;

import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;


import com.bamboo.bambooheli.R;

import org.opencv.android.Utils;
import org.opencv.core.Mat;

public class ManualActivity extends AppCompatActivity {

    private ImageView mImageView;
    private BitmapDrawable mdrawable;
    private Bitmap mbitmap;
    private Button button1;
    private Mat img_input;
    private Mat img_output;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_manual);


        img_input = new Mat();
        img_output = new Mat();
        mdrawable = (BitmapDrawable) getResources().getDrawable(R.drawable.test1);
        mbitmap = mdrawable.getBitmap();
        button1 = (Button)findViewById(R.id.button1);
        mImageView = (ImageView)findViewById(R.id.ManualView);
        mImageView.setImageResource(R.drawable.test1);
        button1.setOnClickListener(new Button.OnClickListener() {
            @Override
            public void onClick(View view) {
                Utils.bitmapToMat(mbitmap, img_input);

                // catchCarPlate(img_input.getNativeObjAddr(), img_output.getNativeObjAddr());
                // C++ 로 넘어갑시다.
                Utils.matToBitmap(img_output, mbitmap);
                mImageView.setImageBitmap(mbitmap);
            }
        });
    }
}