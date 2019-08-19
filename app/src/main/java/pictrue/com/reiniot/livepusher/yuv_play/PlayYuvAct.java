package pictrue.com.reiniot.livepusher.yuv_play;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.Button;

import java.io.FileInputStream;
import java.io.InputStream;

import pictrue.com.reiniot.livepusher.R;

/**
 * 2019/8/19.
 */
public class PlayYuvAct extends AppCompatActivity {
    YuvView yuvView;
    private FileInputStream fis;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.yuv_act);
        yuvView = findViewById(R.id.yuv_view);
        Button button = findViewById(R.id.start);
        button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                start();
            }
        });
    }

    public void start() {

        new Thread(new Runnable() {
            @Override
            public void run() {

                try {
                    int w = 640;
                    int h = 360;
                    InputStream inputStream = getResources().openRawResource(R.raw.sintel_640_360);
                    //   fis = new FileInputStream(inputStream);
                    byte[] y = new byte[w * h];
                    byte[] u = new byte[w * h / 4];
                    byte[] v = new byte[w * h / 4];

                    while (true) {
                        int ry = inputStream.read(y);
                        int ru = inputStream.read(u);
                        int rv = inputStream.read(v);
                        if (ry > 0 && ru > 0 && rv > 0) {
                            yuvView.setFrameData(w, h, y, u, v);
                            Thread.sleep(40);
                        } else {
                            Log.d("Test", "完成");
                            break;
                        }
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }).start();
    }
}
