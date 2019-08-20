#include <jni.h>
#include "RtmpPush.h"
#include "CallJava.h"


RtmpPush *rtmpPush = NULL;
CallJava *callJava = NULL;
JavaVM *javaVM = NULL;

JNIEXPORT void JNICALL
Java_pictrue_com_reiniot_livepusher_push_PushVideo_initPush(JNIEnv *env, jobject instance,
                                                            jstring pushUrl_) {
    const char *pushUrl = env->GetStringUTFChars(pushUrl_, 0);

    // TODO
    callJava = new CallJava(javaVM,env,&instance);
    rtmpPush = new RtmpPush(pushUrl,callJava);
    rtmpPush->init();


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

