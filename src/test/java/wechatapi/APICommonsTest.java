package wechatapi;

import java.util.List;

import org.junit.Assert;
import org.junit.Test;

import com.alibaba.fastjson.JSONObject;
import com.spiderclould.entity.AccessToken;
import com.spiderclould.entity.Ticket;
import com.spiderclould.service.Wechat;

public class APICommonsTest {

    private Wechat wechatAPI = WechatAPIEntity.getWechatAPI();

    @Test
    public void getAppidTest(){
        String appid = wechatAPI.getAppid();

        Assert.assertEquals(appid, "wxa471b0eac7fab4db");
    }
    @Test
    public void getAppsecretTest(){
        String appsecret = wechatAPI.getAppsecret();

        Assert.assertEquals(appsecret, "f433f5f26a4911c4ff22f550167d7b0e");
    }

    @Test
    public void getAccessTokenTest(){

        AccessToken accessToken = wechatAPI.ensureAccessToken();

        Assert.assertNotEquals(accessToken.getAccessToken(), null);
        Assert.assertNotEquals(accessToken.getExpireTime(), null);

    }

    @Test
    public void getIpTest(){

        List<String> ips = wechatAPI.getIp();

        Assert.assertTrue(ips.size() > 0);

    }

    @Test
    public void getLatestTicketTest(){

        Ticket ticket = wechatAPI.getLatestTicket();

        Assert.assertNotEquals(ticket.getTicket(), null);
        System.out.println(ticket.getTicket());

    }

    @Test
    public void uploadPicture(){
    	String picPath  = "C:\\Users\\user\\Pictures\\pig.jpg";
        JSONObject ret = wechatAPI.uploadPicture(picPath);

        System.out.println(ret);
        Assert.assertEquals(ret.getIntValue("errcode"), 0);

    }


}
