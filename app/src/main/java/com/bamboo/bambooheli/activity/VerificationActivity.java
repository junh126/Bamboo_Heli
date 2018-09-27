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
import java.util.HashSet;

public class VerificationActivity extends AppCompatActivity {
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
    private HashSet<String> tmp_set;
    private Button next_button;
    private Mat img_input;
    private Mat img_output;
    private Uri uri;
    private TessBaseAPI mTess;
    private String[] imgList;
    private String copy_path;
    private File copy_File;
    private int count1;
    String datapath = "";
    String path;
    private String txt_path;
    File txt_File;
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



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_manual);

        tmp_set = new HashSet<String>();

        count1 = 0;

        mImageView = (ImageView)findViewById(R.id.ManualView);

        //Path 찾는 작업
        path = Environment.getExternalStorageDirectory().getAbsolutePath() + File.separator
                + "ARSDKMedias" + File.separator;
        txt_path = path + "result.txt";
        //txt_File = new File(txt_path);
        //Toast.makeText(getApplicationContext(),"Accessing",Toast.LENGTH_SHORT).show();
        if(!isExternalStorageReadable()){
            Toast.makeText(getApplicationContext(),"Can't Access",Toast.LENGTH_SHORT).show();
            return;
        }
        try{
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
        }
        catch (ArrayIndexOutOfBoundsException e){
            e.printStackTrace();
            Toast.makeText(getApplicationContext(),"Invalid path",Toast.LENGTH_LONG).show();
        }





        img_input = new Mat();
        img_output = new Mat();

        //mdrawable = (BitmapDrawable) getResources().getDrawable(R.drawable.test2);


        try{
            if(imgFile.exists()){
                mbitmap = BitmapFactory.decodeFile(imgFile.getAbsolutePath());
                mbitmap = imgRotate(mbitmap);
                mbitmap = Bitmap.createBitmap(mbitmap,1024,825,2048,1500);

                mImageView.setImageBitmap(mbitmap);
            }

        }
        catch (NullPointerException e){
            e.printStackTrace();
            Toast.makeText(getApplicationContext(),"Invalid path",Toast.LENGTH_LONG).show();
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
//                copy_path = path + "After" + File.separator;
//                copy_File = new File(copy_path + 1 +".jpg");
//                imgFile.renameTo(copy_File);
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
                    String OCRresult = "invalid number";

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
                    //button1.setText("ascii 48~ 57 : " + 48 +" " + 49 +" " + 50 +" " + 51 +" " + 52);
//                    button1.setText("temp : " + temp + "\r\n" + "char[] c.length " + String.valueOf(c.length) + "\r\n" +
//                            "OCRresult : "+ OCRresult + "\r\n" + "carnum : " + Carnum);
                    if(!OCRresult.equals("invalid number")){
                        WriteTextFile(path,"result.txt", OCRresult + " ");
                        Toast.makeText(getApplicationContext(),"result.txt에 쓰는 중",Toast.LENGTH_SHORT).show();
                    }
                    else{
                        //WriteTextFile(path,"result.txt", OCRresult + " ");
                        Toast.makeText(getApplicationContext(),"검출되지 않음",Toast.LENGTH_SHORT).show();
                    }

                    //result.txt에서 번호 가져오기
                    String read = ReadTextFile(txt_path);

                    char[] CC = read.toCharArray();
                    try{
                        if(CC!=null){
                            for(int i =0;i<CC.length ; i++){
                                if(i % 5 == 0 && (i + 3 <= CC.length -1)
                                        && (CC[i] >= 48) && (CC[i] <= 57 )
                                        && (CC[i+1] >= 48) && (CC[i+1] <= 57 )
                                        && (CC[i+2] >= 48) && (CC[i+2] <= 57 )
                                        && (CC[i+3] >= 48) && (CC[i+3] <= 57 )){

                                    char[] ee3 = {CC[i],CC[i+1],CC[i+2],CC[i+3]};
                                    String tmp3 = new String(ee3);
                                    //Toast.makeText(getApplicationContext(),"tmp3 : " + tmp3,Toast.LENGTH_SHORT).show();
                                    tmp_set.add(tmp3);

                                }
                            }
                        }
                    }
                    catch(ArrayIndexOutOfBoundsException e){
                        e.printStackTrace();
                        Toast.makeText(getApplicationContext(),"ArrayIndexOutOfBoundsException!!",Toast.LENGTH_SHORT).show();
                    }
                    catch (NullPointerException e){
                        e.printStackTrace();
                        //Toast.makeText(getApplicationContext(),"NullPointerException!!",Toast.LENGTH_SHORT).show();
                    }
                    try{
                        String ERER = "";
                        if(!tmp_set.isEmpty()){
                            for(String item: tmp_set){
                                ERER += item + " ";
                            }

                            //WriteTextFile(path,"result.txt", ERER);
                        }
                    }
                    catch (NullPointerException e){
                        e.printStackTrace();
                        //Toast.makeText(getApplicationContext(),"NullPointerException222222",Toast.LENGTH_SHORT).show();
                    }


                }
                else{
                    button1.setText("No image");
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