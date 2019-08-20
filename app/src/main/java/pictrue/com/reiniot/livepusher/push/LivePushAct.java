package pictrue.com.reiniot.livepusher.push;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.Button;

import pictrue.com.reiniot.livepusher.R;
import pictrue.com.reiniot.livepusher.util.DisplayUtil;

/**
 * 2019/8/20.
 */
public class LivePushAct extends AppCompatActivity {
    private pictrue.com.reiniot.livepusher.camera.CameraView live_view;
    private Button start_push;
    private boolean isStart = false;
    PushVideo pushVideo = new PushVideo();
    private PushEncodec pushEncodec;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.live_push);
        live_view = findViewById(R.id.live_view);
        start_push = findViewById(R.id.start_push);
        pushVideo.setConnectListenr(new ConnectListenr() {
            @Override
            public void onConnecting() {

            }

            @Override
            public void onConnectSuccess() {
                pushEncodec = new PushEncodec(LivePushAct.this, live_view.getTextureId());
                pushEncodec.initEncodc(live_view.getEglContext()
                        , DisplayUtil.getScreenWidth(LivePushAct.this)
                        , DisplayUtil.getScreenHeight(LivePushAct.this)
                        , 44100,
                        2);
                pushEncodec.start();
                pushEncodec.setOnMediaInfoListener(new BasePushEncoder.onMediaInfoListener() {
                    @Override
                    public void onMediaTime(long time) {

                    }

                    @Override
                    public void onSpsPpsInfo(byte[] sps, byte[] pps) {
                        pushVideo.pushSpsPPsData(sps, pps);
                    }

                    @Override
                    public void onVidoInfo(byte[] data, boolean isKeyFrame) {
                        pushVideo.pushVideoData(data, data.length, isKeyFrame);
                    }
                });
            }

            @Override
            public void onConnectFail(String msg) {

            }
        });
        start_push.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                isStart = !isStart;
                if (isStart) {
                    pushVideo.init("");

                } else {
                    if (pushEncodec != null) {
                        pushEncodec.stop();
                    }
                }
            }
        });


    }
}
