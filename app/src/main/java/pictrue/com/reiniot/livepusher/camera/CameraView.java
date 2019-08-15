package pictrue.com.reiniot.livepusher.camera;

import android.content.Context;
import android.graphics.SurfaceTexture;
import android.hardware.Camera;
import android.util.AttributeSet;
import android.util.Log;
import android.view.Surface;
import android.view.WindowManager;

import pictrue.com.reiniot.livepusher.egl.EGLSurfaceView;

public class CameraView extends EGLSurfaceView {
    private static final String TAG = "CameraView";
    private Context mContext;

    private CameraRender cameraRender;
    private LgCamera camera;

    private int cameraId = Camera.CameraInfo.CAMERA_FACING_BACK;

    public CameraView(Context context) {
        this(context, null);
    }

    public CameraView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public CameraView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        mContext = context;
        start();
    }

    public void start() {
        cameraRender = new CameraRender(mContext);
        camera = new LgCamera(mContext);
        setRender(cameraRender);
        previewAngle(mContext);
        cameraRender.setOnSurfaceCreateListener(new CameraRender.OnSurfaceCreateListener() {
            @Override
            public void onSurfaceCreate(SurfaceTexture surfaceTexture) {
                camera.initCamera(surfaceTexture, cameraId);
            }
        });
    }

    public void onDestory() {
        if (camera != null) {
            camera.stopPreview();
        }
    }

    public void previewAngle(Context context) {
        int angle = ((WindowManager) context.getSystemService(Context.WINDOW_SERVICE)).getDefaultDisplay().getRotation();
        Log.e(TAG, "previewAngle: angle=" + angle);
        /**
         *     0
         * 1__ |___3
         *     |
         *     2
         *
         *
         */
        cameraRender.resetMatrix();
        switch (angle) {
            case Surface.ROTATION_0:
                Log.d(TAG, "0");
                if (cameraId == Camera.CameraInfo.CAMERA_FACING_BACK) {
                    //旋转
                    cameraRender.setAngle(90, 0, 0, 1);
                    // 翻转
                    cameraRender.setAngle(180, 1, 0, 0);
                } else {
                    cameraRender.setAngle(90f, 0f, 0f, 1f);
                }

                break;
            case Surface.ROTATION_90:
                Log.d(TAG, "90");
                if (cameraId == Camera.CameraInfo.CAMERA_FACING_BACK) {
                    cameraRender.setAngle(180, 0, 0, 1);
                    cameraRender.setAngle(180, 0, 1, 0);
                } else {
                    cameraRender.setAngle(90f, 0f, 0f, 1f);
                }
                break;
            case Surface.ROTATION_180:
                Log.d(TAG, "180");
                if (cameraId == Camera.CameraInfo.CAMERA_FACING_BACK) {
                    cameraRender.setAngle(90f, 0.0f, 0f, 1f);
                    cameraRender.setAngle(180f, 0.0f, 1f, 0f);
                } else {
                    cameraRender.setAngle(-90, 0f, 0f, 1f);
                }
                break;
            case Surface.ROTATION_270:
                Log.d(TAG, "270");
                if (cameraId == Camera.CameraInfo.CAMERA_FACING_BACK) {
                    cameraRender.setAngle(180f, 0.0f, 1f, 0f);
                } else {
                    cameraRender.setAngle(0f, 0f, 0f, 1f);
                }
                break;
        }
    }

    public int getTextureId() {
        if (cameraRender != null) {
            return cameraRender.getFboTextureid();
        }
        return -1;
    }
}
