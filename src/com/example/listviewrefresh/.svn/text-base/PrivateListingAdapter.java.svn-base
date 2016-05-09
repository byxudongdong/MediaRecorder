package com.example.listviewrefresh;

import java.util.List;

import com.android.xiong.mediarecordertest.R;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

public class PrivateListingAdapter extends BaseAdapter {
	private Context mContext;
	private LayoutInflater mInflater;
	private List<String> list;

	public PrivateListingAdapter(Context context) {
		// TODO Auto-generated constructor stub
		mContext = context;
		mInflater = LayoutInflater.from(mContext);
	}

	public void setListData(List<String> list){
		this.list = list;
	}

	public void deletData(){
		list.clear();
	}
	@Override
	public int getCount() {
		return list.size();
	}

	@Override
	public Object getItem(int position) {
		return list.get(position);
	}

	@Override
	public long getItemId(int position) {
		return position;
	}

	int i = 0;

	@Override
	public View getView(int position, View convertView, ViewGroup parent) {
		// INDEX = position;
		ViewHolder holder = null;
		View itemView = mInflater.inflate(R.layout.list_item, null);

		holder = new ViewHolder(itemView);

		String item = list.get(position);
		
		holder.text_data.setText(item);
		notifyDataSetChanged();
		return itemView;
	}

	private static class ViewHolder {
//		public CheckBox msg;
		public TextView text_data;

		ViewHolder(View view) {
			text_data = (TextView) view.findViewById(R.id.text_data);
		}
	}

}
