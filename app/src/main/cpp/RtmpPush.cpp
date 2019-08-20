
/**
  *2019/8/20.
  *
 */

#include "RtmpPush.h"

RtmpPush::RtmpPush(char *url) {
    this->url = (char *) malloc(512);
    strcpy(this->url, url);
    pushQueue = new PushQueue();
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
        goto end;

    }

    //连接流
    if (RTMP_ConnectStream(rtmpPush->rtmp, 0)) {
        goto end;
        LOGE("can not connect stream");
    }

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
    pthread_create(&push_thread, NULL, pushCallBack, this);
}
