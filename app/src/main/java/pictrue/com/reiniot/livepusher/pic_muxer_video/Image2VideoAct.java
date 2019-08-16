package pictrue.com.reiniot.livepusher.pic_muxer_video;

import android.os.Bundle;
import android.os.Environment;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.Button;

import com.ywl5320.libmusic.WlMusic;
import com.ywl5320.listener.OnErrorListener;
import com.ywl5320.listener.OnPreparedListener;
import com.ywl5320.listener.OnShowPcmDataListener;

import pictrue.com.reiniot.livepusher.R;
import pictrue.com.reiniot.livepusher.encodec.LgMediaEncodec;
import pictrue.com.reiniot.livepusher.encodec.WlMediaEncodec;
import pictrue.com.reiniot.livepusher.util.DisplayUtil;

/**
 * 2019/8/16.
 * 图片合成视频
 */
public class Image2VideoAct extends AppCompatActivity {
    private static final String TAG = "Image2VideoAct";
    Imge2VideoView surfaceView;
    LgMediaEncodec mediaEncodec;
    private WlMusic music;
    Button start;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.imge_video);
        surfaceView = findViewById(R.id.surface_view);
        start = findViewById(R.id.start);
        surfaceView.setCurrentImg(R.drawable.img_1);
        start.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (music != null) {
                    music.setSource(Environment.getExternalStorageDirectory().getAbsolutePath() + "/girl.m4a");
                    music.prePared();

                }
            }
        });

        music = WlMusic.getInstance();
        music.setOnErrorListener(new OnErrorListener() {
            @Override
            public void onError(int code, String msg) {
                Log.e(TAG, "onError: " + msg);
            }
        });

        music.setCallBackPcmData(true);

        music.setOnPreparedListener(new OnPreparedListener() {
            @Override
            public void onPrepared() {
                Log.e(TAG, "onPrepared: --");
                music.cutAudio(0, 60);
            }
        });

        music.setOnShowPcmDataListener(new OnShowPcmDataListener() {
            @Override
            public void onPcmInfo(int samplerate, int bit, int channels) {
                Log.e(TAG, "onPcmInfo: ------"+Thread.currentThread().getName());
                mediaEncodec = new LgMediaEncodec(Image2VideoAct.this, surfaceView.getFbotextureid());
                String name = System.currentTimeMillis() + "" + ".mp4";
                mediaEncodec.initEncodc(surfaceView.getEglContext(), Environment.getExternalStorageDirectory().getAbsolutePath() + "/DCIM/" + name,
                        DisplayUtil.getScreenWidth(Image2VideoAct.this), 500, samplerate, channels);
                startImgs();
                mediaEncodec.start();
            }

            @Override
            public void onPcmData(byte[] pcmdata, int size, long clock) {
                if (mediaEncodec != null ) {
                    mediaEncodec.putPcmData(pcmdata, size);
                }

            }
        });
    }


    private void startImgs() {
        new Thread(new Runnable() {
            @Override
            public void run() {
                for (int i = 1; i <= 257; i++) {
                    int imgsrc = getResources().getIdentifier("img_" + i, "drawable", "pictrue.com.reiniot.livepusher");
                    surfaceView.setCurrentImg(imgsrc);
                    try {
                        Thread.sleep(80);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }

                if (mediaEncodec != null) {
                    music.stop();
                    mediaEncodec.stop();
                    mediaEncodec = null;
                }
            }
        }).start();
    }
}
