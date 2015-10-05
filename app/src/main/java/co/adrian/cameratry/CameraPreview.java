package co.adrian.cameratry;

import android.content.Context;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Paint;
import android.graphics.YuvImage;
import android.hardware.Camera;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.SurfaceView;

import org.opencv.android.Utils;
import org.opencv.core.Core;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.MatOfRect;
import org.opencv.core.Rect;
import org.opencv.core.Size;
import org.opencv.objdetect.CascadeClassifier;

import java.io.ByteArrayOutputStream;





public class CameraPreview extends SurfaceView implements
        SurfaceHolder.Callback{

    public static final String TAG = "logIspis";

    private Camera mCamera;
    private CascadeClassifier faceDetector = null;
    private MainActivity mActivity;
    private DrawingSurface drawSurface;
    private boolean processInProgress = false;
    private Bitmap b;
    private MatOfRect faceDetections;
    private ProcessPreviewDataTask processingTask;
    private int orient;


    // Constructor that obtains context and camera
    @SuppressWarnings("deprecation")
    public CameraPreview(Context context, Camera camera, DrawingSurface drawSurface) {

        super(context);
        this.mCamera = camera;
        SurfaceHolder mSurfaceHolder = this.getHolder();
        mSurfaceHolder.addCallback(this);
        mSurfaceHolder.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);
        this.drawSurface = drawSurface;
        Paint textPaint = new Paint();
        textPaint.setARGB(255, 200, 0, 0);
        textPaint.setTextSize(60);


        mActivity = null;
        try{
            mActivity = (MainActivity) context;
            faceDetector = mActivity.faceDetector;
        } catch (Exception e){
            Log.e(TAG,"[CameraPreview] error catching face detector. "+e.toString());
        }


    }

    @Override
    public void surfaceCreated(SurfaceHolder surfaceHolder) {
        Log.e(TAG, "[CameraPreview] surface created");

        try {
            mCamera.setPreviewDisplay(surfaceHolder);
            mCamera.startPreview();
            mCamera.setPreviewCallback(null);
        } catch (Exception e) {
            Log.e(TAG,"[CameraPreview] error creating preview "+e.toString());
            mActivity.onCreate(new Bundle());
        }

    }

    @Override
    public void surfaceDestroyed(SurfaceHolder surfaceHolder) {
            Log.e(TAG, "[CameraPreview] surface destroyed");
            this.getHolder().removeCallback(this);
            mCamera.setPreviewCallback(null);
            mCamera.stopPreview();
            mCamera.release();
    }






    @Override
    public void surfaceChanged(SurfaceHolder surfaceHolder, int format,
            int width, int height) {
        Log.e(TAG,"[CameraPreview] surface changed");


        try {


            mCamera.setPreviewCallback(new Camera.PreviewCallback() {

                @Override
                public void onPreviewFrame(byte[] data, Camera camera) {
                    try {

                        if (mActivity.isOver && !processInProgress) {
                            if (MainActivity.detectionStarted) {

                                try {
                                    orient = getResources().getConfiguration().orientation;
                                    processingTask = new ProcessPreviewDataTask(camera, data);
                                    processingTask.execute();

                                } catch (Exception e) {
                                    Log.e(TAG, "[CameraPreview] Error executing AsynTask " + e.toString());
                                }
                            } else {
                                mActivity.btnStartDetection.setBackgroundResource(R.drawable.start);
                            }
                        }

                    } catch (OutOfMemoryError e) {
                        Log.e(TAG, "[CameraPreview] OutOfMemoryError occured during image processing");
                    }
                }
            });

            mCamera.setPreviewDisplay(surfaceHolder);
            mCamera.startPreview();


        } catch (Exception e) {
            Log.i(TAG, "[CameraPreview] Error setting preview callback");

        }

    }


 public  class ProcessPreviewDataTask
            extends AsyncTask<Void, Void, Boolean> {

        Camera camera;
        byte[] data;
        int numberOfPictures = MainActivity.numberOfImages;

        public ProcessPreviewDataTask(Camera camera, byte[] data){
            this.camera = camera;
            this.data = data;
        }

        @Override
        protected Boolean doInBackground(Void... datas) {
            processInProgress = true;

            Camera.Parameters parameters = camera.getParameters();
            int width = parameters.getPreviewSize().width;
            int height = parameters.getPreviewSize().height;

            YuvImage yuv = new YuvImage(data, parameters.getPreviewFormat(), width, height, null);

            ByteArrayOutputStream out = new ByteArrayOutputStream();
            yuv.compressToJpeg(new android.graphics.Rect(0, 0, width, height), 50, out);

            byte[] bytes = out.toByteArray();
            b = BitmapFactory.decodeByteArray(bytes, 0, bytes.length);

            Mat mat = new Mat(b.getWidth(), b.getHeight(), CvType.CV_8UC1);
            Utils.bitmapToMat(b, mat);

            if (orient == Configuration.ORIENTATION_PORTRAIT){
                Core.transpose(mat, mat);
                if (MainActivity.cameraOrientation == Camera.CameraInfo.CAMERA_FACING_FRONT){
                    Core.flip(mat, mat, 0);
                } else  {
                    Core.flip(mat, mat, 1);
                }

            }
            faceDetections = new MatOfRect();



            try {
                faceDetector.empty();
                faceDetector.detectMultiScale(mat, faceDetections);
                faceDetector.detectMultiScale(mat, faceDetections,1.1,4,1,new Size(50,50),new Size(1000,1000));
            } catch (Exception e) {
                Log.i(TAG, "[CameraPreview] Error during face detection" + e);
            }

            return true;
        }

        @Override
        protected void onPostExecute(Boolean result){
            Log.i(TAG, "[CameraPreview] running onPostExecute");

            if (faceDetections.toArray().length > 0 && mActivity.picturesTaken < numberOfPictures && !mActivity.locked) {

                try {
                    drawSurface.playSound();
                    drawSurface.clearCanvas();

                } catch (Exception e) {
                    Log.i(TAG, "[CameraPreview]  First block error." + e);
                }

                android.graphics.Rect focusRect = null;

                for (Rect rect : faceDetections.toArray()) {
                    drawSurface.drawFaces(rect, b.getWidth(), b.getHeight(),orient);

                    focusRect = new android.graphics.Rect(rect.x, rect.y, rect.x + rect.width, rect.y + rect.height);
                }

                try {
                    faceDetections.empty();
                    mActivity.takePicture(focusRect);
                } catch (Exception e) {
                    Log.i(TAG, "[CameraPreview] Error:empty face det. take picture. " + e);
                }


            } else {


                try {

                    drawSurface.clearCanvas();

                } catch (Exception e) {
                    Log.i(TAG, "[CameraPreview] Clear canvas error. " + e);
                }

            }

            if (mActivity.picturesTaken >= numberOfPictures) {
                mActivity.picturesTaken = 0;
                MainActivity.detectionStarted = false;

            }
            b.recycle();
            b=null;
            processInProgress = false;
        }
    }

}