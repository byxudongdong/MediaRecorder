package com.android.xiong.mediarecordertest;

import android.app.Activity;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.media.AudioManager;
import android.os.Bundle;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

public class MainActivity extends Activity implements OnClickListener{
	public static String HEADSET_PLUG = "android.intent.action.HEADSET_PLUG";
	//发送广播
	public static String ACTION_SOUND_CHECK = "com.haochuang.bluetooth.DeviceControlActivity.SOUND_CHECK";//左右声道检测
	public static String ACTION_MIC_CHECK = "com.haochuang.bluetooth.DeviceControlActivity.MIC_CHECK";//MIC检测
	public static String ACTION_TRACK_CHANGE = "com.haochuang.bluetooth.DeviceControlActivity.TRACK_CHANGE";//音频通道切换
	public static String ACTION_DESTORY = "com.haochuang.bluetooth.DeviceControlActivity.DESTORY";//通知后台服务停止
	public static String ACTION_record = "com.haochuang.bluetooth.DeviceControlActivity.record";//记录数据
		
	//接收广播
	public static String ACTION_NUMBER_DATA = "com.android.xiong.mediarecordertest.MainActivityService.NUMBER";//条码
	public static String W200_EXTRA_DATA = "com.android.xiong.mediarecordertest.MainActivityService.NAME";
	public TextView SOUND_CHECK ;
	public TextView MIC_CHECK;
	public TextView TRACK_CHANGE;
	public TextView DESTORY;
	public TextView record;
	public TextView exit;
	
	public EditText data_show;

	private Intent ServiceIntent;
	
	/**
	 * 广播接收器
	 */
	private final BroadcastReceiver mGattUpdateReceiver = new BroadcastReceiver() {
		@Override
		public void onReceive(Context context, Intent intent) {
			final String action = intent.getAction();
			if(ACTION_NUMBER_DATA.equals(action)){
				//条码
				String DATA = intent
						.getStringExtra(W200_EXTRA_DATA);
				
				if (DATA.length() > 0) {
					data_show.setText(DATA);
					Log.i("编辑框：", DATA + "//////");
//					// 打开扬声器
//					am.setSpeakerphoneOn(true);
//					playBeep.playBeepSoundAndVibrate(true);
//					// 关闭扬声器
//					try {
//						Thread.sleep(200);
//						am.setSpeakerphoneOn(false);// 使用扬声器播放，即使已经插入耳机
//					} catch (InterruptedException e) {
//						// TODO Auto-generated catch block
//						e.printStackTrace();
//					}
					
				}
			}else if(HEADSET_PLUG.equals(action)){
				
				if (intent.hasExtra("state")) {
					if (intent.getIntExtra("state", 0) == 0) {
						Toast.makeText(MainActivity.this, "请插入W200", Toast.LENGTH_SHORT).show();
//						((InputMethodManager)MainActivity.this.getSystemService("input_method")).showInputMethodPicker();	
						
					} else if (intent.getIntExtra("state", 0) == 1) {
						
					}
				}
			}
		}
	};
	private static IntentFilter makeGattUpdateIntentFilter() {
		final IntentFilter intentFilter = new IntentFilter();
		intentFilter.addAction(ACTION_NUMBER_DATA);
		return intentFilter;
	}
//	public PlayBeepSound playBeep;
	private int AudioTrack_Manager = AudioManager.STREAM_MUSIC;
	AudioManager am;
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
		findView();
		setOnClick();
		am = (AudioManager) this.getSystemService(Context.AUDIO_SERVICE);	//获取系统音量控制实例
		am.setStreamVolume(AudioTrack_Manager,								//直接设置音量大小
				SaveShardMessage.getMotion_volume(MainActivity.this),
				AudioManager.FLAG_PLAY_SOUND);
		am.setMode(AudioManager.MODE_NORMAL);								//返回当前音频模式，如 NORMAL（普通）, RINGTONE（铃声）, orIN_CALL（通话）
		setVolumeControlStream(AudioManager.STREAM_MUSIC);				//设置该Activity中音量控制键控制的音频流
		/*
		AudioManager.STREAM_MUSIC  /音乐回放即媒体音量/

		AudioManager.STREAM_RING /铃声/

		AudioManager.STREAM_ALARM  /警报/

		AudioManager.STREAM_NOTIFICATION /窗口顶部状态栏通知声/

		AudioManager.STREAM_SYSTEM  /系统/

		AudioManager.STREAM_VOICECALL /通话 /

		AudioManager.STREAM_DTMF /双音多频,不是很明白什么东西 /		*/
//		playBeep = new PlayBeepSound(MainActivity.this);
//		playBeep.init_beep_sound();
		registerReceiver(mGattUpdateReceiver, makeGattUpdateIntentFilter());		//注册广播接受者
		DisplayMetrics	dm = new DisplayMetrics();									//获取分辨率
		getWindowManager().getDefaultDisplay().getMetrics(dm);						//获取分辨率
		MainActivityService.window_x = dm.widthPixels;								//屏幕宽度
		 ServiceIntent=new Intent(this,MainActivityService.class);	//启动后台
		 startService(ServiceIntent);
		Log.i("本地数据库","极性：" + ",音量:"+ ",声道:" );
		if(MainActivityService.headset_flag)
			finish();		//关闭APP
	}

	public void findView(){
		data_show = (EditText) findViewById(R.id.data_show);
		SOUND_CHECK = (TextView) findViewById(R.id.ACTION_SOUND_CHECK);
		MIC_CHECK = (TextView) findViewById(R.id.ACTION_MIC_CHECK);
		TRACK_CHANGE = (TextView) findViewById(R.id.ACTION_TRACK_CHANGE);
		DESTORY = (TextView) findViewById(R.id.ACTION_DESTORY);
		record = (TextView) findViewById(R.id.ACTION_record);
		exit = (TextView) findViewById(R.id.ACTION_exit);
	}

	public void setOnClick(){
		SOUND_CHECK.setOnClickListener(this);
		MIC_CHECK.setOnClickListener(this);
		TRACK_CHANGE.setOnClickListener(this);
		DESTORY.setOnClickListener(this);
		record.setOnClickListener(this);
		exit.setOnClickListener(this);
	}
	@Override
	public void onClick(View v) {
		switch(v.getId()){
		case R.id.ACTION_MIC_CHECK:
			//MIC检测 
			sendBroadCast(ACTION_MIC_CHECK);
			break;
		case R.id.ACTION_SOUND_CHECK:
			sendBroadCast(ACTION_SOUND_CHECK);
			break;
		case R.id.ACTION_TRACK_CHANGE:
			//音频通道交换
			sendBroadCast(ACTION_TRACK_CHANGE);
			break;
		case R.id.ACTION_DESTORY:
			//结束后台应用
			sendBroadCast(ACTION_DESTORY);
			break;	
			
		case R.id.ACTION_record:
			sendBroadCast(ACTION_record);
			Log.i("record", "保存记录0");
			break;	
			
		case R.id.ACTION_exit:
			android.os.Process.killProcess(android.os.Process.myPid());    //获取PID 
			System.exit(0);   //常规java、c#的标准退出法，返回值为0代表正常退出
			stopService(ServiceIntent);
			finish();
		}

		
	}
	@Override
	protected void onDestroy() {
		// TODO Auto-generated method stub
		super.onDestroy();
		unregisterReceiver(mGattUpdateReceiver);
	}
	
	public void sendBroadCast(String ACTION){
		Intent STATE_intent = new Intent(ACTION);
//		STATE_intent.putExtra(EXTRA_DATA,prams);
		sendBroadcast(STATE_intent);
	}
	
}
