package com.example.listviewrefresh;



import com.android.xiong.mediarecordertest.MySqlite;
import com.android.xiong.mediarecordertest.R;

import android.app.Activity;
import android.app.Dialog;
import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.AbsListView;
import android.widget.ListView;


public class ShowDialogDele {
	public Context context;
	public ListView mListView;
	public PrivateListingAdapter mAdapter;
	public SQLiteDatabase database;

	public ShowDialogDele() {
		// TODO Auto-generated constructor stub
	}

	public ShowDialogDele(Context context,
						  ListView mListView,
						 PrivateListingAdapter mAdapter,
						 SQLiteDatabase database) {
		super();
		this.context = context;
		this.mListView = mListView;
		this.mAdapter = mAdapter;
		this.database = database;
	}




	public void showDialog() {
		View view = ((Activity) context).getLayoutInflater().inflate(R.layout.photo_choose_dialog,
				null);
		final Dialog dialog = new Dialog(context,
				R.style.transparentFrameWindowStyle);
		dialog.setContentView(view, new AbsListView.LayoutParams(AbsListView.LayoutParams.FILL_PARENT,
				AbsListView.LayoutParams.WRAP_CONTENT));
		Window window = dialog.getWindow();
		// 设置显示动画
		window.setWindowAnimations(R.style.main_menu_animstyle);
		WindowManager.LayoutParams wl = window.getAttributes();
		wl.x = 0;
		wl.y = ((Activity) context).getWindowManager().getDefaultDisplay().getHeight();
		// 以下这两句是为了保证按钮可以水平满屏
		wl.width = ViewGroup.LayoutParams.MATCH_PARENT;
		wl.height = ViewGroup.LayoutParams.WRAP_CONTENT;
		// 设置显示位置
		dialog.onWindowAttributesChanged(wl);
		// 设置点击外围解散
		dialog.setCanceledOnTouchOutside(true);
		dialog.show();
		view.findViewById(R.id.cancle_button).setOnClickListener(
				new View.OnClickListener() {
					@Override
					public void onClick(View v) {
						dialog.cancel();
					}
				});
		view.findViewById(R.id.delete_button).setOnClickListener(
				new View.OnClickListener() {
					@Override
					public void onClick(View v) {
						database.execSQL("drop table if exists Shoujian");
						database.execSQL(MySqlite.SHOU_JIAN);
						mAdapter.deletData();
						mListView.setAdapter(mAdapter);
						dialog.cancel();
					}
				});
	}
}
