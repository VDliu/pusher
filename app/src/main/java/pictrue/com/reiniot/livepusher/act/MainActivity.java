package pictrue.com.reiniot.livepusher.act;

import android.content.res.Configuration;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.OrientationEventListener;

import pictrue.com.reiniot.livepusher.R;
import pictrue.com.reiniot.livepusher.camera.CameraView;

public class MainActivity extends AppCompatActivity {
    private static final String TAG = "MainActivity";

    // Used to load the 'native-lib' library on application startup.
    static {
        System.loadLibrary("native-lib");
    }

    CameraView cameraView;
    private boolean isPermissioned;
    private OrientationEventListener mOrientationListener;
    private int old_oritention;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        initView();
    }

    private void initView() {
        setContentView(R.layout.activity_main);
        cameraView = findViewById(R.id.camera_view);

        mOrientationListener = new OrientationEventListener(this,
                SensorManager.SENSOR_DELAY_NORMAL) {

            @Override
            public void onOrientationChanged(int orientation) {

                Log.e(TAG,
                        "Orientation changed to " + orientation);
            }
        };

        /**     0
         *       |
         *  270 --|---------------  90
         *      180
         *
         */
        if (mOrientationListener.canDetectOrientation()) {
            Log.e(TAG, "Can detect orientation");
            //mOrientationListener.enable();
        } else {
            Log.e(TAG, "Cannot detect orientation");
            // mOrientationListener.disable();
        }
    }

    @Override
    protected void onStart() {
        super.onStart();
        Log.e(TAG, "onStart: " );
    }

    @Override
    protected void onStop() {
        super.onStop();
        Log.e(TAG, "onStop: " );
    }


    @Override
    protected void onResume() {
        super.onResume();
        Log.e(TAG, "onResume: " );
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        Log.e(TAG, "onConfigurationChanged: " );
        cameraView.previewAngle(this);
    }

    @Override
    protected void onPause() {
        super.onPause();
        Log.e(TAG, "onPause: " );
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        cameraView.onDestory();
        // mOrientationListener.disable();
    }

    /**
     * A native method that is implemented by the 'native-lib' native library,
     * which is packaged with this application.
     */
    public native String stringFromJNI();
}
