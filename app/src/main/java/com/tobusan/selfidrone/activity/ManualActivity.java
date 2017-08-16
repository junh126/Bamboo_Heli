package com.tobusan.selfidrone.activity;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.MotionEvent;
import android.widget.ImageView;

import com.tobusan.selfidrone.R;

public class ManualActivity extends AppCompatActivity {
    private int count = 1;
    private int[] idArray = {R.drawable.ic_manual_1, R.drawable.ic_manual_2, R.drawable.ic_manual_3, R.drawable.ic_manual_4, R.drawable.ic_manual_5};
    private ImageView mImageView;
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
                break;
            case MotionEvent.ACTION_UP :
                if(count < idArray.length) mImageView.setImageResource(idArray[count++]);
                else finish();
                break;
            case MotionEvent.ACTION_MOVE :
                break;
        }

        return super.onTouchEvent(event);

    }
}