package thuntm.uet.drowsywarning;

import android.annotation.TargetApi;
import android.app.Activity;
import android.content.Context;
import android.content.res.AssetFileDescriptor;
import android.content.res.AssetManager;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.PixelFormat;
import android.graphics.Point;
import android.graphics.PorterDuff;
import android.graphics.Rect;
import android.hardware.Camera;
import android.media.MediaPlayer;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Trace;
import android.renderscript.Allocation;
import android.renderscript.Element;
import android.renderscript.RenderScript;
import android.renderscript.ScriptIntrinsicYuvToRGB;
import android.renderscript.Type;
import android.util.Log;
import android.view.Display;
import android.view.Surface;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.WindowManager;

import com.tzutalin.dlib.Constants;
import com.tzutalin.dlib.FaceDet;
import com.tzutalin.dlib.VisionDetRet;
import com.tzutalin.dlibtest.R;

import java.io.File;
import java.io.IOException;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;

public class MainActivity extends Activity implements SurfaceHolder.Callback {

    private static final String TAG = "MainActivity";
    SurfaceView cameraView, transparentView, rectView;

    SurfaceHolder holder, holderTransparent, holderRect;

    private RenderScript rs;
    private ScriptIntrinsicYuvToRGB yuvToRgbIntrinsic;
    private Type.Builder yuvType, rgbaType;
    private Allocation in, out;
    Camera camera;

    private Paint mFaceLandmardkPaint, TextPaint;
    private Handler mInferenceHandler;
    private HandlerThread inferenceThread;
    private Canvas canvas = null;
    private int count = 0, k = 0, cnt = 0;

    int deviceHeight, deviceWidth;
    Bitmap imageBitmap;
    private FaceDet mFaceDet;
    private Context mContext;
    private float RectLeft, RectTop, RectRight, RectBottom;

    MediaPlayer mp;

    public static int getScreenWidth() {
        return Resources.getSystem().getDisplayMetrics().widthPixels;
    }

    public static int getScreenHeight() {
        return Resources.getSystem().getDisplayMetrics().heightPixels;
    }

    @TargetApi(Build.VERSION_CODES.JELLY_BEAN_MR1)
    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);

        rs = RenderScript.create(this);
        yuvToRgbIntrinsic = ScriptIntrinsicYuvToRGB.create(rs, Element.U8_4(rs));

        setContentView(R.layout.activity_drawon_camera);

        cameraView = (SurfaceView) findViewById(R.id.CameraView);

        holder = cameraView.getHolder();

        holder.addCallback((SurfaceHolder.Callback) this);

        //holder.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);

        cameraView.setSecure(true);
        mp = (MediaPlayer) MediaPlayer.create(this, R.raw.warning);
        AssetManager am = this.getAssets();
        try {
            AssetFileDescriptor afd = am.openFd("android.resource://"+getPackageName()+"/"+R.raw.warning);
            mp.setDataSource(afd.getFileDescriptor(), afd.getStartOffset(), afd.getLength());
        } catch (IOException e) {
            e.printStackTrace();
        }

        mp.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
            @Override
            public void onCompletion(MediaPlayer mediaPlayer) {
            }
        });

        mp.setLooping(true);
       // playWarning();
        transparentView = (SurfaceView) findViewById(R.id.TransparentView);

        holderTransparent = transparentView.getHolder();

        holderTransparent.addCallback((SurfaceHolder.Callback) this);

        holderTransparent.setFormat(PixelFormat.TRANSLUCENT);

        transparentView.setZOrderMediaOverlay(true);

        rectView = (SurfaceView) findViewById(R.id.RectView);

        holderRect = rectView.getHolder();
        holderRect.addCallback(this);
        holderRect.setFormat(PixelFormat.TRANSPARENT);

        //getting the device heigth and width

        deviceWidth = getScreenWidth();

        deviceHeight = getScreenHeight();
        mFaceDet = new FaceDet(Constants.getFaceShapeModelPath());
        mFaceLandmardkPaint = new Paint();
        mFaceLandmardkPaint.setColor(Color.GREEN);
        mFaceLandmardkPaint.setStrokeWidth(2);
        mFaceLandmardkPaint.setStyle(Paint.Style.STROKE);
        mFaceLandmardkPaint.setTextSize(20 * getResources().getDisplayMetrics().density);

        TextPaint = new Paint();
        TextPaint.setColor(Color.RED);
        TextPaint.setStrokeWidth(2);
        TextPaint.setStyle(Paint.Style.STROKE);
        TextPaint.setTextSize(20 * getResources().getDisplayMetrics().density);

        inferenceThread = new HandlerThread("InferenceThread");
        inferenceThread.start();
        mInferenceHandler = new Handler(inferenceThread.getLooper());


    }


    private void Draw() {

        Canvas canvas1 = holderRect.lockCanvas(null);

        Paint  paint = new Paint(Paint.ANTI_ALIAS_FLAG);

        paint.setStyle(Paint.Style.STROKE);

        paint.setColor(Color.GREEN);

        paint.setStrokeWidth(3);

        RectLeft = 380;

        RectTop = 10 ;

        RectRight = RectLeft + 1000;

        RectBottom = RectTop + 1000;

        Rect rec=new Rect((int) RectLeft,(int)RectTop,(int)RectRight,(int)RectBottom);

        canvas1.drawRect(rec,paint);
        canvas1.drawText("EAR:",50, 90, mFaceLandmardkPaint);
        holderRect.unlockCanvasAndPost(canvas1);

    }
    @Override

    public void surfaceCreated(final SurfaceHolder holder) {

        try {

            synchronized (holder) {
                Draw();
            }

            camera = Camera.open(Camera.CameraInfo.CAMERA_FACING_FRONT);

        } catch (Exception e) {

            Log.i("Exception", e.toString());

            return;

        }
        Camera.Parameters param = camera.getParameters();
        Display display = ((WindowManager) getSystemService(WINDOW_SERVICE)).getDefaultDisplay();

        if (display.getRotation() == Surface.ROTATION_0) {
            camera.setDisplayOrientation(90);
        }


        try {
            camera.setPreviewDisplay(holder);
            camera.setPreviewCallback(new Camera.PreviewCallback() {
                @Override
                public void onPreviewFrame(byte[] data, final Camera camera) {
                    Camera.Parameters parameters = camera.getParameters();
                    int width = parameters.getPreviewSize().width;
                    int height = parameters.getPreviewSize().height;

                    Trace.beginSection("imageAvailable");
                    int[] RGBBytes  = convertYUV420_NV21toRGB8888(data, width, height);
                    imageBitmap = Bitmap.createBitmap(RGBBytes, width, height, Bitmap.Config.ARGB_8888);
                    imageBitmap = Bitmap.createScaledBitmap(imageBitmap,width/4,height/4,false);
                    final int widthX = imageBitmap.getWidth()/4;
                    imageBitmap = Bitmap.createBitmap(imageBitmap,widthX,0, 250, 250);

                    Log.i(TAG, "Param  " + parameters.getPreviewFormat());


                                    if (!new File(Constants.getFaceShapeModelPath()).exists()) {
                                        FileUtils.copyFileFromRawToOthers(mContext, R.raw.shape_predictor_68_face_landmarks, Constants.getFaceShapeModelPath());
                                    }
                                    List<VisionDetRet> results;
                                    synchronized (MainActivity.this) {
                                        results = mFaceDet.detect(imageBitmap);
                                    }
                                    Log.i(TAG, "on cameraframe");
                                    if (results != null) {
                                        for (final VisionDetRet ret : results) {
                                            float resizeRatio = 4.0f;
                                            float w = imageBitmap.getWidth();
                                            float h = imageBitmap.getHeight();
                                            int offsetX = 30;
                                            int offsetY = 6;
                                            Rect bounds = new Rect();
                                            bounds.left = (int) ((w - ret.getLeft() + widthX- offsetX) * resizeRatio );
                                            bounds.top = (int) ((ret.getTop() - offsetY) * resizeRatio);
                                            bounds.right = (int) ((w - ret.getRight() + widthX - offsetX) * resizeRatio);
                                            bounds.bottom = (int) ((ret.getBottom() - offsetY) * resizeRatio);
                                            canvas = holderTransparent.lockCanvas(null);
                                            canvas.drawColor(Color.TRANSPARENT, PorterDuff.Mode.CLEAR);
                                            canvas.drawRect(bounds, mFaceLandmardkPaint);

                                            // Draw landmark
                                            ArrayList<Point> landmarks = ret.getFaceLandmarks();
                                            List<Point> eyePointsL = landmarks.subList(36, 42);
                                            canvas.drawLine((w-eyePointsL.get(0).x + widthX - offsetX)*resizeRatio, (eyePointsL.get(0).y - offsetY)*resizeRatio, (w-eyePointsL.get(1).x + widthX - offsetX)*resizeRatio, (eyePointsL.get(1).y - offsetY)*resizeRatio, mFaceLandmardkPaint);
                                            canvas.drawLine((w-eyePointsL.get(1).x + widthX - offsetX)*resizeRatio, (eyePointsL.get(1).y - offsetY)*resizeRatio, (w-eyePointsL.get(2).x + widthX - offsetX)*resizeRatio, (eyePointsL.get(2).y - offsetY)*resizeRatio, mFaceLandmardkPaint);
                                            canvas.drawLine((w-eyePointsL.get(2).x + widthX - offsetX)*resizeRatio, (eyePointsL.get(2).y - offsetY)*resizeRatio, (w-eyePointsL.get(3).x + widthX - offsetX)*resizeRatio, (eyePointsL.get(3).y - offsetY)*resizeRatio, mFaceLandmardkPaint);
                                            canvas.drawLine((w-eyePointsL.get(3).x + widthX - offsetX)*resizeRatio, (eyePointsL.get(3).y - offsetY)*resizeRatio, (w-eyePointsL.get(4).x + widthX - offsetX)*resizeRatio, (eyePointsL.get(4).y - offsetY)*resizeRatio, mFaceLandmardkPaint);
                                            canvas.drawLine((w-eyePointsL.get(4).x + widthX - offsetX)*resizeRatio, (eyePointsL.get(4).y - offsetY)*resizeRatio, (w-eyePointsL.get(5).x + widthX - offsetX)*resizeRatio, (eyePointsL.get(5).y - offsetY)*resizeRatio, mFaceLandmardkPaint);
                                            canvas.drawLine((w-eyePointsL.get(5).x + widthX - offsetX)*resizeRatio, (eyePointsL.get(5).y - offsetY)*resizeRatio, (w-eyePointsL.get(0).x + widthX - offsetX)*resizeRatio, (eyePointsL.get(0).y - offsetY)*resizeRatio, mFaceLandmardkPaint);

                                            List<Point> eyePointsR = landmarks.subList(42, 48);
                                            canvas.drawLine((w-eyePointsR.get(0).x + widthX - offsetX)*resizeRatio, (eyePointsR.get(0).y - offsetY)*resizeRatio, (w-eyePointsR.get(1).x + widthX - offsetX)*resizeRatio, (eyePointsR.get(1).y - offsetY)*resizeRatio, mFaceLandmardkPaint);
                                            canvas.drawLine((w-eyePointsR.get(1).x + widthX - offsetX)*resizeRatio, (eyePointsR.get(1).y - offsetY)*resizeRatio, (w-eyePointsR.get(2).x + widthX - offsetX)*resizeRatio, (eyePointsR.get(2).y - offsetY)*resizeRatio, mFaceLandmardkPaint);
                                            canvas.drawLine((w-eyePointsR.get(2).x + widthX - offsetX)*resizeRatio, (eyePointsR.get(2).y - offsetY)*resizeRatio, (w-eyePointsR.get(3).x + widthX - offsetX)*resizeRatio, (eyePointsR.get(3).y - offsetY)*resizeRatio, mFaceLandmardkPaint);
                                            canvas.drawLine((w-eyePointsR.get(3).x + widthX - offsetX)*resizeRatio, (eyePointsR.get(3).y - offsetY)*resizeRatio, (w-eyePointsR.get(4).x + widthX - offsetX)*resizeRatio, (eyePointsR.get(4).y - offsetY)*resizeRatio, mFaceLandmardkPaint);
                                            canvas.drawLine((w-eyePointsR.get(4).x + widthX - offsetX)*resizeRatio, (eyePointsR.get(4).y - offsetY)*resizeRatio, (w-eyePointsR.get(5).x + widthX - offsetX)*resizeRatio, (eyePointsR.get(5).y - offsetY)*resizeRatio, mFaceLandmardkPaint);
                                            canvas.drawLine((w-eyePointsR.get(5).x + widthX - offsetX)*resizeRatio, (eyePointsR.get(5).y - offsetY)*resizeRatio, (w-eyePointsR.get(0).x + widthX - offsetX)*resizeRatio, (eyePointsR.get(0).y - offsetY)*resizeRatio, mFaceLandmardkPaint);

                                            Log.i(TAG,"Test======");

                                            double EAR = (compute_EAR(eyePointsL) + compute_EAR(eyePointsR))/2;
                                            DecimalFormat twoDForm = new DecimalFormat("#.##");
                                            EAR = Double.valueOf(twoDForm.format(EAR));
                                            String ear = String.valueOf(EAR);

                                            Log.i(TAG,"EAR ===== ");
                                            if(EAR < 0.2)
                                            {
                                                count++;
                                                canvas.drawText(ear, 180, 90, TextPaint);
                                                if(count >=14)
                                                {
                                                    playWarning();
                                                }
                                            }
                                            else {
                                                canvas.drawText(ear, 180, 90, mFaceLandmardkPaint);
                                                if(mp.isPlaying())
                                                    mp.pause();

                                                count = 0;
                                            }
                                            holderTransparent.unlockCanvasAndPost(canvas);

                                        }
                                    }
                                    holderTransparent.setKeepScreenOn(true);

                    Trace.endSection();
                }
            });
            camera.startPreview();

        } catch (Exception e) {
            return;
        }
    }
    private void playWarning() {
        if (mp != null) {
            // mp.reset();
            //mp.release();
        }
        mp.start();
    }


    public static double compute_EAR(List<Point> eye){
        float aX = Math.abs(eye.get(1).x - eye.get(5).x);
        float bX = Math.abs(eye.get(2).x - eye.get(4).x);
        float cX = Math.abs(eye.get(0).x - eye.get(3).x);

        float aY = Math.abs(eye.get(1).y - eye.get(5).y);
        float bY = Math.abs(eye.get(2).y - eye.get(4).y);
        float cY = Math.abs(eye.get(0).y - eye.get(3).y);


        double a = Math.sqrt(aX * aX + aY * aY);
        double b = Math.sqrt(bX * bX + bY * bY);
        double c = Math.sqrt(cX * cX + cY * cY);

        double ear = (a + b) / (2 * c);
        return ear;
    }

    public static int[] convertYUV420_NV21toRGB8888(byte [] data, int width, int height) {
        int size = width*height;
        int offset = size;
        int[] pixels = new int[size];
        int u, v, y1, y2, y3, y4;

        // i percorre os Y and the final pixels
        // k percorre os pixles U e V
        for(int i=0, k=0; i < size; i+=2, k+=2) {
            y1 = data[i  ]&0xff;
            y2 = data[i+1]&0xff;
            y3 = data[width+i  ]&0xff;
            y4 = data[width+i+1]&0xff;

            u = data[offset+k  ]&0xff;
            v = data[offset+k+1]&0xff;
            u = u-128;
            v = v-128;

            pixels[i  ] = convertYUVtoRGB(y1, u, v);
            pixels[i+1] = convertYUVtoRGB(y2, u, v);
            pixels[width+i  ] = convertYUVtoRGB(y3, u, v);
            pixels[width+i+1] = convertYUVtoRGB(y4, u, v);

            if (i!=0 && (i+2)%width==0)
                i+=width;
        }

        return pixels;
    }

    private static int convertYUVtoRGB(int y, int u, int v) {
        int r,g,b;

        r = y + (int)(1.402f*v);
        g = y - (int)(0.344f*u +0.714f*v);
        b = y + (int)(1.772f*u);
        r = r>255? 255 : r<0 ? 0 : r;
        g = g>255? 255 : g<0 ? 0 : g;
        b = b>255? 255 : b<0 ? 0 : b;
        return 0xff000000 | (b<<16) | (g<<8) | r;
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
    }

    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {

        refreshCamera(); //call method for refress camera

    }

    public void refreshCamera() {

        if (holder.getSurface() == null) {
            return;
        }

        try {
            camera.stopPreview();
        } catch (Exception e) {

        }


        try {

            camera.setPreviewDisplay(holder);

            camera.startPreview();
        } catch (Exception e) {

        }

    }

    @Override

    public void surfaceDestroyed(SurfaceHolder holder) {

        camera.release(); //for release a camera

    }

}