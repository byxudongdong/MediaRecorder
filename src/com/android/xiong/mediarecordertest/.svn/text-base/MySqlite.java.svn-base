package com.android.xiong.mediarecordertest;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

public class MySqlite extends SQLiteOpenHelper {
	public static String SHOU_JIAN = "CREATE TABLE Shoujian("
			+ "_id integer primary key autoincrement,"
			+ "acceptMessageTime String)";

	public MySqlite(Context context) {
		super(context, "haozhuang.db", null, 1);
	}

	@Override
	public void onCreate(SQLiteDatabase haozhuang) {
		haozhuang.execSQL(SHOU_JIAN);
	}

	@Override
	public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {

	}

	public boolean deleteDatabase(Context context) {
		return context.deleteDatabase("haozhuang.db");
	}

}
