package pictrue.com.reiniot.livepusher.push;

public interface ConnectListenr {

    void onConnecting();

    void onConnectSuccess();

    void onConnectFail(String msg);

}
