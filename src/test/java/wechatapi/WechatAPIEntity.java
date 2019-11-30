package wechatapi;

import com.spiderclould.service.Wechat;

public class WechatAPIEntity {

    private static Wechat wechatAPI = null;

    private static String filepath = "d:\\workspace\\conf.properties";
    static {
    	Wechat.createInstance(filepath);
        wechatAPI = Wechat.getInstance();

    }

    public static Wechat getWechatAPI(){
        return wechatAPI;
    }


}
