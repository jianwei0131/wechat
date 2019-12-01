package wechatapi;

import org.junit.Assert;
import org.junit.Test;

import com.alibaba.fastjson.JSONObject;
import com.spiderclould.service.Wechat;

public class APIUserTest {

    private Wechat wechatAPI = WechatAPIEntity.getWechatAPI();

    @Test
    public void getUserTest(){
        JSONObject userInfo = wechatAPI.getUser("o82bmwPeVADOaz-x6l4seFMUvg0U");

        System.out.println(userInfo);

        Assert.assertNotNull(userInfo);
        Assert.assertTrue(userInfo.containsKey("nickname"));
        Assert.assertTrue(userInfo.containsKey("sex"));
        Assert.assertTrue(userInfo.containsKey("openid"));

    }
}
