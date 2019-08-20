
/**
  *2019/8/19.
  *
 */

#include "PushQueue.h"

PushQueue::PushQueue() {
    pthread_mutex_init(&mutex, NULL);
    pthread_cond_init(&cond, NULL);

}

PushQueue::~PushQueue() {
    notifyQueue();
    clearQueue();
    pthread_mutex_destroy(&mutex);
    pthread_cond_destroy(&cond);

}

int PushQueue::putPacket(RTMPPacket *packet) {
    pthread_mutex_lock(&mutex);
    queuePacket.push(packet);
    pthread_cond_signal(&cond);
    pthread_mutex_unlock(&mutex);
    return 0;
}

RTMPPacket *PushQueue::getPacket() {
    pthread_mutex_lock(&mutex);
    RTMPPacket *p;
    if (!queuePacket.empty()) {
        p = queuePacket.front();
        queuePacket.pop();
    } else {
        pthread_cond_wait(&cond, NULL);
    }
    pthread_mutex_unlock(&mutex);

    return p;
}

void PushQueue::clearQueue() {
    pthread_mutex_lock(&mutex);
    while (true) {
        if (queuePacket.empty()) {
            break;
        }

        RTMPPacket *p = queuePacket.front();
        queuePacket.pop();
        RTMPPacket_Free(p);
        p = NULL;
    }
    pthread_mutex_unlock(&mutex);

}

void PushQueue::notifyQueue() {
    pthread_mutex_lock(&mutex);
    pthread_cond_signal(&cond);
    pthread_mutex_unlock(&mutex);
}
