package co.adrian.cameratry;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Paint;
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
import org.opencv.objdetect.CascadeClassifier;

import java.util.ArrayList;
import java.util.List;


public class CameraPreview extends SurfaceView implements
        SurfaceHolder.Callback{

    public static final String TAG = "logIspis";

    private SurfaceHolder mSurfaceHolder;
    private Camera mCamera;
    private Paint textPaint = new Paint();
    private CascadeClassifier faceDetector = null;

    private Custom_CameraActivity ac = null;
    private DrawingSurface drawSurface;
    private List<Rect> listOfFaces;
    private List<Rect> assistantListOfFaces;
    boolean processInProgress = false;
    Bitmap b;
    Mat mat;
    MatOfRect faceDetections;


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
        assistantListOfFaces = new ArrayList<Rect>();

        Log.i(TAG,"drawing surface u camera preview "+drawSurface);


        try{
            ac = (Custom_CameraActivity) context;
            faceDetector = ac.faceDetector;
            Log.e(TAG, "face detector: "+faceDetector.toString());
        } catch (Exception e){
            Log.e(TAG,"GRESKA FACEDETECTOR: "+e.toString());
        }



}

    @Override
    public void surfaceCreated(SurfaceHolder surfaceHolder) {
        try {

            mCamera.setPreviewDisplay(surfaceHolder);
            mCamera.startPreview();
            mCamera.setPreviewCallback(null);
            Log.i(TAG,"faces max= "+ mCamera.getParameters().getMaxNumDetectedFaces());

        } catch (Exception e) {
            Log.e(TAG,"ERROR: "+e.toString());
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
        // start preview with new settings



        try {

            mCamera.setPreviewCallback(new Camera.PreviewCallback() {

                Bitmap b = null;
                Mat mat = null;
                MatOfRect faceDetections;

                @Override
                public void onPreviewFrame(byte[] data, Camera camera) {


                    if (ac.isOver && !processInProgress) {
                        if (ac.detectionStarted) {

                            int numberOfPictures = ac.numberOfImages;



                            try {
/*
                                int width = camera.getParameters().getPreviewSize().width;
                                int height = camera.getParameters().getPreviewSize().height;
                                int[] rgb = decodeYUV420SP(data, width, height);


                                b = Bitmap.createBitmap(rgb, width, height, Bitmap.Config.ARGB_8888);
                                mat = new Mat(b.getWidth(), b.getHeight(), CvType.CV_8UC1);
                                Utils.bitmapToMat(b, mat);
                                faceDetections = new MatOfRect();
                                */
                          //      if (!processInProgress){
                                    new ProcessPreviewDataTask(camera,data,mat,faceDetections,drawSurface,b,numberOfPictures).execute();
                           //     }


/*
                                try {
                                    faceDetector.empty();
                                    faceDetector.detectMultiScale(mat, faceDetections);
                                } catch (Exception e) {
                                    Log.i(TAG, "facedetection " + e);
                                }

                                // Log.e(TAG,faceDetector.toString());

                                Log.i(TAG, "drawsurface=" + drawSurface);
                                if (faceDetections.toArray().length > 0 && ac.picturesTaken < numberOfPictures && !ac.locked) {

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
                                        android.graphics.Rect rectan = new android.graphics.Rect(rect.x, rect.y, rect.x + rect.width, rect.y + rect.height);
                                        focusRect = rectan;
                                        //addToListOfFaces(rect);
                                    }

                                    try {
                                        faceDetections.empty();
                                        ac.takePicture(focusRect);


                                    } catch (Exception e) {
                                        Log.i(TAG, "POKUSAJ NEUSPIO 2 " + e);
                                    }


                                    for (Rect rect : listOfFaces) {
                                        drawSurface.drawFaces(rect, b.getWidth(), b.getHeight());
                                        android.graphics.Rect rectan = new android.graphics.Rect(rect.x, rect.y, rect.x + rect.width, rect.y + rect.height);
                                        ac.takePicture(rectan);
                                    }


                                } else {
                                    Log.i(TAG, "DRAW SURFACE JEBEMU" + drawSurface);

                                    try {

                                        drawSurface.clearCanvas();


                                    } catch (Exception e) {
                                        Log.i(TAG, "zapelo " + e);
                                    }


                                }

                                if (ac.picturesTaken >= numberOfPictures) {
                                    ac.picturesTaken = 0;
                                    ac.detectionStarted = false;

                                }
*/
                            } catch (Exception e) {
                                Log.e(TAG, "greska je: " + e.toString());
                            }
                        } else {
                            ac.tvCountDown.setTextSize(20);
                            ac.tvCountDown.setText("touch to start detection");
                            ac.btnStartDetection.setBackgroundResource(R.drawable.start_shadow);
                        }
                    }

                }




            });


            mCamera.setPreviewDisplay(surfaceHolder);
            mCamera.startPreview();


        } catch (Exception e) {
            Log.i(TAG,"GRESKA KOJA SE TRAZI");
        }

    }




    public int[] decodeYUV420SP( byte[] yuv420sp, int width, int height) {

        final int frameSize = width * height;

        int rgb[]=new int[width*height];
        for (int j = 0, yp = 0; j < height; j++) {
            int uvp = frameSize + (j >> 1) * width, u = 0, v = 0;
            for (int i = 0; i < width; i++, yp++) {
                int y = (0xff & ((int) yuv420sp[yp])) - 16;
                if (y < 0) y = 0;
                if ((i & 1) == 0) {
                    v = (0xff & yuv420sp[uvp++]) - 128;
                    u = (0xff & yuv420sp[uvp++]) - 128;
                }

                int y1192 = 1192 * y;
                int r = (y1192 + 1634 * v);
                int g = (y1192 - 833 * v - 400 * u);
                int b = (y1192 + 2066 * u);

                if (r < 0) r = 0; else if (r > 262143) r = 262143;
                if (g < 0) g = 0; else if (g > 262143) g = 262143;
                if (b < 0) b = 0; else if (b > 262143) b = 262143;

                rgb[yp] = 0xff000000 | ((r << 6) & 0xff0000) | ((g >> 2) &
                        0xff00) | ((b >> 10) & 0xff);


            }
        }
        return rgb;
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
          //  this.mat = mat;
           // this.faceDetections = faceDetections;
            this.drawSurface = drawSurface;
          //  this.b = b;
            this.numberOfPictures = numberOfPictures;
        }

        @Override
        protected Boolean doInBackground(Void... datas) {
            processInProgress = true;
            int width = camera.getParameters().getPreviewSize().width;
            int height = camera.getParameters().getPreviewSize().height;
            int[] rgb = decodeYUV420SP(data, width, height);


            b = Bitmap.createBitmap(rgb, width, height, Bitmap.Config.ARGB_8888);
            mat = new Mat(b.getWidth(), b.getHeight(), CvType.CV_8UC1);
            Utils.bitmapToMat(b, mat);
            faceDetections = new MatOfRect();


            try {
                faceDetector.empty();
                faceDetector.detectMultiScale(mat, faceDetections);
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

            if (faceDetections.toArray().length > 0 && ac.picturesTaken < numberOfPictures && !ac.locked) {

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
                    android.graphics.Rect rectan = new android.graphics.Rect(rect.x, rect.y, rect.x + rect.width, rect.y + rect.height);
                    focusRect = rectan;
                    //addToListOfFaces(rect);
                }

                try {
                    faceDetections.empty();
                    ac.takePicture(focusRect);


                } catch (Exception e) {
                    Log.i(TAG, "POKUSAJ NEUSPIO 2 " + e);
                }


                for (Rect rect : listOfFaces) {
                    drawSurface.drawFaces(rect, b.getWidth(), b.getHeight());
                    android.graphics.Rect rectan = new android.graphics.Rect(rect.x, rect.y, rect.x + rect.width, rect.y + rect.height);
                    try {
                        ac.takePicture(rectan);
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

            if (ac.picturesTaken >= numberOfPictures) {
                ac.picturesTaken = 0;
                ac.detectionStarted = false;

            }
            processInProgress = false;
            // set pixels once processing is done



        }

        public int[] decodeYUV420SP( byte[] yuv420sp, int width, int height) {

            final int frameSize = width * height;

            int rgb[]=new int[width*height];
            for (int j = 0, yp = 0; j < height; j++) {
                int uvp = frameSize + (j >> 1) * width, u = 0, v = 0;
                for (int i = 0; i < width; i++, yp++) {
                    int y = (0xff & ((int) yuv420sp[yp])) - 16;
                    if (y < 0) y = 0;
                    if ((i & 1) == 0) {
                        v = (0xff & yuv420sp[uvp++]) - 128;
                        u = (0xff & yuv420sp[uvp++]) - 128;
                    }

                    int y1192 = 1192 * y;
                    int r = (y1192 + 1634 * v);
                    int g = (y1192 - 833 * v - 400 * u);
                    int b = (y1192 + 2066 * u);

                    if (r < 0) r = 0; else if (r > 262143) r = 262143;
                    if (g < 0) g = 0; else if (g > 262143) g = 262143;
                    if (b < 0) b = 0; else if (b > 262143) b = 262143;

                    rgb[yp] = 0xff000000 | ((r << 6) & 0xff0000) | ((g >> 2) &
                            0xff00) | ((b >> 10) & 0xff);


                }
            }
            return rgb;
        }

    }

}