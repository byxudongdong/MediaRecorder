package com.jason.audioplayer;

import android.media.AudioManager;
import android.media.AudioTrack;
import android.os.Handler;
import android.os.Message;
import android.util.Log;

/**
 * @author: Jason
 * @date: 15/4/23.
 * @time: 下午10:15.
 * 播放
 */
public class AudioPlayer implements  IPlayComplete{

    private static final String TAG = AudioPlayer.class.getSimpleName();

    public static final int STATE_MSG_ID = 0x0010;

    private Handler mHandler;

    /**
     * 音频参数
     */
    private AudioParam mAudioParam;

    /**
     * 音频数据 byte
     */
    private byte[] mData;


    private AudioTrack mAudioTrack;

    /**
     * 是否准备就绪
     */
    private boolean mBReady = false;

    /**
     * 播放线程
     */
    private PlayAudioThread mPlayAudioThread;

    /**
     * 线程退出标志
     */
    private boolean mThreadExitFlag = false;

    /**
     * 较优播放块大小
     */
    private int mPrimePlaySize = 0;

    /**
     * 当前播放位置
     */
    private int mPlayOffset = 0;

    /**
     * 当前播放状态
     */

    private int mPlayState = 0;


    public AudioPlayer(Handler handler) {
        this.mHandler = handler;
    }

    public AudioPlayer(Handler handler, AudioParam audioParam) {
        this.mHandler = handler;
        setAudioParam(audioParam);
    }

    /**
     * 设置音频参数
     * @param audioParam
     */
    public void setAudioParam(AudioParam audioParam) {
        this.mAudioParam = audioParam;
    }


    /**
     * 设置音频源
     */
    public void setDataSource(byte[] byteArr) {
        this.mData = byteArr;
    }


    /**
     * 准备播放源
     */
    public boolean prepare() {

        if (mData == null || mAudioParam == null) {
            return false;
        }

        if (mBReady == true) {
            return true;
        }

        createAudioTrack();
        mBReady = true;

        setPlayState(PlayState.MPS_PREPARE);
        return true;
    }


    /**
     * 释放播放源
     */
    public boolean release() {
        stop();

        releaseAudioTrack();

        mBReady = false;
        setPlayState(PlayState.MPS_UNINIT);

        return true;
    }

    /**
     * 播放
     */
    public boolean play() {

        if (mBReady == false) {
            return false;
        }

        switch (mPlayState) {
            case PlayState.MPS_PREPARE:
                mPlayOffset = 0;
                setPlayState(PlayState.MPS_PLAYING);
                startThread();
                break;

            case PlayState.MPS_PAUSE:
                setPlayState(PlayState.MPS_PLAYING);
                startThread();
                break;
        }

        return true;
    }

    /**
     * 暂停
     */
    public boolean pause() {
        if (mBReady == false) {
            return false;
        }

        if (mPlayState == PlayState.MPS_PLAYING) {
            setPlayState(PlayState.MPS_PAUSE);
            stopThread();
        }

        return true;
    }

    /**
     * 停止
     */
    public boolean stop() {
        if (mBReady == false) {
            return false;
        }

        setPlayState(PlayState.MPS_PREPARE);
        stopThread();

        return true;
    }




    /**
     * 开启线程
     */
    private void startThread() {
        if (mPlayAudioThread == null) {
            mThreadExitFlag = false;
            mPlayAudioThread = new PlayAudioThread();
            mPlayAudioThread.start();
        }
    }



    /**
     * 停止线程
     */
    private void stopThread() {
        if (mPlayAudioThread != null) {
            mThreadExitFlag = true;
            mPlayAudioThread = null;
        }
    }

    /**
     * 释放AudioTrack
     */
    private void releaseAudioTrack() {
        if (mAudioTrack != null) {
            mAudioTrack.stop();
            mAudioTrack.release();
            mAudioTrack = null;
        }
    }




    private synchronized void setPlayState(int state) {
        mPlayState = state;

        if (mHandler != null) {
            Message msg = mHandler.obtainMessage(STATE_MSG_ID);
            msg.obj = mPlayState;
            msg.sendToTarget();

        }

    }


    private void createAudioTrack() {

        // 获得构建对象的最小缓冲区大小
        int minBufSize = AudioTrack.getMinBufferSize(mAudioParam.mFrequency,
                mAudioParam.mChannel,
                mAudioParam.mSampBit);

        mPrimePlaySize = minBufSize * 2;

        Log.d(TAG, "---mPrimePlaySize---" + mPrimePlaySize);

//		         STREAM_ALARM：警告声
//		         STREAM_MUSCI：音乐声，例如music等
//		         STREAM_RING：铃声
//		         STREAM_SYSTEM：系统声音
//		         STREAM_VOCIE_CALL：电话声音


        mAudioTrack = new AudioTrack(AudioManager.STREAM_MUSIC,
                mAudioParam.mFrequency,
                mAudioParam.mChannel,
                mAudioParam.mSampBit,
                minBufSize,
                AudioTrack.MODE_STREAM
        );

//				AudioTrack中有MODE_STATIC和MODE_STREAM两种分类。
//      		STREAM的意思是由用户在应用程序通过write方式把数据一次一次得写到audiotrack中。
//				这个和我们在socket中发送数据一样，应用层从某个地方获取数据，例如通过编解码得到PCM数据，然后write到audiotrack。
//				这种方式的坏处就是总是在JAVA层和Native层交互，效率损失较大。
//				而STATIC的意思是一开始创建的时候，就把音频数据放到一个固定的buffer，然后直接传给audiotrack，
//				后续就不用一次次得write了。AudioTrack会自己播放这个buffer中的数据。
//				这种方法对于铃声等内存占用较小，延时要求较高的声音来说很适用。

    }


    class PlayAudioThread extends Thread {

        @Override
        public void run() {

            Log.e(TAG, "PlayAudioThread  run mPlayOffset =  " + mPlayOffset);

            mAudioTrack.play();

            while (true) {
                if (mThreadExitFlag == true) {
                    break;
                }


                try {
                    mAudioTrack.write(mData, mPlayOffset, mPrimePlaySize);

                    mPlayOffset += mPrimePlaySize;
                } catch (Exception e) {
                    e.printStackTrace();
                    AudioPlayer.this.onPlayComplete();
                    break;

                }

                if (mPlayOffset >= mData.length) {
                    AudioPlayer.this.onPlayComplete();
                    break;
                }

            }

            mAudioTrack.stop();

            Log.d(TAG, "PlayAudioThread    complete....");

        }
    }




    @Override
    public void onPlayComplete() {

        mPlayAudioThread = null;
        if (mPlayState != PlayState.MPS_PAUSE) {
            setPlayState(PlayState.MPS_PREPARE);

        }

    }
}
