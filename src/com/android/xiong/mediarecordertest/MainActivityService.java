package com.android.xiong.mediarecordertest;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;

import org.apache.http.util.EncodingUtils;

import com.staticmessage.data.StaticByteMessageData;
import com.staticmessage.data.StaticByteMessageData2;
import com.staticmessage.data.StaticByteReverseData;
import com.staticmessage.data.StaticByteReverseData2;

import android.R.integer;
import android.annotation.SuppressLint;
import android.app.Notification;
import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.PixelFormat;
import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioRecord;
import android.media.AudioTrack;
import android.media.MediaRecorder;
import android.os.Process;
import android.os.Environment;
import android.os.Handler;
import android.os.Bundle;
import android.os.IBinder;
import android.os.Message;
import android.util.Log;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.view.View.OnTouchListener;
import android.widget.Button;
import android.widget.Toast;

public class MainActivityService extends Service {

	public boolean insert_detect_Data_flag = false;// 设备检测
	public boolean responses_detect_Data_flag = false;// 设备确认
	private boolean insert_detect_sleep = true;// 检测指令发送后是否播放充电波形
	public static boolean headset_flag = false;
	
	public MainActivityService() {
		// TODO Auto-generated constructor stub
	}
	/**
	 * 耳机连接或断开广播接收
	 */
	public BroadcastReceiver HeadsetPlugReceiver = new BroadcastReceiver() {
		@Override
		public void onReceive(Context context, Intent intent) {
			final String action = intent.getAction();
			if(HEADSET_PLUG.equals(action)){
				if (intent.hasExtra("state")) {
					if (intent.getIntExtra("state", 0) == 0) {
						if (headset_flag) {
							headset_flag = false;
							if (PlayAudioThread != null && audioRecord != null) {
								insert_detect_success_flag = false;
								mThreadExitFlag = true;
								isAdded = false;
								am.setStreamVolume(AudioTrack_Manager, audioCurrentVolumn,
										AudioManager.FLAG_PLAY_SOUND);
								wm.removeView(btn_floatView);
								Log.i("W200断开连接>> = true", ">>>>>>>>>");
								ACTION_STATE_DATA_Parms = 0;
							}
						}
					} else if (intent.getIntExtra("state", 0) == 1) {
						headset_flag = true;
						ACTION_STATE_DATA_Parms = 1;
						Log.i("W200连接>> = true", ">>>>>>>>>");
						if(!isAdded)
							createFloatView();
						start_audio_track();
					}
				}
			}else if(ACTION_SOUND_CHECK.equals(action)){
				//左右声道检测
//				MIC_SOUND_CHECK();
				if(SaveShardMessage.change_track_flag)
				SaveShardMessage.change_track_flag = false;
				else SaveShardMessage.change_track_flag = true;
				Toast.makeText(MainActivityService.this, "左右声道极性：" + (SaveShardMessage.change_track_flag? "正向" : "反向"), Toast.LENGTH_SHORT).show();
			}else if(ACTION_MIC_CHECK.equals(action)){
				//MIC检测
//				MIC_SOUND_CHECK();
				if(SaveShardMessage.reversePolarity <= 0)
				SaveShardMessage.reversePolarity = 1;
				else SaveShardMessage.reversePolarity = 0;
				Toast.makeText(MainActivityService.this, "MIC极性：" + SaveShardMessage.reversePolarity, Toast.LENGTH_SHORT).show();
			}else if(ACTION_TRACK_CHANGE.equals(action)){
				//音频通道检测
				track_change();
			}else if(ACTION_DESTROY_A.equals(action)){
				//销毁Service
				if(!mThreadExitFlag)
				mThreadExitFlag = true;
				if(isAdded){
					isAdded = false;
					wm.removeView(btn_floatView);
					unregisterReceiver(HeadsetPlugReceiver);
				}
				stopSelf();
			}else if (ACTION_record.equals(action)) {
				copyWaveFile(getTempFilename(), paths);
				Log.i("record", "保存记录1");
			}
		}
	};

	private String path;
	private String paths = path;
	private File saveFilePath;
	
	public void start_audio_track(){
		start_play();			//播放音乐
		startRecord();  		//录音
		Log.i("recordfile", "创建文件");
		String text = "test001";
		paths = path + "/" + text + ".wav";
		saveFilePath = new File(paths);
		try {
			saveFilePath.createNewFile();
		} catch (IOException e1) {
			// TODO 自动生成的 catch 块
			e1.printStackTrace();
		}
		Log.i("recordfile", "创建文件结束");
		
		try {
			// 系统播放器启动时音量会从小到大有一个渐变过程，而检测指令需要音量足够大才可以发送成功，
			// 因此这里添加500ms的前导音频。播完前导音频后音量就足够大，满足检测指令成功发出条件。
			Thread.sleep(500);
			insert_detect_Data_flag = true;
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	public static String ACTION_MIC_SOUND_CHECK = "com.android.xiong.mediarecordertest.MainActivityService.MIC_SOUND_CHECK";//MIC和声道检测结果
	public static String HEADSET_PLUG = "android.intent.action.HEADSET_PLUG";
	
	private static final String AUDIO_RECORDER_FOLDER = "AudioRecorder"; // 默认录音文件的存储位置
	//注册广播
	private void registerHeadsetPlugReceiver() {
		IntentFilter intentFilter = new IntentFilter();
		intentFilter.addAction(HEADSET_PLUG);
		intentFilter.addAction(ACTION_SOUND_CHECK);
		intentFilter.addAction(ACTION_MIC_CHECK);
		intentFilter.addAction(ACTION_TRACK_CHANGE);
		intentFilter.addAction(ACTION_DESTROY_A);
		intentFilter.addAction(ACTION_record);
		registerReceiver(HeadsetPlugReceiver, intentFilter);
	}
	
	@Override
	public IBinder onBind(Intent intent) {
		return null;
	}

	AudioManager am;
	int audioMaxVolumn;// 最大音量
	int audioCurrentVolumn;// 当前音量
	private SQLiteDatabase database;
	private MySqlite mySqlite;
	
	@SuppressWarnings("deprecation")
	public void onCreate() {
		
		if (Environment.getExternalStorageState().equals(
				Environment.MEDIA_MOUNTED)) {
			path = Environment.getExternalStorageDirectory().getPath() + "/"
					+ AUDIO_RECORDER_FOLDER;
		}
		
		//提升Service的优先级
		Notification notification = new Notification(R.drawable.ic_launcher,
				"启动服务发出通知", System.currentTimeMillis());
		playBeep = new PlayBeepSound(MainActivityService.this);
		// 设置内容和点击事件
		Intent intent1 = new Intent(this, MainActivity.class);
		PendingIntent contentIntent = PendingIntent.getActivity(this, 0,
				intent1, 0);
		notification.setLatestEventInfo(this, "优先级通知", "提高优先级",  contentIntent);
		notification.flags = Notification.FLAG_AUTO_CANCEL; // 设置为点击后自动取消
		startForeground(1235, notification);
		
		registerHeadsetPlugReceiver();
		am = (AudioManager) this.getSystemService(this.AUDIO_SERVICE);
		audioMaxVolumn = am.getStreamMaxVolume(AudioTrack_Manager);
		audioCurrentVolumn = am.getStreamVolume(AudioTrack_Manager);
		am.setMode(AudioTrack_Manager);
		Log.i("当前音量", audioMaxVolumn + "," + audioCurrentVolumn);
		
		mySqlite = new MySqlite(this);
		database = mySqlite.getWritableDatabase();
		//MIC_SOUND_CHECK();
		saveShardMessage = new SaveShardMessage(database, this);
		SaveShardMessage.getChange_track_flag(this);
		SaveShardMessage.getMotion_volume(this);
		SaveShardMessage.getReversePolarity(this);
		Log.i("启动service","极性：" + ",最大音量:"+ audioMaxVolumn +",当前音量:"+ audioCurrentVolumn + ",声道:" );
	};
	
	/**
	 * 获取mic录音数据同步解析；
	 */
	int s;
	/**
	 * 发送广播
	 */
	public static String ACTION_NUMBER_DATA = "com.android.xiong.mediarecordertest.MainActivityService.NUMBER";//条码
	public static String ACTION_STATE_DATA = "com.android.xiong.mediarecordertest.MainActivityService.STATE";//W200状态
	public static String ACTION_BATTERY_DATA = "com.android.xiong.mediarecordertest.MainActivityService.BATTERY";//电量
	public static String ACTION_DESTROY = "com.android.xiong.mediarecordertest.MainActivityService.DESTORY";//销毁服务
	public static String ACTION_record = "com.haochuang.bluetooth.DeviceControlActivity.record";//记录数据
	
	public static String ACTION_AudioTrack_Manager = "com.android.xiong.mediarecordertest.MainActivityService.ACTION_AudioTrack_Manager";
	/**
	 * 接收广播
	 */
	public static String ACTION_SOUND_CHECK = "com.haochuang.bluetooth.DeviceControlActivity.SOUND_CHECK";//左右声道检测
	public static String ACTION_MIC_CHECK = "com.haochuang.bluetooth.DeviceControlActivity.MIC_CHECK";//MIC检测
	public static String ACTION_TRACK_CHANGE = "com.haochuang.bluetooth.DeviceControlActivity.TRACK_CHANGE";//音频通道切换
	public static String ACTION_DESTROY_A = "com.haochuang.bluetooth.DeviceControlActivity.DESTORY";
	public static String EXTRA_DATA = "com.android.xiong.mediarecordertest.MainActivityService.NAME";
	
	
	private int AudioTrack_Manager = AudioManager.STREAM_MUSIC;
	AudioTrack trackplayer;
	public static boolean mThreadExitFlag = false; // 线程退出标志f
	private Thread PlayAudioThread = null;// 播放音频线程
	private boolean start_play_audio;
	public PlayBeepSound playBeep;
	
	/**
	 * 录音数据解析结果
	 */
	public void sendData(byte[] data) {
		final StringBuilder stringBuilder = new StringBuilder();
		if (data != null && data.length > 0) {
			for (byte byteChar : data)
				stringBuilder.append(String.format("%02X ", byteChar));// 表示以十六进制形式输出,02表示不足两位，前面补0输出；出过两位，不影响
			if (stringBuilder.toString().contains("53 01 ")
					&& stringBuilder.toString().startsWith("40")
					&& stringBuilder.toString().endsWith("2A ")) {
				if (Util.checkCurrentNumber(stringBuilder.toString())){
					String ss = Util.getCurrentNumberString(stringBuilder
							.toString());
					stringBuilder.delete(0, stringBuilder.length());
					insert_detect_success_flag = true;
					button_background = true;
					if(ss.length() > 2){
						Log.i("YYYYYYYYYYY", "条码：" + ss);
//						try {
//							am.setSpeakerphoneOn(true);
							playBeep.init_beep_sound();
							playBeep.playBeepSoundAndVibrate(true);
//							Thread.sleep(300);
//							am.setSpeakerphoneOn(false);// 使用扬声器播放，即使已经插入耳机
//						} catch (InterruptedException e) {
//							e.printStackTrace();
//						}
						sendBroadCast(ACTION_NUMBER_DATA,ss);
						handler.sendEmptyMessage(0x001);
						
						//copyWaveFile(getTempFilename(), paths);		
						//Log.i("record", "保存记录1");
						//flag_read_number = false;
					}
				}
			} else if (stringBuilder.toString().startsWith("40 03 52 0D")) {
				String ss = Util.getPowerNumberString(stringBuilder.toString());
				stringBuilder.delete(0, stringBuilder.length());
				int prams = Integer.parseInt(ss.substring(1, 2));
				Log.i("YYYYYYYYYYY", "电量：" + prams);
				sendBroadCast(ACTION_BATTERY_DATA,prams);
				Toast.makeText(MainActivityService.this, "W200电量"+ prams, Toast.LENGTH_SHORT).show();
			} else if (stringBuilder.toString().startsWith("40 02 52 A0 F4 2A")) {
				responses_detect_Data_flag = true;
				insert_detect_success_flag = true;
				ACTION_STATE_DATA_Parms = 2;
				if (adaptation_flag)
					reversePolarity = 0;
				Log.i("YYYYYYYYYYY", "检测到W200");
				stringBuilder.delete(0, stringBuilder.length());
			} else if (stringBuilder.toString().startsWith("BF FD AD 5F 0B D5")) {
				responses_detect_Data_flag = true;
				insert_detect_success_flag = true;
				ACTION_STATE_DATA_Parms = 2;
				if (adaptation_flag)
					reversePolarity = 1;
				Log.i("YYYYYYYYYYY", "反向检测到W200");
				stringBuilder.delete(0, stringBuilder.length());
			}
		}
	}
	public void sendBroadCast(String ACTION,int prams){
		Intent STATE_intent = new Intent(ACTION);
		STATE_intent.putExtra(EXTRA_DATA,prams);
		sendBroadcast(STATE_intent);
	}
	
	public void sendBroadCast(String ACTION,String prams){
		Intent STATE_intent = new Intent(ACTION);
		STATE_intent.putExtra(EXTRA_DATA,prams);
		sendBroadcast(STATE_intent);
	}

	/**
	 * 启动录音
	 */
	private void startRecord() {
		if(audioRecord == null)
			createAudioRecord();
		int audioRecordState = audioRecord.getState(); 
		if( audioRecordState  != AudioRecord.STATE_INITIALIZED){ 
            Log.i("ERROR: not initialized state=" , getString(audioRecordState));
            //finish(); //小例子直接关闭Activity 
        }else{ 
            Log.i("AudioRecord is initialized!","----------");
        }            
		//Process.setThreadPriority(Process.THREAD_PRIORITY_BACKGROUND); //设置线程优先级
		
		audioRecord.startRecording();
		isRecording = true;
		recordingThread = new Thread(new Runnable() {
			public void run() {
				//recordingThread.setThreadPriority(Process.THREAD_PRIORITY_URGENT_AUDIO); // -19
				recordingThread.setPriority(Thread.MAX_PRIORITY); // 10
				try {
					 	int state = audioRecord.getRecordingState(); 
				        if(state != AudioRecord.RECORDSTATE_RECORDING){ 
				            Log.i("AudioRecord is not recording... state=" , getString(state)); 
				            //finish(); 
				            return; 
				            }
				        else 
				            {
				            	writeAudioDataToFile();
				            }
				} catch (IOException e) {
					// TODO 自动生成的 catch 块
					e.printStackTrace();
				}
			}
		}, "AudioRecorder Thread");
		recordingThread.start();
	}
	
	public void createAudioRecord() {
		recBufSize = AudioRecord.getMinBufferSize(frequency,
				channelConfiguration, EncodingBitRate);
		if(recBufSize != AudioRecord.ERROR_BAD_VALUE){
			audioRecord = new AudioRecord(MediaRecorder.AudioSource.MIC, frequency,
					channelConfiguration, EncodingBitRate, recBufSize*2);
			System.out.println("AudioRecord成功");
		}
	}
	
	private static final String AUDIO_RECORDER_TEMP_FILE = "record_temp.raw";
	private String getTempFilename() {
		String filepath = Environment.getExternalStorageDirectory().getPath();
		File file = new File(filepath, AUDIO_RECORDER_FOLDER);

		if (!file.exists()) {
			file.mkdirs();
		}
		File tempFile = new File(filepath, AUDIO_RECORDER_TEMP_FILE);

		if (tempFile.exists())
			tempFile.delete();

		return (file.getAbsolutePath() + "/" + AUDIO_RECORDER_TEMP_FILE);
	}
	
	int length;
	//读SD中的文件  
	public byte[] readFileSdcardFile(String fileName) throws IOException{   
	  String res="";  
	  byte [] buffer = null;
	  try{   
	         FileInputStream fin = new FileInputStream(fileName);   
	  
	         length = fin.available();   
	  
	         buffer = new byte[length];   
	         fin.read(buffer);       
	  
	         res = EncodingUtils.getString(buffer, "UTF-8");   
	  
	         fin.close();       
	        }   
	  
	        catch(Exception e){   
	         e.printStackTrace();   
	        }   
			return buffer;   
	}   
	
	/**
	 * 录音数据解析
	 * @throws IOException 
	 */
	int read = 0;
	private void writeAudioDataToFile() throws IOException {
		byte data[] = new byte[recBufSize];//new byte[length];//
		//int read = 0;
		//byte [] buffer = new byte[length];
		String filename = getTempFilename();
		FileOutputStream os = null;
		try {
			Log.i("recordfile", "记录数据文件");
			os = new FileOutputStream(filename);
			
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		}
//		if (SaveShardMessage.reversePolarity <= 0) {
//			MyNative.audioInterface(44100, 0, 0);
//			Log.i("启动录音0000",
//					reversePolarity
//							+ "<<"
//							+ SaveShardMessage.reversePolarity);
//		} else {
//			MyNative.audioInterface(44100, 0,
//					SaveShardMessage.reversePolarity);
//			Log.i("启动录音1111",
//					reversePolarity
//							+ "<<"
//							+ SaveShardMessage.reversePolarity);
//		}
		
		if (SaveShardMessage.reversePolarity >= 0)
			MyNative.audioInterface(44100, 0, SaveShardMessage.reversePolarity);
		while (isRecording) {
			
			read = audioRecord.read(data, 0, recBufSize);
			Log.d("recorddata", String.valueOf(read));
			//data=readFileSdcardFile(path + "/" + "recordtest.wav");
			if (AudioRecord.ERROR_INVALID_OPERATION != read) {
				//simple_c2java(data);
				
				try {
					os.write(data);
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
//			try {
//				Thread.sleep(500);
//			} catch (InterruptedException e) {
//				// TODO 自动生成的 catch 块
//				e.printStackTrace();
//			}
		}
		
		try {
			os.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	/**
	 * 波形数据解析
	 * @param byte_source
	 */
	public void simple_c2java(byte[] byte_source) {
		code_data = new byte[320];
		int deal_length= 800;
		byte[] byte_int = new byte[deal_length];
		int datalength = read;
		for (int n = 0; n < datalength; n += deal_length) {
			if ((datalength - n) < deal_length) {
				System.arraycopy(byte_source, n, byte_int, 0,
						datalength - n);
				s = MyNative.cToJava(byte_int, datalength - n,
						code_data);
			} else {
				System.arraycopy(byte_source, n, byte_int, 0, deal_length);
				s = MyNative.cToJava(byte_int, deal_length, code_data);
			}
//			Log.i("数据解析结果：S = 0", "S = " + s);
			if (s > 0) {
//				Log.i("数据解析结果：S > 0", "S = " + s);
				byte[] send_data = new byte[s - 1];
				System.arraycopy(code_data, 0, send_data, 0, s - 1);
				sendData(send_data);
				
				byte[] srtbyte = send_data;
				String res = new String(srtbyte);
				Log.i("CodeRec", "解码有输出:" + res);
				printHexString("数据：",send_data);

				break;
			}
		}
	}
	
	/**
     * 将指定byte数组以16进制的形式打印到控制台
     * 
     * @param hint
     *            String
     * @param b
     *            byte[]
     * @return void
     */
    public static void printHexString(String hint, byte[] b)
    {
        System.out.print(hint);
        for (int i = 0; i < b.length; i++)
        {
            String hex = Integer.toHexString(b[i] & 0xFF);
            if (hex.length() == 1)
            {
                hex = '0' + hex;
            }
            System.out.print(hex.toUpperCase() + " ");
        }
        System.out.println("");
    }
	
	public int ACTION_STATE_DATA_Parms = -1;
	private static int frequency = 8000;
	private static int channelConfiguration = AudioFormat.CHANNEL_IN_MONO;// 单声道
	private static int EncodingBitRate = AudioFormat.ENCODING_PCM_16BIT; // 音频数据格式：脉冲编码调制（PCM）每个样品16位
	private AudioRecord audioRecord;
	private int recBufSize = 0;
	private Thread recordingThread = null;
	private boolean isRecording = false;
	public int reversePolarity = -1;
	byte[] code_data;
	private boolean adaptation_flag = false;
	private boolean button_background = false;
//	public PlayBeepSound playBeep;
	
	@SuppressLint("HandlerLeak")
	Handler handler = new Handler() {
		public void handleMessage(Message msg) {
			if (msg.what == 0x001) {
				 btn_floatView.setBackgroundResource(R.drawable.button_scan);
			}else if(msg.what == 0x002){
				//检测指令发出后500ms后是否收到回应
				if(!insert_detect_success_flag){
					insert_detect_sleep = false;
					insert_detect_success_flag = false;
					ACTION_STATE_DATA_Parms = 3;
				}
				sendBroadCast(ACTION_STATE_DATA, ACTION_STATE_DATA_Parms);
				Toast.makeText(MainActivityService.this, "W200状态检测结果" + ACTION_STATE_DATA_Parms, Toast.LENGTH_SHORT).show();
			}else if(msg.what == 0x003){
				//扫描按钮背景
				if(!button_background)
					btn_floatView.setBackgroundResource(R.drawable.button_scan);
				button_background = false;
				//flag_read_number = false;
			}
		};
	};
	
	/**
	 * 播放器
	 */
	@SuppressWarnings("deprecation")
	public void start_play() {
		// 根据采样率，采样精度，单双声道来得到frame的大小。
		int bufsize = AudioTrack.getMinBufferSize(48000,//
				AudioFormat.CHANNEL_CONFIGURATION_STEREO,// 双声道
				AudioFormat.ENCODING_PCM_16BIT);// 一个采样点16比特-2个字节
		// 注意，按照数字音频的知识，这个算出来的是一秒钟buffer的大小。
		// 创建AudioTrack
		am.setStreamVolume(AudioTrack_Manager,
				audioMaxVolumn * 3/4,//SaveShardMessage.volume
				AudioManager.FLAG_PLAY_SOUND);
		trackplayer = new AudioTrack(AudioTrack_Manager, 48000,
				AudioFormat.CHANNEL_CONFIGURATION_STEREO,
				AudioFormat.ENCODING_PCM_16BIT, bufsize, AudioTrack.MODE_STREAM);//

		mThreadExitFlag = false;
		trackplayer.play();
		PlayAudioThread = new Thread(new Runnable(){
			public void run() {
				start_scan();
			}
		},"PlayAudio");
		PlayAudioThread.start();
	}
	
	public static boolean flag_read_number = false;// 插入播放扫描指令音频byte[]
	byte[] byte_damo;// 循环播放的数据
	public boolean insert_detect_success_flag = false;
	byte[] byte_test = new byte[2048];
	/**
	 * 播放指令数据
	 */

	public void start_scan() {
		start_play_audio = true;
		while (start_play_audio) {
			if (mThreadExitFlag) {
				stopRecord();
				stopPlayAudio();
				break;
			} else {
				// java.lang.IllegalStateException: play() called on
				// uninitialized AudioTrack.
				if (SaveShardMessage.change_track_flag){
					if (flag_read_number) {
						byte_damo = StaticByteMessageData.scan_byte;
						flag_read_number = false;
						remove_buttery();
						My_thread_sleep(3000, 0x003);
						Log.i("扫描指令>> = true", ">>>>>>>>>");
						
						try {
						
							trackplayer.write(byte_damo, 0, byte_damo.length);// 往track中写数据
							byte_damo =null;
						} catch (Exception e) {
							PlayAudioThread.interrupt();
							break;
						}
						
					} else if (insert_detect_Data_flag) {
						byte_damo = StaticByteMessageData2.insert_detect_byte;
						remove_buttery();
						Log.i("检测指令 = true", ">>>>>>>>>");
						My_thread_sleep(500, 0x002); 
						
						try {
						
							trackplayer.write(byte_damo, 0, byte_damo.length);// 往track中写数据
							byte_damo =null;
						} catch (Exception e) {
							PlayAudioThread.interrupt();
							break;
						}
						
					} else if (responses_detect_Data_flag) { 
						byte_damo = StaticByteMessageData2.responses_detect_byte;
						responses_detect_Data_flag = false;
						Log.i("设备确认指令 = true", ">>>>>>>>>");
						
						try {
					
							trackplayer.write(byte_damo, 0, byte_damo.length);// 往track中写数据
							byte_damo =null;
						} catch (Exception e) {
							PlayAudioThread.interrupt();
							break;
						}
						
					}/* else {
						//充电指令
						//charge_buttery();
					}*/
				} else {
					if (flag_read_number) {
						byte_damo = StaticByteReverseData.scan_reverse_byte;
						flag_read_number = false;
						remove_buttery();
						My_thread_sleep(3000, 0x003);
						Log.i("R扫描指令 = true", ">>>>>>>>>");
						
						try {
							
							trackplayer.write(byte_damo, 0, byte_damo.length);// 往track中写数据
							byte_damo =null;
						} catch (Exception e) {
							PlayAudioThread.interrupt();
							break;
						}
						
					} else if (insert_detect_Data_flag) {
						byte_damo = StaticByteReverseData2.insert_reverse_byte;
						remove_buttery();
						Log.i("R检测指令 = true", ">>>>>>>>>");
						My_thread_sleep(500, 0x002);
						
						try {
							
							trackplayer.write(byte_damo, 0, byte_damo.length);// 往track中写数据
							byte_damo =null;
						} catch (Exception e) {
							PlayAudioThread.interrupt();
							break;
						}
						
					} else if (responses_detect_Data_flag) {
						byte_damo = StaticByteReverseData2.responses_reverse_byte;
						responses_detect_Data_flag = false;
						Log.i("R设备确认指令", ">>>>>>>>>");
						
						try {
							
							trackplayer.write(byte_damo, 0, byte_damo.length);// 往track中写数据
							byte_damo =null;
						} catch (Exception e) {
							PlayAudioThread.interrupt();
							break;
						}
						
					} else {
						//charge_buttery();
					}
				}

				//Log.i("发送扫描指令", ">>>>>>>>>");
//				try {
//					Thread.sleep(9);
//					trackplayer.write(byte_damo, 0, byte_damo.length);// 往track中写数据
//				} catch (Exception e) {
//					PlayAudioThread.interrupt();
//					break;
//				}
			}
		}
	}

/**	
	public void start_scan() {
		start_play_audio = true;
		while (start_play_audio) {
			if (mThreadExitFlag) {
				stopRecord();
				stopPlayAudio();
				break;
			} else {
				// java.lang.IllegalStateException: play() called on
				// uninitialized AudioTrack.
				if (SaveShardMessage.change_track_flag){
					if (flag_read_number) {
						byte_damo = StaticByteMessageData.scan_byte;
						flag_read_number = false;
						remove_buttery();
						My_thread_sleep(3000, 0x003);
						Log.i("扫描指令>> = true", ">>>>>>>>>");
					} else if (insert_detect_Data_flag) {
						byte_damo = StaticByteMessageData2.insert_detect_byte;
						remove_buttery();
						Log.i("检测指令 = true", ">>>>>>>>>");
						My_thread_sleep(500, 0x002);
					} else if (responses_detect_Data_flag) {
						byte_damo = StaticByteMessageData2.responses_detect_byte;
						responses_detect_Data_flag = false;
						Log.i("设备确认指令 = true", ">>>>>>>>>");
					} else {
						//充电指令
						charge_buttery();
					}
				} else {
					if (flag_read_number) {
						byte_damo = StaticByteReverseData.scan_reverse_byte;
						flag_read_number = false;
						remove_buttery();
						My_thread_sleep(3000, 0x003);
						Log.i("R扫描指令 = true", ">>>>>>>>>");
					} else if (insert_detect_Data_flag) {
						byte_damo = StaticByteReverseData2.insert_reverse_byte;
						remove_buttery();
						Log.i("R检测指令 = true", ">>>>>>>>>");
						My_thread_sleep(500, 0x002);
					} else if (responses_detect_Data_flag) {
						byte_damo = StaticByteReverseData2.responses_reverse_byte;
						responses_detect_Data_flag = false;
						Log.i("R设备确认指令", ">>>>>>>>>");
					} else {
						charge_buttery();
					}
				}

				try {
					
					trackplayer.write(byte_damo, 0, byte_damo.length);// 往track中写数据
				} catch (Exception e) {
					PlayAudioThread.interrupt();
					break;
				}
			}
		}
	}
**/	
	
	public void remove_buttery(){
		//取消充电
		insert_detect_Data_flag = false;
		insert_detect_sleep = false;
	}
	/**
	 * 充电指令
	 */
	public void charge_buttery(){
		byte[] emptybyte = new byte[2048];
		if (insert_detect_success_flag) {
			byte_damo = emptybyte;//StaticByteReverseData.default_reverse_byte;
			 //Log.i("R充电指令 = true", ">>>>>>>>>");
		} else {
			// 检测指令发送前播放充电波形，500ms后停止。
			if (insert_detect_sleep) {
				byte_damo = emptybyte;//StaticByteReverseData.default_reverse_byte;
				//Log.i("R充电指令222 = true", ">>>>>>>>>");
			} else {
				byte_damo = emptybyte;
				//Log.i("R取消充电 = true", ">>>>>>>>>");
			}
		}
	}
	
	public void My_thread_sleep(final long time, final int message){
		new Thread(new Runnable() {
			public void run() {
				try {
					Thread.sleep(time);
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
				handler.sendEmptyMessage(message);
			}
		}).start();
	}
	/**
	 * 切换音频通道
	 */
	public void track_change(){
		mThreadExitFlag = true;
		try {
			Thread.sleep(200);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		if (AudioTrack_Manager == AudioManager.STREAM_SYSTEM) {
			AudioTrack_Manager = AudioManager.STREAM_MUSIC;
			sendBroadCast(ACTION_AudioTrack_Manager, 0);
			Toast.makeText(MainActivityService.this, "切换到媒体音量", Toast.LENGTH_SHORT).show();
			Log.i("AudioTrack_Manager", "//////AudioManager.STREAM_MUSIC");
		} else if (AudioTrack_Manager == AudioManager.STREAM_MUSIC) {
			AudioTrack_Manager = AudioManager.STREAM_SYSTEM;
			sendBroadCast(ACTION_AudioTrack_Manager, 1);
			Toast.makeText(MainActivityService.this, "切换到系统音量", Toast.LENGTH_SHORT).show();
			Log.i("AudioTrack_Manager", "//////AudioManager.STREAM_SYSTEM");
		}
		stopRecord();
		stopPlayAudio();
		
		start_audio_track();
	}
	
	public SaveShardMessage saveShardMessage;
	/**
	 * MIC和声道检测
	 */
	public void MIC_SOUND_CHECK(){
		if (SaveShardMessage.getReversePolarity(this) == -1)
			MyNative.audioInterface(44100, 0, 0);
		for (int i = audioMaxVolumn; i > audioMaxVolumn / 2; i -= 2) {
			am.setStreamVolume(AudioTrack_Manager, i,
					AudioManager.FLAG_PLAY_SOUND);
			insert_detect_Data_flag = true;
			adaptation_flag = true;
			Log.i("适配W200？？？？？", reversePolarity + "<<" + i);
			try {
				Thread.sleep(500);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
			if (reversePolarity >= 0) {
				adaptation_flag = false;
				MyNative.audioInterface(44100, 0, reversePolarity);
				sendBroadCast(ACTION_MIC_SOUND_CHECK, 0);
				Toast.makeText(MainActivityService.this, "手机MIC检测结果成功", Toast.LENGTH_SHORT).show();
				audioCurrentVolumn = i;
				//saveShardMessage.Sharedvolume(i);
				//saveShardMessage.SharedreversePolarity(reversePolarity);
				//saveShardMessage.Sharedchange_track_flag(true);
				break;
			}
		}
		if (adaptation_flag) {
			for (int j = audioMaxVolumn; j > audioMaxVolumn / 2; j -= 2) {
				am.setStreamVolume(AudioTrack_Manager, j,
						AudioManager.FLAG_PLAY_SOUND);
				insert_detect_Data_flag = true;
				Log.i("适配W200@@@@@", reversePolarity + "<<" + j);
				try {
					Thread.sleep(500);
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
				if (reversePolarity >= 0) {
					adaptation_flag = false;
					sendBroadCast(ACTION_MIC_SOUND_CHECK, 0);
					Toast.makeText(MainActivityService.this, "手机MIC检测结果成功", Toast.LENGTH_SHORT).show();
					MyNative.audioInterface(44100, 0, reversePolarity);
					audioCurrentVolumn = j;
					//saveShardMessage.Sharedvolume(j);
					//saveShardMessage.SharedreversePolarity(reversePolarity);
					//saveShardMessage.Sharedchange_track_flag(false);
					break;
				}
			}
			if (adaptation_flag) {
				am.setStreamVolume(AudioTrack_Manager, audioMaxVolumn,
						AudioManager.FLAG_PLAY_SOUND);
				//saveShardMessage.Sharedchange_track_flag(true);
				//saveShardMessage
				//		.SharedreversePolarity(SaveShardMessage.reversePolarity);
				sendBroadCast(ACTION_MIC_SOUND_CHECK, 1);
				Toast.makeText(MainActivityService.this, "手机MIC检测结果失败", Toast.LENGTH_SHORT).show();
			}
		}
	}
	
	private static WindowManager wm;
	private static WindowManager.LayoutParams params;
	private Button btn_floatView;
	private boolean isAdded = false; // 是否已增加悬浮窗
	public static int window_x;//屏幕宽度
	/**
	 * 创建悬浮窗
	 */
	private void createFloatView() {
		btn_floatView = new Button(getApplicationContext());
        btn_floatView.setText("扫描");
        btn_floatView.setBackgroundResource(R.drawable.button_scan);
        
        wm = (WindowManager) getApplicationContext()
        	.getSystemService(Context.WINDOW_SERVICE);
        params = new WindowManager.LayoutParams();
        // 设置window type
        params.type = WindowManager.LayoutParams.TYPE_SYSTEM_ALERT;
        /*
         * 如果设置为params.type = WindowManager.LayoutParams.TYPE_PHONE;
         * 那么优先级会降低一些, 即拉下通知栏不可见
         */
        params.format = PixelFormat.RGBA_8888; // 设置图片格式，效果为背景透明
        // 设置Window flag
        params.flags = WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL
                              | WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE;
        /*
         * 下面的flags属性的效果形同“锁定”。
         * 悬浮窗不可触摸，不接受任何事件,同时不影响后面的事件响应。
        wmParams.flags=LayoutParams.FLAG_NOT_TOUCH_MODAL
                               | LayoutParams.FLAG_NOT_FOCUSABLE
                               | LayoutParams.FLAG_NOT_TOUCHABLE;
         */
        
        // 设置悬浮窗的长、宽
        params.width = window_x / 5;
        params.height = window_x / 5;
//        params.width = 100;
//        params.height = 100;
        params.gravity=Gravity.LEFT;
        params.x=200;
        params.y=000;
        // 设置悬浮窗的Touch监听
        btn_floatView.setOnTouchListener(new OnTouchListener() {
        	int lastX, lastY, currentX, currentY, startX, startY;
        	int paramX, paramY;
        	
			public boolean onTouch(View v, MotionEvent event) {
				switch(event.getAction()) {
				case MotionEvent.ACTION_DOWN:
					startX = lastX = (int) event.getRawX();
					startY = lastY = (int) event.getRawY();
					paramX = params.x;
					paramY = params.y;
					 btn_floatView.setBackgroundResource(R.drawable.button_scan111);
					break;
				case MotionEvent.ACTION_MOVE:
					int dx = (int) event.getRawX() - lastX;
					int dy = (int) event.getRawY() - lastY;
					params.x = paramX + dx;
					params.y = paramY + dy;
					// 更新悬浮窗位置
			        wm.updateViewLayout(btn_floatView, params);
					break;
				case MotionEvent.ACTION_UP:
					currentX = (int) event.getRawX();// 获取触摸事件触摸位置的原始X坐标
					currentY = (int) event.getRawY();
					if (!(getMoveX_Y(currentX, startX) > 100
							|| getMoveX_Y(currentY, startY) > 100))
						flag_read_number = true;
					else  btn_floatView.setBackgroundResource(R.drawable.button_scan);
				}
				return true;
			}
		});
        wm.addView(btn_floatView, params);
        isAdded = true;
	}
	
	public int getMoveX_Y(int lx, int nx) {
		int leng_x = 0;
		if (lx > nx)
			leng_x = lx - nx;
		else
			leng_x = nx - lx;
		return leng_x;
	}
	
	private void stopPlayAudio() {
		if (null != PlayAudioThread) {
			start_play_audio = false;
			trackplayer.stop();
			trackplayer.release();
			trackplayer = null;
			PlayAudioThread.interrupt();
			PlayAudioThread = null;
		}
	}

	private void stopRecord() {
		if (null != audioRecord) {
			isRecording = false;
			audioRecord.stop();
			audioRecord.release();
			audioRecord = null;
			recordingThread.interrupt();
			recordingThread = null;
		}
	}
	@Override
	public void onDestroy() {
		onstop();
		Log.i("stop","销毁服务");
		super.onDestroy();
//		onstop();
	}
	private void onstop() {
		Log.i("stop","停止服务");
		stopRecord();
		stopPlayAudio();
		unregisterReceiver(HeadsetPlugReceiver);		
		mThreadExitFlag = true;
		headset_flag = false;
		isRecording = false; 
		insert_detect_success_flag = false;
		stopSelf();
	}
	
	private void copyWaveFile(String inFilename, String outFilename) {
		FileInputStream in = null;
		FileOutputStream out = null;
		long totalAudioLen = 0;
		long totalDataLen = totalAudioLen + 36;
		long longSampleRate = frequency;
		int channels = 1;
		long byteRate = RECORDER_BPP * frequency * channels / 8;
		byte[] data = new byte[recBufSize];

		try {
			in = new FileInputStream(inFilename);
			out = new FileOutputStream(outFilename);
			totalAudioLen = in.getChannel().size();
			totalDataLen = totalAudioLen + 36;

			WriteWaveFileHeader(out, totalAudioLen, totalDataLen,
					longSampleRate, channels, byteRate);

			while (in.read(data) != -1) {
				out.write(data);
			}
			in.close();
			out.close();
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	private void WriteWaveFileHeader(FileOutputStream out, long totalAudioLen,
			long totalDataLen, long longSampleRate, int channels, long byteRate)
			throws IOException {
		byte[] header = new byte[44];
		header[0] = 'R'; // RIFF/WAVE header
		header[1] = 'I';
		header[2] = 'F';
		header[3] = 'F';
		header[4] = (byte) (totalDataLen & 0xff);
		header[5] = (byte) ((totalDataLen >> 8) & 0xff);
		header[6] = (byte) ((totalDataLen >> 16) & 0xff);
		header[7] = (byte) ((totalDataLen >> 24) & 0xff);
		header[8] = 'W';
		header[9] = 'A';
		header[10] = 'V';
		header[11] = 'E';
		header[12] = 'f'; // 'fmt ' chunk
		header[13] = 'm';
		header[14] = 't';
		header[15] = ' ';
		header[16] = 16; // 4 bytes: size of 'fmt ' chunk
		header[17] = 0;
		header[18] = 0;
		header[19] = 0;
		header[20] = 1; // format = 1
		header[21] = 0;
		header[22] = (byte) channels;
		header[23] = 0;
		header[24] = (byte) (longSampleRate & 0xff);
		header[25] = (byte) ((longSampleRate >> 8) & 0xff);
		header[26] = (byte) ((longSampleRate >> 16) & 0xff);
		header[27] = (byte) ((longSampleRate >> 24) & 0xff);
		header[28] = (byte) (byteRate & 0xff);
		header[29] = (byte) ((byteRate >> 8) & 0xff);
		header[30] = (byte) ((byteRate >> 16) & 0xff);
		header[31] = (byte) ((byteRate >> 24) & 0xff);
		header[32] = (byte) (1 * 16 / 8); // block align
		header[33] = 0;
		header[34] = RECORDER_BPP; // bits per sample
		header[35] = 0;
		header[36] = 'd';
		header[37] = 'a';
		header[38] = 't';
		header[39] = 'a';
		header[40] = (byte) (totalAudioLen & 0xff);
		header[41] = (byte) ((totalAudioLen >> 8) & 0xff);
		header[42] = (byte) ((totalAudioLen >> 16) & 0xff);
		header[43] = (byte) ((totalAudioLen >> 24) & 0xff);
		out.write(header, 0, 44);
	}
	private static final int RECORDER_BPP = 16;
}
