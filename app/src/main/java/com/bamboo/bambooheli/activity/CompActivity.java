package com.bamboo.bambooheli.activity;

import android.content.Intent;
import android.os.Environment;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import com.bamboo.bambooheli.R;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.HashSet;
import java.util.Set;

public class CompActivity extends AppCompatActivity {

    private TextView mResult1_textView;
    private TextView mResult2_textView;
    private TextView mResult_textView;
    private ImageButton mBtn;
    private String path;
    private String strResult_2;
    private String strResult_1;
    private HashSet<String> tmp_set1;
    private HashSet<String> tmp_set2;

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
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_comp);

        path= Environment.getExternalStorageDirectory().getAbsolutePath() + File.separator
                + "ARSDKMedias" + File.separator;

        strResult_1 = ReadTextFile(path + "result_1.txt");
        strResult_2 = ReadTextFile(path + "result_2.txt");

        tmp_set1 = new HashSet<String>();
        tmp_set2 = new HashSet<String>();

        mResult1_textView = (TextView)findViewById(R.id.result1_textView);
        mResult2_textView = (TextView)findViewById(R.id.result2_textView);
        mResult_textView = (TextView)findViewById(R.id.result_textView);
        mResult_textView.setVisibility(View.INVISIBLE);

        char[] CC = strResult_1.toCharArray();
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
                        tmp_set1.add(tmp3);

                    }
                }
            }
        } catch(ArrayIndexOutOfBoundsException e){
            e.printStackTrace();
            Toast.makeText(getApplicationContext(),"ArrayIndexOutOfBoundsException!!",Toast.LENGTH_SHORT).show();
        } catch (NullPointerException e){
            e.printStackTrace();
        }


        char[] DD = strResult_2.toCharArray();
        try{
            if(DD!=null){
                for(int i =0;i<DD.length ; i++){
                    if(i % 5 == 0 && (i + 3 <= DD.length -1)
                            && (DD[i] >= 48) && (DD[i] <= 57 )
                            && (DD[i+1] >= 48) && (DD[i+1] <= 57 )
                            && (DD[i+2] >= 48) && (DD[i+2] <= 57 )
                            && (DD[i+3] >= 48) && (DD[i+3] <= 57 )){

                        char[] ee3 = {DD[i],DD[i+1],DD[i+2],DD[i+3]};
                        String tmp3 = new String(ee3);
                        //Toast.makeText(getApplicationContext(),"tmp3 : " + tmp3,Toast.LENGTH_SHORT).show();
                        tmp_set2.add(tmp3);

                    }
                }
            }
        } catch(ArrayIndexOutOfBoundsException e){
            e.printStackTrace();
            Toast.makeText(getApplicationContext(),"ArrayIndexOutOfBoundsException!!",Toast.LENGTH_SHORT).show();
        } catch (NullPointerException e){
            e.printStackTrace();
        }
        String text1 = "result_1.txt에서 검출된 차량 번호 : " + "\n";
        if(!tmp_set1.isEmpty()){
            for(String item : tmp_set1){
                text1 += item + "\n";
            }
        }else{
            text1 += "검출된 차량 없음.";
        }
        String text2 = "result_2.txt에서 검출된 차량 번호 : " + "\n";
        if(!tmp_set2.isEmpty()){
            for(String item : tmp_set2){
                text2 += item + "\n";
            }
        }else{
            text2 += "검출된 차량 없음.";
        }
        mResult1_textView.setText(text1);
        mResult2_textView.setText(text2);

        mBtn = (ImageButton)findViewById(R.id.testbutton);
        mBtn.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View view) {


                try{
                    Set<String> intersection = new HashSet<String>(tmp_set1); // use the copy constructor
                    intersection.retainAll(tmp_set2);
                    String RESULT = "불법 주정차 차량 : " + "\n";
                    if(!intersection.isEmpty()){
                        for(String item : intersection){
                            RESULT += item + "\n";
                        }
                    }else{
                        RESULT += "검출된 차량 없음.";
                    }
                    mResult_textView.setText(RESULT);
                    mResult_textView.setVisibility(View.VISIBLE);
                } catch (NullPointerException e){
                    e.printStackTrace();
                }


            }
        });
    }
}
