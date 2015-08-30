package co.adrian.cameratry;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.media.AudioManager;
import android.media.SoundPool;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.SurfaceView;

import org.opencv.core.Rect;

import java.util.Random;

/**
 * Created by Jadranka on 01.08.15..
 */
public class DrawingSurface extends SurfaceView implements SurfaceHolder.Callback {


    SurfaceHolder mHolder;
    Paint paint;
    Random random;
    float ratioX;
    float ratioY;
    SoundPool soundPool;
    int soundId;
    public  Custom_CameraActivity ac;

    public static final String TAG = "logIspis";

    public DrawingSurface(Context context) {
        super(context);
        ac = (Custom_CameraActivity)context;
        this.mHolder = getHolder();
        this.mHolder.addCallback(this);
        this.mHolder.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);
        setWillNotDraw(false);
        random = new Random();
        soundPool = new SoundPool(1, AudioManager.STREAM_MUSIC, 0);
        soundId = soundPool.load(context, R.raw.face_detection_sound, 1);
        paint = new Paint();
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(10);
        paint.setColor(Color.RED);
    }

    @Override
    public void surfaceCreated(SurfaceHolder holder) {
    }

    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {

            Canvas canvas = holder.lockCanvas();
            holder.unlockCanvasAndPost(canvas);




    }

    @Override
    public void surfaceDestroyed(SurfaceHolder holder) {
        this.getHolder().removeCallback(this);

    }



    public void drawFaces(Rect rect, int width, int height) {

        Log.i(TAG, "Trying to draw...");

        Canvas canvas = mHolder.lockCanvas();
        ratioX = (float)canvas.getWidth()/(float)width;
        ratioY = (float)canvas.getHeight()/(float)height;


        //canvas.drawRect(x1,y1,x2,y2, paint);

        // rectangle(new Point(rect.x, rect.y), new Point(rect.x + rect.width, rect.y + rect.height), paint);
        //canvas.drawRect(200, 0, 400, 100, paint);
        Log.i(TAG,"ratio x = "+ratioX);
        Log.i(TAG,"ratio y = "+ratioY);
        Log.i(TAG,"canvas height = "+canvas.getHeight()+" height= "+height);
        Log.i(TAG,"rect x = "+rect.x);
        Log.i(TAG,"rect Y = "+rect.y);
        Log.i(TAG,"width  = "+rect.width);
        Log.i(TAG, "height = " + rect.height);
        canvas.drawRect(rect.x*ratioX, rect.y*ratioY, (rect.x + rect.width)*ratioX, (rect.y + rect.height)*ratioY, paint);
        mHolder.unlockCanvasAndPost(canvas);

    }

    public void clearCanvas() {

        Log.i(TAG, "Trying to clear...");
        Canvas canvas = mHolder.lockCanvas();
        canvas.drawColor(Color.TRANSPARENT, PorterDuff.Mode.CLEAR);
        mHolder.unlockCanvasAndPost(canvas);

    }



    public void playSound(){
        if (ac.soundDetection){
            soundPool.play(soundId, 1, 1, 0, 0, 1);
        }
    }

}
