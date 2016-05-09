package com.example.listviewrefresh;

/**
 * @file XListViewHeader.java
 * @description XListView's header
 */

import com.android.xiong.mediarecordertest.R;

import android.content.Context;
import android.util.AttributeSet;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.RotateAnimation;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.TextView;

public class XListViewHeader extends LinearLayout {
	private RelativeLayout mContainer = null;	
	//	private LinearLayout mContainer = null;
	private ImageView mArrowImageView = null;
	private ProgressBar mProgressBar = null;
	private TextView mHintTextView = null;
	private int mState = STATE_NORMAL;

	private Animation mRotateUpAnim = null;
	private Animation mRotateDownAnim = null;
	
	private final int ROTATE_ANIM_DURATION = 180;
	
	public final static int STATE_NORMAL = 0;
	public final static int STATE_READY = 1;
	public final static int STATE_REFRESHING = 2;

	public XListViewHeader(Context context) {
		super(context);
		initView(context);
	}
	public XListViewHeader(Context context, AttributeSet attrs) {
		super(context, attrs);
		initView(context);
	}

	/**
	 * 得到控件，初始化动画变量
	 */
	private void initView(Context context) {
		 // 初始情况，设置下拉刷新view高度为0
		LayoutParams lp = new LayoutParams(
				LayoutParams.MATCH_PARENT, 0);
//		mContainer = (LinearLayout) LayoutInflater.from(context).inflate(
//				R.layout.xlistview_header, null);
		mContainer = (RelativeLayout) LayoutInflater.from(context).inflate(
				R.layout.xlistview_header, null);
		addView(mContainer, lp);
		setGravity(Gravity.BOTTOM);

		mArrowImageView = (ImageView)findViewById(R.id.xlistview_header_arrow);
		mHintTextView = (TextView)findViewById(R.id.xlistview_header_hint_textview);
		mProgressBar = (ProgressBar)findViewById(R.id.xlistview_header_progressbar);
		/***以0.0的位置为起始点，逆时针旋转180度，围绕自身的中心点*/
		mRotateUpAnim = new RotateAnimation(0.0f, -180.0f,
				Animation.RELATIVE_TO_SELF, 0.5f, Animation.RELATIVE_TO_SELF,
				0.5f);
		mRotateUpAnim.setDuration(ROTATE_ANIM_DURATION);
		mRotateUpAnim.setFillAfter(true);
		/*** 以逆时针180度的位置为起始点，顺时针旋转到0.0的位置，围绕自身的中心点*/
		mRotateDownAnim = new RotateAnimation(-180.0f, 0.0f,
				Animation.RELATIVE_TO_SELF, 0.5f, Animation.RELATIVE_TO_SELF,
				0.5f);
		mRotateDownAnim.setDuration(ROTATE_ANIM_DURATION);
		mRotateDownAnim.setFillAfter(true);
	}

	/**
	 * 根据不同的状态得到不同的处理
	 */
	public void setState(int state) {
		if (state == mState) return ;
		
		if (state == STATE_REFRESHING) {	
			mArrowImageView.clearAnimation();
			mArrowImageView.setVisibility(View.INVISIBLE);
			mProgressBar.setVisibility(View.VISIBLE);
		} else {	
			mArrowImageView.setVisibility(View.VISIBLE);
			mProgressBar.setVisibility(View.INVISIBLE);
		}
		
		switch(state){
		case STATE_NORMAL:
			if (mState == STATE_READY) {
				mArrowImageView.startAnimation(mRotateDownAnim);
			}
			if (mState == STATE_REFRESHING) {
				mArrowImageView.clearAnimation();
			}
			mHintTextView.setText(R.string.xlistview_header_hint_normal);
			break;
		case STATE_READY:
			if (mState != STATE_READY) {
				mArrowImageView.clearAnimation();
				//开始动画，逆时针旋转180度
				mArrowImageView.startAnimation(mRotateUpAnim);
				mHintTextView.setText(R.string.xlistview_header_hint_ready);
			}
			break;
		case STATE_REFRESHING:
			mHintTextView.setText(R.string.xlistview_header_hint_loading);
			break;
			default:
		}
		
		mState = state;
	}
	/**
	 * 将header.xml的高度设为参数的高度
	 * @param height
	 */
	public void setVisiableHeight(int height) {
		if (height < 0)
			height = 0;
		LayoutParams lp = (LayoutParams) mContainer
				.getLayoutParams();
		lp.height = height;
		mContainer.setLayoutParams(lp);
	}
	/**
	 * 得到header.xml的高度
	 * @return
	 */
	public int getVisiableHeight() {

		return mContainer.getHeight();
	}

}
