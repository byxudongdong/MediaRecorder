package com.android.xiong.mediarecordertest;

import android.app.Activity;
import android.content.ContentValues;
import android.content.Context;
import android.content.SharedPreferences;
import android.database.sqlite.SQLiteDatabase;

public class SaveShardMessage {
	public SQLiteDatabase database;
	public Context context;
	public SaveShardMessage(SQLiteDatabase database,Context context) {
		super();
		this.database = database;
		this.context = context;
	}
	
	/**
	 * 数据库
	 * @param accept_msg
	 */
	public void saveShardAcceptMessage(String accept_msg) {
		ContentValues contentvalues = new ContentValues();
		contentvalues.put("acceptMessageTime", accept_msg);
		database.insert("Shoujian", null, contentvalues);
	}
	
	
	/**
	 * 左右声道
	 * 
	 * @param change_track_flag
	 */
	public void Sharedchange_track_flag(boolean change_track_flag) {
		SharedPreferences settings = context.getSharedPreferences(
				"change_track_flag", Activity.MODE_PRIVATE);
		SharedPreferences.Editor editor = settings.edit();

		editor.putBoolean("change_track_flag", change_track_flag);
		editor.commit();
	}

	/**
	 * 手机MIC极性
	 * 
	 * @param reversePolarity
	 */
	public void SharedreversePolarity(int reversePolarity) {
		SharedPreferences settings = context.getSharedPreferences(
				"reversePolarity", Activity.MODE_PRIVATE);
		SharedPreferences.Editor editor = settings.edit();

		editor.putInt("reversePolarity", reversePolarity);
		editor.commit();
	}

	/**
	 * 手机音量
	 * 
	 * @param volume
	 */
	public void Sharedvolume(int volume) {
		SharedPreferences settings = context.getSharedPreferences(
				"volume", Activity.MODE_PRIVATE);
		SharedPreferences.Editor editor = settings.edit();

		editor.putInt("volume", volume);
		editor.commit();
	}


	/**
	 * 手机极性0，1
	 */
	public static int reversePolarity = 0;
	public static int getReversePolarity(Context activity){
		SharedPreferences acceptMessage = activity.getSharedPreferences("reversePolarity", Activity.MODE_PRIVATE);
		reversePolarity = acceptMessage.getInt("reversePolarity", reversePolarity);
		return reversePolarity;
	}
	
	/**
	 * 左右声道默认true，false
	 */
	public static boolean change_track_flag = true;
	public static boolean getChange_track_flag(Context activity){
		SharedPreferences acceptMessage = activity.getSharedPreferences("change_track_flag", Activity.MODE_PRIVATE);
		change_track_flag = acceptMessage.getBoolean("change_track_flag", true);
		return change_track_flag;

	}
	/**
	 * 手机音量
	 * 
	 */
	public static int volume = 13;
	public static int getMotion_volume(Context activity){
		SharedPreferences acceptMessage = activity.getSharedPreferences("volume", Activity.MODE_PRIVATE);
		volume = acceptMessage.getInt("volume", volume);
		return volume;
	}
}
