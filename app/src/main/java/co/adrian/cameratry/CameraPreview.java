package co.adrian.cameratry;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Paint;
import android.hardware.Camera;
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
import java.util.Random;

public class CameraPreview extends SurfaceView implements
        SurfaceHolder.Callback{
    private SurfaceHolder mSurfaceHolder;
    private Camera mCamera;
    public static final String TAG = "logIspis";
    Mat mat;
    Paint textPaint = new Paint();
    CascadeClassifier faceDetector = null;
    MatOfRect faceDetections;
    Custom_CameraActivity ac = null;
    DrawingSurface drawSurface;
    List<Rect> listOfFaces;
    List<Rect> assistantListOfFaces;



    public Random random;

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
        random = new Random();
        listOfFaces = new ArrayList<Rect>();
        assistantListOfFaces = new ArrayList<Rect>();


        // This call is necessary, or else the
        // draw method will not be called.

        try{
            ac = (Custom_CameraActivity) context;
            faceDetector = ac.faceDetector;
            Log.e(TAG, "face detector: "+faceDetector.toString());
        } catch (Exception e){
            Log.e(TAG,"GRESKA: "+e.toString());
        }



}

    @Override
    public void surfaceCreated(SurfaceHolder surfaceHolder) {
        try {

            mCamera.setPreviewDisplay(surfaceHolder);
            mCamera.startPreview();
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


                @Override
                public void onPreviewFrame(byte[] data, Camera camera) {
                    if (ac.isOver) {
                        if (ac.detectionStarted) {
                            Bitmap b;
                            int numberOfPictures = ac.numberOfImages;


                            try {

                                int width = camera.getParameters().getPreviewSize().width;
                                int height = camera.getParameters().getPreviewSize().height;
                                int[] rgb = decodeYUV420SP(data, width, height);


                                b = Bitmap.createBitmap(rgb, width, height, Bitmap.Config.ARGB_8888);
                                // Log.e(TAG,"bit= "+b.toString());
                                mat = new Mat(b.getWidth(), b.getHeight(), CvType.CV_8UC1);
                                Utils.bitmapToMat(b, mat);
                                // Log.e(TAG,"mat= "+mat.toString());
                                faceDetections = new MatOfRect();
                                faceDetector.empty();
                                try {
                                    faceDetector.detectMultiScale(mat, faceDetections);
                                } catch (Exception e){
                                    Log.i(TAG,"facedetection "+e);
                                }

                                // Log.e(TAG,faceDetector.toString());


                                if (faceDetections.toArray().length > 0 && ac.picturesTaken < numberOfPictures && !ac.locked) {


                                    drawSurface.playSound();
                                    listOfFaces.clear();
                                    drawSurface.clearCanvas();

                                    android.graphics.Rect focusRect = null;

                                    for (Rect rect : faceDetections.toArray()) {

                                        Log.v(TAG, rect.toString());
                                        drawSurface.drawFaces(rect, b.getWidth(), b.getHeight());
                                        android.graphics.Rect rectan = new android.graphics.Rect(rect.x, rect.y, rect.x + rect.width, rect.y + rect.height);
                                        focusRect = rectan;
                                        //addToListOfFaces(rect);
                                    }

                                    faceDetections.empty();
                                    ac.takePicture(focusRect);


                                    for (Rect rect : listOfFaces) {
                                        drawSurface.drawFaces(rect, b.getWidth(), b.getHeight());
                                        android.graphics.Rect rectan = new android.graphics.Rect(rect.x, rect.y, rect.x + rect.width, rect.y + rect.height);
                                        ac.takePicture(rectan);
                                    }


                                } else {
                                    drawSurface.clearCanvas();
                                }

                                if (ac.picturesTaken >= numberOfPictures) {
                                    ac.picturesTaken = 0;
                                    ac.detectionStarted = false;

                                }

                            } catch (Exception e) {
                                Log.e(TAG, "greska: " + e.toString());
                            }
                        } else {
                            ac.tvCountDown.setTextSize(20);
                            ac.tvCountDown.setText("touch to start detection");
                        }
                    }

                }


                private void addToListOfFaces(Rect rect) {
                    if (checkifRectIsValid(rect)){
                        listOfFaces.add(rect);
                    }

                }

                private boolean checkifRectIsValid(Rect rect) {
                    android.graphics.Rect graphRect = new android.graphics.Rect(rect.x, rect.y, rect.x + rect.width, rect.y + rect.height);
                    for (Rect rectangle:listOfFaces){
                        android.graphics.Rect graphRectangle = new android.graphics.Rect(rectangle.x, rectangle.y, rectangle.x + rectangle.width, rectangle.y + rectangle.height);
                        if (android.graphics.Rect.intersects(graphRect,graphRectangle)){
                            Log.i(TAG,"INTERSECTS");
                            return false;
                        }
                    }
                    return true;
                }
            });


            mCamera.setPreviewDisplay(surfaceHolder);
            mCamera.startPreview();


        } catch (Exception e) {
            // intentionally left blank for a test
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

}