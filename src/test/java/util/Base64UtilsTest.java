package util;

import org.junit.Assert;
import org.junit.Test;

import com.spiderclould.util.Base64Utils;

public class Base64UtilsTest {

    @Test
    public void encodeTest() {

        String str = "Hello Kitty.";

        String finalStr = Base64Utils.encode(str);

        System.out.println(str + " -> encode Base64 -> " + finalStr);

        Assert.assertTrue("SGVsbG8gS2l0dHku".equals(finalStr));


    }

    @Test
    public void decodeTest() {

        String str = "SGVsbG8gS2l0dHku";

        String finalStr = Base64Utils.decode(str);

        System.out.println(str + " -> decode Base64 -> " + finalStr);

        Assert.assertTrue("Hello Kitty.".equals(finalStr));


    }

}
