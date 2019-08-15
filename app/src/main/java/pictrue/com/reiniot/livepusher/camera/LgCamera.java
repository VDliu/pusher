package pictrue.com.reiniot.livepusher.camera;

import android.content.Context;
import android.graphics.ImageFormat;
import android.graphics.SurfaceTexture;
import android.hardware.Camera;
import android.util.Log;

import java.io.IOException;
import java.util.List;

import pictrue.com.reiniot.livepusher.util.DisplayUtil;

public class LgCamera {
    private static final String TAG = "LgCamera";

    private Camera camera;


    private SurfaceTexture surfaceTexture;

    private int width;
    private int height;

    public LgCamera(Context context) {
        this.width = DisplayUtil.getScreenWidth(context);
        this.height = DisplayUtil.getScreenHeight(context);
    }

    public void initCamera(SurfaceTexture surfaceTexture, int cameraId) {
        this.surfaceTexture = surfaceTexture;
        setCameraParm(cameraId);
    }

    private void setCameraParm(int cameraId) {
        try {
            camera = Camera.open(cameraId);
            camera.setPreviewTexture(surfaceTexture);
            Camera.Parameters parameters = camera.getParameters();

            parameters.setFlashMode("off");
            parameters.setPreviewFormat(ImageFormat.NV21);

            Camera.Size size = getFitSize(parameters.getSupportedPictureSizes());
            Log.e(TAG, "setCameraParm: picture size =" + size.width + ",h=" + size.height);
            parameters.setPictureSize(size.width, size.height);

            size = getFitSize(parameters.getSupportedPreviewSizes());
            Log.e(TAG, "setCameraParm: priview size =" + size.width + ",h=" + size.height);
            parameters.setPreviewSize(size.width, size.height);
            Log.e(TAG, "setCameraParm: w =" + width + ",h=" + height);

            camera.setParameters(parameters);
            camera.startPreview();

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void stopPreview() {
        if (camera != null) {
            camera.startPreview();
            camera.release();
            camera = null;
        }
    }

    public void changeCamera(int cameraId) {
        if (camera != null) {
            stopPreview();
        }
        setCameraParm(cameraId);
    }

    private Camera.Size getFitSize(List<Camera.Size> sizes) {
        if (width < height) {
            int t = height;
            height = width;
            width = t;
        }
        float screenRatio = width / height * 1.0f;
        float min_delta = 10000;
        int min_index = 0;
        int index = -1;
        for (Camera.Size size : sizes) {
            index += 1;
            float selectRatio = size.width / size.height * 1.0f;
            float delta = Math.abs(screenRatio - selectRatio);
            if (delta < min_delta) {
                min_delta = delta;
                min_index = index;
            }
        }
        return sizes.get(min_index);
    }

}
