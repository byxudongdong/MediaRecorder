package com.android.xiong.mediarecordertest;

public class MyNative {
	static
	{
	System.loadLibrary( "MediaRecorderTest" );
	}
	public native static int audioInterface(int sample_rate, int data_rate,int reversePolarity);
	public native static int cToJava(byte[] adc_value,int adc_value_length, byte[] code_data);
}
