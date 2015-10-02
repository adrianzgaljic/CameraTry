package co.adrian.cameratry;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.ContentValues;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Path;
import android.graphics.PixelFormat;
import android.graphics.Rect;
import android.graphics.drawable.AnimationDrawable;
import android.hardware.Camera;
import android.media.AudioManager;
import android.media.SoundPool;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.util.Log;
import android.view.Display;
import android.view.Surface;
import android.view.SurfaceHolder;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import org.opencv.objdetect.CascadeClassifier;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import static android.hardware.Camera.Area;
import static android.hardware.Camera.AutoFocusCallback;
import static android.hardware.Camera.CameraInfo;
import static android.hardware.Camera.Parameters.FLASH_MODE_OFF;
import static android.hardware.Camera.Parameters.FLASH_MODE_ON;
import static android.hardware.Camera.Parameters.FOCUS_MODE_AUTO;
import static android.hardware.Camera.PictureCallback;
import static android.hardware.Camera.getCameraInfo;
import static android.hardware.Camera.open;


public class MainActivity extends Activity implements  AutoFocusCallback{
    private Camera mCamera;


    @Override
    public void onAutoFocus(boolean success, Camera camera) {

    }

    public static final String TAG = "logIspis";

    private TextView tvCountDown;
    protected CascadeClassifier faceDetector = null;
    private static String flash = FLASH_MODE_OFF;
    private final Context context = this;
    private static int timerTime = 0;
    protected static int numberOfImages;
    protected int picturesTaken = 0;
    protected boolean locked = false;
    protected boolean isOver =true;
    protected static boolean detectionStarted;
    protected static boolean soundDetection = false;
    private static boolean soundCountdown = false;
    private static boolean soundShutter = false;
    private SoundPool soundPool;
    private int soundIdCountdown;
    private int soundIdShutter;
    private int cameraOrientation = CameraInfo.CAMERA_FACING_BACK;
    private Button flashButton;
    private Button timerButton;
    private Button imagesNumberButton;
    protected Button btnStartDetection;
    private ImageView ivPhoto;
    private SharedPreferences.Editor editor;
    private SharedPreferences prefs;

    public final int RATIO = 8;

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
            Log.i(TAG,"[MainActivity] classifier successfully loaded: "+faceDetector);
        } catch (Exception e) {
            Log.e(TAG, "[MainActivity] Error loading classifier: ", e);
        }



        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);

        soundPool = new SoundPool(1, AudioManager.STREAM_MUSIC, 0);
        soundIdCountdown = soundPool.load(context, R.raw.countdown_sound, 1);
        soundIdShutter = soundPool.load(context, R.raw.shutter_sound, 1);

        mCamera = open(cameraOrientation);
        setCameraDisplayOrientation(this, 0, mCamera);

        tvCountDown = (TextView) findViewById(R.id.textView);
        tvCountDown.setText("");



        final FrameLayout drawingSurfaceLayout = (FrameLayout) findViewById(R.id.drawing_surface);
        drawingSurface = new DrawingSurface(this);
        drawingSurface.setZOrderOnTop(true);
        SurfaceHolder sfhTrackHolder = drawingSurface.getHolder();
        sfhTrackHolder.setFormat(PixelFormat.TRANSPARENT);
        drawingSurfaceLayout.removeAllViews();
        drawingSurfaceLayout.addView(drawingSurface);

        CameraPreview mCameraPreview = new CameraPreview(this, mCamera, drawingSurface);
        FrameLayout preview = (FrameLayout) findViewById(R.id.camera_preview);
        preview.addView(mCameraPreview);

        flashButton = (Button) findViewById(R.id.button_flash);
        timerButton = (Button) findViewById(R.id.button_timer);
        imagesNumberButton = (Button) findViewById(R.id.button_number_of_images);
        Button soundButton = (Button) findViewById(R.id.button_sound);
        Button cameraChangeButton = (Button) findViewById(R.id.button_change_camera);
        btnStartDetection = (Button) findViewById(R.id.button);


        if (numberOfImages==0){
            numberOfImages=1;
        }
        imagesNumberButton.setText(Integer.toString(numberOfImages));

        switch(timerTime){
            case 0:
                timerButton.setBackgroundResource(R.drawable.timer_zero_new);
                break;
            case 2:
                timerButton.setBackgroundResource(R.drawable.timer_two_new);
                break;
            case 5:
                timerButton.setBackgroundResource(R.drawable.timer_five_new);
                break;
        }

        if (flash.equals(FLASH_MODE_OFF)){
            flashButton.setBackgroundResource(R.drawable.flash_off_new);
        } else {
            flashButton.setBackgroundResource(R.drawable.flash_on_new);
        }



        ImageView imageView = new ImageView(this);
        LinearLayout.LayoutParams vp =
                new LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT);
        imageView.setLayoutParams(vp);
        imageView.setImageResource(R.drawable.number_of_images_new);

        prefs = getSharedPreferences("selphy", 0);
        editor = prefs.edit();
        ivPhoto = (ImageView) findViewById(R.id.lastPhoto);
        Bitmap image = BitmapFactory.decodeFile(prefs.getString("lastPhoto",null));
        ivPhoto.setImageBitmap(getRoundedShape(image));
        int orient = getResources().getConfiguration().orientation;

        View viewInstance;
        LinearLayout myll;
        ViewGroup.LayoutParams params;

        Display display = getWindowManager().getDefaultDisplay();

        switch(orient) {

            case Configuration.ORIENTATION_LANDSCAPE:

                viewInstance = findViewById(R.id.linearLayoutDown);
                viewInstance.setBackgroundResource(R.drawable.down_vertical);
                params=viewInstance.getLayoutParams();
                params.width = display.getWidth()/RATIO;
                params.height= ViewGroup.LayoutParams.MATCH_PARENT;
                myll = (LinearLayout)viewInstance;
                myll.setOrientation(LinearLayout.VERTICAL);
                viewInstance.setLayoutParams(params);


                viewInstance = findViewById(R.id.linearLayoutUp);
                viewInstance.setBackgroundResource(R.drawable.up_vertical);
                params=viewInstance.getLayoutParams();
                params.width = display.getWidth()/RATIO;
                params.height= ViewGroup.LayoutParams.MATCH_PARENT;
                myll = (LinearLayout)viewInstance;
                myll.setOrientation(LinearLayout.VERTICAL);
                viewInstance.setLayoutParams(params);

                params = preview.getLayoutParams();
                params.width = display.getWidth()*(RATIO-2)/RATIO;
                params.height = display.getHeight();
                preview.setLayoutParams(params);
                break;

            case Configuration.ORIENTATION_PORTRAIT:
                viewInstance = findViewById(R.id.linearLayoutDown);
                viewInstance.setBackgroundResource(R.drawable.down);
                params=viewInstance.getLayoutParams();
                params.width= ViewGroup.LayoutParams.MATCH_PARENT;
                params.height= display.getHeight()/RATIO;
                myll = (LinearLayout)viewInstance;
                myll.setOrientation(LinearLayout.HORIZONTAL);
                viewInstance.setLayoutParams(params);

                viewInstance = findViewById(R.id.linearLayoutUp);
                viewInstance.setBackgroundResource(R.drawable.up);
                params=viewInstance.getLayoutParams();
                params.width= ViewGroup.LayoutParams.MATCH_PARENT;
                params.height= display.getHeight()/RATIO;
                myll = (LinearLayout)viewInstance;
                myll.setOrientation(LinearLayout.HORIZONTAL);
                viewInstance.setLayoutParams(params);

                params = preview.getLayoutParams();
                params.width = display.getWidth();
                params.height = display.getHeight()*(RATIO-2)/RATIO;
                preview.setLayoutParams(params);
                break;

            default:
                setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED);

        }


        btnStartDetection.setOnClickListener(new View.OnClickListener() {


            @Override
            public void onClick(View arg0) {
                btnStartDetection.setBackgroundResource(R.drawable.animation);
                AnimationDrawable b1Amin = (AnimationDrawable) btnStartDetection.getBackground();
                b1Amin.start();
                detectionStarted = true;

            }
        });





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
                                        timerButton.setBackgroundResource(R.drawable.timer_zero_new);
                                        break;
                                    case 1:
                                        timerTime = 2;
                                        timerButton.setBackgroundResource(R.drawable.timer_two_new);
                                        break;
                                    case 2:
                                        timerTime = 5;
                                        timerButton.setBackgroundResource(R.drawable.timer_five_new);
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

                if (flash.equals(FLASH_MODE_ON)) {
                    flash = FLASH_MODE_OFF;
                    flashButton.setBackgroundResource(R.drawable.flash_off_new);

                } else {
                    flash = FLASH_MODE_ON;
                    flashButton.setBackgroundResource(R.drawable.flash_on_new);
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

                final CharSequence[] items = {"Detection sound", "Countdown sound", "Shutter sound"};
                // arraylist to keep the selected items
                final ArrayList seletedItems = new ArrayList();

                AlertDialog.Builder builder = new AlertDialog.Builder(context);
                builder.setTitle("Select sounds");
                boolean[] selected = {soundDetection, soundCountdown, soundShutter};
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
                                soundDetection = seletedItems.contains(0);
                                soundCountdown = seletedItems.contains(1);
                                soundShutter = seletedItems.contains(2);

                            }
                        })
                        .setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int id) {

                            }
                        });

                dialog = builder.create();
                dialog.show();
            }
        });

        cameraChangeButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                if (cameraOrientation == Camera.CameraInfo.CAMERA_FACING_BACK) {
                    cameraOrientation = Camera.CameraInfo.CAMERA_FACING_FRONT;
                } else {
                    cameraOrientation = Camera.CameraInfo.CAMERA_FACING_BACK;
                }
                Bundle state = new Bundle();
                onCreate(state);


            }
        });


        tvCountDown.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                detectionStarted = true;
                tvCountDown.setText("");
            }
        });

        ivPhoto.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                String lastPhoto = prefs.getString("lastPhoto","");
                if (!lastPhoto.equals("")){
                    Intent intent=new Intent();
                    intent.setAction(Intent.ACTION_VIEW);
                    intent.setDataAndType(Uri.fromFile(new File(lastPhoto)), "image/*");

                    PackageManager pm = getPackageManager();
                    List<ResolveInfo> resInfo = pm.queryIntentActivities(intent, 0);
                    for (ResolveInfo info:resInfo){
                        Log.i(TAG,info.toString());
                    }
                    for (int i = 0; i < resInfo.size(); i++) {
                        ResolveInfo ri = resInfo.get(i);
                        String packageName = ri.activityInfo.packageName;
                        intent.setPackage(packageName);
                    }
                    startActivity(intent);
                } else {
                    showToast("No images yet");
                }

            }
        });




    }

    @Override
    public void onResume() {
        Log.e(TAG, "[MainActivity] onResume");
        super.onResume();  // Always call the superclass method first
    }

    @Override public void onPause() {
        Log.e(TAG, "[MainActivity] onPause");
        super.onPause();
    }








    public void takePicture(android.graphics.Rect focusArea) throws InterruptedException {

        locked = true;

        Camera.Parameters cameraParameters = mCamera.getParameters();
        ArrayList<Camera.Area> focusAreas = new ArrayList<Camera.Area>();
        focusAreas.add(new Area(focusArea, 1000));
        try{

            if (cameraParameters.getMaxNumFocusAreas() > 0){
                cameraParameters.setFocusMode(FOCUS_MODE_AUTO);
                cameraParameters.setFocusAreas(focusAreas);
            }

            if (cameraParameters.getFlashMode() != null){
                cameraParameters.setFlashMode(flash);
            }

            mCamera.setParameters(cameraParameters);

        }
        catch (Exception e){
            Log.i(TAG,"[MainActivity] Unable to set camera parameters: "+e);
        }


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

    public void setCameraDisplayOrientation(Activity activity,
                                            int cameraId, android.hardware.Camera camera) {

        CameraInfo info = new CameraInfo();

        getCameraInfo(cameraId, info);

        int rotation = activity.getWindowManager().getDefaultDisplay().getRotation();
        int degrees = 0;

        switch (rotation) {
            case Surface.ROTATION_0:
                degrees = 0;
                break;
            case Surface.ROTATION_90:
                degrees = 90;
                break;
            case Surface.ROTATION_180:
                degrees = 180;
                break;
            case Surface.ROTATION_270:
                degrees = 270;
                break;
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
                        Log.i(TAG,"[MainActivity] Error writing data");
                    }

                    BitmapFactory.Options options = new BitmapFactory.Options();
                    options.inDither = false;
                    options.inPreferredConfig = Bitmap.Config.ARGB_8888;

                    Bitmap image = BitmapFactory.decodeByteArray(data, 0, data.length, options);
                    ivPhoto.setImageBitmap(getRoundedShape(image));
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


    private File getOutputMediaFile() {

        File mediaStorageDir = new File(
                Environment
                        .getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES),
                "Selphy");
        if (!mediaStorageDir.exists()) {
            if (!mediaStorageDir.mkdirs()) {
                Log.i(TAG, "[MainActivity] Failed to create directory.");
                return null;
            }
        }
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss")
                .format(new Date());

        File mediaFile = new File(mediaStorageDir.getPath() + File.separator
                + "selphy_" + timeStamp + ".jpg");

        editor.putString("lastPhoto", mediaFile.getAbsolutePath());
        editor.apply();
        Log.i(TAG, "media: " + mediaFile.getAbsolutePath());

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

    @Override
    public void onConfigurationChanged(Configuration myConfig) {

        setCameraDisplayOrientation(this, 0, mCamera);
        super.onConfigurationChanged(myConfig);
    }

    public Bitmap getRoundedShape(Bitmap scaleBitmapImage) {
        if (scaleBitmapImage!=null){
            int targetWidth = 50;
            int targetHeight = 50;
            Bitmap targetBitmap = Bitmap.createBitmap(targetWidth,
                    targetHeight,Bitmap.Config.ARGB_8888);

            Canvas canvas = new Canvas(targetBitmap);
            Path path = new Path();
            path.addCircle(((float) targetWidth - 1) / 2,
                    ((float) targetHeight - 1) / 2,
                    (Math.min(((float) targetWidth),
                            ((float) targetHeight)) / 2),
                    Path.Direction.CCW);

            canvas.clipPath(path);
            canvas.drawBitmap(scaleBitmapImage,
                    new Rect(0, 0, scaleBitmapImage.getWidth(),
                            scaleBitmapImage.getHeight()),
                    new Rect(0, 0, targetWidth, targetHeight), null);
            return targetBitmap;
        } else {
            return null;
        }

    }

    public void showToast(String text){
        Context context = getApplicationContext();
        int duration = Toast.LENGTH_SHORT;

        Toast toast = Toast.makeText(context, text, duration);
        toast.show();
    }


}