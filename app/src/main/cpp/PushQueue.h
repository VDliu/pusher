
/**
  *2019/8/19.
  *
 */

#ifndef LIVEPUSHER_PUSHQUEUE_H
#define LIVEPUSHER_PUSHQUEUE_H

#include "queue"
#include "pthread.h"
#include "AndroidLog.h"

extern "C" {
#include "librtmp/rtmp.h"
};

class PushQueue {
public:
    std::queue<RTMPPacket *> queuePacket;
    pthread_mutex_t mutex;
    pthread_cond_t cond;
public:
    PushQueue();

    ~PushQueue();

    int putPacket(RTMPPacket *packet);

    RTMPPacket* getPacket();

    void clearQueue();

    void notifyQueue();


};


#endif //LIVEPUSHER_PUSHQUEUE_H
