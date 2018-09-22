package com.bamboo.bambooheli.activity;
import android.content.res.AssetManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.graphics.drawable.BitmapDrawable;
import android.net.Uri;
import android.os.Environment;
import android.provider.MediaStore;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.Toast;


import com.bamboo.bambooheli.R;
import com.googlecode.tesseract.android.TessBaseAPI;

import org.opencv.android.Utils;
import org.opencv.core.Mat;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

public class ManualActivity extends AppCompatActivity {
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
    private Bitmap imgRotate(Bitmap bmp){
        int width = bmp.getWidth();
        int height = bmp.getHeight();

        Matrix matrix = new Matrix();
        matrix.postRotate(270);

        Bitmap resizedBitmap = Bitmap.createBitmap(bmp, 0, 0, width, height, matrix, true);
        bmp.recycle();

        return resizedBitmap;
    }


    public native void car_plate(long a1,long a2);
    static{
        System.loadLibrary("native-lib");
        //System.loadLibrary("tess");
    }
    private ImageView mImageView;
    private BitmapDrawable mdrawable;
    private Bitmap mbitmap;
    private File imgFile;
    private Bitmap mbitmap2;
    private Button button1;
    private Button next_button;
    private Mat img_input;
    private Mat img_output;
    private Uri uri;
    private TessBaseAPI mTess;
    private String[] imgList;
    private int count1;
    String datapath = "";
    String path;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_manual);

        count1 = 0;

        mImageView = (ImageView)findViewById(R.id.ManualView);

        //Path 찾는 작업
        path = Environment.getExternalStorageDirectory().getAbsolutePath() + File.separator
                + "ARSDKMedias" + File.separator;
        //Toast.makeText(getApplicationContext(),"Accessing",Toast.LENGTH_SHORT).show();
        if(!isExternalStorageReadable()){
            Toast.makeText(getApplicationContext(),"Can't Access",Toast.LENGTH_SHORT).show();
            return;
        }
        File list1 = new File(path);
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
            Toast.makeText(getApplicationContext(),"path : " + path + imgList[0],Toast.LENGTH_LONG).show();
            //mImageView.setImageURI(uri);
        }
        else{
            Toast.makeText(getApplicationContext(),"imgList is empty",Toast.LENGTH_LONG).show();
        }


        img_input = new Mat();
        img_output = new Mat();

        //mdrawable = (BitmapDrawable) getResources().getDrawable(R.drawable.test2);



        if(imgFile.exists()){
            mbitmap = BitmapFactory.decodeFile(imgFile.getAbsolutePath());
            mbitmap = imgRotate(mbitmap);
            mbitmap = Bitmap.createBitmap(mbitmap,1024,825,2048,1500);

            mImageView.setImageBitmap(mbitmap);
        }

        next_button = (Button) findViewById(R.id.next_button);
        next_button.setOnClickListener(new Button.OnClickListener() {
            @Override
            public void onClick(View view) {
                count1++;
                if(imgList != null){
                    if(imgList.length > count1){
                        imgFile =  new File(path + imgList[count1]);
                        Toast.makeText(getApplicationContext(),"path : " + path + imgList[count1],Toast.LENGTH_LONG).show();
                        if(imgFile.exists()){
                            mbitmap = BitmapFactory.decodeFile(imgFile.getAbsolutePath());
                            mbitmap = imgRotate(mbitmap);
                            mbitmap = Bitmap.createBitmap(mbitmap,1024,825,2048,1500);

                            mImageView.setImageBitmap(mbitmap);
                        }
                    }
                    else{
                        Toast.makeText(getApplicationContext(),"다음 이미지가 없습니다.",Toast.LENGTH_LONG).show();
                    }
                }
                else{
                    Toast.makeText(getApplicationContext(),"imgList is empty",Toast.LENGTH_LONG).show();
                }
            }
        });

        button1 = (Button)findViewById(R.id.button1);
        button1.setOnClickListener(new Button.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (mbitmap != null) {
                    Utils.bitmapToMat(mbitmap, img_input);
                    Log.i("ㅇㅇ2" ,"2번");
                    car_plate(img_input.getNativeObjAddr(), img_output.getNativeObjAddr());
                    Log.i("ㅇㅇ1" ,"1번");
                    if(img_output.empty()){
                        Toast.makeText(getApplicationContext(),"cpp 검출을 못했습니다.",Toast.LENGTH_LONG).show();
                        return;
                    }
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

                    String temp = null;
                    String OCRresult = "invalid car number";
                    String Carnum = "invalid car number";
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
                                char[] tmp2 = {c[i],c[i+1],c[i+2],c[i+3]};
                                OCRresult = new String(tmp1);
                                Carnum = new String(tmp2);
                                break;
                            }
                        }
                    }
                    //button1.setText("ascii 48~ 57 : " + 48 +" " + 49 +" " + 50 +" " + 51 +" " + 52);
                    button1.setText("temp : " + temp + "\r\n" + "char[] c.length " + String.valueOf(c.length) + "\r\n" +
                            "OCRresult : "+ OCRresult + "\r\n" + "carnum : " + Carnum);
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