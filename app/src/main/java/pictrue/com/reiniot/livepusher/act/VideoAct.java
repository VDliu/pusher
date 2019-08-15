package pictrue.com.reiniot.livepusher.act;

import android.media.MediaFormat;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.Button;

import pictrue.com.reiniot.livepusher.R;
import pictrue.com.reiniot.livepusher.camera.CameraView;
import pictrue.com.reiniot.livepusher.encodec.BaseMediaEncoder;
import pictrue.com.reiniot.livepusher.encodec.LgMediaEncodec;
import pictrue.com.reiniot.livepusher.util.DisplayUtil;

/**
 * 2019/8/15.
 */
public class VideoAct extends AppCompatActivity {
    private static final String TAG = "VideoAct";
    private CameraView cameraView;
    private Button record;
    private LgMediaEncodec mediaEncodec;
    private int screenWidth;
    private int screenHeight;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.video_act);
        cameraView = findViewById(R.id.camera_view);
        record = findViewById(R.id.record);
        screenWidth = DisplayUtil.getScreenWidth(this);
        screenHeight = DisplayUtil.getScreenHeight(this);
        record.setText("开始录制");

        record.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mediaEncodec == null) {
                    mediaEncodec = new LgMediaEncodec(VideoAct.this, cameraView.getTextureId());
                    String name = System.currentTimeMillis() + "" + ".mp4";
                    mediaEncodec.initEncodc(cameraView.getEglContext(), "/storage/emulated/0/DCIM/" + name, MediaFormat.MIMETYPE_VIDEO_AVC, screenWidth, screenHeight);
                    mediaEncodec.setOnMediaInfoListener(new BaseMediaEncoder.onMediaInfoListener() {
                        @Override
                        public void onMediaTime(long time) {
                            Log.e(TAG, "onMediaTime: time =" + time);
                        }
                    });
                    mediaEncodec.start();
                    record.setText("正在录制");
                } else {
                    mediaEncodec.stop();
                    record.setText("开始录制");
                    mediaEncodec = null;
                }

            }
        });
    }
}
