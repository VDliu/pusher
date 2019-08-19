package pictrue.com.reiniot.livepusher.audio_record;

import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaRecorder;

/**
 * 2019/8/19.
 */
public class AudioRecordUtil {
    //PCM
    private AudioRecord audioRecord;
    private int bufferSizeInBytes;
    private boolean exit = false;
    private boolean start = false;
    private int readSize;
    OnRecordListener listener;

    public void setListener(OnRecordListener listener) {
        this.listener = listener;
    }

    public AudioRecordUtil() {
        bufferSizeInBytes = AudioRecord.getMinBufferSize(
                44100,
                AudioFormat.CHANNEL_IN_STEREO,
                AudioFormat.ENCODING_PCM_16BIT);

        audioRecord = new AudioRecord(MediaRecorder.AudioSource.MIC
                , 44100
                , AudioFormat.CHANNEL_IN_STEREO
                , AudioFormat.ENCODING_PCM_16BIT
                , bufferSizeInBytes);
    }

    public void start() {
        if (audioRecord != null) {
            new Thread() {
                @Override
                public void run() {
                    super.run();
                    audioRecord.startRecording();
                    start = true;
                    byte[] audioData = new byte[bufferSizeInBytes];
                    while (start) {
                        readSize = audioRecord.read(audioData, 0, bufferSizeInBytes);
                        if (listener != null) {
                            listener.onRecorder(audioData, readSize);
                        }
                    }

                    audioRecord.stop();
                    audioRecord.release();
                }
            }.start();
        }
    }

    public void stopRecord() {
        start = false;
    }

    public interface OnRecordListener {
        void onRecorder(byte[] data, int size);
    }
}
