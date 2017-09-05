package com.tobusan.selfidrone.view;


import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.os.Handler;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ToggleButton;


import org.opencv.android.Utils;
import org.opencv.core.Mat;
import org.opencv.core.MatOfRect;
import org.opencv.core.Rect;
import org.opencv.core.Size;
import org.opencv.imgproc.Imgproc;
import org.opencv.objdetect.CascadeClassifier;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.util.Locale;

import com.tobusan.selfidrone.R;
import com.tobusan.selfidrone.drone.BebopDrone;
import com.tobusan.selfidrone.drone.Beeper;

public class FaceDetect extends View {
    private final static String CLASS_NAME = FaceDetect.class.getSimpleName();

    private final Context ctx;

    private CascadeClassifier faceClassifier;
    private CascadeClassifier smileClassifier;

    private Handler openCVHandler = new Handler();
    private Thread openCVThread = null;

    private BebopVideoView bebopVideoView = null;
    private ImageView cvPreviewView = null;
    private BebopDrone bebopDrone = null;

    private Beeper beepFinsh = null;

    private Rect[] facesArray = null;
    private Rect[] smileArray = null;

    private Paint paint_green;
    private Paint paint_red;

    private final Object lock = new Object();

    private float mainCenterX = 0;
    private float mainCenterY = 0;
    private float mainBoundRateX = 0;
    private float mainBoundRateY = 0;

    private float mainFaceArea = 15000;

    private float mX = 0;
    private float mY = 0;

    private float centerX = 0;
    private float centerY = 0;

    private float faceCenterX = 0;
    private float faceCenterY = 0;

    private float rateX = 0;
    private float rateY = 0;

    private float top_x = 0;
    private float top_y = 0;

    private long sTime = System.currentTimeMillis();
    private long cTime;

    private boolean isFirst = true;
    private boolean followEnabled = false;
    private boolean smileEnabled = false;

    private int count = 0;

    private ToggleButton followBtn;
    private ToggleButton smileBtn;

    private Rect smileRect = null;

    public FaceDetect(Context context) {
        super(context);
        ctx = context;

        faceClassifier = new CascadeClassifier(cascadeFile(R.raw.haarcascade_frontalface_alt2));
        smileClassifier = new CascadeClassifier(cascadeFile(R.raw.haarcascade_smile));

        // initialize our canvas paint object
        paint_green = new Paint();
        paint_green.setAntiAlias(true);
        paint_green.setColor(Color.parseColor("#22b3ab"));
        paint_green.setStyle(Paint.Style.STROKE);
        paint_green.setStrokeWidth(4f);

        paint_red = new Paint();
        paint_red.setAntiAlias(true);
        paint_red.setStyle(Paint.Style.STROKE);
        paint_red.setColor(Color.RED);
        paint_red.setStrokeWidth(2f);
    }

    public FaceDetect(Context context, AttributeSet attrs) {
        super(context,attrs);
        ctx = context;

        faceClassifier = new CascadeClassifier(cascadeFile(R.raw.haarcascade_frontalface_alt2));
        smileClassifier = new CascadeClassifier(cascadeFile(R.raw.haarcascade_smile));

        // initialize our canvas paint object
        paint_green = new Paint();
        paint_green.setAntiAlias(true);
        paint_green.setColor(Color.parseColor("#22b3ab"));
        paint_green.setStyle(Paint.Style.STROKE);
        paint_green.setStrokeWidth(4f);

        paint_red = new Paint();
        paint_red.setAntiAlias(true);
        paint_red.setStyle(Paint.Style.STROKE);
        paint_red.setColor(Color.RED);
        paint_red.setStrokeWidth(2f);
    }

    public FaceDetect(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);

        ctx = context;

        // initialize our opencv cascade classifiers
        faceClassifier = new CascadeClassifier(cascadeFile(R.raw.haarcascade_frontalface_alt2));
        smileClassifier = new CascadeClassifier(cascadeFile(R.raw.haarcascade_smile));

        // initialize our canvas paint object
        paint_green = new Paint();
        paint_green.setAntiAlias(true);
        paint_green.setColor(Color.parseColor("#22b3ab"));
        paint_green.setStyle(Paint.Style.STROKE);
        paint_green.setStrokeWidth(4f);

        paint_red = new Paint();
        paint_red.setAntiAlias(true);
        paint_red.setStyle(Paint.Style.STROKE);
        paint_red.setColor(Color.RED);
        paint_red.setStrokeWidth(2f);
    }

    public void setFollow() {
        followEnabled = true;
    }

    public void resetFollow() {
        followEnabled = false;
    }

    public void setSmileShot() {
        smileEnabled = true;
    }

    public void resetSmileShot() {
        smileEnabled = false;
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

    public void resume(final BebopVideoView bebopVideoView, final ImageView cvPreviewView, final BebopDrone bebopDrone, ToggleButton followBtn, ToggleButton smileBtn, final Beeper beepFinish) {
        if (getVisibility() == View.VISIBLE) {
            this.bebopVideoView = bebopVideoView;
            this.cvPreviewView = cvPreviewView;
            this.bebopDrone = bebopDrone;
            this.followBtn = followBtn;
            this.smileBtn = smileBtn;
            this.beepFinsh = beepFinish;

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

    private void FaceRecognition(Mat mat) {
        if(isFirst == true) {
            mainCenterX = mat.width() / 2;
            mainCenterY = mat.height() / 2;
            mainBoundRateX = mat.width() * 0.05f;
            mainBoundRateY = mat.height() * 0.05f;

            centerX = mainCenterX;
            centerY = mainCenterY;

            rateX = mat.width() * 0.4f;
            rateY = mat.height() * 0.4f;

            top_x = centerX - (rateX) / 2;
            top_y = centerY - (rateY) / 2;

            isFirst=false;
        }
    }

    private void FaceTrack(Mat mat) {
        if(facesArray!=null && facesArray.length>0 ) {
            centerX = faceCenterX;
            centerY = faceCenterY;

            top_x = centerX - (rateX)/2;
            top_y = centerY - (rateY)/2;

            if(top_x < 0)
                top_x = 0;
            if(top_y < 0)
                top_y = 0;
            if(top_x + rateX >= mat.width())
                top_x = mat.width() - rateX;
            if(top_y + rateY >= mat.height())
                top_y = mat.height() - rateY;
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
            followEnabled = false;
            smileEnabled = false;
            isFirst = true;
            facesArray = null;
            smileArray = null;
            smileRect = null;
            invalidate();
        }

        @Override
        public void run() {
            final Mat firstMat = new Mat();
            final Mat mat = new Mat();

            Mat submat= new Mat();
            Mat sub_submat = new Mat();
            Mat smile_submat = new Mat();
            Mat smile_sub_submat = new Mat();
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

                    final Size min_smile_Size = new Size(minRows*0.2f, minRows*0.2f);

                    FaceRecognition(mat);
                    FaceTrack(mat);

                    rect = new Rect((int)top_x,(int)top_y,(int)rateX,(int)rateY);

                    submat = mat.submat(rect);
                    submat.assignTo(sub_submat);

                    if(smileRect != null){
                        smile_submat = mat.submat(smileRect);
                        smile_submat.assignTo(smile_sub_submat);
                    }

                    faceClassifier.detectMultiScale(sub_submat, faces,1.1, 6, 0, minSize, maxSize);
                    if(smileEnabled == true)
                        smileClassifier.detectMultiScale(smile_sub_submat, smiles, 3 , 6 , 0, min_smile_Size,maxSize);
                    synchronized (lock) {
                        facesArray = faces.toArray();
                        mX = submat.width() / sub_submat.width();
                        mY = submat.height() / sub_submat.height();
                        faces.release();

                        if (followEnabled  && facesArray != null && facesArray.length > 0 & faceCenterX != 0 && faceCenterY != 0) {
                            // 얼굴 사이즈 비교
                            if (facesArray[0].area() != 0) {
                                // 얼굴이 뒤로 간 경우
                                if (mainFaceArea / facesArray[0].area() > 1.25f) {
                                    bebopDrone.setPitch((byte) 7);
                                    bebopDrone.setFlag((byte) 1);
                                }
                                // 얼굴이 앞으로 간 경우
                                else if (mainFaceArea / facesArray[0].area() < 0.75f) {
                                    bebopDrone.setPitch((byte) -7);
                                    bebopDrone.setFlag((byte) 1);
                                }
                                else {
                                    bebopDrone.setPitch((byte) 0);
                                    bebopDrone.setFlag((byte) 0);
                                }
                            }
                            // 얼굴이 중심좌표에서 좌우로 갔을때
                            if (Math.abs(faceCenterX - mainCenterX) > mainBoundRateX) {
                                // 얼굴이 왼쪽에 있는 경우
                                if (mainCenterX > faceCenterX) {
                                    bebopDrone.setYaw((byte) -15);
                                }
                                // 얼굴이 오른쪽에 있는 경우
                                else
                                    bebopDrone.setYaw((byte) 15);
                            } else {
                                bebopDrone.setYaw((byte) 0);
                            }
                            // 얼굴이 중심좌표에서 위아래로 갔을때
                            if (Math.abs(faceCenterY - mainCenterY) > mainBoundRateY * 1.5) {
                                // 얼굴이 위쪽에 있는 경우
                                if (mainCenterY > faceCenterY)
                                    bebopDrone.setGaz((byte) 13);
                                    // 얼굴이 아래쪽에 있는 경우
                                else
                                    bebopDrone.setGaz((byte) -13);
                            } else {
                                bebopDrone.setGaz((byte) 0);
                            }
                        } else {
                            bebopDrone.setYaw((byte) 0);
                            bebopDrone.setGaz((byte) 0);
                            bebopDrone.setPitch((byte) 0);
                            bebopDrone.setFlag((byte) 0);
                        }

                        if(smileEnabled == true) {
                            smileArray = smiles.toArray();
                            smiles.release();
                            cTime = System.currentTimeMillis();
                            if (cTime - sTime > 1200) {
                                if (count >= 2) {
                                    beepFinsh.play();
                                    bebopDrone.takePicture();
                                }
                                sTime = cTime;
                                count = 0;
                            }
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
                    sleep(60);
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
        synchronized(lock) {
            if(followBtn != null && smileBtn != null && followEnabled != true) {
                followBtn.setEnabled(false);
                followBtn.setVisibility(INVISIBLE);
                smileBtn.setEnabled(false);
                smileBtn.setVisibility(INVISIBLE);
            }
            if (facesArray != null && facesArray.length > 0) {
                followBtn.setEnabled(true);
                followBtn.setVisibility(VISIBLE);
                smileBtn.setEnabled(true);

                float faceTLX = (float) facesArray[0].tl().x * mX;
                float faceBRX = (float) facesArray[0].br().x * mX;
                float faceTLY = (float) facesArray[0].tl().y * mY;
                float faceBRY = (float) facesArray[0].br().y * mY;

                canvas.drawRect(top_x + faceTLX, top_y + faceTLY, top_x + faceBRX, top_y + faceBRY, paint_green);

                smileRect = new Rect((int)(top_x + faceTLX), (int)(top_y + faceTLY), (int)(faceBRX - faceTLX), (int)(faceBRY - faceTLY));

                faceCenterX = (top_x*2 + faceTLX + faceBRX)/2;
                faceCenterY = (top_y*2 + faceTLY + faceBRY)/2;

                float smileTopX = top_x + faceTLX;
                float smileTopY = top_y + faceTLY;

                if(smileEnabled == true) {
                    for (Rect target : smileArray) {
                        float TopLeftX = (float) target.tl().x * mX;
                        float BottomRightX = (float) target.br().x * mX;
                        float TopLeftY = (float) target.tl().y * mY;
                        float BottomRightY = (float) target.br().y * mY;
                        canvas.drawRect(smileTopX + TopLeftX, smileTopY + TopLeftY, smileTopX + BottomRightX, smileTopY + BottomRightY, paint_red);
                        count++;
                    }
                }
            }
        }
        super.onDraw(canvas);
    }
}