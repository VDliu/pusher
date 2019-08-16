package pictrue.com.reiniot.livepusher.encodec;

import android.content.Context;
import android.media.MediaCodec;
import android.media.MediaCodecInfo;
import android.media.MediaFormat;
import android.media.MediaMuxer;
import android.util.Log;
import android.view.Surface;

import java.io.IOException;
import java.lang.ref.WeakReference;
import java.nio.ByteBuffer;

import javax.microedition.khronos.egl.EGLContext;

import pictrue.com.reiniot.livepusher.egl.EGLSurfaceView;
import pictrue.com.reiniot.livepusher.egl.EglHelper;

/**
 * 2019/8/15.
 */
public abstract class BaseMediaEncoder {
    private static final String TAG = "BaseMediaEncoder";


    private EGLMediaThread eglMediaThread;
    //视频编码线程
    private VideoEncodecThread videoEncodecThread;
    //音频编码线程
    private AudioEncodecThread audioEncodecThread;

    private boolean isMediaMuxerStart = false;
    private boolean audioExit = false;
    private boolean videoExit = false;

    protected Object stop_lock = new Object();
    private boolean stop_exit = false;
    //mediacodec

    private MediaCodec videoEncodec;
    private MediaFormat videoFormat;
    private MediaCodec.BufferInfo videoBufferInfo;
    private MediaMuxer mediaMuxer;


    private MediaCodec audioEncodec;
    private MediaCodec.BufferInfo audioBufferInfo;
    private MediaFormat audioFormat;
    private long pts;
    private long audio_pts = 0;
    private int audioSampleRate;
    protected int audioChannel;
    onMediaInfoListener onMediaInfoListener;


    //egl
    private Surface surface;
    private EGLContext eglContext;

    public final static int RENDERMODE_WHEN_DIRTY = 0;
    public final static int RENDERMODE_CONTINUOUSLY = 1;

    private int mRenderMode = RENDERMODE_CONTINUOUSLY;

    private EGLSurfaceView.GLRender render;

    private int width;
    private int height;

    public BaseMediaEncoder(Context context) {

    }

    public void setRender(EGLSurfaceView.GLRender render) {
        this.render = render;
    }


    public void initEncodc(EGLContext eglContext, String savePath, int width, int height, int sampleRate, int channel) {
        this.width = width;
        this.height = height;
        this.eglContext = eglContext;
        initMediaEncodec(savePath, width, height, sampleRate, channel);
    }

    private void initMediaEncodec(String savePath, int width, int height, int sampleRate, int channel) {
        try {
            mediaMuxer = new MediaMuxer(savePath, MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4);
            initVideoEncodec(MediaFormat.MIMETYPE_VIDEO_AVC, width, height);
            initAudioEncodec(MediaFormat.MIMETYPE_AUDIO_AAC, sampleRate, channel);
        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    //创建编码器
    private void initVideoEncodec(String mimeType, int width, int height) {

        try {
            videoBufferInfo = new MediaCodec.BufferInfo();

            videoFormat = MediaFormat.createVideoFormat(mimeType, width, height);
            videoFormat.setInteger(MediaFormat.KEY_COLOR_FORMAT, MediaCodecInfo.CodecCapabilities.COLOR_FormatSurface);
            //设置码率
            videoFormat.setInteger(MediaFormat.KEY_BIT_RATE, width * height * 4);
            videoFormat.setInteger(MediaFormat.KEY_FRAME_RATE, 30);
            //关键帧之间的间隔为1s
            videoFormat.setInteger(MediaFormat.KEY_I_FRAME_INTERVAL, 1);


            videoEncodec = MediaCodec.createEncoderByType(mimeType);
            //surface没有设置为null
            videoEncodec.configure(videoFormat, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE);
            //可以通过OpenGL将图像绘制到这个Surface上，MediaCodec就可以通过这个Surface录制出H264

            //该surface不是用来直接渲染到手机界面的，在录制视频过程中，绘制的界面的surface是CameraView中的surface
            //opengl绘制在该surface对用户是不可见的，这个surface用于向mediacodec传递数据
            surface = videoEncodec.createInputSurface();
        } catch (IOException e) {
            e.printStackTrace();
            videoEncodec = null;
            videoFormat = null;
            videoBufferInfo = null;
        }
    }

    private void initAudioEncodec(String mimeType, int sampleRate, int channel) {

        audioSampleRate = sampleRate;
        audioChannel = channel;
        try {
            audioBufferInfo = new MediaCodec.BufferInfo();
            audioFormat = MediaFormat.createAudioFormat(mimeType, sampleRate, channel);
            audioFormat.setInteger(MediaFormat.KEY_BIT_RATE, 96000);
            //aac等级
            audioFormat.setInteger(MediaFormat.KEY_AAC_PROFILE, MediaCodecInfo.CodecProfileLevel.AACObjectLC);
            audioFormat.setInteger(MediaFormat.KEY_MAX_INPUT_SIZE, 4096);
            audioEncodec = MediaCodec.createEncoderByType(mimeType);

            audioEncodec.configure(audioFormat, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE);
        } catch (IOException e) {
            e.printStackTrace();
            audioBufferInfo = null;
            audioFormat = null;
            audioEncodec = null;
        }
    }

    public void setOnMediaInfoListener(BaseMediaEncoder.onMediaInfoListener onMediaInfoListener) {
        this.onMediaInfoListener = onMediaInfoListener;
    }

    public void start() {
        if (surface != null && eglContext != null) {

            audioExit = false;
            videoExit = false;
            isMediaMuxerStart = false;

            eglMediaThread = new EGLMediaThread(new WeakReference<BaseMediaEncoder>(this));
            videoEncodecThread = new VideoEncodecThread(new WeakReference<BaseMediaEncoder>(this));
            audioEncodecThread = new AudioEncodecThread(new WeakReference<BaseMediaEncoder>(this));
            eglMediaThread.isCreate = true;
            eglMediaThread.isChange = true;
            eglMediaThread.start();
            videoEncodecThread.start();
            audioEncodecThread.start();
        }
    }

    public void putPcmData(byte[] data, int size) {
        if (data != null && size > 0 && audioEncodecThread != null && !audioEncodecThread.isExit) {
            try {
                int inputBufferIndex = audioEncodec.dequeueInputBuffer(50);
                while (inputBufferIndex >= 0) {
                    ByteBuffer buffer = audioEncodec.getInputBuffers()[inputBufferIndex];
                    buffer.clear();
                    buffer.put(data);
                    long pts = getAudioPts(size, audioSampleRate, audioChannel);
                    audioEncodec.queueInputBuffer(inputBufferIndex, 0, size, pts, 0);
                }
            }catch (Exception e){
                Log.i(TAG, "putPcmData: " + e.getMessage() );
            }

        }
    }

    private long getAudioPts(int size, int sampleRate, int channel) {
        audio_pts += ((long) 1.0f * size / (sampleRate * channel * 2)) * 1000 * 1000;
        return audio_pts;
    }


    public void stop() {
        if (eglMediaThread != null && videoEncodecThread != null && audioEncodecThread != null) {
            videoEncodecThread.exit();
            audioEncodecThread.exit();
            eglMediaThread.onDestory();
            videoEncodecThread = null;
            eglMediaThread = null;
            audioEncodecThread = null;
            isMediaMuxerStart = false;
        }
    }

    public void setmRenderMode(int mRenderMode) {
        if (render == null) {
            throw new RuntimeException("must set render before");
        }
        this.mRenderMode = mRenderMode;
    }

    //egl渲染线程
    static class EGLMediaThread extends Thread {
        private WeakReference<BaseMediaEncoder> encoder;
        private EglHelper eglHelper;
        private Object lock;


        private boolean isExit = false;
        private boolean isCreate = false;
        private boolean isChange = false;
        private boolean isStart = false;

        public EGLMediaThread(WeakReference<BaseMediaEncoder> encoder) {
            this.encoder = encoder;
        }

        @Override
        public void run() {
            isExit = false;
            isStart = false;
            lock = new Object();
            eglHelper = new EglHelper();
            //初始化egl环境
            eglHelper.initEgl(encoder.get().surface, encoder.get().eglContext);
            super.run();

            while (true) {
                if (isExit) {
                    release();
                    break;
                }

                if (isStart) {
                    if (encoder.get().mRenderMode == RENDERMODE_WHEN_DIRTY) {
                        synchronized (lock) {
                            try {
                                lock.wait();
                            } catch (InterruptedException e) {
                                e.printStackTrace();
                            }
                        }
                    } else if (encoder.get().mRenderMode == RENDERMODE_CONTINUOUSLY) {
                        try {
                            Thread.sleep(1000 / 60);
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                    } else {
                        throw new RuntimeException("mRenderMode is wrong value");
                    }
                }

                onCreate();
                onChange(encoder.get().width, encoder.get().height);
                onDraw();

                isStart = true;


            }
        }

        private void onCreate() {
            if (isCreate && encoder.get().render != null) {
                isCreate = false;
                encoder.get().render.onSurfaceCreated();
            }
        }

        private void onChange(int width, int height) {
            if (isChange && encoder.get().render != null) {
                isChange = false;
                encoder.get().render.onSurfaceChanged(width, height);
            }
        }

        private void onDraw() {
            if (encoder.get().render != null && eglHelper != null) {
                encoder.get().render.onDrawFrame();
                if (!isStart) {
                    encoder.get().render.onDrawFrame();
                }
                eglHelper.swapBuffers();

            }
        }

        private void requestRender() {
            if (lock != null) {
                synchronized (lock) {
                    lock.notifyAll();
                }
            }
        }

        public void onDestory() {
            isExit = true;
            requestRender();
        }


        public void release() {
            if (eglHelper != null) {
                eglHelper.destoryEgl();
                eglHelper = null;
                lock = null;
                encoder = null;
            }
        }
    }


    //视频编码线程
    static class VideoEncodecThread extends Thread {
        private WeakReference<BaseMediaEncoder> encoder;
        private boolean isExit;
        private MediaCodec videoEncodec;
        private MediaFormat videoFormat;
        private MediaCodec.BufferInfo videoBufferInfo;
        private int videoTrackIndex;
        private MediaMuxer mediaMuxer;
        private long pts;


        public VideoEncodecThread(WeakReference<BaseMediaEncoder> encoder) {
            this.encoder = encoder;
            this.videoEncodec = encoder.get().videoEncodec;
            this.videoFormat = encoder.get().videoFormat;
            this.videoBufferInfo = encoder.get().videoBufferInfo;
            this.mediaMuxer = encoder.get().mediaMuxer;
            this.pts = encoder.get().pts;
            videoTrackIndex = -1;
        }

        @Override
        public void run() {
            super.run();
            pts = 0;
            isExit = false;
            videoTrackIndex = -1;
            videoEncodec.start();
            while (true) {

                if (isExit) {
                    videoEncodec.stop();
                    videoEncodec.release();
                    videoEncodec = null;

                    //录制本地，只有录制结束的时候才会把头信息写入
                    synchronized (encoder.get().stop_lock) {
                        Log.e(TAG, "video thread run: audio exit1 =" + encoder.get().audioExit);
                        encoder.get().videoExit = true;
                        if (encoder.get().audioExit) {
                            Log.e(TAG, "video thread run: audio exit2 =" + encoder.get().audioExit);

                            if (encoder.get().stop_exit) {
                                break;
                            }
                            encoder.get().stop_exit = true;
                            Log.e(TAG, "run: stop video");
                            mediaMuxer.stop();
                            mediaMuxer.release();
                            mediaMuxer = null;
                        }
                        Log.e(TAG, "run: 录制完成");
                        break;
                    }
                }
                try {
                    int outputBufferIndex = videoEncodec.dequeueOutputBuffer(videoBufferInfo, 0);
                    if (outputBufferIndex == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED) {
                        if (mediaMuxer != null) {
                            synchronized (encoder.get().stop_lock) {
                                videoTrackIndex = mediaMuxer.addTrack(videoEncodec.getOutputFormat());
                                if (encoder.get().audioEncodecThread.audioTrackIndex != -1 && !encoder.get().isMediaMuxerStart) {
                                    mediaMuxer.start();
                                    encoder.get().isMediaMuxerStart = true;
                                }
                            }
                        }

                    } else {
                        while (outputBufferIndex >= 0) {
                            if (encoder.get().isMediaMuxerStart) {
                                ByteBuffer outputBuffer = videoEncodec.getOutputBuffers()[outputBufferIndex];
                                outputBuffer.position(videoBufferInfo.offset);
                                outputBuffer.limit(videoBufferInfo.offset + videoBufferInfo.size);
                                //pts默认为当前的时间戳
                                if (pts == 0) {
                                    pts = videoBufferInfo.presentationTimeUs;
                                    // Log.e(TAG, "run: pts = " + pts);
                                }
                                videoBufferInfo.presentationTimeUs = videoBufferInfo.presentationTimeUs - pts;
                                // Log.e(TAG, "run: videoBufferInfo.presentationTimeUs =" + videoBufferInfo.presentationTimeUs);
                                //写入数据
                                mediaMuxer.writeSampleData(videoTrackIndex, outputBuffer, videoBufferInfo);
                                Log.e(TAG, "run: video write ok" );
                                if (encoder.get().onMediaInfoListener != null) {
                                    encoder.get().onMediaInfoListener.onMediaTime(videoBufferInfo.presentationTimeUs / (1000 * 1000));
                                }
                            }

                            //第二个参数表示是否需要渲染
                            videoEncodec.releaseOutputBuffer(outputBufferIndex, false);
                            outputBufferIndex = videoEncodec.dequeueOutputBuffer(videoBufferInfo, 0);
                        }

                    }
                }catch (Exception e){
                    Log.i(TAG, "run: video encoder" +e.getMessage() );
                }
            }


        }

        public void exit() {
            isExit = true;
        }
    }

    //音频编码线程
    static class AudioEncodecThread extends Thread {
        private WeakReference<BaseMediaEncoder> encoder;
        private boolean isExit;
        private MediaCodec audioEncodec;
        private MediaCodec.BufferInfo audioBufferInfo;
        private int audioTrackIndex;
        private MediaMuxer mediaMuxer;
        private long pts;

        public AudioEncodecThread(WeakReference<BaseMediaEncoder> encoder) {
            this.encoder = encoder;
            this.audioEncodec = encoder.get().audioEncodec;
            this.audioBufferInfo = encoder.get().audioBufferInfo;
            this.mediaMuxer = encoder.get().mediaMuxer;
            audioTrackIndex = -1;
            pts = 0;
        }

        public void exit() {
            isExit = true;
        }

        @Override
        public void run() {
            super.run();
            isExit = false;
            audioEncodec.start();
            Log.e(TAG, "run: audio mediaCode start");

            while (true) {
                //回收资源
                if (isExit) {
                    audioEncodec.stop();
                    audioEncodec.release();
                    audioEncodec = null;
                    synchronized (encoder.get().stop_lock) {
                        Log.e(TAG, "audio thread run: video exit1 =" + encoder.get().videoExit);
                        encoder.get().audioExit = true;
                        if (encoder.get().videoExit) {
                            Log.e(TAG, "audio thread run: video exit2 =" + encoder.get().videoExit);
                            Log.e(TAG, "audio thread run: stop audio");
                            if (encoder.get().stop_exit) {
                                break;
                            }
                            encoder.get().stop_exit = true;
                            mediaMuxer.stop();
                            mediaMuxer.release();
                            mediaMuxer = null;
                        }
                        Log.e(TAG, "audio thread run: break");
                        break;
                    }
                }

                try {
                    int outputBufferIndex = audioEncodec.dequeueOutputBuffer(audioBufferInfo, 0);
                    if (outputBufferIndex == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED) {
                        if (mediaMuxer != null) {
                            synchronized (encoder.get().stop_lock) {
                                audioTrackIndex = mediaMuxer.addTrack(audioEncodec.getOutputFormat());
                                Log.e(TAG, "run: audioTrackIndex = "+audioTrackIndex );
                                if (encoder.get().videoEncodecThread.videoTrackIndex != -1 && !encoder.get().isMediaMuxerStart) { //音视频的track都添加完毕后，才能开启start
                                    Log.e(TAG, "run: audio mediacodec-- start");
                                    mediaMuxer.start();
                                    Log.e(TAG, "run: audio mediacodec --start");
                                    encoder.get().isMediaMuxerStart = true;
                                    Log.e(TAG, "run: audio mediacodec-- start");
                                }
                            }
                        }
                    } else {
                        while (outputBufferIndex >= 0) {
                            if (encoder.get().isMediaMuxerStart) {
                                ByteBuffer outputBuffer = audioEncodec.getOutputBuffers()[outputBufferIndex];
                                outputBuffer.position(audioBufferInfo.offset);
                                outputBuffer.limit(audioBufferInfo.offset + audioBufferInfo.size);
                                //pts默认为当前的时间戳
                                if (pts == 0) {
                                    pts = audioBufferInfo.presentationTimeUs;
                                    Log.e(TAG, "run: pts = " + pts);
                                }
                                audioBufferInfo.presentationTimeUs = audioBufferInfo.presentationTimeUs - pts;
                                //写入数据
                                mediaMuxer.writeSampleData(audioTrackIndex, outputBuffer, audioBufferInfo);
                            }

                            audioEncodec.releaseOutputBuffer(outputBufferIndex, false);
                            outputBufferIndex = audioEncodec.dequeueOutputBuffer(audioBufferInfo, 0);
                        }
                    }
                } catch (Exception e) {
                    Log.i(TAG, "run: audio dequeue failed --reason:" + e.getMessage());
                }

            }
        }
    }

    public interface onMediaInfoListener {
        void onMediaTime(long time);
    }
}
