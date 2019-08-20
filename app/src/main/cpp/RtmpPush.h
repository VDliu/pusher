
/**
  *2019/8/20.
  *
 */

#ifndef LIVEPUSHER_RTMPPUSH_H
#define LIVEPUSHER_RTMPPUSH_H

#include <malloc.h>
#include <cstring>
#include "PushQueue.h"
#include "pthread.h"
#include "CallJava.h"

extern "C" {
#include "librtmp/rtmp.h"
};


class RtmpPush {
public:
    RTMP *rtmp = NULL;
    char *url = NULL;
    PushQueue *pushQueue = NULL;
    pthread_t push_thread;
    CallJava *callJava = NULL;
    bool startPush;
    long startTime;
public:
    RtmpPush(const char *url,CallJava *callJava);

    ~RtmpPush();

    void init();

    void pushSpsPps(char *sps,int spsLen,char*pps,int ppsLen);

    void pushVideo(char *data,int len, bool isKeyFrame);

};


#endif //LIVEPUSHER_RTMPPUSH_H
