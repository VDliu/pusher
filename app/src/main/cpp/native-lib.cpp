#include <jni.h>
#include "RtmpPush.h"

JNIEXPORT void JNICALL
Java_pictrue_com_reiniot_livepusher_push_PushVideo_initPush(JNIEnv *env, jobject instance,
                                                            jstring pushUrl_) {
    const char *pushUrl = env->GetStringUTFChars(pushUrl_, 0);

    // TODO
    RtmpPush *rtmpPush = new RtmpPush(pushUrl);
    rtmpPush->init();


    env->ReleaseStringUTFChars(pushUrl_, pushUrl);
}
