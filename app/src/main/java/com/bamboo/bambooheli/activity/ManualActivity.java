package com.bamboo.bambooheli.activity;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.MotionEvent;
import android.widget.ImageView;

import com.bamboo.bambooheli.R;

public class ManualActivity extends AppCompatActivity {
    private int count = 0;
    private int[] idArray = {R.drawable.ic_manual_1, R.drawable.ic_manual_2, R.drawable.ic_manual_3, R.drawable.ic_manual_4, R.drawable.ic_manual_5};
    private ImageView mImageView;
    private float downX, upX;
    static final int MIN_DISTANCE = 100;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_manual);
        mImageView = (ImageView)findViewById(R.id.ManualView);
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        int action = event.getAction();
        switch(action) {
            case MotionEvent.ACTION_DOWN :
                downX = event.getX();
                break;
            case MotionEvent.ACTION_UP :
                upX = event.getX();
                float deltaX = upX - downX;
                if(Math.abs(deltaX) > MIN_DISTANCE){
                    if(deltaX < 0){
                        if(count  < idArray.length - 1) mImageView.setImageResource(idArray[++count]);
                        else{
                            finish();
                        }
                    }else if(deltaX > 0){
                        if(count > 0) mImageView.setImageResource(idArray[--count]);
                        else{
                            finish();
                        }
                    }
                }
                break;
            case MotionEvent.ACTION_MOVE :
                break;
        }

        return super.onTouchEvent(event);

    }
}