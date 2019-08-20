
/**
  *2019/8/20.
  *
 */

#include "RtmpPush.h"

RtmpPush::RtmpPush(const char *url, CallJava *callJava) {
    this->url = (char *) malloc(512);
    strcpy(this->url, url);
    pushQueue = new PushQueue();
    this->callJava = callJava;
    startPush = false;
    startTime = 0;

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
    rtmpPush->startPush = true;
    rtmpPush->startTime = RTMP_GetTime();

    //循环推流
    while (true) {

        if (!rtmpPush->startPush) {
            break;
        }

        RTMPPacket *packet = rtmpPush->pushQueue->getPacket();
        if (packet != NULL) {
            //第三个参数设置为1表示实时
            int res = RTMP_SendPacket(rtmpPush->rtmp, packet, 1);
            LOGE("push res ----%d", res);
            RTMPPacket_Free(packet);
            packet = NULL;
        }
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

void RtmpPush::pushSpsPps(char *sps, int sps_len, char *pps, int pps_len) {
    int bodysize = sps_len + pps_len + 16;
    RTMPPacket *packet = static_cast<RTMPPacket *>(malloc(sizeof(RTMPPacket)));
    RTMPPacket_Alloc(packet, bodysize);
    RTMPPacket_Reset(packet);

    char *body = packet->m_body;

    int i = 0;
    //添加sps pps头部信息 设置sps pps信息帧为关键帧 这种数据不是视频数据

    body[i++] = 0x17;

    body[i++] = 0x00; //为0表示配置
    body[i++] = 0x00;
    body[i++] = 0x00;
    body[i++] = 0x00;

    body[i++] = 0x01;
    body[i++] = sps[1];
    body[i++] = sps[2];
    body[i++] = sps[3];

    body[i++] = 0xFF;

    body[i++] = 0xE1;
    body[i++] = (sps_len >> 8) & 0xff;
    body[i++] = sps_len & 0xff;
    memcpy(&body[i], sps, sps_len);
    i += sps_len;

    body[i++] = 0x01;
    body[i++] = (pps_len >> 8) & 0xff;
    body[i++] = pps_len & 0xff;
    memcpy(&body[i], pps, pps_len);

    packet->m_packetType = RTMP_PACKET_TYPE_VIDEO;
    packet->m_nBodySize = bodysize;
    //这种数据不是视频数据,所以不需要显示 时间设置为0即可
    packet->m_nTimeStamp = 0;
    packet->m_hasAbsTimestamp = 0;
    packet->m_nChannel = 0x04; //音视频的channel
    packet->m_headerType = RTMP_PACKET_SIZE_MEDIUM;
    packet->m_nInfoField2 = rtmp->m_stream_id;
    pushQueue->putPacket(packet);

}

void RtmpPush::pushVideo(char *data, int len, bool isKeyFrame) {
    int bodysize = len + 9;
    RTMPPacket *packet = static_cast<RTMPPacket *>(malloc(sizeof(RTMPPacket)));
    RTMPPacket_Alloc(packet, bodysize);
    RTMPPacket_Reset(packet);

    char *body = packet->m_body;

    int i = 0;
    //添加sps pps头部信息 1 表示关键帧  2表示非关键帧 7 表示avc codeid
    if (isKeyFrame) {
        body[i++] = 0x17;
    } else {
        body[i++] = 0x27;
    }

    body[i++] = 0x01; //表示推送nalu单元
    body[i++] = 0x00;
    body[i++] = 0x00;
    body[i++] = 0x00;

    body[i++] = (len >> 24) & 0xff;
    body[i++] = (len >> 16) & 0xff;
    body[i++] = (len >> 8) & 0xff;
    body[i++] = len & 0xff;
    memcpy(&body[i], data, len);

    packet->m_packetType = RTMP_PACKET_TYPE_VIDEO;
    packet->m_nBodySize = bodysize;
    //视频数据 ，需要设置时间
    packet->m_nTimeStamp = RTMP_GetTime() - startTime;
    packet->m_nTimeStamp = 0;
    packet->m_hasAbsTimestamp = 0;
    packet->m_nChannel = 0x04;
    packet->m_headerType = RTMP_PACKET_SIZE_LARGE;
    packet->m_nInfoField2 = rtmp->m_stream_id;

    pushQueue->putPacket(packet);

}

//音频 头信息加2个字节
void RtmpPush::pushAudio(char *data, int len) {
    int bodysize = len + 2;
    RTMPPacket *packet = static_cast<RTMPPacket *>(malloc(sizeof(RTMPPacket)));
    RTMPPacket_Alloc(packet, bodysize);
    RTMPPacket_Reset(packet);

    char *body = packet->m_body;
    body[0] = 0xAF;
    body[1] = 0x01;


    //添加body数据
    memcpy(&body[2], data, len);


    packet->m_packetType = RTMP_PACKET_TYPE_AUDIO;
    packet->m_nBodySize = bodysize;
    //视频数据 ，需要设置时间
    packet->m_nTimeStamp = RTMP_GetTime() - startTime;
    packet->m_nTimeStamp = 0;
    packet->m_hasAbsTimestamp = 0;
    packet->m_nChannel = 0x04;
    packet->m_headerType = RTMP_PACKET_SIZE_LARGE;
    packet->m_nInfoField2 = rtmp->m_stream_id;

    pushQueue->putPacket(packet);
}

void RtmpPush::stop() {
    startPush = false;
    pushQueue->notifyQueue();
    //等待线程结束
    pthread_join(push_thread, NULL);
}
