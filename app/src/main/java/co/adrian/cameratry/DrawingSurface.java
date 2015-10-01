package co.adrian.cameratry;

import android.content.Context;
import android.content.res.Configuration;
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


/**
 * Created by Adrian
 */
public class DrawingSurface extends SurfaceView implements SurfaceHolder.Callback {

    public static final String TAG = "logIspis";

    public MainActivity mActivity;
    private SurfaceHolder mHolder;
    private Paint paint;

    private SoundPool soundPool;
    private int soundId;


    public DrawingSurface(Context context) {
        super(context);
        int detectionSquareStroke  = 10;
        int detectionSquareColor = Color.RED;
        mActivity = (MainActivity)context;
        this.mHolder = getHolder();
        this.mHolder.addCallback(this);
        this.mHolder.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);
        setWillNotDraw(false);
        
        soundPool = new SoundPool(1, AudioManager.STREAM_MUSIC, 0);
        soundId = soundPool.load(context, R.raw.face_detection_sound, 1);

        paint = new Paint();
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(detectionSquareStroke);
        paint.setColor(detectionSquareColor);
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



    public void drawFaces(Rect rect, int width, int height, int orient) {

        Log.i(TAG, "[DrawingSurface] Trying to draw...");

        float ratioX;
        float ratioY;

        Canvas canvas = mHolder.lockCanvas();

        if (orient == Configuration.ORIENTATION_PORTRAIT){
            ratioX = (float)canvas.getHeight()/(float)width;
            ratioY = (float)canvas.getWidth()/(float)height;
        } else  {
            ratioX = (float)canvas.getWidth()/(float)width;
            ratioY = (float)canvas.getHeight()/(float)height;
        }



       // canvas.drawRect(rect.x*ratioX, rect.y*ratioY, (rect.x + rect.width)*ratioX, (rect.y + rect.height)*ratioY, paint);
        canvas.drawRect((float)(rect.tl().x)*ratioX, (float)(rect.tl().y)*ratioY, ((float)(rect.tl().x)+ rect.width)*ratioX, ((float)(rect.tl().y) + rect.height)*ratioY, paint);

        mHolder.unlockCanvasAndPost(canvas);

    }

    public void clearCanvas() {

        Log.i(TAG, "[DrawingSurface] Trying to clear...");
        Canvas canvas = mHolder.lockCanvas();
        canvas.drawColor(Color.TRANSPARENT, PorterDuff.Mode.CLEAR);
        mHolder.unlockCanvasAndPost(canvas);

    }



    public void playSound(){
        if (mActivity.soundDetection){
            soundPool.play(soundId, 1, 1, 0, 0, 1);
        }
    }

}
