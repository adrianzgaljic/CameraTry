package co.adrian.cameratry;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.ContentValues;
import android.content.Context;
import android.content.DialogInterface;
import android.graphics.PixelFormat;
import android.hardware.Camera;
import android.media.AudioManager;
import android.media.SoundPool;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.util.Log;
import android.view.Surface;
import android.view.SurfaceHolder;
import android.view.View;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;

import org.opencv.objdetect.CascadeClassifier;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;

import static android.hardware.Camera.Area;
import static android.hardware.Camera.AutoFocusCallback;
import static android.hardware.Camera.CameraInfo;
import static android.hardware.Camera.Parameters.FLASH_MODE_OFF;
import static android.hardware.Camera.Parameters.FLASH_MODE_ON;
import static android.hardware.Camera.Parameters.FOCUS_MODE_AUTO;
import static android.hardware.Camera.PictureCallback;
import static android.hardware.Camera.getCameraInfo;
import static android.hardware.Camera.open;


public class Custom_CameraActivity extends Activity implements  AutoFocusCallback{
    private Camera mCamera;


    @Override
    public void onAutoFocus(boolean success, Camera camera) {

    }

    private CameraPreview mCameraPreview;
    public static final String TAG = "logIspis";
    ImageView lastPhoto;
    public TextView tvCountDown;
    public CascadeClassifier faceDetector = null;
    public String flash = FLASH_MODE_OFF;
    final Context context = this;
    public int timerTime = 0;
    public int numberOfImages = 1;
    public int picturesTaken = 0;
    public boolean locked = false;
    public boolean isOver =true;
    public boolean detectionStarted = false;
    public boolean soundDetection = false;
    public boolean soundCountdown = false;
    public boolean soundShutter = false;
    SoundPool soundPool;
    int soundIdCountdown;
    int soundIdShutter;
    int cameraOrientation = CameraInfo.CAMERA_FACING_BACK;
    Button flashButton;
    Button timerButton;
    Button imagesNumberButton;
    Button soundButton;
    Button cameraChangeButton;
    //LinearLayout layoutDown;
    //private ViewSwitcher switcher;



    public AutoFocusCallback thisClass = this;
    DrawingSurface drawingSurface;
    static{
        System.loadLibrary("opencv_java3");
    }






    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {

        // Lodaing cascade classifier
        try {
            InputStream is = getResources().openRawResource(R.raw.lbpcascade_frontalface);
            File cascadeDir = getDir("cascade", Context.MODE_PRIVATE);
            File mCascadeFile = new File(cascadeDir, "haarcascade_frontalface_default.xml");
            FileOutputStream os = new FileOutputStream(mCascadeFile);
            byte[] buffer = new byte[4096];
            int bytesRead;
            while ((bytesRead = is.read(buffer)) != -1) {
                os.write(buffer, 0, bytesRead);
            }
            is.close();
            os.close();
            faceDetector = new CascadeClassifier(mCascadeFile.getAbsolutePath());
            Log.i(TAG,"CASCADE ="+faceDetector);
        } catch (Exception e) {
            Log.e(TAG, "Error loading cascade ", e);
        }

        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);

        soundPool = new SoundPool(1, AudioManager.STREAM_MUSIC, 0);
        soundIdCountdown = soundPool.load(context, R.raw.countdown_sound, 1);
        soundIdShutter = soundPool.load(context, R.raw.shutter_sound, 1);

        lastPhoto = (ImageView) findViewById(R.id.lastPhoto);
        mCamera = open(cameraOrientation);

        tvCountDown = (TextView) findViewById(R.id.textView);
        tvCountDown.setTextSize(20);
        tvCountDown.setText("touch to start detection");
        setCameraDisplayOrientation(this, 0, mCamera);

        FrameLayout preview = (FrameLayout) findViewById(R.id.camera_preview);
        FrameLayout drawingSurfaceLayout = (FrameLayout) findViewById(R.id.drawing_surface);
        drawingSurface = new DrawingSurface(this);
        drawingSurface.setZOrderOnTop(true);
        SurfaceHolder sfhTrackHolder = drawingSurface.getHolder();
        sfhTrackHolder.setFormat(PixelFormat.TRANSPARENT);
        drawingSurfaceLayout.addView(drawingSurface);

        mCameraPreview = new CameraPreview(this,  mCamera,drawingSurface);
        preview.addView(mCameraPreview);

        flashButton = (Button) findViewById(R.id.button_flash);
        timerButton = (Button) findViewById(R.id.button_timer);
        imagesNumberButton = (Button) findViewById(R.id.button_number_of_images);
        soundButton = (Button) findViewById(R.id.button_sound);
        cameraChangeButton = (Button) findViewById(R.id.button_change_camera);

        //layoutDown = (LinearLayout)findViewById(R.id.linearLayoutDown);
        //switcher = (ViewSwitcher) findViewById(R.id.profileSwitcher);



        // add button listener
        timerButton.setOnClickListener(new View.OnClickListener() {
            AlertDialog alertDialog;

            @Override
            public void onClick(View arg0) {

                AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(context);
                final CharSequence[] items = {"0", "2", "5"};
                // set title
                alertDialogBuilder.setTitle("Choose countdown time")
                        .setSingleChoiceItems(items, -1, new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int item) {


                                switch (item) {
                                    case 0:
                                        timerTime = 0;
                                        timerButton.setBackgroundResource(R.drawable.timer_zero_white);
                                        break;
                                    case 1:
                                        timerTime = 2;
                                        timerButton.setBackgroundResource(R.drawable.timer_two_white);
                                        break;
                                    case 2:
                                        timerTime = 5;
                                        timerButton.setBackgroundResource(R.drawable.timer_five_white);
                                        break;


                                }
                                alertDialog.dismiss();
                            }
                        });

                // create alert dialog
                alertDialog = alertDialogBuilder.create();

                // show it
                alertDialog.show();
            }
        });



        flashButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Log.i("flash", "stisnuo");

                if (flash.equals(FLASH_MODE_ON)) {
                    flash = FLASH_MODE_OFF;
                    flashButton.setBackgroundResource(R.drawable.flash_off_white);

                } else {
                    flash = FLASH_MODE_ON;
                    flashButton.setBackgroundResource(R.drawable.flash_on_white);
                }

            }
        });

        imagesNumberButton.setOnClickListener(new View.OnClickListener() {
                AlertDialog alertDialog;

                @Override
                public void onClick(View arg0) {

                AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(context);
                final CharSequence[] items = {"1", "2", "3", "4", "5"};
                // set title
                alertDialogBuilder.setTitle("Select number of photos")
                        .setSingleChoiceItems(items, -1, new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int item) {

                                numberOfImages = item + 1;
                                imagesNumberButton.setText(Integer.toString(item + 1));
                                alertDialog.dismiss();
                            }
                        });

                // create alert dialog
                alertDialog = alertDialogBuilder.create();

                // show it
                alertDialog.show();
            }
        });

        soundButton.setOnClickListener(new View.OnClickListener() {
            AlertDialog dialog;

            @Override
            public void onClick(View arg0) {

            final CharSequence[] items = {"Detection sound","Countdown sound","Shutter sound"};
        // arraylist to keep the selected items
            final ArrayList seletedItems=new ArrayList();

            AlertDialog.Builder builder = new AlertDialog.Builder(context);
            builder.setTitle("Select sounds");
            boolean[] selected = {soundDetection,soundCountdown,soundShutter};
            builder.setMultiChoiceItems(items, selected,
                new DialogInterface.OnMultiChoiceClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int indexSelected,
                                        boolean isChecked) {
                        if (isChecked) {
                            // If the user checked the item, add it to the selected items
                            seletedItems.add(indexSelected);
                        } else if (seletedItems.contains(indexSelected)) {
                            // Else, if the item is already in the array, remove it
                            seletedItems.remove(Integer.valueOf(indexSelected));
                        }
                    }
                })
                // Set the action buttons
                .setPositiveButton("OK", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int id) {
                        if (seletedItems.contains(0)) {
                            soundDetection = true;
                        } else {
                            soundDetection = false;
                        }
                        if (seletedItems.contains(1)) {
                            soundCountdown = true;
                        } else {
                            soundCountdown = false;
                        }
                        if (seletedItems.contains(2)) {
                            soundShutter = true;
                        } else {
                            soundShutter = false;
                        }

                    }
                })
                .setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int id) {
                        //  Your code when user clicked on Cancel

                    }
                });

                dialog = builder.create();//AlertDialog dialog; create like this outside onClick
                dialog.show();
            }
        });

        cameraChangeButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mCamera.stopPreview();
                mCamera.release();
                if(cameraOrientation == Camera.CameraInfo.CAMERA_FACING_BACK){
                    cameraOrientation = Camera.CameraInfo.CAMERA_FACING_FRONT;
                }
                else {
                    cameraOrientation = Camera.CameraInfo.CAMERA_FACING_BACK;
                }
                mCamera = Camera.open(cameraOrientation);
               //setCameraDisplayOrientation(thisClass,0,mCamera);
                mCamera.startPreview();

            }
        });


        tvCountDown.setOnClickListener(new View.OnClickListener() {
                    @Override
            public void onClick(View v) {
                detectionStarted = true;
                tvCountDown.setText("");
                    }
        });


    }




    public void takePicture(android.graphics.Rect focusArea) throws InterruptedException {

        locked = true;

        Camera.Parameters cameraParameters = mCamera.getParameters();
        ArrayList<Camera.Area> focusAreas = new ArrayList<>();
        focusAreas.add(new Area(focusArea, 1000));

        cameraParameters.setFocusMode(FOCUS_MODE_AUTO);
        cameraParameters.setFocusAreas(focusAreas);
        cameraParameters.setFlashMode(flash);
        mCamera.setParameters(cameraParameters);


        tvCountDown.setTextSize(100);
        if (timerTime != 0) {
            tvCountDown.setText(Integer.toString(timerTime));
            Thread th = new Thread(new Runnable() {
                private long startTime = System.currentTimeMillis();

                public void run() {
                    while (true) {
                        isOver = false;
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                Long elapsedTime = System.currentTimeMillis() - startTime;
                                String currentText;
                                if (elapsedTime < 1000 * timerTime) {
                                    currentText = tvCountDown.getText().toString();
                                    tvCountDown.setText(Long.toString((1000 * (timerTime + 1) - elapsedTime) / 1000));
                                    if (!currentText.equals(tvCountDown.getText())){
                                        playCountdownSound();
                                    }
                                } else {
                                    isOver = true;
                                }
                            }

                        });
                        if (isOver) {
                            mCamera.autoFocus(thisClass);
                            mCamera.takePicture(null, null, mPicture.get());
                            picturesTaken ++;
                            break;
                        }



                        try {
                            Thread.sleep(1);
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }

                    }
                }
            });
            th.start();
        } else {
            mCamera.autoFocus(thisClass);
            mCamera.takePicture(null, null, mPicture.get());
            picturesTaken ++;

        }

    }

    public static void setCameraDisplayOrientation(Activity activity,
                                                   int cameraId, android.hardware.Camera camera) {

        CameraInfo info =
                new CameraInfo();

        getCameraInfo(cameraId, info);

        int rotation = activity.getWindowManager().getDefaultDisplay().getRotation();
        int degrees = 0;

        switch (rotation) {
            case Surface.ROTATION_0: degrees = 0; break;
            case Surface.ROTATION_90: degrees = 90; break;
            case Surface.ROTATION_180: degrees = 180; break;
            case Surface.ROTATION_270: degrees = 270; break;
        }

        int result;
        if (info.facing == CameraInfo.CAMERA_FACING_FRONT) {
            result = (info.orientation + degrees) % 360;
            result = (360 - result) % 360;  // compensate the mirror
        } else {  // back-facing
            result = (info.orientation - degrees + 360) % 360;
        }
        camera.setDisplayOrientation(result);

    }


    private final ThreadLocal<Camera.PictureCallback> mPicture = new ThreadLocal<Camera.PictureCallback>() {
        @Override
        protected PictureCallback initialValue() {
            return new PictureCallback() {
                @Override
                public void onPictureTaken(byte[] data, Camera camera) {


                    playShutterSound();
                    File pictureFile = getOutputMediaFile();

                    if (pictureFile == null) {
                        Log.d(TAG, "null");
                        return;

                    }

                    try {
                        FileOutputStream fos = new FileOutputStream(pictureFile);
                        fos.write(data);
                        fos.close();
                    }  catch (IOException e) {
                    }

                    lastPhoto.setImageURI(Uri.fromFile(pictureFile));
                    ContentValues values = new ContentValues();

                    values.put(MediaStore.Images.Media.DATE_TAKEN, System.currentTimeMillis());
                    values.put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg");
                    values.put(MediaStore.MediaColumns.DATA, pictureFile.getAbsolutePath());
                    context.getContentResolver().insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values);
                    locked = false;



                }
            };
        }
    };

    private static File getOutputMediaFile() {

        File mediaStorageDir = new File(
                Environment
                        .getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES),
                "MyCameraApp");
        if (!mediaStorageDir.exists()) {
            if (!mediaStorageDir.mkdirs()) {
                Log.d("MyCameraApp", "failed to create directory");
                return null;
            }
        }
        // Create a media file name
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss")
                .format(new Date());
        File mediaFile;
        mediaFile = new File(mediaStorageDir.getPath() + File.separator
                + "adrianfotka_" +timeStamp + ".jpg");

        return mediaFile;
    }

    public void playCountdownSound(){
        if (soundCountdown){
            soundPool.play(soundIdCountdown, 1, 1, 0, 0, 1);
        }
    }

    public void playShutterSound(){
        if (soundShutter){
            soundPool.play(soundIdShutter, 1, 1, 0, 0, 1);
        }
    }
/*
    @Override
    public void onConfigurationChanged(Configuration myConfig) {
        super.onConfigurationChanged(myConfig);
        int orient = getResources().getConfiguration().orientation;
        switch(orient) {
            case Configuration.ORIENTATION_LANDSCAPE:
                //setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);


                break;
            case Configuration.ORIENTATION_PORTRAIT:
                //setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
                //LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
                //params.gravity = RelativeLayout.ALIGN_PARENT_BOTTOM;
                //params.height = 80;
                //layoutDown.setLayoutParams(params);
                //layoutDown.setGravity(Gravity.FILL);
                //layoutDown.setOrientation(LinearLayout.HORIZONTAL);



                Log.i("orientation", "portrait");
                break;
            default:
                setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED);
                Log.i("orientation", "uncs");
        }
    }
    */





}