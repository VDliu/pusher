package pictrue.com.reiniot.livepusher.act;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.Button;

import pictrue.com.reiniot.livepusher.R;
import pictrue.com.reiniot.livepusher.audio_record.AudioRecordUtil;
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
    private AudioRecordUtil audioRecordUtil;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.video_act);
        cameraView = findViewById(R.id.camera_view);
        record = findViewById(R.id.record);
        screenWidth = DisplayUtil.getScreenWidth(this);
        screenHeight = DisplayUtil.getScreenHeight(this);
        record.setText("开始录制");
        audioRecordUtil = new AudioRecordUtil();
        audioRecordUtil.setListener(new AudioRecordUtil.OnRecordListener() {
            @Override
            public void onRecorder(byte[] data, int size) {
                if (mediaEncodec != null) {
                    Log.e(TAG, "onRecorder: size =" + size );
                    mediaEncodec.putPcmData(data, size);
                }
            }
        });

        record.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mediaEncodec == null) {
                    mediaEncodec = new LgMediaEncodec(VideoAct.this, cameraView.getTextureId());
                    String name = System.currentTimeMillis() + "" + ".mp4";
                    mediaEncodec.initEncodc(cameraView.getEglContext(), "/storage/emulated/0/avideo/" + name, screenWidth, screenHeight, 44100, 2);
                    mediaEncodec.setOnMediaInfoListener(new BaseMediaEncoder.onMediaInfoListener() {
                        @Override
                        public void onMediaTime(long time) {
                            Log.e(TAG, "onMediaTime: time =" + time);
                        }
                    });
                    mediaEncodec.start();
                    audioRecordUtil.start();

                    record.setText("正在录制");
                } else {
                    mediaEncodec.stop();
                    record.setText("开始录制");
                    mediaEncodec = null;
                    audioRecordUtil.stopRecord();
                }

            }
        });
    }
}
