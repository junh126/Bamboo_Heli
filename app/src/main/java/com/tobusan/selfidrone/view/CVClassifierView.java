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

public class CVClassifierView extends View {
    private final static String CLASS_NAME = CVClassifierView.class.getSimpleName();

    private final Context ctx;

    private CascadeClassifier faceClassifier;

    private Handler openCVHandler = new Handler();
    private Thread openCVThread = null;

    private BebopVideoView bebopVideoView = null;
    private ImageView cvPreviewView = null;
    private BebopDrone bebopDrone = null;

    private Rect[] facesArray = null;

    private Paint paint;

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

    private boolean isFirst = true;
    private boolean followEnabled = false;

    private Button followBtn;

    public CVClassifierView(Context context) {
        super(context);
        ctx = context;

        faceClassifier = new CascadeClassifier(cascadeFile(R.raw.haarcascade_frontalface_alt2));

        // initialize our canvas paint object
        paint = new Paint();
        paint.setAntiAlias(true);
        paint.setColor(Color.GREEN);
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(4f);
    }

    public CVClassifierView(Context context, AttributeSet attrs) {
        super(context,attrs);
        ctx = context;

        faceClassifier = new CascadeClassifier(cascadeFile(R.raw.haarcascade_frontalface_alt2));

        // initialize our canvas paint object
        paint = new Paint();
        paint.setAntiAlias(true);
        paint.setColor(Color.GREEN);
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(4f);
    }

    public CVClassifierView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);

        ctx = context;

        // initialize our opencv cascade classifiers
        faceClassifier = new CascadeClassifier(cascadeFile(R.raw.haarcascade_frontalface_alt2));

        // initialize our canvas paint object
        paint = new Paint();
        paint.setAntiAlias(true);
        paint.setColor(Color.GREEN);
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(4f);
    }

    public void setFollow() {
        followEnabled = !followEnabled;
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

    public void resume(final BebopVideoView bebopVideoView, final ImageView cvPreviewView, final BebopDrone bebopDrone, Button sub_btn) {
        if (getVisibility() == View.VISIBLE) {
            this.bebopVideoView = bebopVideoView;
            this.cvPreviewView = cvPreviewView;
            this.bebopDrone = bebopDrone;
            this.followBtn = sub_btn;

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

    private void FaceDetect(Mat mat) {
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
            isFirst = true;
            facesArray = null;
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

                    final int minRows = Math.round(mat.rows() * 0.07f);

                    final Size minSize = new Size(minRows, minRows);
                    final Size maxSize = new Size(0, 0);

                    FaceDetect(mat);
                    FaceTrack(mat);

                    rect = new Rect((int)top_x,(int)top_y,(int)rateX,(int)rateY);

                    submat = mat.submat(rect);
                    submat.assignTo(sub_submat);

                    faceClassifier.detectMultiScale(sub_submat, faces, 1.05, 6, 0, minSize, maxSize);

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
                                    bebopDrone.setPitch((byte) 4);
                                    bebopDrone.setFlag((byte) 1);
                                }
                                // 얼굴이 앞으로 간 경우
                                else if (mainFaceArea / facesArray[0].area() < 0.75f) {
                                    bebopDrone.setPitch((byte) -4);
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
                                    bebopDrone.setYaw((byte) -10);
                                }
                                // 얼굴이 오른쪽에 있는 경우
                                else
                                    ;
                                bebopDrone.setYaw((byte) 10);
                            } else {
                                bebopDrone.setYaw((byte) 0);
                            }
                            // 얼굴이 중심좌표에서 위아래로 갔을때
                            if (Math.abs(faceCenterY - mainCenterY) > mainBoundRateY * 1.5) {
                                // 얼굴이 위쪽에 있는 경우
                                if (mainCenterY > faceCenterY)
                                    bebopDrone.setGaz((byte) 10);
                                    // 얼굴이 아래쪽에 있는 경우
                                else
                                    bebopDrone.setGaz((byte) -10);
                                //
                            } else {
                                bebopDrone.setGaz((byte) 0);
                            }
                        } else {
                            bebopDrone.setYaw((byte) 0);
                            bebopDrone.setGaz((byte) 0);
                            bebopDrone.setPitch((byte) 0);
                            bebopDrone.setFlag((byte) 0);
                        }

                        if(interrupted)
                            facesArray = null;

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

        synchronized(lock) {
            if(followBtn != null) {
                followBtn.setEnabled(false);
                followBtn.setVisibility(INVISIBLE);
            }
            if (facesArray != null && facesArray.length > 0) {
                followBtn.setEnabled(true);
                followBtn.setVisibility(VISIBLE);
                for (Rect target : facesArray) {
                    float TopLeftX = (float) target.tl().x * mX;
                    float BottomRightX = (float) target.br().x * mX;
                    float TopLeftY = (float) target.tl().y * mY;
                    float BottomRightY = (float) target.br().y * mY;

                    canvas.drawRect(top_x + TopLeftX, top_y + TopLeftY, top_x + BottomRightX, top_y + BottomRightY, paint);

                    faceCenterX = (top_x*2 + TopLeftX + BottomRightX)/2;
                    faceCenterY = (top_y*2 + TopLeftY + BottomRightY)/2;
                }
            }
        }
        super.onDraw(canvas);
    }
}