#include <jni.h>
#include "stdio.h"
#include "string.h"
#include "comm2.cpp"
#include "com_android_xiong_mediarecordertest_MyNative.h"

JNIEXPORT jint JNICALL Java_com_android_xiong_mediarecordertest_MyNative_cToJava(
		JNIEnv *env, jclass cls, jbyteArray array, jint array_length,
		jbyteArray code_data) {
	//int arraylen   =  (int) env->GetArrayLength(array); //获取长度
	jbyte * olddata = (jbyte*) env->GetByteArrayElements(array, 0);
	jbyte * codedata = (jbyte*) env->GetByteArrayElements(code_data, 0);

	jshort *x;
	int n, ret;
	jbyte *data;
	x = (jshort *) olddata;
	n = array_length / 2;

	ret = audioInterface_wav2digital(x, n, codedata);
	if (ret > 0) {
		audioInterface_clearCount();
	}

	env->ReleaseByteArrayElements(array, olddata, 0);
	env->ReleaseByteArrayElements(code_data, codedata, 0);
	return ret;

}

JNIEXPORT jint JNICALL Java_com_android_xiong_mediarecordertest_MyNative_audioInterface(
		JNIEnv *env, jclass cls, jint sample_rate, jint data_rate,
		jint reversePolarity) {
	audioInterface_init(sample_rate, data_rate);
	if (reversePolarity == 0)
		audioInterface_reversePolarity(0);
	else
		audioInterface_reversePolarity(1);
	return 1;
}
