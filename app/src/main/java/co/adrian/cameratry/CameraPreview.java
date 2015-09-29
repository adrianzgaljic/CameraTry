package co.adrian.cameratry;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Paint;
import android.graphics.YuvImage;
import android.hardware.Camera;
import android.os.AsyncTask;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.SurfaceView;

import org.opencv.android.Utils;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.MatOfRect;
import org.opencv.core.Rect;
import org.opencv.core.Size;
import org.opencv.objdetect.CascadeClassifier;

import java.io.ByteArrayOutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;




public class CameraPreview extends SurfaceView implements
        SurfaceHolder.Callback{

    public static final String TAG = "logIspis";

    private SurfaceHolder mSurfaceHolder;
    private Camera mCamera;
    private Paint textPaint = new Paint();
    private CascadeClassifier faceDetector = null;

    private MainActivity mActivity = null;
    private DrawingSurface drawSurface;
    private List<Rect> listOfFaces;
    boolean processInProgress = false;
    Bitmap b;
    Mat mat;
    MatOfRect faceDetections;
    ProcessPreviewDataTask processingTask;


    // Constructor that obtains context and camera
    @SuppressWarnings("deprecation")
    public CameraPreview(Context context, Camera camera, DrawingSurface drawSurface) {

        super(context);
        this.mCamera = camera;
        this.mSurfaceHolder = this.getHolder();
        this.mSurfaceHolder.addCallback(this);
        this.mSurfaceHolder.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);
        this.drawSurface = drawSurface;
        textPaint.setARGB(255, 200, 0, 0);
        textPaint.setTextSize(60);

        listOfFaces = new ArrayList<Rect>();


        try{
            mActivity = (MainActivity) context;
            faceDetector = mActivity.faceDetector;
        } catch (Exception e){
            Log.e(TAG,"[CameraPreview] error catching face detector "+e.toString());
        }


    }

    @Override
    public void surfaceCreated(SurfaceHolder surfaceHolder) {

        try {
            mCamera.setPreviewDisplay(surfaceHolder);
            mCamera.startPreview();
            mCamera.setPreviewCallback(null);
        } catch (Exception e) {
            Log.e(TAG,"[CameraPreview] error creating preview "+e.toString());
        }

    }

    @Override
    public void surfaceDestroyed(SurfaceHolder surfaceHolder) {

            mCamera.stopPreview();
            mCamera.setPreviewCallback(null);
            mCamera.release();
    }






    @Override
    public void surfaceChanged(SurfaceHolder surfaceHolder, int format,
            int width, int height) {


        try {

            mCamera.setPreviewCallback(new Camera.PreviewCallback() {

                Bitmap b = null;
                Mat mat = null;
                MatOfRect faceDetections;

                @Override
                public void onPreviewFrame(byte[] data, Camera camera) {
                    try {

                        if (mActivity.isOver && !processInProgress) {
                            if (mActivity.detectionStarted) {
                                int numberOfPictures = mActivity.numberOfImages;
                                try {

                                    processingTask = new ProcessPreviewDataTask(camera, data, mat, faceDetections, drawSurface, b, numberOfPictures);
                                    processingTask.execute();

                                } catch (Exception e) {
                                    Log.e(TAG, "Error executing AsynTask " + e.toString());
                                }
                            } else {
                                mActivity.btnStartDetection.setBackgroundResource(R.drawable.start);
                            }
                        }

                    } catch(OutOfMemoryError e){
                        Log.e(TAG,"OutOfMemoryError occured during image processing");
                    }
                }




            });

            mCamera.setPreviewDisplay(surfaceHolder);
            mCamera.startPreview();


        } catch (Exception e) {
            Log.i(TAG,"GRESKA KOJA SE TRAZI");
        }

    }





    public  class ProcessPreviewDataTask
            extends AsyncTask<Void, Void, Boolean> {

        Camera camera;
        byte[] data;
        DrawingSurface drawSurface;
        int numberOfPictures;

        public ProcessPreviewDataTask(Camera camera, byte[] data, Mat mat, MatOfRect faceDetections, DrawingSurface drawSurface, Bitmap b, int numberOfPictures){
            this.camera = camera;
            this.data = data;
            this.drawSurface = drawSurface;
            this.numberOfPictures = numberOfPictures;
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

            mat = new Mat(b.getWidth(), b.getHeight(), CvType.CV_8UC1);
            Utils.bitmapToMat(b, mat);
            faceDetections = new MatOfRect();



            try {
                faceDetector.empty();
                faceDetector.detectMultiScale(mat, faceDetections);
                faceDetector.detectMultiScale(mat, faceDetections,1.1,4,1,new Size(50,50),new Size(500,500));
            } catch (Exception e) {
                Log.i(TAG, "facedetection " + e);
            }

            // Log.e(TAG,faceDetector.toString());

            Log.i(TAG, "drawsurface=" + drawSurface);





            return true;
        }

        @Override
        protected void onPostExecute(Boolean result){
            Log.i(TAG, "running onPostExecute");

            if (faceDetections.toArray().length > 0 && mActivity.picturesTaken < numberOfPictures && !mActivity.locked) {

                try {
                    drawSurface.playSound();
                    listOfFaces.clear();
                    drawSurface.clearCanvas();


                } catch (Exception e) {
                    Log.i(TAG, "POKUSAJ NEUSPIO " + e);
                }
                android.graphics.Rect focusRect = null;

                for (Rect rect : faceDetections.toArray()) {

                    Log.v(TAG, rect.toString());
                    drawSurface.drawFaces(rect, b.getWidth(), b.getHeight());
                    focusRect = new android.graphics.Rect(rect.x, rect.y, rect.x + rect.width, rect.y + rect.height);
                    //addToListOfFaces(rect);
                }

                try {
                    faceDetections.empty();
                    mActivity.takePicture(focusRect);


                } catch (Exception e) {
                    Log.i(TAG, "POKUSAJ NEUSPIO 2 " + e);
                }


                for (Rect rect : listOfFaces) {
                    drawSurface.drawFaces(rect, b.getWidth(), b.getHeight());
                    android.graphics.Rect rectan = new android.graphics.Rect(rect.x, rect.y, rect.x + rect.width, rect.y + rect.height);
                    try {
                        mActivity.takePicture(rectan);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }


            } else {
                Log.i(TAG, "DRAW SURFACE JEBEMU" + drawSurface);

                try {

                    drawSurface.clearCanvas();


                } catch (Exception e) {
                    Log.i(TAG, "zapelo " + e);
                }


            }

            if (mActivity.picturesTaken >= numberOfPictures) {
                mActivity.picturesTaken = 0;
                mActivity.detectionStarted = false;

            }
            b.recycle();

            b=null;
            processInProgress = false;
            // set pixels once processing is done



        }


    }

}