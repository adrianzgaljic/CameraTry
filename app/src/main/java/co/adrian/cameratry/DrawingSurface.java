package co.adrian.cameratry;

import android.content.Context;
import android.content.res.Configuration;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PorterDuff;
import android.hardware.Camera;
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
        int detectionSquareColor = Color.BLUE;
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
        try {
            Canvas canvas = mHolder.lockCanvas();

            if (orient == Configuration.ORIENTATION_PORTRAIT){
                ratioX = (float)canvas.getHeight()/(float)width;
                ratioY = (float)canvas.getWidth()/(float)height;
            } else  {
                ratioX = (float)canvas.getWidth()/(float)width;
                ratioY = (float)canvas.getHeight()/(float)height;
            }

            if (MainActivity.cameraOrientation == Camera.CameraInfo.CAMERA_FACING_BACK){
                drawRoundRectFront((float) (rect.tl().x) * ratioX, (float) (rect.tl().y) * ratioY, ((float) (rect.tl().x) + rect.width) * ratioX, ((float) (rect.tl().y) + rect.height) * ratioY, (float) rect.width / 8, paint, canvas);
            } else {
                drawRoundRectBack((float) canvas.getWidth() - (float) (rect.tl().x) * ratioX, (float) (rect.tl().y) * ratioY, ((float) canvas.getWidth() - (float) (rect.tl().x) - rect.width) * ratioX, ((float) (rect.tl().y) + rect.height) * ratioY, (float) rect.height / 8, paint, canvas);
            }

            mHolder.unlockCanvasAndPost(canvas);

        } catch (Exception e){

        }


    }

    public void clearCanvas() {

        Log.i(TAG, "[DrawingSurface] Trying to clear...");
        Canvas canvas = mHolder.lockCanvas();
        canvas.drawColor(Color.TRANSPARENT, PorterDuff.Mode.CLEAR);
        mHolder.unlockCanvasAndPost(canvas);

    }



    public void playSound(){
        if (MainActivity.soundDetection){
            soundPool.play(soundId, 1, 1, 0, 0, 1);
        }
    }

    private void drawRoundRectFront(float left, float top, float right, float bottom, float radius,Paint paint, Canvas canvas) {
        Path path = new Path();
        path.moveTo(left + radius, top);
        path.lineTo(right - radius, top);

        path.moveTo(right, top + radius);
        path.lineTo(right, bottom - radius);

        path.moveTo(left + radius, bottom);
        path.lineTo(right - radius, bottom);

        path.moveTo(left, top + radius);
        path.lineTo(left, bottom - radius);

        path.moveTo(left, top + radius);
        path.quadTo(left, top, left + radius, top);

        path.moveTo(right - radius, top);
        path.quadTo(right, top, right, top+radius);

        path.moveTo(left, bottom - radius);
        path.quadTo(left, bottom, left + radius, bottom);

        path.moveTo(right - radius, bottom);
        path.quadTo(right, bottom, right, bottom-radius);

        canvas.drawPath(path, paint);
    }

    private void drawRoundRectBack(float left, float top, float right, float bottom, float radius,Paint paint, Canvas canvas) {
        Path path = new Path();
        path.moveTo(left - radius, top);
        path.lineTo(right + radius, top);

        path.moveTo(right, top + radius);
        path.lineTo(right, bottom - radius);

        path.moveTo(left - radius, bottom);
        path.lineTo(right + radius, bottom);

        path.moveTo(left, top + radius);
        path.lineTo(left, bottom - radius);

        path.moveTo(left, top + radius);
        path.quadTo(left, top, left - radius, top);

        path.moveTo(right + radius, top);
        path.quadTo(right, top, right, top + radius);

        path.moveTo(left, bottom - radius);
        path.quadTo(left, bottom, left - radius, bottom);

        path.moveTo(right + radius, bottom);
        path.quadTo(right, bottom, right, bottom-radius);

        canvas.drawPath(path, paint);
    }

}
