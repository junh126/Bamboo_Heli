package com.bamboo.bambooheli.activity;

import android.content.Intent;
import android.graphics.Bitmap;
import android.os.Environment;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;


import com.bamboo.bambooheli.R;

import org.opencv.core.Mat;

import java.io.File;
import java.io.FilenameFilter;

public class Carplate extends AppCompatActivity {
    public native void car_plate(String addr);
    static{
        System.loadLibrary("native-lib");
        //System.loadLibrary("tess");
    }
    private Button mbtn;
    private Bitmap mbitmap;
    private TextView cptextView;
    private TextView CarPlatetextView;
    private ImageView mimageView;
    private Mat img_input;
    private Mat img_output;
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
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_carplate);

        CarPlatetextView = (TextView) findViewById(R.id.carplatetextView);

        mimageView = (ImageView) findViewById(R.id.imageView);
        mimageView.setVisibility(View.INVISIBLE);

        img_input = new Mat();
        img_output = new Mat();


        mbtn = (Button) findViewById(R.id.search_button);
        mbtn.setOnClickListener(new View.OnClickListener(){
            public void onClick(View v){
                String path = Environment.getExternalStorageDirectory().getAbsolutePath() + File.separator
                                    + "ARSDKMedias" + File.separator;
                //Toast.makeText(getApplicationContext(),"Accessing",Toast.LENGTH_SHORT).show();
                if(!isExternalStorageReadable()){
                    Toast.makeText(getApplicationContext(),"Can't Access",Toast.LENGTH_SHORT).show();
                    return;
                }
                File list1 = new File(path);
                String[] imgList = list1.list(new FilenameFilter() {
                    public boolean accept(File dir, String filename) {
                        boolean bOK = false;
                        if(filename.toLowerCase().endsWith(".jpg")) bOK = true;
                        //if(bOK) Toast.makeText(getApplicationContext(),"file name : " + filename + " is true",Toast.LENGTH_LONG).show();
                        return bOK;
                    }
                });
                if(imgList != null){
                    for(int i = 0 ; i < imgList.length;i++){
                        Toast.makeText(getApplicationContext(),path + imgList[i] +"가 함수에 들어간다아아아아아",Toast.LENGTH_LONG).show();
                        car_plate(path + imgList[i]);
                        Toast.makeText(getApplicationContext(),"나왔따",Toast.LENGTH_LONG).show();

                    }
                }
                else{
                    Toast.makeText(getApplicationContext(),"imgList is empty",Toast.LENGTH_LONG).show();
                }


            }
        });

    }
}
