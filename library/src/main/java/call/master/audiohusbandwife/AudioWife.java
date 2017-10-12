package call.master.audiohusbandwife;

import android.content.Context;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.media.MediaPlayer.OnCompletionListener;
import android.media.TimedMetaData;
import android.media.audiofx.Visualizer;
import android.net.Uri;
import android.os.Build;
import android.os.Handler;
import android.util.Log;
import android.view.View;

import java.io.IOException;
import java.util.ArrayList;

public class AudioWife {

    private static final String TAG = AudioWife.class.getSimpleName();

    /***
     * Keep a single copy of this in memory unless required to create a new instance explicitly.
     ****/
    private static AudioWife mAudioWife;

    /****
     * Playback progress update time in milliseconds
     ****/
    private static final int AUDIO_PROGRESS_UPDATE_TIME = 1000;

    private static final String ERROR_PLAYVIEW_NULL = "Play view cannot be null";
    private static final String ERROR_PLAYTIME_CURRENT_NEGATIVE = "Current playback time cannot be negative";
    private static final String ERROR_PLAYTIME_TOTAL_NEGATIVE = "Total playback time cannot be negative";

    private Handler mProgressUpdateHandler;

    private MediaPlayer mMediaPlayer;

    public long getDuration() {
        return duration;
    }

    public enum STATUS {STOPPED, PLAYING, PAUSED}

    public STATUS status = STATUS.STOPPED;

    private long duration;

    public interface OnPlayPauseListener {
        public void onPlayingStarts();

        public void onPaused();
    }

    public interface OnProgressListener {
        public void onProgress(long current, long total);
    }

    /****
     * Array to hold custom completion listeners
     ****/
    private ArrayList<OnCompletionListener> mCompletionListeners = new ArrayList<OnCompletionListener>();
    private ArrayList<MediaPlayer.OnErrorListener> mErrorListeners = new ArrayList<MediaPlayer.OnErrorListener>();
    private ArrayList<OnProgressListener> mProgressListeners = new ArrayList<OnProgressListener>();
    private ArrayList<OnPlayPauseListener> mPlayPauseListeners = new ArrayList<OnPlayPauseListener>();

    private ArrayList<View.OnClickListener> mPlayListeners = new ArrayList<View.OnClickListener>();

    private ArrayList<View.OnClickListener> mPauseListeners = new ArrayList<View.OnClickListener>();

    /***
     * Audio URI
     ****/
    private static Uri mUri;

    public static AudioWife getInstance() {

        if (mAudioWife == null) {
            mAudioWife = new AudioWife();
        }

        return mAudioWife;
    }

    private float mCurrentAmplitude = 0f;

    private Runnable mUpdateProgress = new Runnable() {

        public void run() {

            if (mProgressUpdateHandler != null && mMediaPlayer.isPlaying()) {

                int currentTime = mMediaPlayer.getCurrentPosition();
                // repeat the process

                mProgressUpdateHandler.postDelayed(this, AUDIO_PROGRESS_UPDATE_TIME);
                mProgressListener.onProgress(currentTime, mMediaPlayer.getDuration());


            } else {
                // DO NOT update UI if the player is paused
            }
        }
    };

    /***
     * Starts playing audio file associated. Before playing the audio, visibility of appropriate UI
     * controls is made visible. Calling this method has no effect if the audio is already being
     * played.
     ****/
    public void play() {

        if (mUri == null) {
            throw new IllegalStateException("Uri cannot be null. Call init() before calling this method");
        }

        if (mMediaPlayer == null) {
            throw new IllegalStateException("Call init() before calling this method");
        }

        if (mMediaPlayer.isPlaying()) {
            return;
        }

        mProgressUpdateHandler.removeCallbacks(mUpdateProgress);
        mProgressUpdateHandler.postDelayed(mUpdateProgress, AUDIO_PROGRESS_UPDATE_TIME);

        mMediaPlayer.start();
        duration = mMediaPlayer.getDuration();
        mPlayPauseListener.onPlayingStarts();

    }


    /***
     * Pause the audio being played. Calling this method has no effect if the audio is already
     * paused
     */
    public void pause() {

        if (mMediaPlayer == null) {
            return;
        }

        if (mMediaPlayer.isPlaying()) {
            mMediaPlayer.pause();
        }

        mPlayPauseListener.onPaused();
    }

    /***
     * Initialize the audio player. This method should be the first one to be called before starting
     * to play audio using
     *
     * @param ctx
     *            {@link android.app.Activity} Context
     * @param uri
     *            Uri of the audio to be played.
     ****/
    public AudioWife init(Context ctx, Uri uri) {

        Log.d(TAG, "init() called with: ctx = [" + ctx + "], uri = [" + uri + "]");
        if (uri == null) {
            throw new IllegalArgumentException("Uri cannot be null");
        }

        if (mAudioWife == null) {
            mAudioWife = new AudioWife();
        }

        mUri = uri;

        mProgressUpdateHandler = new Handler();

        initPlayer(ctx);

        return this;
    }

    /****
     * Add custom playback completion listener. Adding multiple listeners will queue up all the
     * listeners and fire them on media playback completes.
     */
    public AudioWife addOnCompletionListener(OnCompletionListener listener) {

        // add default click listener to the top
        // so that it is the one that gets fired first
        mCompletionListeners.clear();
        mCompletionListeners.add(0, listener);

        return this;
    }

    public AudioWife addOnErrorListener(MediaPlayer.OnErrorListener listener) {

        // add default click listener to the top
        // so that it is the one that gets fired first
        mErrorListeners.clear();
        mErrorListeners.add(0, listener);

        return this;
    }

    public AudioWife addProgressListener(OnProgressListener listener) {

        // add default click listener to the top
        // so that it is the one that gets fired first
        mProgressListeners.clear();
        mProgressListeners.add(0, listener);

        return this;
    }

    public AudioWife addPlayPauseListener(OnPlayPauseListener listener) {

        // add default click listener to the top
        // so that it is the one that gets fired first
        mPlayPauseListeners.clear();
        mPlayPauseListeners.add(0, listener);

        return this;
    }


    /****
     * Initialize and prepare the audio player
     ****/
    private void initPlayer(Context ctx) {

        mMediaPlayer = new MediaPlayer();
        mMediaPlayer.setAudioStreamType(AudioManager.STREAM_MUSIC);

        try {
            mMediaPlayer.setDataSource(ctx, mUri);
        } catch (IllegalArgumentException e) {
            e.printStackTrace();
        } catch (SecurityException e) {
            e.printStackTrace();
        } catch (IllegalStateException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } catch (Exception e) {
            e.printStackTrace();
        }

        try {
            mMediaPlayer.prepare();
        } catch (IllegalStateException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } catch (Exception e) {
            e.printStackTrace();
        }

        mMediaPlayer.setOnCompletionListener(mOnCompletion);
        mMediaPlayer.setOnErrorListener(mOnErrorListener);
        mMediaPlayer.setOnSeekCompleteListener(new MediaPlayer.OnSeekCompleteListener() {
            @Override
            public void onSeekComplete(MediaPlayer mp) {
                Log.d(TAG, "onSeekComplete() called with: mp = [" + mp + "]");
            }
        });
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            mMediaPlayer.setOnTimedMetaDataAvailableListener(new MediaPlayer.OnTimedMetaDataAvailableListener() {
                @Override
                public void onTimedMetaDataAvailable(MediaPlayer mp, TimedMetaData data) {
                    Log.d(TAG, "onTimedMetaDataAvailable() called with: mp = [" + mp + "], data = [" + data + "]");
                }
            });
        }

        setupVisualizerFxAndUI();
    }

    Visualizer mVisualizer;

    private void setupVisualizerFxAndUI() {

        mVisualizer = new Visualizer(mMediaPlayer.getAudioSessionId());
        mVisualizer.setCaptureSize(Visualizer.getCaptureSizeRange()[1]);

        mRunningSoundAvg = new double[3];
        mCurrentAvgEnergyOneSec = new double[3];
        mCurrentAvgEnergyOneSec[0] = -1;
        mCurrentAvgEnergyOneSec[1] = -1;
        mCurrentAvgEnergyOneSec[2] = -1;

        Visualizer.OnDataCaptureListener captureListener = new Visualizer.OnDataCaptureListener() {
            @Override
            public void onWaveFormDataCapture(Visualizer visualizer,
                                              byte[] bytes, int samplingRate) {
                // DO NOTHING
                System.out.println("");
            }

            @Override
            public void onFftDataCapture(Visualizer visualizer, byte[] bytes,
                                         int samplingRate) {
                System.out.println("onFftDataCapture " + samplingRate);
                updateVisualizerFFT(bytes);
            }
        };

        mVisualizer.setDataCaptureListener(captureListener, Visualizer.getMaxCaptureRate() / 2, false, true);
        mVisualizer.setEnabled(true);

    }

    private double mRunningSoundAvg[];
    private double mCurrentAvgEnergyOneSec[];
    private int mNumberOfSamplesInOneSec;
    private long mSystemTimeStartSec;
    // FREQS
    private static final int LOW_FREQUENCY = 300;
    private static final int MID_FREQUENCY = 2500;
    private static final int HIGH_FREQUENCY = 10000;

    public void updateVisualizerFFT(byte[] audioBytes) {
        int energySum = 0;
        energySum += Math.abs(audioBytes[0]);
        int k = 2;
        double captureSize = mVisualizer.getCaptureSize() / 2;
        int sampleRate = mVisualizer.getSamplingRate() / 2000;
        double nextFrequency = ((k / 2) * sampleRate) / (captureSize);
        while (nextFrequency < LOW_FREQUENCY) {
            energySum += Math.sqrt((audioBytes[k] * audioBytes[k])
                    * (audioBytes[k + 1] * audioBytes[k + 1]));
            k += 2;
            nextFrequency = ((k / 2) * sampleRate) / (captureSize);
        }
        double sampleAvgAudioEnergy = (double) energySum
                / (double) ((k * 1.0) / 2.0);

        mRunningSoundAvg[0] += sampleAvgAudioEnergy;
        /*if ((sampleAvgAudioEnergy > mCurrentAvgEnergyOneSec[0])
                && (mCurrentAvgEnergyOneSec[0] > 0)) {
            fireBeatDetectedLowEvent(sampleAvgAudioEnergy);
        }*/
        energySum = 0;
        while (nextFrequency < MID_FREQUENCY) {
            energySum += Math.sqrt((audioBytes[k] * audioBytes[k])
                    * (audioBytes[k + 1] * audioBytes[k + 1]));
            k += 2;
            nextFrequency = ((k / 2) * sampleRate) / (captureSize);
        }

        sampleAvgAudioEnergy = (double) energySum / (double) ((k * 1.0) / 2.0);
        mRunningSoundAvg[1] += sampleAvgAudioEnergy;
        /*if ((sampleAvgAudioEnergy > mCurrentAvgEnergyOneSec[1])
                && (mCurrentAvgEnergyOneSec[1] > 0)) {
            fireBeatDetectedMidEvent(sampleAvgAudioEnergy);
        }*/
        energySum = Math.abs(audioBytes[1]);

        while ((nextFrequency < HIGH_FREQUENCY) && (k < audioBytes.length)) {
            energySum += Math.sqrt((audioBytes[k] * audioBytes[k])
                    * (audioBytes[k + 1] * audioBytes[k + 1]));
            k += 2;
            nextFrequency = ((k / 2) * sampleRate) / (captureSize);
        }

        sampleAvgAudioEnergy = (double) energySum / (double) ((k * 1.0) / 2.0);
        mRunningSoundAvg[2] += sampleAvgAudioEnergy;
        /*if ((sampleAvgAudioEnergy > mCurrentAvgEnergyOneSec[2])
                && (mCurrentAvgEnergyOneSec[2] > 0)) {
            fireBeatDetectedHighEvent(sampleAvgAudioEnergy);
        }*/

        double avg = (mRunningSoundAvg[0] + mRunningSoundAvg[1] + mRunningSoundAvg[2]) / 3d;
        mCurrentAmplitude = (float) avg;
//        manageAnimation((float) avg);

        mNumberOfSamplesInOneSec++;
        if ((System.currentTimeMillis() - mSystemTimeStartSec) > 1000) {
            mCurrentAvgEnergyOneSec[0] = mRunningSoundAvg[0]
                    / mNumberOfSamplesInOneSec;
            mCurrentAvgEnergyOneSec[1] = mRunningSoundAvg[1]
                    / mNumberOfSamplesInOneSec;
            mCurrentAvgEnergyOneSec[2] = mRunningSoundAvg[2]
                    / mNumberOfSamplesInOneSec;
            mNumberOfSamplesInOneSec = 0;
            mRunningSoundAvg[0] = 0.0;
            mRunningSoundAvg[1] = 0.0;
            mRunningSoundAvg[2] = 0.0;
            mSystemTimeStartSec = System.currentTimeMillis();
        }
    }


    //*********************
    private OnCompletionListener mOnCompletion = new OnCompletionListener() {

        @Override
        public void onCompletion(MediaPlayer mp) {

            mPlayPauseListener.onPaused();
            status = STATUS.STOPPED;
            // ensure that our completion listener fires first.
            // This will provide the developer to over-ride our
            // completion listener functionality

            for (OnCompletionListener listener : mCompletionListeners) {
                listener.onCompletion(mp);
            }
        }
    };

    private MediaPlayer.OnErrorListener mOnErrorListener = new MediaPlayer.OnErrorListener() {

        @Override
        public boolean onError(MediaPlayer mp, int what, int extra) {
            for (MediaPlayer.OnErrorListener listener : mErrorListeners) {
                listener.onError(mp, what, extra);
            }
            return false;
        }
    };

    private OnProgressListener mProgressListener = new OnProgressListener() {

        @Override
        public void onProgress(long current, long total) {
            Log.d(TAG, "onProgress() called with: current = [" + current + "], total = [" + total + "]");
            for (OnProgressListener listener : mProgressListeners) {
                listener.onProgress(current, total);
            }
        }
    };

    private OnPlayPauseListener mPlayPauseListener = new OnPlayPauseListener() {

        @Override
        public void onPlayingStarts() {
            status = STATUS.PLAYING;
            for (OnPlayPauseListener listener : mPlayPauseListeners) {
                listener.onPlayingStarts();
            }
        }

        @Override
        public void onPaused() {
            status = STATUS.PAUSED;
            for (OnPlayPauseListener listener : mPlayPauseListeners) {
                listener.onPaused();
            }
        }
    };
    //*********************

    public void release() {

        if (mMediaPlayer != null) {
            mMediaPlayer.stop();
            mMediaPlayer.reset();
            mMediaPlayer.release();
            mMediaPlayer = null;
            mProgressUpdateHandler = null;
        }
    }
}