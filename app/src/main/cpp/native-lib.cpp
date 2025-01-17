#include <jni.h>
#include "RtmpPush.h"
#include "CallJava.h"


RtmpPush *rtmpPush = NULL;
CallJava *callJava = NULL;
JavaVM *javaVM = NULL;
bool exit_push = true;

JNIEXPORT void JNICALL
Java_pictrue_com_reiniot_livepusher_push_PushVideo_initPush(JNIEnv *env, jobject instance,
                                                            jstring pushUrl_) {
    const char *pushUrl = env->GetStringUTFChars(pushUrl_, 0);

    // TODO
    if (callJava == NULL) {
        exit_push = false;
        callJava = new CallJava(javaVM, env, &instance);
        rtmpPush = new RtmpPush(pushUrl, callJava);
        rtmpPush->init();
    }


    env->ReleaseStringUTFChars(pushUrl_, pushUrl);
}

extern "C"
JNIEXPORT jint JNICALL JNI_OnLoad(JavaVM *vm, void *reserved) {
    javaVM = vm;
    JNIEnv *env;
    if (vm->GetEnv((void **) &env, JNI_VERSION_1_4) != JNI_OK) {
        if (LOG_SHOW) {
            LOGE("GetEnv failed!");
        }
        return -1;
    }
    return JNI_VERSION_1_4;
}

extern "C"
JNIEXPORT void JNICALL
Java_pictrue_com_reiniot_livepusher_push_PushVideo_pushSpsPps(JNIEnv *env, jobject instance,
                                                              jbyteArray sps_, jint spslen,
                                                              jbyteArray pps_, jint ppsLen) {
    jbyte *sps = env->GetByteArrayElements(sps_, NULL);
    jbyte *pps = env->GetByteArrayElements(pps_, NULL);

    if (rtmpPush != NULL && !exit_push) {
        rtmpPush->pushSpsPps((char *) sps, spslen, (char *) pps, ppsLen);
    }

    env->ReleaseByteArrayElements(sps_, sps, 0);
    env->ReleaseByteArrayElements(pps_, pps, 0);
}


extern "C"
JNIEXPORT void JNICALL
Java_pictrue_com_reiniot_livepusher_push_PushVideo_pushVideo(JNIEnv *env, jobject instance,
                                                             jbyteArray avc_, jint len,
                                                             jboolean isKeyFame) {
    jbyte *avc = env->GetByteArrayElements(avc_, NULL);

    if (rtmpPush != NULL && exit_push) {
        rtmpPush->pushVideo((char *) avc, len, isKeyFame);
    }

    env->ReleaseByteArrayElements(avc_, avc, 0);
}extern "C"
JNIEXPORT void JNICALL
Java_pictrue_com_reiniot_livepusher_push_PushVideo_pushAudio(JNIEnv *env, jobject instance,
                                                             jbyteArray data_, jint len) {
    jbyte *data = env->GetByteArrayElements(data_, NULL);

    if (rtmpPush != NULL && exit_push) {
        rtmpPush->pushAudio((char *) data, len);
    }

    env->ReleaseByteArrayElements(data_, data, 0);
}extern "C"
JNIEXPORT void JNICALL
Java_pictrue_com_reiniot_livepusher_push_PushVideo_pushStop(JNIEnv *env, jobject instance) {

    if (rtmpPush != NULL) {
        exit_push = true;
        rtmpPush->stop();
        delete (rtmpPush);
        delete (callJava);
        rtmpPush = NULL;
        callJava = NULL;
    }
}