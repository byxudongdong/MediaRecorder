package com.jason.audioplayer;

import android.media.AudioFormat;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;



public class MainActivity implements View.OnClickListener {

    private Button bt_pause;
    private Button bt_stop;
    private Button bt_player;
    private Handler mHandler;
    private  TextView textView;
    private AudioPlayer mAudioPlayer; // 播放器

    protected void onCreate(Bundle savedInstanceState) {
//        super.onCreate(savedInstanceState);
//        setContentView(R.layout.activity_main);
//
//
//        initView();
//        initAudioTrack();

    }

    private void initView() {

//        textView = (TextView) findViewById(R.id.tv);
//        bt_player = (Button) findViewById(R.id.bt_player);
//        bt_pause = (Button) findViewById(R.id.bt_pause);
//        bt_stop = (Button) findViewById(R.id.bt_stop);
//        bt_player.setOnClickListener(this);
//        bt_pause.setOnClickListener(this);
//        bt_stop.setOnClickListener(this);
    }

    private void initAudioTrack() {

        mHandler = new Handler() {
            @Override
            public void handleMessage(Message msg) {
                switch (msg.what) {
                    case AudioPlayer.STATE_MSG_ID:
                        showState((Integer) msg.obj);
                        break;
                }
            }
        };

        mAudioPlayer = new AudioPlayer(mHandler);
        // 获取音频参数
        AudioParam audioParam = getAudioParam();

        mAudioPlayer.setAudioParam(audioParam);

        // 获取音频数据
//        byte[] byteArr = getPCMData();
//        mAudioPlayer.setDataSource(byteArr);

        // 音频源准备
        mAudioPlayer.prepare();
    }

    /**
     * 获取PCM音频源数据
     * @return byte[]
     */
//    private byte[] getPCMData() {
//
//        InputStream is1 = getResources().openRawResource(R.raw.cesushexiang);
//        InputStream is2 = getResources().openRawResource(R.raw.ninchaochele);
//        InputStream is3 = getResources().openRawResource(R.raw.jiankongshexiang);

//        Vector<InputStream> vector = new Vector<InputStream>();
//        vector.add(is1);
//        vector.add(is2);
//        vector.add(is3);
//
//        Enumeration<InputStream> elements = vector.elements();
//
//        SequenceInputStream sis = new SequenceInputStream(elements);
//
//        ByteArrayOutputStream bos = new ByteArrayOutputStream();
//
//        byte[] buffer = new byte[1024];
//        int len = 0;
//
//        try {
//            while ((len = sis.read(buffer)) != -1) {
//                bos.write(buffer, 0, len);
//            }
//        } catch (IOException e) {
//            e.printStackTrace();
//        } finally {
//            try {
//                sis.close();
//                bos.flush();
//                bos.close();
//                is1.close();
//                is2.close();
//                is3.close();
//            } catch (IOException e) {
//                e.printStackTrace();
//            }
//        }
//        return bos.toByteArray();
//
//    }

    /**
     * 获得PCM音频数据参数
     * 注意 这里的参数的音频数据一般需要跟生成的音频参数一一对应
     */
    private AudioParam getAudioParam() {
        AudioParam audioParam = new AudioParam();
        audioParam.mFrequency = 8000;
        audioParam.mChannel = AudioFormat.CHANNEL_IN_STEREO;
        audioParam.mSampBit = AudioFormat.ENCODING_PCM_16BIT;

        return audioParam;
    }

    private void showState(int state) {
        String showString = "";

        switch (state) {
            case PlayState.MPS_UNINIT:
                showString = "未准备";
                break;

            case PlayState.MPS_PREPARE:
                showString = "准备就绪";
                break;

            case PlayState.MPS_PLAYING:
                showString = "播放中";
                break;

            case PlayState.MPS_PAUSE:
                showString = "暂停";
                break;
        }

        showTextState(showString);

    }

    private void showTextState(String showString) {
        textView.setText(showString);
    }


    @Override
    public void onClick(View v) {

//        switch (v.getId()) {

//            case R.id.bt_player:
//                mAudioPlayer.play();
//                break;
//
//            case R.id.bt_pause:
//                mAudioPlayer.pause();
//                break;
//
//            case R.id.bt_stop:
//                mAudioPlayer.stop();
//                break;

//        }

    }

    protected void onDestroy() {
//        super.onDestroy();

//        mAudioPlayer.release(); // 释放资源
    }
}
