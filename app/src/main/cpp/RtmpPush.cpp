
/**
  *2019/8/20.
  *
 */

#include "RtmpPush.h"

RtmpPush::RtmpPush(const char *url,CallJava *callJava) {
    this->url = (char *) malloc(512);
    strcpy(this->url, url);
    pushQueue = new PushQueue();
    this->callJava = callJava;

}

RtmpPush::~RtmpPush() {
    if (this->url != NULL) {
        free(this->url);
        this->url = NULL;
    }

    if (pushQueue != NULL) {
        delete pushQueue;
        pushQueue = NULL;
    }

}

void *pushCallBack(void *data) {
    RtmpPush *rtmpPush = (RtmpPush *) data;
    rtmpPush->rtmp = RTMP_Alloc();
    RTMP_Init(rtmpPush->rtmp);
    rtmpPush->rtmp->Link.timeout = 10;
    rtmpPush->rtmp->Link.lFlags |= RTMP_LF_LIVE;

    //设置url
    RTMP_SetupURL(rtmpPush->rtmp, rtmpPush->url);
    //设置可写
    RTMP_EnableWrite(rtmpPush->rtmp);

    if (!RTMP_Connect(rtmpPush->rtmp, NULL)) {
        //连接失败
        LOGE("can not connect url");
        rtmpPush->callJava->onConnectFail("can not connect the url");
        goto end;

    }

    //连接流
    if (RTMP_ConnectStream(rtmpPush->rtmp, 0)) {
        rtmpPush->callJava->onConnectFail("can not connect the stream of service");
        goto end;
    }

    rtmpPush->callJava->onConnectsuccess();


    //循环推流
    while (true) {

    }


    end:
        RTMP_Close(rtmpPush->rtmp);
        RTMP_Free(rtmpPush->rtmp);
        rtmpPush->rtmp = NULL;
    pthread_exit(&rtmpPush->push_thread);

}

void RtmpPush::init() {
    callJava->onConnectint(WL_THREAD_MAIN);
    pthread_create(&push_thread, NULL, pushCallBack, this);
}
