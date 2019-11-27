import org.junit.Assert;
import org.junit.Test;

import com.spiderclould.service.Wechat;

import java.util.HashMap;
import java.util.Map;

public class WechatApiTest {
	private static String filepath = "F:\\workspace\\conf.properties";
    @Test
    public void rawTest(){
    	Wechat.createInstance(filepath);
        Wechat wechatAPI = Wechat.getInstance();

        Map<String, String> map = new HashMap<String, String>();
        map.put("aw","");
        map.put("dw","");
        map.put("d","");
        map.put("yw","");
        map.put("zw","");
        map.put("qw","");
        map.put("dw","");

        String ret = wechatAPI.raw(map);
        System.out.println(ret);
        Assert.assertEquals(ret, "aw=&d=&dw=&qw=&yw=&zw=");

    }

}
