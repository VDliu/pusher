package pictrue.com.reiniot.livepusher.act;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.Button;

import pictrue.com.reiniot.livepusher.R;
import pictrue.com.reiniot.livepusher.imgvideo.ImageVideoActivity;
import pictrue.com.reiniot.livepusher.imgvideo.VideoActivity;
import pictrue.com.reiniot.livepusher.pic_muxer_video.Image2VideoAct;

/**
 * 2019/8/12.
 */
public class EnterAct extends AppCompatActivity {
    private boolean isPermissioned;
    private Button go;
    private Button record;
    private Button pic_muxer_video;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (PackageManager.PERMISSION_DENIED == ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)) {
                ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.RECORD_AUDIO,
                        Manifest.permission.CAMERA,
                }, 1);
            } else {
                isPermissioned = true;
            }
        }
        setContentView(R.layout.act_enter);
        go = findViewById(R.id.id_go_camera);
        record = findViewById(R.id.record);
        pic_muxer_video = findViewById(R.id.pic_muxer_video);

        pic_muxer_video.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent it = new Intent(EnterAct.this, ImageVideoActivity.class);
                startActivity(it);
            }
        });


        go.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (isPermissioned) {
                    Intent it = new Intent(EnterAct.this, MainActivity.class);
                    startActivity(it);
                }
            }
        });
        record.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent it = new Intent(EnterAct.this, VideoAct.class);
                startActivity(it);
            }
        });
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        for (int i = 0; i < grantResults.length; i++) {
            if (grantResults[i] != PackageManager.PERMISSION_GRANTED) {
                return;
            }
        }
        isPermissioned = true;
    }
}
