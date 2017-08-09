package com.tobusan.selfidrone.view;


import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.os.Handler;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;


import org.opencv.android.Utils;
import org.opencv.core.Core;
import org.opencv.core.Mat;
import org.opencv.core.MatOfInt;
import org.opencv.core.MatOfPoint;
import org.opencv.core.MatOfRect;
import org.opencv.core.Point;
import org.opencv.core.Rect;
import org.opencv.core.Scalar;
import org.opencv.core.Size;
import org.opencv.imgcodecs.Imgcodecs;
import org.opencv.imgproc.Imgproc;
import org.opencv.imgproc.Moments;
import org.opencv.objdetect.CascadeClassifier;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import static org.opencv.core.CvType.CV_8U;

import com.tobusan.selfidrone.R;
import com.tobusan.selfidrone.drone.BebopDrone;
import com.tobusan.selfidrone.drone.Beeper;

public class SmileShot extends View{
    private final static String CLASS_NAME = SmileShot.class.getSimpleName();

    private final Context ctx;

    private CascadeClassifier faceClassifier;
    private CascadeClassifier smileClassifier;

    private Thread openCVThread = null;

    private BebopVideoView bebopVideoView = null;
    private ImageView cvPreviewView = null;
    private BebopDrone bebopDrone = null;
    private Beeper beepFinsh = null;
    private Rect[] facesArray = null;
    private Rect[] smileArray = null;
    private Paint paint;

    private final Object lock = new Object();

    private float mX = 0;
    private float mY = 0;

    private float centerX = 0;
    private float centerY = 0;

    private float rateX = 0;
    private float rateY = 0;

    private float top_x = 0;
    private float top_y = 0;

    private long sTime = System.currentTimeMillis();
    private long cTime;

    private int count = 0;
    public SmileShot(Context context) {
        super(context);
        ctx = context;

        faceClassifier = new CascadeClassifier(cascadeFile(R.raw.haarcascade_frontalface_alt2));
        smileClassifier = new CascadeClassifier(cascadeFile(R.raw.haarcascade_smile));

        // initialize our canvas paint object
        paint = new Paint();
        paint.setAntiAlias(true);
        paint.setColor(Color.GREEN);
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(4f);
    }

    public SmileShot(Context context, AttributeSet attrs) {
        super(context,attrs);
        ctx = context;

        faceClassifier = new CascadeClassifier(cascadeFile(R.raw.haarcascade_frontalface_alt2));
        smileClassifier = new CascadeClassifier(cascadeFile(R.raw.haarcascade_smile));

        // initialize our canvas paint object
        paint = new Paint();
        paint.setAntiAlias(true);
        paint.setColor(Color.GREEN);
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(4f);
    }

    public SmileShot(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context,attrs,defStyleAttr);
        ctx = context;

        // initialize our opencv cascade classifiers
        faceClassifier = new CascadeClassifier(cascadeFile(R.raw.haarcascade_frontalface_alt2));
        smileClassifier = new CascadeClassifier(cascadeFile(R.raw.haarcascade_smile));

        // initialize our canvas paint object
        paint = new Paint();
        paint.setAntiAlias(true);
        paint.setColor(Color.GREEN);
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(4f);
    }

    private void FaceDetect(Mat mat) {
        centerX = mat.width() / 2;
        centerY =  mat.height() / 2;

        rateX = mat.width() * 0.4f;
        rateY = mat.height() * 0.4f;

        top_x = centerX - (rateX) / 2;
        top_y = centerY - (rateY) / 2;
    }

    private String cascadeFile(final int id) {
        final InputStream is = getResources().openRawResource(id);

        final File cascadeDir = ctx.getDir("cascade", Context.MODE_PRIVATE);
        final File cascadeFile = new File(cascadeDir, String.format(Locale.US, "%d.xml", id));

        try {
            final FileOutputStream os = new FileOutputStream(cascadeFile);
            final byte[] buffer = new byte[4096];

            int bytesRead;

            while ((bytesRead = is.read(buffer)) != -1) {
                os.write(buffer, 0, bytesRead);
            }

            is.close();
            os.close();
        } catch (Exception e) {
            Log.e(CLASS_NAME, "unable to open cascade file: " + cascadeFile.getName(), e);
            return null;
        }

        return cascadeFile.getAbsolutePath();
    }

    public void resume(final BebopVideoView bebopVideoView, final ImageView cvPreviewView, final BebopDrone bebopDrone, final Beeper beepFinsh) {
        if (getVisibility() == View.VISIBLE) {
            this.bebopVideoView = bebopVideoView;
            this.cvPreviewView = cvPreviewView;
            this.bebopDrone = bebopDrone;
            this.beepFinsh = beepFinsh;

            openCVThread = new CascadingThread(ctx);
            openCVThread.start();
        }
    }

    public void pause() {
        if (getVisibility() == View.VISIBLE) {
            openCVThread.interrupt();
            try {
                openCVThread.join();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    private class CascadingThread extends Thread {
        private final Handler handler;
        boolean interrupted = false;

        private CascadingThread(final Context ctx) {
            handler = new Handler(ctx.getMainLooper());
        }

        @Override
        public void interrupt() {
            interrupted = true;
            facesArray = null;
            smileArray = null;
            invalidate();
        }

        @Override
        public void run() {
            final Mat firstMat = new Mat();
            final Mat mat = new Mat();

            Mat submat= new Mat();
            Mat sub_submat = new Mat();

            Rect rect = new Rect();

            while (!interrupted) {
                final Bitmap source = bebopVideoView.getBitmap();
                if (source != null) {
                    Utils.bitmapToMat(source, firstMat);
                    firstMat.assignTo(mat);
                    Imgproc.cvtColor(mat, mat, Imgproc.COLOR_RGBA2GRAY);

                    final MatOfRect faces = new MatOfRect();
                    final MatOfRect smiles = new MatOfRect();

                    final int minRows = Math.round(mat.rows() * 0.07f);

                    final Size minSize = new Size(minRows, minRows);
                    final Size maxSize = new Size(0, 0);

                    FaceDetect(mat);

                    rect = new Rect((int)top_x,(int)top_y,(int)rateX,(int)rateY);

                    submat = mat.submat(rect);
                    submat.assignTo(sub_submat);

                    faceClassifier.detectMultiScale(sub_submat, faces, 1.05, 6, 0, minSize, maxSize);
                    smileClassifier.detectMultiScale(sub_submat, smiles, 1.05, 6, 0, minSize, maxSize);

                    synchronized (lock) {
                        facesArray = faces.toArray();
                        smileArray = smiles.toArray();
                        mX = submat.width() / sub_submat.width();
                        mY = submat.height() / sub_submat.height();
                        faces.release();
                        smiles.release();

                        cTime = System.currentTimeMillis();
                        if(cTime - sTime > 1000) {
                            if(count >= 4) {
                                beepFinsh.play();
                                bebopDrone.takePicture();
                            }
                            sTime = cTime;
                            count = 0;
                        }

                        if(interrupted){
                            facesArray = null;
                            smileArray = null;
                        }
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                invalidate();
                            }
                        });
                    }
                }
                try {
                    sleep(70);
                } catch (InterruptedException e) {
                    interrupted = true;
                }
            }
            firstMat.release();
            mat.release();
            submat.release();
            sub_submat.release();
        }
        private void runOnUiThread(Runnable r) {
            handler.post(r);
        }

    }

    @Override
    protected void onDraw(Canvas canvas) {
        // Log.d(CLASS_NAME, "onDraw");

        synchronized(lock) {
            if (facesArray != null && facesArray.length > 0) {
                float faceTLX = (float) facesArray[0].tl().x * mX;
                float faceBRX = (float) facesArray[0].br().x * mX;
                float faceTLY = (float) facesArray[0].tl().y * mY;
                float faceBRY = (float) facesArray[0].br().y * mY;

                for (Rect target : smileArray) {
                    float TopLeftX = (float) target.tl().x * mX;
                    float BottomRightX = (float) target.br().x * mX;
                    float TopLeftY = (float) target.tl().y * mY;
                    float BottomRightY = (float) target.br().y * mY;

                    if(faceTLX < TopLeftX && faceBRX > BottomRightX && (faceTLY + (faceBRY-faceTLY)/2) < TopLeftY && faceBRY > BottomRightY) {
                        canvas.drawRect(top_x + TopLeftX, top_y + TopLeftY, top_x + BottomRightX, top_y + BottomRightY, paint);
                        count++;
                    }
                }
            }
        }
        super.onDraw(canvas);
    }
}