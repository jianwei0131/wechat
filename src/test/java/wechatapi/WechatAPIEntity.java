package wechatapi;

import com.spiderclould.service.Wechat;

public class WechatAPIEntity {

    private static Wechat wechatAPI = null;

    private static String filepath = "F:\\workspace\\conf.properties";
    static {

        wechatAPI = new Wechat(filepath);

    }

    public static Wechat getWechatAPI(){
        return wechatAPI;
    }


}
