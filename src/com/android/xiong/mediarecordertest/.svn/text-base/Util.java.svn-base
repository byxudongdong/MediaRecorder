package com.android.xiong.mediarecordertest;

import java.util.ArrayList;
import java.util.List;

import android.app.Activity;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

public class Util {
	private SQLiteDatabase database;
	public Util(SQLiteDatabase database) {
		// TODO Auto-generated constructor stub
		this.database = database;
	}

	List<String> users=new ArrayList<String>();


	public  List<String> getShoujianData(SQLiteDatabase database,int current_num, boolean pullUP2lod){
		int index_start = current_num;
		int count = 10;
		users.clear();
		if(pullUP2lod){
			if(current_num >= 0 && current_num < 10){
				index_start = 0;
				count = current_num;
			}
			else{
				index_start = current_num - 10;
			}
		}else{
			if(current_num > 10){
				index_start = current_num - 10;
			}else{
				index_start = 0;
				count = current_num;
			}
		}
		Cursor cursor =database.rawQuery("select * from Shoujian order by _id asc limit ?,?", new String[]{String.valueOf(index_start), String.valueOf(count)});
		while(cursor.moveToNext()){
			int uid2=cursor.getInt(cursor.getColumnIndex("_id"));
			String arriveMessageTime=cursor.getString(cursor.getColumnIndex("acceptMessageTime"));
			
			String user=arriveMessageTime;
			
			
			users.add(user.replaceAll(" ;", ""));
		}
		return users;
	}
	
	
	
	
	
	
	/**
	 * 获取数据库条码数量
	 * @param actionBarName
	 * @return
	 */

	public long getCount(){
		
		Cursor cursor =database.rawQuery("select count(*) from " + "Shoujian", null);
		cursor.moveToFirst();
		long reslut=cursor.getLong(0);
		return reslut;
	}
	
	public static byte[] hex2byte(byte[] b) {
		if ((b.length % 2) != 0) {
			throw new IllegalArgumentException("长度不是偶数");
		}
		byte[] b2 = new byte[b.length / 2];
		for (int n = 0; n < b.length; n += 2) {
			String item = new String(b, n, 2);
			// 两位一组，表示一个字节,把这样表示的16进制字符串，还原成一个进制字节
			b2[n / 2] = (byte) Integer.parseInt(item, 16);
		}
		b = null;
		return b2;
	}

	
	/**
	 * 获取电量
	 * @param string
	 * @return
	 */
	public static String getPowerNumberString(String string) {
		String Current = string.replaceAll(" ", "");

		char[] chars = Current.toCharArray();
		String number = "";
		for (int i = 8; i < chars.length - 4; i++) {
			number += chars[i];
		}
		return number;
	}
	
	/**
	 * 获得校验后的条码
	 * 
	 * @param string
	 * @return
	 */
	public static String getCurrentNumberString(String string) {
		String Current = string.replaceAll(" ", "");
		char[] chars = Current.toCharArray();
		String number = "";
		for (int i = 8; i < chars.length - 6; i++) {
			number += chars[i];
		}
		return toStringHex1(number);
	}

	public static String toStringHex1(String s) {
		byte[] baKeyword = new byte[s.length() / 2];
		for (int i = 0; i < baKeyword.length; i++) {
			try {
				baKeyword[i] = (byte) (0xff & Integer.parseInt(
						s.substring(i * 2, i * 2 + 2), 16));
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
		try {
			s = new String(baKeyword, "ASCII");
		} catch (Exception e1) {
			e1.printStackTrace();
		}
		return s;
	}

	/**
	 * 校验条码信息
	 * 
	 * @param string
	 * @return
	 */
	public static boolean checkCurrentNumber(String string) {
		String[] strs = string.split(" ");
		int[] numbers = new int[strs.length];
		for (int i = 0; i < strs.length; i++) {
			int integer = Integer.parseInt(strs[i], 16);
			numbers[i] = integer;
		}
		int resout = 0;
		// resout ——》数据段 ； numbers[strs.length - 2]——》校验位 ； numbers[1] ——》帧长度；
		for (int i = 1; i < strs.length - 2; i++) {
			resout = resout + numbers[i];
		}
		if (strs.length - 4 == numbers[1]
				&& (resout - numbers[strs.length - 2]) % 16 == 0) {

			return true;
		}
		return false;
	}
}
