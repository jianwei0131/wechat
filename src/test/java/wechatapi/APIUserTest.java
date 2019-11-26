package wechatapi;

import com.google.gson.JsonObject;
import com.spiderclould.service.Wechat;

import org.junit.Assert;
import org.junit.Test;

public class APIUserTest {

    private Wechat wechatAPI = WechatAPIEntity.getWechatAPI();

    @Test
    public void getUserTest(){
        JsonObject userInfo = wechatAPI.getUser("o82bmwPeVADOaz-x6l4seFMUvg0U");

        System.out.println(userInfo);

        Assert.assertNotNull(userInfo);
        Assert.assertTrue(userInfo.has("nickname"));
        Assert.assertTrue(userInfo.has("sex"));
        Assert.assertTrue(userInfo.has("openid"));

    }
}
