package util;

import org.junit.Test;

import com.spiderclould.entity.ConfigEntity;
import com.spiderclould.util.HttpUtils;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

public class HttpUtilsTest {


    @Test
    public void uploadFormDataTest(){

    	String picPath  = "C:\\Users\\Administrator\\Pictures\\1.png";
        String url = "http://127.0.0.1:8087/file";
        Map<String, Object> data = new HashMap<String, Object>();
        data.put("file", new File(picPath));
        data.put("text", "test1");
        HttpUtils.sendPostFormDataRequest(url, data);


    }

}
