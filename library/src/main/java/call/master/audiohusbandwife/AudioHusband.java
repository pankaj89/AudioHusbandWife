package call.master.audiohusbandwife;

import android.content.Context;
import android.media.MediaRecorder;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.support.annotation.RequiresApi;
import android.util.Log;

import java.io.File;
import java.io.IOException;
import java.util.Timer;
import java.util.TimerTask;

/**
 * Created by Pankaj Sharma on 12/6/17.
 */

public class AudioHusband {
    private static final String TAG = AudioHusband.class.getSimpleName();
    private static boolean NOUGAT_SUPPORT = false;

    private static AudioHusband mAudioHusband;

    public static AudioHusband getInstance(Context context) {
        if (mAudioHusband == null) {
            mAudioHusband = new AudioHusband(context);
        }
        return mAudioHusband;
    }

    public static AudioHusband getNewInstance(Context context) {
        return new AudioHusband(context);
    }

    Context context;
    File mFile;

    private long maxDuration = -1;
    private long minDuration = 1000;

    public AudioHusband setMaxDuration(long maxDuration) {
        this.maxDuration = maxDuration;
        return this;
    }

    private AudioHusband(Context context) {
        this.context = context;
    }

    public boolean isPauseFeatureSupported() {
        return Build.VERSION.SDK_INT >= Build.VERSION_CODES.N;
    }

    public AudioHusband setPauseSupport(boolean isPauseSupport) {
        if (isPauseFeatureSupported()) {
            NOUGAT_SUPPORT = isPauseSupport;
        }
        return this;
    }

    public AudioHusband setFile(File mFile) {
        this.mFile = mFile;
        if (mFile != null && mFile.exists() == false) {
            try {
                mFile.createNewFile();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return this;
    }

    private MediaRecorder mRecorder = null;

    private void onRecord(boolean start) {
        if (start) {
            if (mRecorder == null) {
                startRecording();
            } else {
                if (NOUGAT_SUPPORT && isPauseFeatureSupported()) {
                    resumeRecording();
                } else {
                    startRecording();
                }
            }
        } else {
            if (NOUGAT_SUPPORT && isPauseFeatureSupported()) {
                pauseRecording();
            } else {
                stopRecording();
            }
        }
    }


    public void startRecording() {
        Log.d(TAG, "startRecording() called");
        try {
            recorderSecondsElapsed = 0;
            mRecorder = new MediaRecorder();
            mRecorder.setAudioSource(MediaRecorder.AudioSource.MIC);
            mRecorder.setOutputFormat(MediaRecorder.OutputFormat.THREE_GPP);
            mRecorder.setOutputFile(mFile.getPath());
            mRecorder.setAudioEncoder(MediaRecorder.AudioEncoder.AMR_NB);

            mRecorder.prepare();
        } catch (Exception e) {
            e.printStackTrace();
            Log.e(TAG, "prepare() failed");
        }

        mRecorder.start();
        startTimer();
        if (mCallback != null) {
            mCallback.onRecordingStarts();
        }
    }

    @RequiresApi(api = Build.VERSION_CODES.N)
    private void resumeRecording() {
        Log.d(TAG, "resumeRecording() called");
        mRecorder.resume();
        startTimer();
        if (mCallback != null && recorderSecondsElapsed * 1000 >= minDuration) {
            mCallback.onRecordingResumed(recorderSecondsElapsed * 1000);
        }
    }

    public void stopRecording() {
        Log.d(TAG, "stopRecording() called");

        if (mRecorder != null) {
            try {
                mRecorder.stop();
                mRecorder.release();
            } catch (RuntimeException e) {
                e.printStackTrace();
            }
        }
        mRecorder = null;
        stopTimer();
        if (mCallback != null && recorderSecondsElapsed * 1000 >= minDuration) {
            mCallback.onRecordingStopped(recorderSecondsElapsed * 1000);
        }
    }

    @RequiresApi(api = Build.VERSION_CODES.N)
    private void pauseRecording() {
        Log.d(TAG, "pauseRecording() called");
        if (mRecorder != null) {
            mRecorder.pause();
        }
        stopTimer();
        if (mCallback != null && recorderSecondsElapsed * 1000 >= minDuration) {
            mCallback.onRecordingPaused(recorderSecondsElapsed * 1000);
        }
    }

    private boolean mStartRecording = true;

    public void toggleRecording() {
        onRecord(mStartRecording);
        mStartRecording = !mStartRecording;
    }

    public void onStop() {
        if (mRecorder != null) {
            mRecorder.release();
            mRecorder = null;
        }
    }


    Timer timer;
    Timer timerWave;

    private void startTimer() {
        stopTimer();
        timer = new Timer();
        timer.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                recorderSecondsElapsed++;
                mHandler.sendEmptyMessage(0);
            }
        }, 0, 1000);
    }

    // Defines a Handler object that's attached to the UI thread
    Handler mHandler = new Handler(Looper.getMainLooper()) {
        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            updateOnUIThread();
        }
    };


    private void stopTimer() {
        if (timer != null) {
            timer.cancel();
            timer.purge();
            timer = null;
        }
    }

    long recorderSecondsElapsed = 0;

    private void updateOnUIThread() {

        Log.d(TAG, "updateOnUIThread() called," + recorderSecondsElapsed * 1000);

        if (maxDuration > 0 && recorderSecondsElapsed * 1000 > maxDuration) {
            stopRecording();
        }

        if (mCallback != null && recorderSecondsElapsed * 1000 >= minDuration) {
            mCallback.onProgress(recorderSecondsElapsed * 1000);
        }
    }

    public String getFormattedSeconds() {
        long seconds = recorderSecondsElapsed;
        return getTwoDecimalsValue(seconds / 3600) + ":"
                + getTwoDecimalsValue(seconds / 60) + ":"
                + getTwoDecimalsValue(seconds % 60);
    }

    private static String getTwoDecimalsValue(long value) {
        if (value >= 0 && value <= 9) {
            return "0" + value;
        } else {
            return value + "";
        }
    }

    AudioRecordingCallback mCallback;

    public AudioHusband setRecordingCallback(AudioRecordingCallback mCallback) {
        this.mCallback = mCallback;
        return this;
    }

    public interface AudioRecordingCallback {
        public void onRecordingStarts();

        public void onProgress(long currentMillisRecorded);

        public void onRecordingResumed(long currentMillisRecorded);

        public void onRecordingPaused(long currentMillisRecorded);

        public void onRecordingStopped(long currentMillisRecorded);
    }
}
