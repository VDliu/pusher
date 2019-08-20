package pictrue.com.reiniot.livepusher.push;

import android.content.Context;
import android.media.MediaCodec;
import android.media.MediaCodecInfo;
import android.media.MediaFormat;
import android.util.Log;
import android.view.Surface;

import java.io.IOException;
import java.lang.ref.WeakReference;
import java.nio.ByteBuffer;

import javax.microedition.khronos.egl.EGLContext;

import pictrue.com.reiniot.livepusher.audio_record.AudioRecordUtil;
import pictrue.com.reiniot.livepusher.egl.EGLSurfaceView;
import pictrue.com.reiniot.livepusher.egl.EglHelper;

/**
 * 2019/8/15.
 * mediacodec 默认使用baseline 没有b帧
 */
public abstract class BasePushEncoder {
    private static final String TAG = "BaseMediaEncoder";

    private EGLMediaThread eglMediaThread;
    //视频编码线程
    private VideoEncodecThread videoEncodecThread;
    //音频编码线程
    private AudioEncodecThread audioEncodecThread;

    private boolean audioExit = false;
    private boolean videoExit = false;

    protected Object stop_lock = new Object();
    private boolean stop_exit = false;
    //mediacodec

    private MediaCodec videoEncodec;
    private MediaFormat videoFormat;
    private MediaCodec.BufferInfo videoBufferInfo;


    private MediaCodec audioEncodec;
    private MediaCodec.BufferInfo audioBufferInfo;
    private MediaFormat audioFormat;
    private long pts;
    private long audio_pts = 0;
    private int audioSampleRate;
    protected int audioChannel;
    onMediaInfoListener onMediaInfoListener;

    AudioRecordUtil audioRecordUtil;


    //egl
    private Surface surface;
    private EGLContext eglContext;

    public final static int RENDERMODE_WHEN_DIRTY = 0;
    public final static int RENDERMODE_CONTINUOUSLY = 1;

    private int mRenderMode = RENDERMODE_CONTINUOUSLY;

    private EGLSurfaceView.GLRender render;

    private int width;
    private int height;

    public BasePushEncoder(Context context) {

    }

    public void setRender(EGLSurfaceView.GLRender render) {
        this.render = render;
    }


    public void initEncodc(EGLContext eglContext, int width, int height, int sampleRate, int channel) {
        this.width = width;
        this.height = height;
        this.eglContext = eglContext;
        initMediaEncodec(width, height, sampleRate, channel);
    }

    private void initMediaEncodec(int width, int height, int sampleRate, int channel) {
        initVideoEncodec(MediaFormat.MIMETYPE_VIDEO_AVC, width, height);
        initAudioEncodec(MediaFormat.MIMETYPE_AUDIO_AAC, sampleRate, channel);
        initPcmRecorder();

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
            audioFormat.setInteger(MediaFormat.KEY_MAX_INPUT_SIZE, 8192);
            audioEncodec = MediaCodec.createEncoderByType(mimeType);

            audioEncodec.configure(audioFormat, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE);
        } catch (IOException e) {
            e.printStackTrace();
            audioBufferInfo = null;
            audioFormat = null;
            audioEncodec = null;
        }
    }

    private void initPcmRecorder() {
        audioRecordUtil = new AudioRecordUtil();
        audioRecordUtil.setListener(new AudioRecordUtil.OnRecordListener() {
            @Override
            public void onRecorder(byte[] data, int size) {
                if (audioRecordUtil.isStart()) {
                    putPcmData(data, size);
                }
            }
        });
    }

    public void setOnMediaInfoListener(BasePushEncoder.onMediaInfoListener onMediaInfoListener) {
        this.onMediaInfoListener = onMediaInfoListener;
    }

    public void start() {
        if (surface != null && eglContext != null) {

            audioExit = false;
            videoExit = false;

            eglMediaThread = new EGLMediaThread(new WeakReference<BasePushEncoder>(this));
            videoEncodecThread = new VideoEncodecThread(new WeakReference<BasePushEncoder>(this));
            audioEncodecThread = new AudioEncodecThread(new WeakReference<BasePushEncoder>(this));
            eglMediaThread.isCreate = true;
            eglMediaThread.isChange = true;
            eglMediaThread.start();
            videoEncodecThread.start();
            audioEncodecThread.start();
            audioRecordUtil.start();
        }
    }

    public void putPcmData(byte[] data, int size) {
        if (data != null && size > 0 && audioEncodecThread != null && !audioEncodecThread.isExit) {
            try {
                int inputBufferIndex = audioEncodec.dequeueInputBuffer(-1);
                while (inputBufferIndex >= 0) {
                    ByteBuffer buffer = audioEncodec.getInputBuffers()[inputBufferIndex];
                    buffer.clear();
                    buffer.put(data);
                    long pts = getAudioPts(size, audioSampleRate, audioChannel);
                    audioEncodec.queueInputBuffer(inputBufferIndex, 0, size, pts, 0);
                }
            } catch (Exception e) {
                Log.i(TAG, "putPcmData: " + e.getMessage());
            }

        } else {
            Log.e(TAG, "putPcmData: nodata");
        }
    }

    private long getAudioPts(int size, int sampleRate, int channel) {
        audio_pts += ((1.0f * size) / (sampleRate * channel * 2 * 1.0f)) * 1000 * 1000 * 1L;
        Log.e(TAG, "getAudioPts: pts =" + audio_pts);
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
            audioRecordUtil.stopRecord();
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
        private WeakReference<BasePushEncoder> encoder;
        private EglHelper eglHelper;
        private Object lock;


        private boolean isExit = false;
        private boolean isCreate = false;
        private boolean isChange = false;
        private boolean isStart = false;

        public EGLMediaThread(WeakReference<BasePushEncoder> encoder) {
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
        private WeakReference<BasePushEncoder> encoder;
        private boolean isExit;
        private MediaCodec videoEncodec;
        private MediaFormat videoFormat;
        private MediaCodec.BufferInfo videoBufferInfo;
        private long pts;
        byte[] sps;
        byte[] pps;
        private boolean isKeyFrame;

        public VideoEncodecThread(WeakReference<BasePushEncoder> encoder) {
            this.encoder = encoder;
            this.videoEncodec = encoder.get().videoEncodec;
            this.videoFormat = encoder.get().videoFormat;
            this.videoBufferInfo = encoder.get().videoBufferInfo;
            this.pts = encoder.get().pts;
        }

        @Override
        public void run() {
            super.run();
            pts = 0;
            isExit = false;
            videoEncodec.start();
            while (true) {

                if (isExit) {
                    videoEncodec.stop();
                    videoEncodec.release();
                    videoEncodec = null;

                    //录制本地，只有录制结束的时候才会把头信息写入
                    synchronized (encoder.get().stop_lock) {
                        encoder.get().videoExit = true;
                        if (encoder.get().audioExit) {
                            if (encoder.get().stop_exit) {
                                break;
                            }
                            encoder.get().stop_exit = true;
                        }
                        break;
                    }
                }
                try {
                    int outputBufferIndex = videoEncodec.dequeueOutputBuffer(videoBufferInfo, 0);
                    isKeyFrame = false;
                    if (outputBufferIndex == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED) {
                        ByteBuffer spsBuffer = videoEncodec.getOutputFormat().getByteBuffer("csd-0");
                        sps = new byte[spsBuffer.remaining()];
                        spsBuffer.get(sps, 0, sps.length);//获取sps

                        ByteBuffer ppsBuffer = videoEncodec.getOutputFormat().getByteBuffer("csd-1");
                        pps = new byte[ppsBuffer.remaining()];
                        ppsBuffer.get(pps, 0, pps.length);//获取pps

                    } else {
                        while (outputBufferIndex >= 0) {
                            ByteBuffer outputBuffer = videoEncodec.getOutputBuffers()[outputBufferIndex];
                            outputBuffer.position(videoBufferInfo.offset);
                            outputBuffer.limit(videoBufferInfo.offset + videoBufferInfo.size);
                            //pts默认为当前的时间戳
                            if (pts == 0) {
                                pts = videoBufferInfo.presentationTimeUs;
                            }
                            videoBufferInfo.presentationTimeUs = videoBufferInfo.presentationTimeUs - pts;
                            //写入数据
                            //在发每一关键帧之前，先发送sps pps
                            //发送的每一帧（sps pps I帧 P帧）数据添加头部信息 （header && data）
                            //获取数据,视频数据
                            if (videoBufferInfo.flags == MediaCodec.BUFFER_FLAG_KEY_FRAME) {
                                //关键帧
                                isKeyFrame = true;
                                if (encoder.get().onMediaInfoListener != null) {
                                    encoder.get().onMediaInfoListener.onSpsPpsInfo(sps, pps);
                                }
                            }
                            byte[] data = new byte[outputBuffer.remaining()];
                            outputBuffer.get(data, 0, data.length);

                            if (encoder.get().onMediaInfoListener != null) {
                                encoder.get().onMediaInfoListener.onVidoInfo(data, isKeyFrame);
                                encoder.get().onMediaInfoListener.onMediaTime(videoBufferInfo.presentationTimeUs / (1000 * 1000));
                            }

                            //第二个参数表示是否需要渲染
                            videoEncodec.releaseOutputBuffer(outputBufferIndex, false);
                            outputBufferIndex = videoEncodec.dequeueOutputBuffer(videoBufferInfo, 0);
                        }

                    }
                } catch (Exception e) {
                    Log.i(TAG, "run: video encoder" + e.getMessage());
                }
            }


        }

        public void exit() {
            isExit = true;
        }
    }

    //音频编码线程
    static class AudioEncodecThread extends Thread {
        private WeakReference<BasePushEncoder> encoder;
        private boolean isExit;
        private MediaCodec audioEncodec;
        private MediaCodec.BufferInfo audioBufferInfo;
        private long pts;

        public AudioEncodecThread(WeakReference<BasePushEncoder> encoder) {
            this.encoder = encoder;
            this.audioEncodec = encoder.get().audioEncodec;
            this.audioBufferInfo = encoder.get().audioBufferInfo;
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
                        encoder.get().audioExit = true;
                        if (encoder.get().videoExit) {
                            if (encoder.get().stop_exit) {
                                break;
                            }
                            encoder.get().stop_exit = true;

                        }
                        break;
                    }
                }

                try {
                    int outputBufferIndex = audioEncodec.dequeueOutputBuffer(audioBufferInfo, 0);
                    if (outputBufferIndex == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED) {

                    } else {
                        while (outputBufferIndex >= 0) {
                            ByteBuffer outputBuffer = audioEncodec.getOutputBuffers()[outputBufferIndex];
                            outputBuffer.position(audioBufferInfo.offset);
                            outputBuffer.limit(audioBufferInfo.offset + audioBufferInfo.size);
                            //pts默认为当前的时间戳
                            if (pts == 0) {
                                pts = audioBufferInfo.presentationTimeUs;
                            }

                            byte[] data = new byte[outputBuffer.remaining()];
                            outputBuffer.get(data, 0, data.length);
                            if (encoder.get().onMediaInfoListener != null) {
                                encoder.get().onMediaInfoListener.onAudioInfo(data);
                            }

                            audioBufferInfo.presentationTimeUs = audioBufferInfo.presentationTimeUs - pts;
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

        void onSpsPpsInfo(byte[] sps, byte[] pps);

        void onVidoInfo(byte[] data, boolean isKeyFrame);

        void onAudioInfo(byte[] data);
    }

    public static String byteToHex(byte[] bytes) {
        StringBuffer stringBuffer = new StringBuffer();
        for (int i = 0; i < bytes.length; i++) {
            String hex = Integer.toHexString(bytes[i]);
            if (hex.length() == 1) {
                stringBuffer.append("0" + hex);
            } else {
                stringBuffer.append(hex);
            }
            if (i > 20) {
                break;
            }
        }
        return stringBuffer.toString();
    }

}
