package com.spiderclould.service;

import java.io.File;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.spiderclould.entity.AccessToken;
import com.spiderclould.entity.ConfigEntity;
import com.spiderclould.entity.DatacubeType;
import com.spiderclould.entity.JsConfig;
import com.spiderclould.entity.MaterialType;
import com.spiderclould.entity.Ticket;
import com.spiderclould.entity.TicketStore;
import com.spiderclould.resolver.TicketStorageResolver;
import com.spiderclould.resolver.TokenStorageResolver;
import com.spiderclould.util.Base64Utils;
import com.spiderclould.util.CryptoUtils;
import com.spiderclould.util.HttpUtils;

public class Wechat {

    private String appid;

    private String appsecret;

    private TokenStorageResolver tokenStorageResolver;

    private TicketStorageResolver ticketStorageResolver;

    private String PREFIX = "https://api.weixin.qq.com/cgi-bin/";

    private String MP_PREFIX = "https://mp.weixin.qq.com/cgi-bin/";

    private String FILE_SERVER_PREFIX = "http://file.api.weixin.qq.com/cgi-bin/";

    private String PAY_PREFIX = "https://api.weixin.qq.com/pay/";

    private String MERCHANT_PREFIX = "https://api.weixin.qq.com/merchant/";

    private String CUSTOM_SERVICE_PREFIX = "https://api.weixin.qq.com/customservice/";

    private String WXA_PREFIX = "https://api.weixin.qq.com/wxa/";

//    private JsonParser jsonParser;
//
//    private Gson gson;
    
    private ConfigEntity config;
    
    private volatile static Wechat WECHAT;
    
    public static void createInstance(String path) {
    	if(WECHAT == null) {
    		synchronized (Wechat.class) {
				if(WECHAT == null) {
					WECHAT = new Wechat(path);
				}
			}
    	}
    }
    
    public static Wechat getInstance() {
    	return WECHAT;
    }

    /**
     * 鏍规嵁 appid 鍜� appsecret 鍒涘缓API鐨勬瀯閫犲嚱鏁�
     * 濡傞渶璺ㄨ繘绋嬭法鏈哄櫒杩涜鎿嶄綔Wechat API锛堜緷璧朼ccess token锛夛紝access token闇�瑕佽繘琛屽叏灞�缁存姢
     * 浣跨敤绛栫暐濡備笅锛�
     * 1. 璋冪敤鐢ㄦ埛浼犲叆鐨勮幏鍙� token 鐨勫紓姝ユ柟娉曪紝鑾峰緱 token 涔嬪悗浣跨敤
     * 2. 浣跨敤appid/appsecret鑾峰彇 token 銆傚苟璋冪敤鐢ㄦ埛浼犲叆鐨勪繚瀛� token 鏂规硶淇濆瓨
     * Tips:
     * - 濡傛灉璺ㄦ満鍣ㄨ繍琛寃echat妯″潡锛岄渶瑕佹敞鎰忓悓姝ユ満鍣ㄤ箣闂寸殑绯荤粺鏃堕棿銆�
     * Examples:
     * ```
     * import com.fanglesoft.WechatAPI;
     * WechatAPI api = new WechatAPI('appid', 'secret');
     * ```
     * 浠ヤ笂鍗冲彲婊¤冻鍗曡繘绋嬩娇鐢ㄣ��
     * 褰撳杩涚▼鏃讹紝token 闇�瑕佸叏灞�缁存姢锛屼互涓嬩负淇濆瓨 token 鐨勬帴鍙ｃ��
     * ```
     * WechatAPI api = new WechatAPI('appid', 'secret');
     * ```
     * @param {String} appid 鍦ㄥ叕浼楀钩鍙颁笂鐢宠寰楀埌鐨刟ppid
     * @param {String} appsecret 鍦ㄥ叕浼楀钩鍙颁笂鐢宠寰楀埌鐨刟pp secret
     */
    private Wechat(String confpath){
        this(confpath, new TokenStorageResolver() {
            @Override
            public AccessToken getToken() {
                return this.getAccessToken();
            }

            @Override
            public void saveToken(AccessToken accessToken) {
                this.setAccessToken(accessToken);
            }
        });
    }

    /**
     * 鏍规嵁 appid 鍜� appsecret 鍒涘缓API鐨勬瀯閫犲嚱鏁�
     * 濡傞渶璺ㄨ繘绋嬭法鏈哄櫒杩涜鎿嶄綔Wechat API锛堜緷璧朼ccess token锛夛紝access token闇�瑕佽繘琛屽叏灞�缁存姢
     * 浣跨敤绛栫暐濡備笅锛�
     * 1. 璋冪敤鐢ㄦ埛浼犲叆鐨勮幏鍙� token 鐨勫紓姝ユ柟娉曪紝鑾峰緱 token 涔嬪悗浣跨敤
     * 2. 浣跨敤appid/appsecret鑾峰彇 token 銆傚苟璋冪敤鐢ㄦ埛浼犲叆鐨勪繚瀛� token 鏂规硶淇濆瓨
     * Tips:
     * - 濡傛灉璺ㄦ満鍣ㄨ繍琛寃echat妯″潡锛岄渶瑕佹敞鎰忓悓姝ユ満鍣ㄤ箣闂寸殑绯荤粺鏃堕棿銆�
     * Examples:
     * ```
     * import com.fanglesoft.WechatAPI;
     * WechatAPI api = new WechatAPI('appid', 'secret');
     * ```
     * 浠ヤ笂鍗冲彲婊¤冻鍗曡繘绋嬩娇鐢ㄣ��
     * 褰撳杩涚▼鏃讹紝token 闇�瑕佸叏灞�缁存姢锛屼互涓嬩负淇濆瓨 token 鐨勬帴鍙ｃ��
     * ```
     * WechatAPI api = new WechatAPI('appid', 'secret');
     * ```
     * ```
     * @param {String} appid 鍦ㄥ叕浼楀钩鍙颁笂鐢宠寰楀埌鐨刟ppid
     * @param {String} appsecret 鍦ㄥ叕浼楀钩鍙颁笂鐢宠寰楀埌鐨刟pp secret
     * @param {TokenStorageResolver} tokenStorageResolver 鍙�夌殑銆傝幏鍙栧叏灞�token瀵硅薄鐨勬柟娉曪紝澶氳繘绋嬫ā寮忛儴缃叉椂闇�鍦ㄦ剰
     */
    private Wechat(String confpath, TokenStorageResolver tokenStorageResolver){
    	config = new ConfigEntity(confpath);
        this.appid = config.getAppid();
        this.appsecret = config.getAppsecret();
        String accessToken = config.getAccessToken();
        Long expiresIn = config.getExpiresIn();
        this.tokenStorageResolver = tokenStorageResolver;
        AccessToken token = new AccessToken(accessToken, expiresIn);
        this.tokenStorageResolver.setAccessToken(token);

        this.ticketStorageResolver = new TicketStorageResolver(new TicketStore()) {
            @Override
            public Ticket getTicket(String type) {
                return this.getTicketStore().get(type);
            }

            @Override
            public void saveTicket(String type, Ticket ticket) {
                this.getTicketStore().put(type, ticket);
            }
        };

    }

    public String getAppid() {
        return appid;
    }

    public String getAppsecret() {
        return appsecret;
    }

    /*!
     * 鏍规嵁鍒涘缓API鏃朵紶鍏ョ殑appid鍜宎ppsecret鑾峰彇access token
     * 杩涜鍚庣画鎵�鏈堿PI璋冪敤鏃讹紝闇�瑕佸厛鑾峰彇access token
     * 璇︾粏璇风湅锛�<http://mp.weixin.qq.com/wiki/index.php?title=鑾峰彇access_token> * 搴旂敤寮�鍙戣�呮棤闇�鐩存帴璋冪敤鏈珹PI銆� * Examples:
     * ```
     * AccessToken token = api.getAccessToken();
     * ```
     * - `err`, 鑾峰彇access token鍑虹幇寮傚父鏃剁殑寮傚父瀵硅薄
     * - `result`, 鎴愬姛鏃跺緱鍒扮殑鍝嶅簲缁撴灉 * Result:
     * ```
     * {"access_token": "ACCESS_TOKEN","expires_in": 7200}
     * ```
     */
    public AccessToken getAccessToken() {
    	
        String url = this.PREFIX + "token?grant_type=client_credential&appid=" + this.appid + "&secret=" + this.appsecret;
        String dataStr = HttpUtils.sendGetRequest(url);
        JSONObject data = JSON.parseObject(dataStr);

        // 杩囨湡鏃堕棿锛屽洜缃戠粶寤惰繜绛夛紝灏嗗疄闄呰繃鏈熸椂闂存彁鍓�10绉掞紝浠ラ槻姝复鐣岀偣
        Long expireTime = new Date().getTime() + (data.getLongValue("expires_in") - 10) * 1000;
        AccessToken token = new AccessToken(data.getString("access_token"), expireTime);

        config.setAccessToken(token);
        
        
        tokenStorageResolver.saveToken(token);
        tokenStorageResolver.setAccessToken(token);

        return token;
    }

    /*!
     * 闇�瑕乤ccess token鐨勬帴鍙ｈ皟鐢ㄥ鏋滈噰鐢╬reRequest杩涜灏佽鍚庯紝灏卞彲浠ョ洿鎺ヨ皟鐢ㄣ��
     * 鏃犻渶渚濊禆 getAccessToken 涓哄墠缃皟鐢ㄣ��
     * 搴旂敤寮�鍙戣�呮棤闇�鐩存帴璋冪敤姝PI銆�
     * Examples:
     * ```
     * api.ensureAccessToken();
     * ```
     */
    public AccessToken ensureAccessToken() {
        // 璋冪敤鐢ㄦ埛浼犲叆鐨勮幏鍙杢oken鐨勫紓姝ユ柟娉曪紝鑾峰緱token涔嬪悗浣跨敤锛堝苟缂撳瓨瀹冿級銆�
        AccessToken token = tokenStorageResolver.getAccessToken();
        if (token != null && token.isValid()) {
            return token;
        }
        return this.getAccessToken();
    }

    /**
     * 鑾峰彇js sdk鎵�闇�鐨勬湁鏁坖s ticket
     * - `err`, 寮傚父瀵硅薄
     * - `result`, 姝ｅ父鑾峰彇鏃剁殑鏁版嵁 * Result:
     * - `errcode`, 0涓烘垚鍔�
     * - `errmsg`, 鎴愬姛涓�'ok'锛岄敊璇垯涓鸿缁嗛敊璇俊鎭�
     * - `ticket`, js sdk鏈夋晥绁ㄦ嵁锛屽锛歜xLdikRXVbTPdHSM05e5u5sUoXNKd8-41ZO3MhKoyN5OfkWITDGgnr2fwJ0m9E8NYzWKVZvdVtaUgWvsdshFKA
     * - `expires_in`, 鏈夋晥鏈�7200绉掞紝寮�鍙戣�呭繀椤诲湪鑷繁鐨勬湇鍔″叏灞�缂撳瓨jsapi_ticket
     */
    public Ticket getTicket (String type) {
        AccessToken token = this.ensureAccessToken();
        String accessToken = token.getAccessToken();

        String url = this.PREFIX + "ticket/getticket?access_token=" + accessToken + "&type=" + type;

        Map<String, Object> reqOpts = new HashMap<String, Object>();
        Map<String, String> headers = new HashMap<String, String>();
        headers.put("content-type", "application/json");
        reqOpts.put("headers", headers);
        String dataStr = HttpUtils.sendGetRequest(url, reqOpts,"utf-8");
        JSONObject data = JSON.parseObject(dataStr);

        // 杩囨湡鏃堕棿锛屽洜缃戠粶寤惰繜绛夛紝灏嗗疄闄呰繃鏈熸椂闂存彁鍓�10绉掞紝浠ラ槻姝复鐣岀偣
        Long expireTime = new Date().getTime() + (data.getLongValue("expires_in") - 10) * 1000;
        Ticket ticket = new Ticket(data.getString("ticket"), expireTime);
        ticketStorageResolver.saveTicket(type, ticket);
        return ticket;
    }

    public Ticket getTicket () {
        return this.getTicket("jsapi");
    }


    /**
     * 鍒涘缓 闅忔満瀛楃涓�
     * @return
     */
    public static String createNonceStr () {
        String ret = "";
        byte[] tmp = new byte[20];
        for(int i = 0; i < 20; i++){
            tmp[i] = (byte) ((Math.random() * (122-97)) + 97);
        }
        return new String(tmp);
    }

    /**
     * 鐢熸垚鏃堕棿鎴�
     */
    public static String createTimestamp () {
        return "" + Math.floor(new Date().getTime() / 1000);
    }


    /*!
     * 鎺掑簭鏌ヨ瀛楃涓� */
    public static String raw (Map<String, String> args) {
        Set<String> setKeys = args.keySet();

        Map<String, String> map = new TreeMap<String, String>(
                new Comparator<String>() {
                    public int compare(String obj1, String obj2) {
                        // 鍗囧簭鎺掑簭
                        return obj1.compareTo(obj2);
                    }
                });

        for(String key : setKeys){
            map.put(key, args.get(key));
        }

        Set<String> mapSetKeys = map.keySet();

        String string = "";
        for (String key : mapSetKeys) {
            Object val = args.get(key);
            string += '&' + key + '=' + val;
        }
        return string.substring(1);
    }

    /*!
     * 绛惧悕绠楁硶 * @param {String} nonceStr 鐢熸垚绛惧悕鐨勯殢鏈轰覆
     * @param {String} jsapi_ticket 鐢ㄤ簬绛惧悕鐨刯sapi_ticket
     * @param {String} timestamp 鏃堕棿鎴�
     * @param {String} url 鐢ㄤ簬绛惧悕鐨剈rl锛屾敞鎰忓繀椤讳笌璋冪敤JSAPI鏃剁殑椤甸潰URL瀹屽叏涓�鑷� */
    public String ticketSign (String nonceStr, String jsapi_ticket, String timestamp, String url) {

        Map<String, String> ret = new HashMap<String, String>();
        ret.put("jsapi_ticket", jsapi_ticket);
        ret.put("nonceStr", nonceStr);
        ret.put("timestamp", timestamp);
        ret.put("url", url);

        String string = raw(ret);
        MessageDigest SHA1MessageDigest = null;
        try {
            SHA1MessageDigest = CryptoUtils.SHA1MessageDigest();
            SHA1MessageDigest.reset();
            SHA1MessageDigest.update(string.getBytes("UTF-8"));
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }

        String signature = CryptoUtils.byteToStr(SHA1MessageDigest.digest());

        return signature;
    }

    /*!
     * 鍗″埜card_ext閲岀殑绛惧悕绠楁硶
     * @name signCardExt
     * @param {String} api_ticket 鐢ㄤ簬绛惧悕鐨勪复鏃剁エ鎹紝鑾峰彇鏂瑰紡瑙�2.鑾峰彇api_ticket銆�
     * @param {String} card_id 鐢熸垚鍗″埜鏃惰幏寰楃殑card_id
     * @param {String} timestamp 鏃堕棿鎴筹紝鍟嗘埛鐢熸垚浠�1970 骞�1 鏈�1 鏃ユ槸寰俊鍗″埜鎺ュ彛鏂囨。00:00:00 鑷充粖鐨勭鏁�,鍗冲綋鍓嶇殑鏃堕棿,涓旀渶缁堥渶瑕佽浆鎹负瀛楃涓插舰寮�;鐢卞晢鎴风敓鎴愬悗浼犲叆銆�
     * @param {String} code 鎸囧畾鐨勫崱鍒竎ode 鐮侊紝鍙兘琚涓�娆°�倁se_custom_code 瀛楁涓簍rue 鐨勫崱鍒稿繀椤诲～鍐欙紝闈炶嚜瀹氫箟code 涓嶅繀濉啓銆�
     * @param {String} openid 鎸囧畾棰嗗彇鑰呯殑openid锛屽彧鏈夎鐢ㄦ埛鑳介鍙栥�俠ind_openid 瀛楁涓簍rue 鐨勫崱鍒稿繀椤诲～鍐欙紝闈炶嚜瀹氫箟code 涓嶅繀濉啓銆�
     * @param {String} balance 绾㈠寘浣欓锛屼互鍒嗕负鍗曚綅銆傜孩鍖呯被鍨嬶紙LUCKY_MONEY锛夊繀濉�佸叾浠栧崱鍒哥被鍨嬩笉蹇呭～銆� */
    public String signCardExt (String api_ticket, String card_id, String timestamp, String code, String openid, String balance) {

        List<String> values = new ArrayList<String>();
        values.add(api_ticket);
        values.add(card_id);
        values.add(timestamp);
        values.add(code != null ? code : "");
        values.add(openid != null ? openid : "");
        values.add(balance != null ? balance : "");

        Collections.sort(values, new Comparator< String >() {
            @Override
            public int compare(String lhs, String rhs) {
                int i = lhs.compareTo(rhs);
                if (i > 0) {
                    return 1;
                } else {
                    return -1;
                }
            }
        });


        String string = "";
        for(String i : values){
            string += i;
        }

        MessageDigest SHA1MessageDigest = null;
        try {
            SHA1MessageDigest = CryptoUtils.SHA1MessageDigest();
            SHA1MessageDigest.reset();
            SHA1MessageDigest.update(string.getBytes("UTF-8"));
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }

        String signature = CryptoUtils.byteToStr(SHA1MessageDigest.digest());

        return signature;
    };

    public Ticket ensureTicket (String type) {
        Ticket cache = ticketStorageResolver.getTicket(type);

        Ticket ticket = null;
        // 鏈塼icket骞朵笖ticket鏈夋晥鐩存帴璋冪敤
        if (cache != null) {
            ticket = new Ticket(cache.getTicket(), cache.getExpireTime());
        }

        // 娌℃湁ticket鎴栬�呮棤鏁�
        if (ticket == null || !ticket.isValid()) {
            // 浠庡井淇＄鑾峰彇ticket
            ticket = getTicket(type);
        }
        return ticket;
    };

    /**
     * 鑾峰彇寰俊JS SDK Config鐨勬墍闇�鍙傛暟
     * Examples:
     * ```
     * var param = {
     *  debug: false,
     *  jsApiList: ['onMenuShareTimeline', 'onMenuShareAppMessage'],
     *  url: 'http://www.xxx.com'
     * };
     * api.getJsConfig(param);
     * ```
     * - `result`, 璋冪敤姝ｅ父鏃跺緱鍒扮殑js sdk config鎵�闇�鍙傛暟
     * @param {Object} param 鍙傛暟
     */
    public JsConfig getJsConfig (Map<String, Object> param) {
        Ticket ticket = this.ensureTicket("jsapi");
        String nonceStr = createNonceStr();
        String jsAPITicket = ticket.getTicket();
        String timestamp = createTimestamp();
        String signature = ticketSign(nonceStr, jsAPITicket, timestamp, param.get("url").toString());
        String debug = param.get("debug").toString();
        List<String> jsApiList = (List<String>) param.get("jsApiList");

        JsConfig jsConfig = new JsConfig(
                debug,
                this.appid,
                timestamp,
                nonceStr,
                signature,
                jsApiList);

        return jsConfig;
    }

    /**
     * 鑾峰彇card ext
     * Examples:
     * ```
     * var param = {
     *  card_id: 'p-hXXXXXXX',
     *  code: '1234',
     *  openid: '111111',
     *  balance: 100
     * };
     * api.getCardExt(param);
     * ```
     * - `result`, 璋冪敤姝ｅ父鏃跺緱鍒扮殑card_ext瀵硅薄锛屽寘鍚墍闇�鍙傛暟
     * @name getCardExt
     * @param {Object} param 鍙傛暟
     */
    public Map<String, String> getCardExt (Map<String, String> param) {
        Ticket apiTicket = this.ensureTicket("wx_card");
        String timestamp = createTimestamp();
        String signature = signCardExt(apiTicket.getTicket(),
                param.get("card_id"),
                timestamp,
                param.get("code"),
                param.get("openid"),
                param.get("balance"));

        Map<String, String> result = new HashMap<String, String>();
        result.put("timestamp", timestamp);
        result.put("signature", signature);
        result.put("code", param.get("code") != null ? param.get("code").toString() : "");
        result.put("openid", param.get("openid") != null ? param.get("openid").toString() : "");

        if (param.containsKey("balance")) {
            result.put("balance", param.get("balance"));
        }

        return result;
    };

    /**
     * 鑾峰彇鏈�鏂扮殑js api ticket
     * Examples:
     * ```
     * api.getLatestTicket();
     * ```
     * - `err`, 鑾峰彇js api ticket鍑虹幇寮傚父鏃剁殑寮傚父瀵硅薄
     * - `ticket`, 鑾峰彇鐨則icket
     */
    public Ticket getLatestTicket () {
        return this.ensureTicket("jsapi");
    }


    /**
     * 鑾峰彇寰俊鏈嶅姟鍣↖P鍦板潃
     * 璇︽儏璇疯锛�<http://mp.weixin.qq.com/wiki/index.php?title=%E8%8E%B7%E5%8F%96%E5%BE%AE%E4%BF%A1%E6%9C%8D%E5%8A%A1%E5%99%A8IP%E5%9C%B0%E5%9D%80>
     * Examples:
     * ```
     * api.getIp();
     * ```
     * Result:
     * ```
     * ["127.0.0.1","127.0.0.1"]
     * ```
     */
    public List<String> getIp () {
        AccessToken token = this.ensureAccessToken();
        String accessToken = token.getAccessToken();
        // https://api.weixin.qq.com/cgi-bin/getcallbackip?access_token=ACCESS_TOKEN
        String url = this.PREFIX + "getcallbackip?access_token=" + accessToken;

        Map<String, Object> reqOpts = new HashMap<String, Object>();
        Map<String, String> headers = new HashMap<String, String>();
        headers.put("content-type", "application/json");
        reqOpts.put("headers", headers);
        String dataStr = HttpUtils.sendGetRequest(url, reqOpts,"utf-8");
        JSONObject data = JSON.parseObject(dataStr);

        JSONArray array = data.getJSONArray("ip_list");

        List<String> list = new ArrayList<String>();
        for(int i = 0; i < array.size(); i++){
            list.add(array.getString(i));
        }

        return list;
    }


    /**
     * 鑾峰彇瀹㈡湇鑱婂ぉ璁板綍
     * 璇︾粏璇风湅锛歨ttp://mp.weixin.qq.com/wiki/19/7c129ec71ddfa60923ea9334557e8b23.html
     * Opts:
     * ```
     * {
     *  "starttime" : 123456789,
     *  "endtime" : 987654321,
     *  "openid": "OPENID", // 闈炲繀椤�
     *  "pagesize" : 10,
     *  "pageindex" : 1,
     * }
     * ```
     * Examples:
     * ```
     * JSONArray result = await api.getRecords(opts);
     * ```
     * Result:
     * ```
     * [
     *    {
     *      "worker": " test1",
     *      "openid": "oDF3iY9WMaswOPWjCIp_f3Bnpljk",
     *      "opercode": 2002,
     *      "time": 1400563710,
     *      "text": " 鎮ㄥソ锛屽鏈峵est1涓烘偍鏈嶅姟銆�"
     *    },
     *    {
     *      "worker": " test1",
     *      "openid": "oDF3iY9WMaswOPWjCIp_f3Bnpljk",
     *      "opercode": 2003,
     *      "time": 1400563731,
     *      "text": " 浣犲ソ锛屾湁浠�涔堜簨鎯咃紵 "
     *    },
     *  ]
     * ```
     * @param {Object} opts 鏌ヨ鏉′欢
     */
    public JSONArray getRecords (Map<String, Object> opts) {
        AccessToken token = this.ensureAccessToken();
        String accessToken = token.getAccessToken();
        // https://api.weixin.qq.com/customservice/msgrecord/getrecord?access_token=ACCESS_TOKEN
        String url = this.CUSTOM_SERVICE_PREFIX + "msgrecord/getrecord?access_token=" + accessToken;
        String data = JSON.toJSONString(opts);
        String respStr = HttpUtils.sendPostJsonRequest(url, data);
        JSONObject resp = JSON.parseObject(respStr);

        JSONArray recordlist = resp.getJSONArray("recordlist");
        return recordlist;
    };

    /**
     * 鑾峰彇瀹㈡湇鍩烘湰淇℃伅
     * 璇︾粏璇风湅锛歨ttp://dkf.qq.com/document-3_1.html
     * Examples:
     * ```
     * JSONArray result = api.getCustomServiceList();
     * ```
     * Result:
     * ```
     * [
     *     {
     *       "kf_account": "test1@test",
     *       "kf_nick": "ntest1",
     *       "kf_id": "1001"
     *     },
     *     {
     *       "kf_account": "test2@test",
     *       "kf_nick": "ntest2",
     *       "kf_id": "1002"
     *     },
     *     {
     *       "kf_account": "test3@test",
     *       "kf_nick": "ntest3",
     *       "kf_id": "1003"
     *     }
     *   ]
     * }
     * ```
     */
    public JSONArray getCustomServiceList () {
        AccessToken token = this.ensureAccessToken();
        String accessToken = token.getAccessToken();
        // https://api.weixin.qq.com/cgi-bin/customservice/getkflist?access_token= ACCESS_TOKEN
        String url = this.PREFIX + "customservice/getkflist?access_token=" + accessToken;

        String respStr = HttpUtils.sendPostJsonRequest(url);
        JSONObject resp = JSON.parseObject(respStr);

        JSONArray kf_list = resp.getJSONArray("kf_list");
        return kf_list;
    };

    /**
     * 鑾峰彇鍦ㄧ嚎瀹㈡湇鎺ュ緟淇℃伅
     * 璇︾粏璇风湅锛歨ttp://dkf.qq.com/document-3_2.html * Examples:
     * ```
     * JSONArray list = api.getOnlineCustomServiceList();
     * ```
     * Result:
     * ```
     * {
     *   "kf_online_list": [
     *     {
     *       "kf_account": "test1@test",
     *       "status": 1,
     *       "kf_id": "1001",
     *       "auto_accept": 0,
     *       "accepted_case": 1
     *     },
     *     {
     *       "kf_account": "test2@test",
     *       "status": 1,
     *       "kf_id": "1002",
     *       "auto_accept": 0,
     *       "accepted_case": 2
     *     }
     *   ]
     * }
     * ```
     */
    public JSONArray getOnlineCustomServiceList() {
        AccessToken token = this.ensureAccessToken();
        String accessToken = token.getAccessToken();
        // https://api.weixin.qq.com/cgi-bin/customservice/getkflist?access_token= ACCESS_TOKEN
        String url = this.PREFIX + "customservice/getonlinekflist?access_token=" + accessToken;

        String respStr = HttpUtils.sendPostJsonRequest(url);
        JSONObject resp = JSON.parseObject(respStr);

        JSONArray kf_online_list = resp.getJSONArray("kf_online_list");
        return kf_online_list;
    };

    /**
     * 娣诲姞瀹㈡湇璐﹀彿
     * 璇︾粏璇风湅锛歨ttp://mp.weixin.qq.com/wiki?t=resource/res_main&id=mp1458044813&token=&lang=zh_CN * Examples:
     * ```
     * boolean result = api.addKfAccount('test@test', 'nickname', 'password');
     * ```
     * Result:
     * ```
     * {
     *  "errcode" : 0,
     *  "errmsg" : "ok",
     * }
     * ```
     * @param {String} account 璐﹀彿鍚嶅瓧锛屾牸寮忎负锛氬墠缂�@鍏叡鍙峰悕瀛�
     * @param {String} nick 鏄电О
     */
    public boolean addKfAccount (String account, String nick, String password) {
        AccessToken token = this.ensureAccessToken();
        String accessToken = token.getAccessToken();
        // https://api.weixin.qq.com/customservice/kfaccount/add?access_token=ACCESS_TOKEN
        String prefix = "https://api.weixin.qq.com/";
        String url = prefix + "customservice/kfaccount/add?access_token=" + accessToken;

        Map<String, String> data = new HashMap<String, String>();
        data.put("kf_account", account);
        data.put("nickname", nick);
        data.put("password", password);

        String respStr = HttpUtils.sendPostJsonRequest(url, JSON.toJSONString(data));

        JSONObject resp = JSON.parseObject(respStr);
        Integer errCode = resp.getIntValue("errcode");

        if(0 == errCode){
            return true;
        }else{
            return false;
        }

    };

    /**
     * 閭�璇风粦瀹氬鏈嶅笎鍙�
     * 璇︾粏璇风湅锛歨ttps://mp.weixin.qq.com/wiki?t=resource/res_main&id=mp1458044813&token=&lang=zh_CN
     * Examples:
     * ```
     * boolean result = api.inviteworker('test@test', 'invite_wx');
     * ```
     * Result:
     * ```
     * {
     *  "errcode" : 0,
     *  "errmsg" : "ok",
     * }
     * ```
     * @param {String} account 璐﹀彿鍚嶅瓧锛屾牸寮忎负锛氬墠缂�@鍏叡鍙峰悕瀛�
     * @param {String} wx 閭�璇风粦瀹氱殑涓汉寰俊璐﹀彿
     */
    public boolean inviteworker (String account, String wx) {
        AccessToken token = this.ensureAccessToken();
        String accessToken = token.getAccessToken();
        // https://api.weixin.qq.com/customservice/kfaccount/inviteworker?access_token=ACCESS_TOKEN
        String prefix = "https://api.weixin.qq.com/";
        String url = prefix + "customservice/kfaccount/inviteworker?access_token=" + accessToken;

        Map<String, String> data = new HashMap<String, String>();
        data.put("kf_account", account);
        data.put("invite_wx", wx);

        String respStr = HttpUtils.sendPostJsonRequest(url, JSON.toJSONString(data));

        JSONObject resp = JSON.parseObject(respStr);
        Integer errCode = resp.getIntValue("errcode");

        if(0 == errCode){
            return true;
        }else{
            return false;
        }
    }

    /**
     * 璁剧疆瀹㈡湇璐﹀彿
     * 璇︾粏璇风湅锛歨ttp://mp.weixin.qq.com/wiki?t=resource/res_main&id=mp1458044813&token=&lang=zh_CN * Examples:
     * ```
     * boolean result = api.updateKfAccount('test@test', 'nickname', 'password');
     * ```
     * Result:
     * ```
     * {
     *  "errcode" : 0,
     *  "errmsg" : "ok",
     * }
     * ```
     * @param {String} account 璐﹀彿鍚嶅瓧锛屾牸寮忎负锛氬墠缂�@鍏叡鍙峰悕瀛�
     * @param {String} nick 鏄电О
     */
    public boolean updateKfAccount (String account, String nick, String password) {
        AccessToken token = this.ensureAccessToken();
        String accessToken = token.getAccessToken();
        // https://api.weixin.qq.com/customservice/kfaccount/add?access_token=ACCESS_TOKEN
        String prefix = "https://api.weixin.qq.com/";
        String url = prefix + "customservice/kfaccount/update?access_token=" + accessToken;
        Map<String, String> data = new HashMap<String, String>();
        data.put("kf_account", account);
        data.put("nickname", nick);
        data.put("password", password);

        String respStr = HttpUtils.sendPostJsonRequest(url, JSON.toJSONString(data));

        JSONObject resp = JSON.parseObject(respStr);
        Integer errCode = resp.getIntValue("errcode");

        if(0 == errCode){
            return true;
        }else{
            return false;
        }
    };

    /**
     * 鍒犻櫎瀹㈡湇璐﹀彿
     * 璇︾粏璇风湅锛歨ttp://mp.weixin.qq.com/wiki/9/6fff6f191ef92c126b043ada035cc935.html * Examples:
     * ```
     * api.deleteKfAccount('test@test');
     * ```
     * Result:
     * ```
     * {
     *  "errcode" : 0,
     *  "errmsg" : "ok",
     * }
     * ```
     * @param {String} account 璐﹀彿鍚嶅瓧锛屾牸寮忎负锛氬墠缂�@鍏叡鍙峰悕瀛�
     */
     public boolean deleteKfAccount (String account) {
         AccessToken token = this.ensureAccessToken();
         String accessToken = token.getAccessToken();
            // https://api.weixin.qq.com/customservice/kfaccount/del?access_token=ACCESS_TOKEN
         String prefix = "https://api.weixin.qq.com/";
         String url = prefix + "customservice/kfaccount/del?access_token=" + accessToken + "&kf_account=" + account;

         Map<String, Object> reqOpts = new HashMap<String, Object>();
         Map<String, String> headers = new HashMap<String, String>();
         headers.put("content-type", "application/json");
         reqOpts.put("headers", headers);
         String dataStr = HttpUtils.sendGetRequest(url, reqOpts,"utf-8");

         JSONObject resp = JSON.parseObject(dataStr);
         Integer errCode = resp.getIntValue("errcode");

         if(0 == errCode){
             return true;
         }else{
             return false;
         }
     };

    /**
     * 璁剧疆瀹㈡湇澶村儚
     * 璇︾粏璇风湅锛歨ttp://mp.weixin.qq.com/wiki/9/6fff6f191ef92c126b043ada035cc935.html * Examples:
     * ```
     * api.setKfAccountAvatar('test@test', '/path/to/avatar.png');
     * ```
     * Result:
     * ```
     * {
     *  "errcode" : 0,
     *  "errmsg" : "ok",
     * }
     * ```
     * @param {String} account 璐﹀彿鍚嶅瓧锛屾牸寮忎负锛氬墠缂�@鍏叡鍙峰悕瀛�
     * @param {String} filepath 澶村儚璺緞
     */
    public boolean setKfAccountAvatar (String account, String filepath) {
        AccessToken token = this.ensureAccessToken();
        String accessToken = token.getAccessToken();
        // http://api.weixin.qq.com/customservice/kfaccount/uploadheadimg?access_token=ACCESS_TOKEN&kf_account=KFACCOUNT
        Map<String, Object> formData = new HashMap<String, Object>();
        formData.put("media", new File(filepath));
        String prefix = "https://api.weixin.qq.com/";
        String url = prefix + "customservice/kfaccount/uploadheadimg?access_token=" + accessToken + "&kf_account=" + account;

        String respStr = HttpUtils.sendPostFormDataRequest(url, formData);
        JSONObject resp = JSON.parseObject(respStr);
        Integer errCode = resp.getIntValue("errcode");

        if(0 == errCode){
            return true;
        }else{
            return false;
        }
    };

    /**
     * 鍒涘缓瀹㈡湇浼氳瘽
     * 璇︾粏璇风湅锛歨ttp://mp.weixin.qq.com/wiki?t=resource/res_main&id=mp1458044820&token=&lang=zh_CN * Examples:
     * ```
     * api.createKfSession('test@test', 'OPENID');
     * ```
     * Result:
     * ```
     * {
     *  "errcode" : 0,
     *  "errmsg" : "ok",
     * }
     * ```
     * @param {String} account 璐﹀彿鍚嶅瓧锛屾牸寮忎负锛氬墠缂�@鍏叡鍙峰悕瀛�
     * @param {String} openid openid
     */
    public boolean createKfSession (String account, String openid) {
        AccessToken token = this.ensureAccessToken();
        String accessToken = token.getAccessToken();
        // https://api.weixin.qq.com/customservice/kfsession/create?access_token=ACCESS_TOKEN
        String prefix = "https://api.weixin.qq.com/";
        String url = prefix + "customservice/kfsession/create?access_token=" + accessToken;

        Map<String, String> data = new HashMap<String, String>();
        data.put("kf_account", account);
        data.put("openid", openid);

        String respStr = HttpUtils.sendPostJsonRequest(url, JSON.toJSONString(data));

        JSONObject resp = JSON.parseObject(respStr);
        Integer errCode = resp.getIntValue("errcode");

        if(0 == errCode){
            return true;
        }else{
            return false;
        }

    }

    /**
     * 涓婁紶Logo
     * Examples:
     * ```
     * api.uploadLogo('filepath');
     * ```
     *
     * Result:
     * ```
     * {
     *  "errcode":0,
     *  "errmsg":"ok",
     *  "url":"http://mmbiz.qpic.cn/mmbiz/iaL1LJM1mF9aRKPZJkmG8xXhiaHqkKSVMMWeN3hLut7X7hicFNjakmxibMLGWpXrEXB33367o7zHN0CwngnQY7zb7g/0"
     * }
     * ``` * @name uploadLogo
     * @param {String} filepath 鏂囦欢璺緞
     */
    public JSONObject uploadLogo (String filepath) {
        AccessToken token = this.ensureAccessToken();
        String accessToken = token.getAccessToken();

        Map<String, Object> formData = new HashMap<String, Object>();
        formData.put("buffer", new File(filepath));

        String url = this.FILE_SERVER_PREFIX + "media/uploadimg?access_token=" + accessToken;

        String respStr = HttpUtils.sendPostFormDataRequest(url, formData);
        JSONObject resp = JSON.parseObject(respStr);
        return resp;
    }

    /**
     * @name addLocations
     * @param {Array} locations 浣嶇疆
     */
     public JSONObject addLocations (List<String> locations) {
         AccessToken token = this.ensureAccessToken();
         String accessToken = token.getAccessToken();

         Map<String, Object> data = new HashMap<String, Object>();
         data.put("location_list", locations);

         String url = "https://api.weixin.qq.com/card/location/batchadd?access_token=" + accessToken;

         String respStr = HttpUtils.sendPostJsonRequest(url, JSON.toJSONString(data));
         JSONObject resp = JSON.parseObject(respStr);
         return resp;
     };

    /**
     * @name getLocations
     * @param {Array} locations 浣嶇疆
     */
    public JSONObject getLocations (String offset, int count) {
        AccessToken token = this.ensureAccessToken();
        String accessToken = token.getAccessToken();

        Map<String, Object> data = new HashMap<String, Object>();
        data.put("offset", offset);
        data.put("count", count);

        String url = "https://api.weixin.qq.com/card/location/batchget?access_token=" + accessToken;
        String respStr = HttpUtils.sendPostJsonRequest(url, JSON.toJSONString(data));
        JSONObject resp = JSON.parseObject(respStr);
        return resp;
    };

    public JSONObject getColors () {
        AccessToken token = this.ensureAccessToken();
        String accessToken = token.getAccessToken();

        String url = "https://api.weixin.qq.com/card/getcolors?access_token=" + accessToken;
        Map<String, Object> reqOpts = new HashMap<String, Object>();
        Map<String, String> headers = new HashMap<String, String>();
        headers.put("content-type", "application/json");
        reqOpts.put("headers", headers);
        String respStr = HttpUtils.sendGetRequest(url, reqOpts,"utf-8");
        JSONObject resp = JSON.parseObject(respStr);
        return resp;
    }

    /**
     * 娣诲姞 鍗″姷
     * Example锛�
     * ```
     * JSONObject ret = api.createCard(card))
     * String card_id = res.get("card_id");
     * ```
     * @param card
     * @return
     */
    public JSONObject createCard (Map<String, Object> card) {
        AccessToken token = this.ensureAccessToken();
        String accessToken = token.getAccessToken();

        String url = "https://api.weixin.qq.com/card/create?access_token=" + accessToken;

        Map<String, Object> data = new HashMap<String, Object>();
        data.put("card", card);

        String respStr = HttpUtils.sendPostJsonRequest(url, JSON.toJSONString(data));
        JSONObject resp = JSON.parseObject(respStr);
        return resp;
    };

    public void getRedirectUrl (String url, String encryptCode, String cardId) {
        // TODO
    };

    public JSONObject createQRCode (Map<String, Object> card) {
        AccessToken token = this.ensureAccessToken();
        String accessToken = token.getAccessToken();

        String url = "https://api.weixin.qq.com/card/qrcode/create?access_token=" + accessToken;

        Map<String, Object> data = new HashMap<String, Object>();
            data.put("action_name", "QR_CARD");
            Map<String, Object> actionInfo = new HashMap<String, Object>();
                actionInfo.put("card", card);
            data.put("action_info", actionInfo);
            data.put("card", card);

        String respStr = HttpUtils.sendPostJsonRequest(url, JSON.toJSONString(data));
        JSONObject resp = JSON.parseObject(respStr);
        return resp;
    };

    public JSONObject consumeCode (String code, String cardId) {

        AccessToken token = this.ensureAccessToken();
        String accessToken = token.getAccessToken();

        String url = "https://api.weixin.qq.com/card/code/consume?access_token=" + accessToken;

        Map<String, Object> data = new HashMap<String, Object>();
        data.put("code", code);
        data.put("cardId", cardId);

        String respStr = HttpUtils.sendPostJsonRequest(url, JSON.toJSONString(data));
        JSONObject resp = JSON.parseObject(respStr);
        return resp;
    };

    public JSONObject decryptCode (String encryptCode) {

        AccessToken token = this.ensureAccessToken();
        String accessToken = token.getAccessToken();

        String url = "https://api.weixin.qq.com/card/code/decrypt?access_token=" + accessToken;

        Map<String, Object> data = new HashMap<String, Object>();
        data.put("encrypt_code", encryptCode);

        String respStr = HttpUtils.sendPostJsonRequest(url, JSON.toJSONString(data));
        JSONObject resp = JSON.parseObject(respStr);
        return resp;
    };

    public JSONObject deleteCard (String cardId) {

        AccessToken token = this.ensureAccessToken();
        String accessToken = token.getAccessToken();

        String url = "https://api.weixin.qq.com/card/delete?access_token=" + accessToken;

        Map<String, Object> data = new HashMap<String, Object>();
        data.put("card_id", cardId);

        String respStr = HttpUtils.sendPostJsonRequest(url, JSON.toJSONString(data));
        JSONObject resp = JSON.parseObject(respStr);
        return resp;
    };

    public JSONObject getCode (String code) {
        return getCode(code, null);
    }

    public JSONObject getCode (String code, String cardId) {

        AccessToken token = this.ensureAccessToken();
        String accessToken = token.getAccessToken();

        String url = "https://api.weixin.qq.com/card/code/get?access_token=" + accessToken;

        Map<String, Object> data = new HashMap<String, Object>();
        data.put("code", code);
        if(cardId != null) {
            data.put("card_id", cardId);
        }
        String respStr = HttpUtils.sendPostJsonRequest(url, JSON.toJSONString(data));
        JSONObject resp = JSON.parseObject(respStr);
        return resp;
    };


    public JSONObject getCards (int offset, int count) {
        return getCards(offset, count, null);
    }

    public JSONObject getCards (int offset, int count, List<String> status_list) {

        AccessToken token = this.ensureAccessToken();
        String accessToken = token.getAccessToken();

        String url = "https://api.weixin.qq.com/card/batchget?access_token=" + accessToken;

        Map<String, Object> data = new HashMap<String, Object>();
        data.put("offset", offset);
        data.put("count", count);
        if(status_list != null) {
            data.put("status_list", status_list);
        }
        String respStr = HttpUtils.sendPostJsonRequest(url, JSON.toJSONString(data));
        JSONObject resp = JSON.parseObject(respStr);
        return resp;
    }

    public JSONObject getCard (String cardId) {

        AccessToken token = this.ensureAccessToken();
        String accessToken = token.getAccessToken();

        String url = "https://api.weixin.qq.com/card/get?access_token=" + accessToken;

        Map<String, Object> data = new HashMap<String, Object>();
        data.put("card_id", cardId);
        String respStr = HttpUtils.sendPostJsonRequest(url, JSON.toJSONString(data));
        JSONObject resp = JSON.parseObject(respStr);
        return resp;
    }
    /**
     * 鑾峰彇鐢ㄦ埛宸查鍙栫殑鍗″埜
     * 璇︾粏缁嗚妭 https://mp.weixin.qq.com/wiki?t=resource/res_main&id=mp1451025272&token=&lang=zh_CN
     * Examples:
     * ```
     * api.getCardList('openid', 'card_id');
     * ```
     *
     * @param {String} openid 鐢ㄦ埛鐨刼penid
     * @param {String} cardId 鍗″埜鐨刢ard_id
     */
     public JSONObject getCardList (String openid, String cardId) {

         AccessToken token = this.ensureAccessToken();
         String accessToken = token.getAccessToken();

         String prefix = "https://api.weixin.qq.com/";
         String url = prefix + "card/user/getcardlist?access_token=" + accessToken;

         Map<String, Object> data = new HashMap<String, Object>();
         data.put("openid", openid);
         data.put("card_id", cardId);

         String respStr = HttpUtils.sendPostJsonRequest(url, JSON.toJSONString(data));
         JSONObject resp = JSON.parseObject(respStr);

         return resp;
     }

     public JSONObject updateCode (String code, String cardId, String newcode) {

         AccessToken token = this.ensureAccessToken();
         String accessToken = token.getAccessToken();

         String url = "https://api.weixin.qq.com/card/code/update?access_token=" + accessToken;

         Map<String, Object> data = new HashMap<String, Object>();
         data.put("code", code);
         data.put("card_id", cardId);
         data.put("newcode", newcode);

         String respStr = HttpUtils.sendPostJsonRequest(url, JSON.toJSONString(data));
         JSONObject resp = JSON.parseObject(respStr);

         return resp;
    }

    public JSONObject unavailableCode (String code) {
         return unavailableCode(code, null);
    }
    public JSONObject unavailableCode (String code, String cardId) {

        AccessToken token = this.ensureAccessToken();
        String accessToken = token.getAccessToken();

        String url = "https://api.weixin.qq.com/card/code/unavailable?access_token=" + accessToken;

        Map<String, Object> data = new HashMap<String, Object>();
        data.put("code", code);

        if(cardId != null) {
            data.put("card_id", cardId);
        }

        String respStr = HttpUtils.sendPostJsonRequest(url, JSON.toJSONString(data));
        JSONObject resp = JSON.parseObject(respStr);

        return resp;
    };

    public JSONObject updateCard (String cardId, Map<String, Object> cardInfo) {

        AccessToken token = this.ensureAccessToken();
        String accessToken = token.getAccessToken();

        String url = "https://api.weixin.qq.com/card/update?access_token=" + accessToken;

        Map<String, Object> data = new HashMap<String, Object>();
        data.put("card_id", cardId);
        data.put("member_card", cardInfo);


        if(cardId != null) {
            data.put("card_id", cardId);
        }

        String respStr = HttpUtils.sendPostJsonRequest(url, JSON.toJSONString(data));
        JSONObject resp = JSON.parseObject(respStr);

        return resp;
    };

    public JSONObject updateCardStock (String cardId, int num) {

        AccessToken token = this.ensureAccessToken();
        String accessToken = token.getAccessToken();

        String url = "https://api.weixin.qq.com/card/modifystock?access_token=" + accessToken;

        Map<String, Object> data = new HashMap<String, Object>();
        data.put("card_id", cardId);

        if (num > 0) {
            data.put("increase_stock_value", Math.abs(num));
        } else {
            data.put("reduce_stock_value", Math.abs(num));
        }

        String respStr = HttpUtils.sendPostJsonRequest(url, JSON.toJSONString(data));
        JSONObject resp = JSON.parseObject(respStr);

        return resp;

    };

    public JSONObject activateMembercard (Map<String, Object> info) {

        AccessToken token = this.ensureAccessToken();
        String accessToken = token.getAccessToken();

        String url = "https://api.weixin.qq.com/card/membercard/activate?access_token=" + accessToken;

        String respStr = HttpUtils.sendPostJsonRequest(url, JSON.toJSONString(info));
        JSONObject resp = JSON.parseObject(respStr);

        return resp;
    };

    public JSONObject getActivateMembercardUrl (Map<String, Object> info) {

        AccessToken token = this.ensureAccessToken();
        String accessToken = token.getAccessToken();

        String url = "https://api.weixin.qq.com/card/membercard/activate/geturl?access_token=" + accessToken;

        String respStr = HttpUtils.sendPostJsonRequest(url, JSON.toJSONString(info));
        JSONObject resp = JSON.parseObject(respStr);

        return resp;
    };


    public JSONObject updateMembercard (Map<String, Object> info) {

        AccessToken token = this.ensureAccessToken();
        String accessToken = token.getAccessToken();

        String url = "https://api.weixin.qq.com/card/membercard/updateuser?access_token=" + accessToken;

        String respStr = HttpUtils.sendPostJsonRequest(url, JSON.toJSONString(info));
        JSONObject resp = JSON.parseObject(respStr);

        return resp;
    };

    public JSONObject getActivateTempinfo (Map<String, Object> activate_ticket) {

        AccessToken token = this.ensureAccessToken();
        String accessToken = token.getAccessToken();

        String url = "https://api.weixin.qq.com/card/membercard/activatetempinfo/get?access_token=" + accessToken;
        Map<String, Object> data = new HashMap<String, Object>();
        data.put("activate_ticket", activate_ticket);
        String respStr = HttpUtils.sendPostJsonRequest(url, JSON.toJSONString(data));
        JSONObject resp = JSON.parseObject(respStr);

        return resp;
    };

    public JSONObject activateUserForm (Map<String, Object> data) {

        AccessToken token = this.ensureAccessToken();
        String accessToken = token.getAccessToken();

        String url = "https://api.weixin.qq.com/card/membercard/activateuserform/set?access_token=" + accessToken;

        String respStr = HttpUtils.sendPostJsonRequest(url, JSON.toJSONString(data));
        JSONObject resp = JSON.parseObject(respStr);

        return resp;
    };

    public JSONObject updateMovieTicket (Map<String, Object> info) {

        AccessToken token = this.ensureAccessToken();
        String accessToken = token.getAccessToken();

        String url = "https://api.weixin.qq.com/card/movieticket/updateuser?access_token=" + accessToken;

        String respStr = HttpUtils.sendPostJsonRequest(url, JSON.toJSONString(info));
        JSONObject resp = JSON.parseObject(respStr);

        return resp;
    };

    public JSONObject checkInBoardingPass (Map<String, Object> info) {

        AccessToken token = this.ensureAccessToken();
        String accessToken = token.getAccessToken();

        String url = "https://api.weixin.qq.com/card/boardingpass/checkin?access_token=" + accessToken;

        String respStr = HttpUtils.sendPostJsonRequest(url, JSON.toJSONString(info));
        JSONObject resp = JSON.parseObject(respStr);

        return resp;
    };

    public JSONObject updateLuckyMonkeyBalance (String code, String cardId, String balance) {

        AccessToken token = this.ensureAccessToken();
        String accessToken = token.getAccessToken();

        String url = "https://api.weixin.qq.com/card/luckymonkey/updateuserbalance?access_token=" + accessToken;

        Map<String, Object> data = new HashMap<String, Object>();
        data.put("code", code);
        data.put("card_id", cardId);
        data.put("balance", balance);

        String respStr = HttpUtils.sendPostJsonRequest(url, JSON.toJSONString(data));
        JSONObject resp = JSON.parseObject(respStr);

        return resp;
    };

    public JSONObject updateMeetingTicket (Map<String, Object> info) {

        AccessToken token = this.ensureAccessToken();
        String accessToken = token.getAccessToken();

        String url = "https://api.weixin.qq.com/card/meetingticket/updateuser?access_token=" + accessToken;

        String respStr = HttpUtils.sendPostJsonRequest(url, JSON.toJSONString(info));
        JSONObject resp = JSON.parseObject(respStr);

        return resp;
    };

    public JSONObject setTestWhitelist (Map<String, Object> info) {

        AccessToken token = this.ensureAccessToken();
        String accessToken = token.getAccessToken();

        String url = "https://api.weixin.qq.com/card/testwhitelist/set?access_token=" + accessToken;

        String respStr = HttpUtils.sendPostJsonRequest(url, JSON.toJSONString(info));
        JSONObject resp = JSON.parseObject(respStr);

        return resp;
    }


    /**
     * 鍏紬骞冲彴瀹樼綉鏁版嵁缁熻妯″潡
     * 璇︽儏璇疯锛�<http://mp.weixin.qq.com/wiki/8/c0453610fb5131d1fcb17b4e87c82050.html>
     * Examples:
     * ```
     * JSONArray result = api.datacube(
     *      DatacubeType.getArticleSummary,
     *      startDate,
     *      endDate
     *      );          // 鑾峰彇鎺ュ彛鍒嗘瀽鍒嗘椂鏁版嵁
     * ```
     * >   // 鐢ㄦ埛鍒嗘瀽鏁版嵁鎺ュ彛
     * >   getUserSummary,                 // 鑾峰彇鐢ㄦ埛澧炲噺鏁版嵁
     * >   getUserCumulate,                // 鑾峰彇绱鐢ㄦ埛鏁版嵁
     * >
     * >   // 鍥炬枃鍒嗘瀽鏁版嵁鎺ュ彛
     * >   getArticleSummary,              // 鑾峰彇鍥炬枃缇ゅ彂姣忔棩鏁版嵁
     * >   getArticleTotal,                // 鑾峰彇鍥炬枃缇ゅ彂鎬绘暟鎹�
     * >   getUserRead,                    // 鑾峰彇鍥炬枃缁熻鏁版嵁
     * >   getUserReadHour,                // 鑾峰彇鍥炬枃缁熻鍒嗘椂鏁版嵁
     * >   getUserShare,                   // 鑾峰彇鍥炬枃鍒嗕韩杞彂鏁版嵁
     * >   getUserShareHour,               // 鑾峰彇鍥炬枃鍒嗕韩杞彂鍒嗘椂鏁版嵁
     * >
     * >   // 娑堟伅鍒嗘瀽鏁版嵁鎺ュ彛
     * >   getUpstreamMsg,                 //鑾峰彇娑堟伅鍙戦�佹鍐垫暟鎹�
     * >   getUpstreamMsgHour,             // 鑾峰彇娑堟伅鍒嗛�佸垎鏃舵暟鎹�
     * >   getUpstreamMsgWeek,             // 鑾峰彇娑堟伅鍙戦�佸懆鏁版嵁
     * >   getUpstreamMsgMonth,            // 鑾峰彇娑堟伅鍙戦�佹湀鏁版嵁
     * >   getUpstreamMsgDist,             // 鑾峰彇娑堟伅鍙戦�佸垎甯冩暟鎹�
     * >   getUpstreamMsgDistWeek,         // 鑾峰彇娑堟伅鍙戦�佸垎甯冨懆鏁版嵁
     * >   getUpstreamMsgDistMonth,        // 鑾峰彇娑堟伅鍙戦�佸垎甯冩湀鏁版嵁
     * >
     * >   // 鎺ュ彛鍒嗘瀽鏁版嵁鎺ュ彛
     * >   getInterfaceSummary,            // 鑾峰彇鎺ュ彛鍒嗘瀽鏁版嵁
     * >   getInterfaceSummaryHour,        // 鑾峰彇鎺ュ彛鍒嗘瀽鍒嗘椂鏁版嵁
     * > startDate 璧峰鏃ユ湡锛屾牸寮忎负 2014-12-08
     * > endDate 缁撴潫鏃ユ湡锛屾牸寮忎负 2014-12-08
     * Result:
     * ```
     * [{
     *     ...
     * }] // 璇︾粏璇峰弬瑙�<http://mp.weixin.qq.com/wiki/8/c0453610fb5131d1fcb17b4e87c82050.html>
     *
     * ```
     * @param {String} startDate 璧峰鏃ユ湡锛屾牸寮忎负2014-12-08
     * @param {String} endDate 缁撴潫鏃ユ湡锛屾牸寮忎负2014-12-08
     */
    public JSONArray datacube (DatacubeType type, String begin, String end) {

        AccessToken token = this.ensureAccessToken();
        String accessToken = token.getAccessToken();

        String url = "https://api.weixin.qq.com/datacube/" + type + "?access_token=" + accessToken;
        Map<String, Object> data = new HashMap<String, Object>();
        data.put("begin_date", begin);
        data.put("end_date", end);

        String respStr = HttpUtils.sendPostJsonRequest(url, JSON.toJSONString(data));
        JSONObject resp = JSON.parseObject(respStr);

        JSONArray list = resp.getJSONArray("list");

        return list;
    }


    /**
     * 浼犺緭娑堟伅
     * Examples:
     * ```
     * JSONObject result = api.transferMessage(
     *      'deviceType',
     *      'deviceId',
     *      'openid',
     *      'content'
     * );
     * ```
     *
     * @param deviceType
     * @param deviceId
     * @param openid
     * @param content
     * @return
     */
    public JSONObject transferMessage (String deviceType, String deviceId, String openid, String content) {

        AccessToken token = this.ensureAccessToken();
        String accessToken = token.getAccessToken();

        // https://api.weixin.qq.com/device/transmsg?access_token=ACCESS_TOKEN
        String url = "https://api.weixin.qq.com/device/transmsg?access_token=" + accessToken;
        Map<String, Object> data = new HashMap<String, Object>();
        data.put("device_type", deviceType);
        data.put("device_id", deviceId);
        data.put("open_id", openid);
        data.put("content", Base64Utils.encode(content));

        String respStr = HttpUtils.sendPostJsonRequest(url, JSON.toJSONString(data));
        JSONObject resp = JSON.parseObject(respStr);

        return resp;
    };

    public JSONObject transferStatus  (String deviceType, String deviceId, String openid, String status) {

        AccessToken token = this.ensureAccessToken();
        String accessToken = token.getAccessToken();

        // https://api.weixin.qq.com/device/transmsg?access_token=ACCESS_TOKEN
        String url = "https://api.weixin.qq.com/device/transmsg?access_token=" + accessToken;
        Map<String, Object> data = new HashMap<String, Object>();
        data.put("device_type", deviceType);
        data.put("device_id", deviceId);
        data.put("open_id", openid);
        data.put("msg_type", 2);
        data.put("device_status", status);

        String respStr = HttpUtils.sendPostJsonRequest(url, JSON.toJSONString(data));
        JSONObject resp = JSON.parseObject(respStr);

        return resp;
    };

    /**
     * 鍒涘缓璁惧浜岀淮鐮�
     * Examples:
     * ```
     * JSONObject result = api.createDeviceQRCode(List<String> deviceIds);
     * ```
     * @param deviceIds
     * @return
     */
    public JSONObject createDeviceQRCode (List<String> deviceIds) {

        AccessToken token = this.ensureAccessToken();
        String accessToken = token.getAccessToken();

        // https://api.weixin.qq.com/device/create_qrcode?access_token=ACCESS_TOKEN
        String url = "https://api.weixin.qq.com/device/create_qrcode?access_token=" + accessToken;
        Map<String, Object> data = new HashMap<String, Object>();
        data.put("device_num", deviceIds.size());
        data.put("device_id_list", deviceIds);

        String respStr = HttpUtils.sendPostJsonRequest(url, JSON.toJSONString(data));
        JSONObject resp = JSON.parseObject(respStr);

        return resp;
    };

    public JSONObject authorizeDevices (List<Map<String, Object>> devices, String optype) {

        AccessToken token = this.ensureAccessToken();
        String accessToken = token.getAccessToken();

        // https://api.weixin.qq.com/device/authorize_device?access_token=ACCESS_TOKEN
        String url = "https://api.weixin.qq.com/device/authorize_device?access_token=" + accessToken;

        Map<String, Object> data = new HashMap<String, Object>();
        data.put("device_num", devices.size());
        data.put("device_list", devices);
        data.put("op_type", optype);

        String respStr = HttpUtils.sendPostJsonRequest(url, JSON.toJSONString(data));
        JSONObject resp = JSON.parseObject(respStr);

        return resp;

    };

    public JSONObject getDeviceQRCode () {

        AccessToken token = this.ensureAccessToken();
        String accessToken = token.getAccessToken();

        // https://api.weixin.qq.com/device/getqrcode?access_token=ACCESS_TOKEN
        String url = "https://api.weixin.qq.com/device/getqrcode?access_token=" + accessToken;

        String respStr = HttpUtils.sendGetRequest(url);
        JSONObject resp = JSON.parseObject(respStr);

        return resp;
    };

    public JSONObject bindDevice (String deviceId, String openid, String ticket) {

        AccessToken token = this.ensureAccessToken();
        String accessToken = token.getAccessToken();

        // https://api.weixin.qq.com/device/bind?access_token=ACCESS_TOKEN
        String  url = "https://api.weixin.qq.com/device/bind?access_token=" + accessToken;

        Map<String, Object> data = new HashMap<String, Object>();
        data.put("ticket", ticket);
        data.put("device_id", deviceId);
        data.put("openid", openid);

        String respStr = HttpUtils.sendPostJsonRequest(url, JSON.toJSONString(data));
        JSONObject resp = JSON.parseObject(respStr);

        return resp;
    };

    public JSONObject unbindDevice (String deviceId, String openid, String ticket) {

        AccessToken token = this.ensureAccessToken();
        String accessToken = token.getAccessToken();

        // https://api.weixin.qq.com/device/unbind?access_token=ACCESS_TOKEN
        String url = "https://api.weixin.qq.com/device/unbind?access_token=" + accessToken;

        Map<String, Object> data = new HashMap<String, Object>();
        data.put("ticket", ticket);
        data.put("device_id", deviceId);
        data.put("openid", openid);

        String respStr = HttpUtils.sendPostJsonRequest(url, JSON.toJSONString(data));
        JSONObject resp = JSON.parseObject(respStr);

        return resp;
    }

    public JSONObject compelBindDevice (String deviceId, String openid) {

        AccessToken token = this.ensureAccessToken();
        String accessToken = token.getAccessToken();

        // https://api.weixin.qq.com/device/compel_bind?access_token=ACCESS_TOKEN
        String url = "https://api.weixin.qq.com/device/compel_bind?access_token=" + accessToken;

        Map<String, Object> data = new HashMap<String, Object>();
        data.put("device_id", deviceId);
        data.put("openid", openid);

        String respStr = HttpUtils.sendPostJsonRequest(url, JSON.toJSONString(data));
        JSONObject resp = JSON.parseObject(respStr);

        return resp;
    }

    public JSONObject compelUnbindDevice (String deviceId, String openid) {

        AccessToken token = this.ensureAccessToken();
        String accessToken = token.getAccessToken();

        // https://api.weixin.qq.com/device/compel_unbind?access_token=ACCESS_TOKEN
        String url = "https://api.weixin.qq.com/device/compel_unbind?access_token=" + accessToken;

        Map<String, Object> data = new HashMap<String, Object>();
        data.put("device_id", deviceId);
        data.put("openid", openid);

        String respStr = HttpUtils.sendPostJsonRequest(url, JSON.toJSONString(data));
        JSONObject resp = JSON.parseObject(respStr);

        return resp;
    }

    public JSONObject getDeviceStatus (String deviceId) {

        AccessToken token = this.ensureAccessToken();
        String accessToken = token.getAccessToken();

        // https://api.weixin.qq.com/device/get_stat?access_token=ACCESS_TOKEN&device_id=DEVICE_ID
        String url = "https://api.weixin.qq.com/device/get_stat?access_token=" + accessToken + "&device_id=" + deviceId;

        String respStr = HttpUtils.sendGetRequest(url);
        JSONObject resp = JSON.parseObject(respStr);

        return resp;
    }

    public JSONObject verifyDeviceQRCode  (String ticket) {

        AccessToken token = this.ensureAccessToken();
        String accessToken = token.getAccessToken();

        // https://api.weixin.qq.com/device/verify_qrcode?access_token=ACCESS_TOKEN
        String url = "https://api.weixin.qq.com/device/verify_qrcode?access_token=" + accessToken;

        Map<String, Object> data = new HashMap<String, Object>();
        data.put("ticket", ticket);

        String respStr = HttpUtils.sendPostJsonRequest(url, JSON.toJSONString(data));
        JSONObject resp = JSON.parseObject(respStr);

        return resp;
    }

    public JSONObject getOpenID (String deviceId, String deviceType) {

        AccessToken token = this.ensureAccessToken();
        String accessToken = token.getAccessToken();

        // https://api.weixin.qq.com/device/get_openid?access_token=ACCESS_TOKEN&device_type=DEVICE_TYPE&device_id=DEVICE_ID
        String url = "https://api.weixin.qq.com/device/get_openid?access_token="
                + accessToken
                + "&device_id="
                + deviceId
                + "&device_type="
                + deviceType;

        String respStr = HttpUtils.sendGetRequest(url);
        JSONObject resp = JSON.parseObject(respStr);

        return resp;
    }

    public JSONObject getBindDevice (String openid) {

        AccessToken token = this.ensureAccessToken();
        String accessToken = token.getAccessToken();

        // https://api.weixin.qq.com/device/get_bind_device?access_token=ACCESS_TOKEN&openid=OPENID
        String url = "https://api.weixin.qq.com/device/get_bind_device?access_token="
                + accessToken
                + "&openid="
                + openid;

        String respStr = HttpUtils.sendGetRequest(url);
        JSONObject resp = JSON.parseObject(respStr);

        return resp;
    }


    /**
     * 鏍囪瀹㈡埛鐨勬姇璇夊鐞嗙姸鎬�
     * Examples:
     * ```
     * api.updateFeedback(openid, feedbackId);
     * ```
     *
     * Result:
     * ```
     * {
     *  "errcode": 0,
     *  "errmsg": "success"
     * }
     * ```
     * @param {String} openid 鐢ㄦ埛ID
     * @param {String} feedbackId 鎶曡瘔ID
     */
    public boolean updateFeedback (String openid, String feedbackId) {

        AccessToken token = this.ensureAccessToken();
        String accessToken = token.getAccessToken();

        String feedbackUrl = "https://api.weixin.qq.com/payfeedback/update?access_token=";
        // https://api.weixin.qq.com/payfeedback/update?access_token=xxxxx&openid=XXXX&feedbackid=xxxx
        String url = feedbackUrl
                + accessToken
                + "&openid="
                + openid
                + "&feedbackid="
                + feedbackId;

        String respStr = HttpUtils.sendGetRequest(url);
        JSONObject resp = JSON.parseObject(respStr);

        int errCode = resp.getIntValue("errcode");
        if(errCode == 0){
            return true;
        }else{
            return false;
        }
    }

    /**
     * 鑾峰彇鍒嗙粍鍒楄〃
     * 璇︽儏璇疯锛�<http://mp.weixin.qq.com/wiki/0/56d992c605a97245eb7e617854b169fc.html>
     * Examples:
     * ```
     * api.getGroups();
     * ```
     * Result:
     * ```
     * {
     *  "groups": [
     *    {"id": 0, "name": "鏈垎缁�", "count": 72596},
     *    {"id": 1, "name": "榛戝悕鍗�", "count": 36}
     *  ]
     * }
     * ```
     */
    public JSONArray getGroups () {
        AccessToken token = this.ensureAccessToken();
        String accessToken = token.getAccessToken();

        // https://api.weixin.qq.com/cgi-bin/groups/get?access_token=ACCESS_TOKEN
        String url = this.PREFIX + "groups/get?access_token=" + accessToken;

        String respStr = HttpUtils.sendGetRequest(url);
        JSONObject resp = JSON.parseObject(respStr);

        JSONArray groups = resp.getJSONArray("groups");
        return groups;
    };

    /**
     * 鏌ヨ鐢ㄦ埛鍦ㄥ摢涓垎缁�
     * 璇︽儏璇疯锛�<http://mp.weixin.qq.com/wiki/0/56d992c605a97245eb7e617854b169fc.html>
     * Examples:
     * ```
     * api.getWhichGroup(openid);
     * ```
     * Result:
     * ```
     * {
     *  "groupid": 102
     * }
     * ```
     * @param {String} openid Open ID
     */
    public JSONObject getWhichGroup (String openid) {

        AccessToken token = this.ensureAccessToken();
        String accessToken = token.getAccessToken();

        // https://api.weixin.qq.com/cgi-bin/groups/getid?access_token=ACCESS_TOKEN
        String url = this.PREFIX + "groups/getid?access_token=" + accessToken;

        Map<String, Object> data = new HashMap<String, Object>();
        data.put("openid", openid);

        String respStr = HttpUtils.sendPostJsonRequest(url, JSON.toJSONString(data));
        JSONObject resp = JSON.parseObject(respStr);

        return resp;
    };

    /**
     * 鍒涘缓鍒嗙粍
     * 璇︽儏璇疯锛�<http://mp.weixin.qq.com/wiki/0/56d992c605a97245eb7e617854b169fc.html>
     * Examples:
     * ```
     * api.createGroup('groupname');
     * ```
     * Result:
     * ```
     * {"group": {"id": 107, "name": "test"}}
     * ```
     * @param {String} name 鍒嗙粍鍚嶅瓧
     */
    public JSONObject createGroup (String name) {

        AccessToken token = this.ensureAccessToken();
        String accessToken = token.getAccessToken();

        // https://api.weixin.qq.com/cgi-bin/groups/create?access_token=ACCESS_TOKEN
        // POST鏁版嵁鏍煎紡锛歫son
        // POST鏁版嵁渚嬪瓙锛歿"group":{"name":"test"}}
        String url = this.PREFIX + "groups/create?access_token=" + accessToken;

        Map<String, Object> data = new HashMap<String, Object>();
        Map<String, Object> group = new HashMap<String, Object>();
            group.put("name", name);
        data.put("group", group);

        String respStr = HttpUtils.sendPostJsonRequest(url, JSON.toJSONString(data));
        JSONObject resp = JSON.parseObject(respStr);

        return resp;
    };

    /**
     * 鏇存柊鍒嗙粍鍚嶅瓧
     * 璇︽儏璇疯锛�<http://mp.weixin.qq.com/wiki/0/56d992c605a97245eb7e617854b169fc.html>
     * Examples:
     * ```
     * api.updateGroup(107, 'new groupname');
     * ```
     * Result:
     * ```
     * {"errcode": 0, "errmsg": "ok"}
     * ```
     * @param {Number} id 鍒嗙粍ID
     * @param {String} name 鏂扮殑鍒嗙粍鍚嶅瓧
     */
    public boolean updateGroup (String id, String name) {

        AccessToken token = this.ensureAccessToken();
        String accessToken = token.getAccessToken();

        // http璇锋眰鏂瑰紡: POST锛堣浣跨敤https鍗忚锛�
        // https://api.weixin.qq.com/cgi-bin/groups/update?access_token=ACCESS_TOKEN
        // POST鏁版嵁鏍煎紡锛歫son
        // POST鏁版嵁渚嬪瓙锛歿"group":{"id":108,"name":"test2_modify2"}}
        String url = this.PREFIX + "groups/update?access_token=" + accessToken;

        Map<String, Object> data = new HashMap<String, Object>();
            Map<String, Object> group = new HashMap<String, Object>();
                group.put("id", id);
                group.put("name", name);
        data.put("group", group);

        String respStr = HttpUtils.sendPostJsonRequest(url, JSON.toJSONString(data));
        JSONObject resp = JSON.parseObject(respStr);

        int errCode = resp.getIntValue("errcode");
        if(errCode == 0){
            return true;
        }else{
            return false;
        }
    };

    /**
     * 绉诲姩鐢ㄦ埛杩涘垎缁�
     * 璇︽儏璇疯锛�<http://mp.weixin.qq.com/wiki/0/56d992c605a97245eb7e617854b169fc.html>
     * Examples:
     * ```
     * api.moveUserToGroup(openid, groupId);
     * ```
     * Result:
     * ```
     * {"errcode": 0, "errmsg": "ok"}
     * ```
     * @param {String} openid 鐢ㄦ埛鐨刼penid
     * @param {Number} groupId 鍒嗙粍ID
     */
    public boolean moveUserToGroup (String openid, String groupId) {

        AccessToken token = this.ensureAccessToken();
        String accessToken = token.getAccessToken();

        // http璇锋眰鏂瑰紡: POST锛堣浣跨敤https鍗忚锛�
        // https://api.weixin.qq.com/cgi-bin/groups/members/update?access_token=ACCESS_TOKEN
        // POST鏁版嵁鏍煎紡锛歫son
        // POST鏁版嵁渚嬪瓙锛歿"openid":"oDF3iYx0ro3_7jD4HFRDfrjdCM58","to_groupid":108}
        String url = this.PREFIX + "groups/members/update?access_token=" + accessToken;

        Map<String, Object> data = new HashMap<String, Object>();
        data.put("openid", openid);
        data.put("to_groupid", groupId);

        String respStr = HttpUtils.sendPostJsonRequest(url, JSON.toJSONString(data));
        JSONObject resp = JSON.parseObject(respStr);

        int errCode = resp.getIntValue("errcode");
        if(errCode == 0){
            return true;
        }else{
            return false;
        }
    };

    /**
     * 鎵归噺绉诲姩鐢ㄦ埛鍒嗙粍
     * 璇︽儏璇疯锛�<http://mp.weixin.qq.com/wiki/8/d6d33cf60bce2a2e4fb10a21be9591b8.html>
     * Examples:
     * ```
     * api.moveUsersToGroup(openids, groupId);
     * ```
     * Result:
     * ```
     * {"errcode": 0, "errmsg": "ok"}
     * ```
     * @param {String} openids 鐢ㄦ埛鐨刼penid鏁扮粍
     * @param {Number} groupId 鍒嗙粍ID
     */
    public boolean moveUsersToGroup (List<String> openids, String groupId) {

        AccessToken token = this.ensureAccessToken();
        String accessToken = token.getAccessToken();

        // http璇锋眰鏂瑰紡: POST锛堣浣跨敤https鍗忚锛�
        // https://api.weixin.qq.com/cgi-bin/groups/members/batchupdate?access_token=ACCESS_TOKEN
        // POST鏁版嵁鏍煎紡锛歫son
        // POST鏁版嵁渚嬪瓙锛歿"openid_list":["oDF3iYx0ro3_7jD4HFRDfrjdCM58","oDF3iY9FGSSRHom3B-0w5j4jlEyY"],"to_groupid":108}
        String url = this.PREFIX + "groups/members/batchupdate?access_token=" + accessToken;

        Map<String, Object> data = new HashMap<String, Object>();
        data.put("openid_list", openids);
        data.put("to_groupid", groupId);

        String respStr = HttpUtils.sendPostJsonRequest(url, JSON.toJSONString(data));
        JSONObject resp = JSON.parseObject(respStr);

        int errCode = resp.getIntValue("errcode");
        if(errCode == 0){
            return true;
        }else{
            return false;
        }
    };


    /**
     * 鍒犻櫎鍒嗙粍
     * 璇︽儏璇疯锛�<http://mp.weixin.qq.com/wiki/0/56d992c605a97245eb7e617854b169fc.html>
     * Examples:
     * ```
     * api.removeGroup(groupId);
     * ```
     * Result:
     * ```
     * {"errcode": 0, "errmsg": "ok"}
     * ```
     * @param {Number} groupId 鍒嗙粍ID
     */
    public boolean removeGroup (String groupId) {
        AccessToken token = this.ensureAccessToken();
        String accessToken = token.getAccessToken();

        String url = this.PREFIX + "groups/delete?access_token=" + accessToken;

        Map<String, Object> data = new HashMap<String, Object>();
        Map<String, Object> group = new HashMap<String, Object>();
            group.put("id", groupId);
        data.put("group", group);

        String respStr = HttpUtils.sendPostJsonRequest(url, JSON.toJSONString(data));
        JSONObject resp = JSON.parseObject(respStr);

        int errCode = resp.getIntValue("errcode");
        if(errCode == 0){
            return true;
        }else{
            return false;
        }
    }



    /**
     * 寰俊鍏紬鍙锋敮浠�: 鍙戣揣閫氱煡
     * 璇︽儏璇疯锛�<http://mp.weixin.qq.com/htmledition/res/bussiness-faq/wx_mp_pay.zip> 鎺ュ彛鏂囨。璁㈠崟鍙戣揣閫氱煡 * Data:
     * ```
     * {
     *   "appid" : "wwwwb4f85f3a797777",
     *   "openid" : "oX99MDgNcgwnz3zFN3DNmo8uwa-w",
     *   "transid" : "111112222233333",
     *   "out_trade_no" : "555666uuu",
     *   "deliver_timestamp" : "1369745073",
     *   "deliver_status" : "1",
     *   "deliver_msg" : "ok",
     *   "app_signature" : "53cca9d47b883bd4a5c85a9300df3da0cb48565c",
     *   "sign_method" : "sha1"
     * }
     * ```
     * Examples:
     * ```
     * api.deliverNotify(data);
     * ```
     * Result:
     * ```
     * {"errcode":0, "errmsg":"ok"}
     * ``` * @param {Object} package package瀵硅薄
     */
    public boolean deliverNotify (Map<String, Object> data) {
        AccessToken token = this.ensureAccessToken();
        String accessToken = token.getAccessToken();

        String url = this.PAY_PREFIX + "delivernotify?access_token=" + accessToken;

        String respStr = HttpUtils.sendPostJsonRequest(url, JSON.toJSONString(data));
        JSONObject resp = JSON.parseObject(respStr);
        int errCode = resp.getIntValue("errcode");
        if(errCode == 0){
            return true;
        }else{
            return false;
        }
    };

    /**
     * 寰俊鍏紬鍙锋敮浠�: 璁㈠崟鏌ヨ
     * 璇︽儏璇疯锛�<http://mp.weixin.qq.com/htmledition/res/bussiness-faq/wx_mp_pay.zip> 鎺ュ彛鏂囨。璁㈠崟鏌ヨ閮ㄥ垎 * Package:
     * ```
     * {
     *   "appid" : "wwwwb4f85f3a797777",
     *   "package" : "out_trade_no=11122&partner=1900090055&sign=4e8d0df3da0c3d0df38f",
     *   "timestamp" : "1369745073",
     *   "app_signature" : "53cca9d47b883bd4a5c85a9300df3da0cb48565c",
     *   "sign_method" : "sha1"
     * }
     * ```
     * Examples:
     * ```
     * api.orderQuery(query);
     * ```
     * Result:
     * ```
     * {
     *   "errcode":0,
     *   "errmsg":"ok",
     *   "order_info": {
     *     "ret_code":0,
     *     "ret_msg":"",
     *     "input_charset":"GBK",
     *     "trade_state":"0",
     *     "trade_mode":"1",
     *     "partner":"1900000109",
     *     "bank_type":"CMB_FP",
     *     "bank_billno":"207029722724",
     *     "total_fee":"1",
     *     "fee_type":"1",
     *     "transaction_id":"1900000109201307020305773741",
     *     "out_trade_no":"2986872580246457300",
     *     "is_split":"false",
     *     "is_refund":"false",
     *     "attach":"",
     *     "time_end":"20130702175943",
     *     "transport_fee":"0",
     *     "product_fee":"1",
     *     "discount":"0",
     *     "rmb_total_fee":""
     *   }
     * }
     * ``` * @param {Object} query query瀵硅薄
     */
    public JSONObject orderQuery (Map<String, String> query) {

        AccessToken token = this.ensureAccessToken();
        String accessToken = token.getAccessToken();

        String url = this.PAY_PREFIX + "orderquery?access_token=" + accessToken;

        String respStr = HttpUtils.sendPostJsonRequest(url, JSON.toJSONString(query));
        JSONObject resp = JSON.parseObject(respStr);

        return resp;
    }


    /**
     * 鍒涘缓涓存椂浜岀淮鐮�
     * 璇︾粏璇风湅锛�<http://mp.weixin.qq.com/wiki/index.php?title=鐢熸垚甯﹀弬鏁扮殑浜岀淮鐮�>
     * Examples:
     * ```
     * api.createTmpQRCode(10000, 1800);
     * ```
     *
     * Result:
     * ```
     * {
     *  "ticket":"gQG28DoAAAAAAAAAASxodHRwOi8vd2VpeGluLnFxLmNvbS9xL0FuWC1DNmZuVEhvMVp4NDNMRnNRAAIEesLvUQMECAcAAA==",
     *  "expire_seconds":1800
     * }
     * ```
     * @param {Number} sceneId 鍦烘櫙ID
     * @param {Number} expire 杩囨湡鏃堕棿锛屽崟浣嶇銆傛渶澶т笉瓒呰繃1800
     */
    public JSONObject createTmpQRCode  (Integer sceneId, Integer expire) {

        AccessToken token = this.ensureAccessToken();
        String accessToken = token.getAccessToken();

        String url = this.PREFIX + "qrcode/create?access_token=" + accessToken;

        Map<String, Object> data = new HashMap<String, Object>();
            Map<String, Object> action_info = new HashMap<String, Object>();
                Map<String, Object> scene = new HashMap<String, Object>();
                scene.put("scene_id", sceneId);
            action_info.put("scene", scene);
        data.put("expire_seconds", expire);
        data.put("action_name", "QR_SCENE");
        data.put("action_info", action_info);

        String respStr = HttpUtils.sendPostJsonRequest(url, JSON.toJSONString(data));
        JSONObject resp = JSON.parseObject(respStr);

        return resp;
    };

    /**
     * 鍒涘缓姘镐箙浜岀淮鐮�
     * 璇︾粏璇风湅锛�<http://mp.weixin.qq.com/wiki/index.php?title=鐢熸垚甯﹀弬鏁扮殑浜岀淮鐮�>
     * Examples:
     * ```
     * api.createLimitQRCode(100);
     * ```
     *
     * Result:
     * ```
     * {
     *  "ticket":"gQG28DoAAAAAAAAAASxodHRwOi8vd2VpeGluLnFxLmNvbS9xL0FuWC1DNmZuVEhvMVp4NDNMRnNRAAIEesLvUQMECAcAAA=="
     * }
     * ```
     * @param {Number} sceneId 鍦烘櫙ID銆侷D涓嶈兘澶т簬100000
     */
    public String createLimitQRCode (Integer sceneId) {

        AccessToken token = this.ensureAccessToken();
        String accessToken = token.getAccessToken();

        String url = this.PREFIX + "qrcode/create?access_token=" + accessToken;

        Map<String, Object> data = new HashMap<String, Object>();
            Map<String, Object> action_info = new HashMap<String, Object>();
                Map<String, Object> scene = new HashMap<String, Object>();
                scene.put("scene_id", sceneId);
            action_info.put("scene", scene);
        data.put("action_name", "QR_LIMIT_SCENE");
        data.put("action_info", action_info);

        String respStr = HttpUtils.sendPostJsonRequest(url, JSON.toJSONString(data));
        JSONObject resp = JSON.parseObject(respStr);

        String ticket = resp.getString("ticket");

        return ticket;
    }

    /**
     * 鐢熸垚鏄剧ず浜岀淮鐮佺殑閾炬帴銆傚井淇℃壂鎻忓悗锛屽彲绔嬪嵆杩涘叆鍦烘櫙
     * Examples:
     * ```
     * api.showQRCodeURL(ticket);
     * // => https://mp.weixin.qq.com/cgi-bin/showqrcode?ticket=TICKET
     * ```
     * @param {String} ticket 浜岀淮鐮乀icket
     * @return {String} 鏄剧ず浜岀淮鐮佺殑URL鍦板潃锛岄�氳繃img鏍囩鍙互鏄剧ず鍑烘潵 */
    public String showQRCodeURL (String ticket) {
        return this.MP_PREFIX + "showqrcode?ticket=" + ticket;
    }


    /**
     * 鐭綉鍧�鏈嶅姟
     * 璇︾粏缁嗚妭 http://mp.weixin.qq.com/wiki/index.php?title=闀块摼鎺ヨ浆鐭摼鎺ユ帴鍙�
     * Examples:
     * ```
     * api.shortUrl('http://mp.weixin.com');
     * ```
     * @param {String} longUrl 闇�瑕佽浆鎹㈢殑闀块摼鎺ワ紝鏀寔http://銆乭ttps://銆亀eixin://wxpay鏍煎紡鐨剈rl
     */
    public JSONObject shortUrl (String longUrl) {

        AccessToken token = this.ensureAccessToken();
        String accessToken = token.getAccessToken();

        // https://api.weixin.qq.com/cgi-bin/shorturl?access_token=ACCESS_TOKEN
        String url = this.PREFIX + "shorturl?access_token=" + accessToken;

        Map<String, Object> data = new HashMap<String, Object>();

        data.put("action", "long2short");
        data.put("long_url", longUrl);

        String respStr = HttpUtils.sendPostJsonRequest(url, JSON.toJSONString(data));
        JSONObject resp = JSON.parseObject(respStr);

        return resp;
    }



    /**
     * 涓婁紶澶氬獟浣撴枃浠讹紝鍒嗗埆鏈夊浘鐗囷紙image锛夈�佽闊筹紙voice锛夈�佽棰戯紙video锛夊拰缂╃暐鍥撅紙thumb锛�
     * 璇︽儏璇疯锛�<http://mp.weixin.qq.com/wiki/15/5380a4e6f02f2ffdc7981a8ed7a40753.html>
     * Examples:
     * ```
     * api.uploadNews(news);
     * ```
     * News:
     * ```
     * [
     *    {
     *      "thumb_media_id":"qI6_Ze_6PtV7svjolgs-rN6stStuHIjs9_DidOHaj0Q-mwvBelOXCFZiq2OsIU-p",
     *      "author":"xxx",
     *      "title":"Happy Day",
     *      "content_source_url":"www.qq.com",
     *      "content":"content",
     *      "digest":"digest",
     *      "show_cover_pic":"1"
     *   },
     *   {
     *      "thumb_media_id":"qI6_Ze_6PtV7svjolgs-rN6stStuHIjs9_DidOHaj0Q-mwvBelOXCFZiq2OsIU-p",
     *      "author":"xxx",
     *      "title":"Happy Day",
     *      "content_source_url":"www.qq.com",
     *      "content":"content",
     *      "digest":"digest",
     *      "show_cover_pic":"0"
     *   }
     *  ]
     * ```
     * Result:
     * ```
     * {
     *  "type":"news",
     *  "media_id":"CsEf3ldqkAYJAU6EJeIkStVDSvffUJ54vqbThMgplD-VJXXof6ctX5fI6-aYyUiQ",
     *  "created_at":1391857799
     * }
     * ```
     * @param {Object} news 鍥炬枃娑堟伅瀵硅薄
     */
    public JSONObject uploadNews (List<Map<String, Object>> news) {

        AccessToken token = this.ensureAccessToken();
        String accessToken = token.getAccessToken();

        // https://api.weixin.qq.com/cgi-bin/media/uploadnews?access_token=ACCESS_TOKEN
        String url = this.PREFIX + "media/uploadnews?access_token=" + accessToken;

        Map<String, Object> data = new HashMap<String, Object>();
        data.put("articles", news);

        String respStr = HttpUtils.sendPostJsonRequest(url, JSON.toJSONString(data));
        JSONObject resp = JSON.parseObject(respStr);

        return resp;
    };

    /**
     * 灏嗛�氳繃涓婁紶涓嬭浇澶氬獟浣撴枃浠跺緱鍒扮殑瑙嗛media_id鍙樻垚瑙嗛绱犳潗
     * 璇︽儏璇疯锛�<http://mp.weixin.qq.com/wiki/15/5380a4e6f02f2ffdc7981a8ed7a40753.html>
     * Examples:
     * ```
     * api.uploadMPVideo(opts);
     * ```
     * Opts:
     * ```
     * {
     *  "media_id": "rF4UdIMfYK3efUfyoddYRMU50zMiRmmt_l0kszupYh_SzrcW5Gaheq05p_lHuOTQ",
     *  "title": "TITLE",
     *  "description": "Description"
     * }
     * ```
     * Result:
     * ```
     * {
     *  "type":"video",
     *  "media_id":"IhdaAQXuvJtGzwwc0abfXnzeezfO0NgPK6AQYShD8RQYMTtfzbLdBIQkQziv2XJc",
     *  "created_at":1391857799
     * }
     * ```
     * @param {Object} opts 寰呬笂浼犱负绱犳潗鐨勮棰�
     */
    public JSONObject uploadMPVideo (Map<String, Object> opts) {

        AccessToken token = this.ensureAccessToken();
        String accessToken = token.getAccessToken();

        // https://file.api.weixin.qq.com/cgi-bin/media/uploadvideo?access_token=ACCESS_TOKEN
        String url = this.FILE_SERVER_PREFIX + "media/uploadvideo?access_token=" + accessToken;
        String respStr = HttpUtils.sendPostJsonRequest(url, JSON.toJSONString(opts));
        JSONObject resp = JSON.parseObject(respStr);

        return resp;
    };

    /**
     * 缇ゅ彂娑堟伅锛屽垎鍒湁鍥炬枃锛坣ews锛夈�佹枃鏈�(text)銆佽闊筹紙voice锛夈�佸浘鐗囷紙image锛夊拰瑙嗛锛坴ideo锛�
     * 璇︽儏璇疯锛�<https://mp.weixin.qq.com/wiki?t=resource/res_main&id=mp1481187827_i0l21>
     * Examples:
     * ```
     * api.massSend(opts, receivers);
     * ```
     * opts:
     * ```
     * {
     *  "image":{
     *    "media_id":"123dsdajkasd231jhksad"
     *  },
     *  "msgtype":"image"
     *  "send_ignore_reprint":0
     * }
     * ```
     * Result:
     * ```
     * {
     *  "errcode":0,
     *  "errmsg":"send job submission success",
     *  "msg_id":34182
     * }
     * ```
     * @param {Object} opts 寰呭彂閫佺殑鏁版嵁
     * @param {String|Array|Boolean} receivers 鎺ユ敹浜恒�備竴涓爣绛撅紝鎴栬�卭penid鍒楄〃,鎴栬�呭竷灏斿�兼槸鍚﹀彂閫佺粰鍏ㄩ儴鐢ㄦ埛
     * @param {String|Array} clientMsgId 寮�鍙戣�呬晶缇ゅ彂msgid锛岄暱搴﹂檺鍒�64瀛楄妭锛屽涓嶅～锛屽垯鍚庡彴榛樿浠ョ兢鍙戣寖鍥村拰缇ゅ彂鍐呭鐨勬憳瑕佸�煎仛涓篶lientmsgid
     * @param {Int} sendIgnoreReprint 鍥炬枃娑堟伅琚垽瀹氫负杞浇鏃讹紝鏄惁缁х画缇ゅ彂銆� 1涓虹户缁兢鍙戯紙杞浇锛夛紝0涓哄仠姝㈢兢鍙戙�� 璇ュ弬鏁伴粯璁や负0銆�
     */
    public JSONObject massSend (Map<String, Object> opts) {
        return massSend(opts, true);
    }
    public JSONObject massSend (Map<String, Object> opts, Object receivers) {
        return massSend(opts, receivers, null);
    }
    public JSONObject massSend (Map<String, Object> opts, List<String> receivers) {
        return massSend(opts, receivers, null);
    }
    public JSONObject massSend (Map<String, Object> opts, String receivers) {
        return massSend(opts, receivers, null);
    }
    public JSONObject massSend (Map<String, Object> opts, Boolean receivers) {
        return massSend(opts, receivers, null);
    }
    public JSONObject massSend (Map<String, Object> opts, Object receivers, List<String> clientMsgId) {
        return massSend(opts, receivers, clientMsgId, 0);
    }
    public JSONObject massSend (Map<String, Object> opts, List<String> receivers, List<String> clientMsgId) {
        return massSend(opts, receivers, clientMsgId, 0);
    }
    public JSONObject massSend (Map<String, Object> opts, String receivers, List<String> clientMsgId) {
        return massSend(opts, receivers, clientMsgId, 0);
    }
    public JSONObject massSend (Map<String, Object> opts, Boolean receivers, List<String> clientMsgId) {
        return massSend(opts, receivers, clientMsgId, 0);
    }
    public JSONObject massSend (Map<String, Object> opts, Object receivers, List<String> clientMsgId, Integer sendIgnoreReprint) {

        AccessToken token = this.ensureAccessToken();
        String accessToken = token.getAccessToken();

        String url = null;
        if (sendIgnoreReprint != null) {
            opts.put("send_ignore_reprint", sendIgnoreReprint);
        }
        if (clientMsgId != null) {
            opts.put("clientmsgid", clientMsgId);
        }
        if (receivers instanceof List) {
            opts.put("touser", receivers);
            url = this.PREFIX + "message/mass/send?access_token=" + accessToken;
        } else {
            Map<String, Object> filter = new HashMap<String, Object>();
            if (receivers instanceof Boolean) {
                filter.put("is_to_all", receivers);
            } else if(receivers instanceof String) {
                filter.put("tag_id", receivers);
            } else {
                filter.put("is_to_all", true);
            }
            url = this.PREFIX + "message/mass/sendall?access_token=" + accessToken;
        }
        // https://api.weixin.qq.com/cgi-bin/message/mass/sendall?access_token=ACCESS_TOKEN

        String respStr = HttpUtils.sendPostJsonRequest(url, JSON.toJSONString(opts));
        JSONObject resp = JSON.parseObject(respStr);

        return resp;
    };

    /**
     * 缇ゅ彂鍥炬枃锛坣ews锛夋秷鎭�
     * 璇︽儏璇疯锛�<http://mp.weixin.qq.com/wiki/15/5380a4e6f02f2ffdc7981a8ed7a40753.html>
     * Examples:
     * ```
     * api.massSendNews(mediaId, receivers);
     * ```
     * Result:
     * ```
     * {
     *  "errcode":0,
     *  "errmsg":"send job submission success",
     *  "msg_id":34182
     * }
     * ```
     * @param {String} mediaId 鍥炬枃娑堟伅鐨刴edia id
     * @param {String|Array|Boolean} receivers 鎺ユ敹浜恒�備竴涓粍锛屾垨鑰卭penid鍒楄〃, 鎴栬�卼rue锛堢兢鍙戠粰鎵�鏈変汉锛�
     * @param {String|Array} clientMsgId 寮�鍙戣�呬晶缇ゅ彂msgid锛岄暱搴﹂檺鍒�64瀛楄妭锛屽涓嶅～锛屽垯鍚庡彴榛樿浠ョ兢鍙戣寖鍥村拰缇ゅ彂鍐呭鐨勬憳瑕佸�煎仛涓篶lientmsgid
     * @param {Int} sendIgnoreReprint 鍥炬枃娑堟伅琚垽瀹氫负杞浇鏃讹紝鏄惁缁х画缇ゅ彂銆� 1涓虹户缁兢鍙戯紙杞浇锛夛紝0涓哄仠姝㈢兢鍙戙�� 璇ュ弬鏁伴粯璁や负0銆�
     */
    public JSONObject massSendNews (String mediaId, Object receivers, List<String> clientMsgId, int sendIgnoreReprint) {
        Map<String, Object> opts = new HashMap<String, Object>();
        Map<String, Object> mpnews = new HashMap<String, Object>();
        mpnews.put("media_id", mediaId);
        opts.put("mpnews", mpnews);
        opts.put("msgtype", "mpnews");
        return this.massSend(opts, receivers, clientMsgId, sendIgnoreReprint);
    };

    /**
     * 缇ゅ彂鏂囧瓧锛坱ext锛夋秷鎭�
     * 璇︽儏璇疯锛�<http://mp.weixin.qq.com/wiki/15/5380a4e6f02f2ffdc7981a8ed7a40753.html>
     * Examples:
     * ```
     * api.massSendText(content, receivers);
     * ```
     * Result:
     * ```
     * {
     *  "errcode":0,
     *  "errmsg":"send job submission success",
     *  "msg_id":34182
     * }
     * ```
     * @param {String} content 鏂囧瓧娑堟伅鍐呭
     * @param {String|Array} clientMsgId 寮�鍙戣�呬晶缇ゅ彂msgid锛岄暱搴﹂檺鍒�64瀛楄妭锛屽涓嶅～锛屽垯鍚庡彴榛樿浠ョ兢鍙戣寖鍥村拰缇ゅ彂鍐呭鐨勬憳瑕佸�煎仛涓篶lientmsgid
     * @param {String|Array} receivers 鎺ユ敹浜恒�備竴涓粍锛屾垨鑰卭penid鍒楄〃
     */
    public JSONObject massSendText (String content, Object receivers, List<String> clientMsgId) {
        Map<String, Object> opts = new HashMap<String, Object>();
        Map<String, Object> text = new HashMap<String, Object>();
        text.put("content", content);
        opts.put("text", text);
        opts.put("msgtype", "text");
        return this.massSend(opts, receivers, clientMsgId);
    };

    /**
     * 缇ゅ彂澹伴煶锛坴oice锛夋秷鎭�
     * 璇︽儏璇疯锛�<http://mp.weixin.qq.com/wiki/15/5380a4e6f02f2ffdc7981a8ed7a40753.html>
     * Examples:
     * ```
     * api.massSendVoice(media_id, receivers);
     * ```
     * Result:
     * ```
     * {
     *  "errcode":0,
     *  "errmsg":"send job submission success",
     *  "msg_id":34182
     * }
     * ```
     * @param {String} mediaId 澹伴煶media id
     * @param {String|Array} clientMsgId 寮�鍙戣�呬晶缇ゅ彂msgid锛岄暱搴﹂檺鍒�64瀛楄妭锛屽涓嶅～锛屽垯鍚庡彴榛樿浠ョ兢鍙戣寖鍥村拰缇ゅ彂鍐呭鐨勬憳瑕佸�煎仛涓篶lientmsgid
     * @param {String|Array} receivers 鎺ユ敹浜恒�備竴涓粍锛屾垨鑰卭penid鍒楄〃
     */
    public JSONObject massSendVoice (String mediaId, Object receivers, List<String> clientMsgId) {
        Map<String, Object> opts = new HashMap<String, Object>();
        Map<String, Object> voice = new HashMap<String, Object>();
        voice.put("media_id", mediaId);
        opts.put("voice", voice);
        opts.put("msgtype", "voice");
        return this.massSend(opts, receivers, clientMsgId);
    };

    /**
     * 缇ゅ彂鍥剧墖锛坕mage锛夋秷鎭�
     * 璇︽儏璇疯锛�<http://mp.weixin.qq.com/wiki/15/5380a4e6f02f2ffdc7981a8ed7a40753.html>
     * Examples:
     * ```
     * api.massSendImage(media_id, receivers);
     * ```
     * Result:
     * ```
     * {
     *  "errcode":0,
     *  "errmsg":"send job submission success",
     *  "msg_id":34182
     * }
     * ```
     * @param {String} mediaId 鍥剧墖media id
     * @param {String|Array} clientMsgId 寮�鍙戣�呬晶缇ゅ彂msgid锛岄暱搴﹂檺鍒�64瀛楄妭锛屽涓嶅～锛屽垯鍚庡彴榛樿浠ョ兢鍙戣寖鍥村拰缇ゅ彂鍐呭鐨勬憳瑕佸�煎仛涓篶lientmsgid
     * @param {String|Array} receivers 鎺ユ敹浜恒�備竴涓粍锛屾垨鑰卭penid鍒楄〃
     */
    public JSONObject massSendImage (String mediaId, Object receivers, List<String> clientMsgId) {
        Map<String, Object> opts = new HashMap<String, Object>();
        Map<String, Object> image = new HashMap<String, Object>();
        image.put("media_id", mediaId);
        opts.put("image", image);
        opts.put("msgtype", "image");
        return this.massSend(opts, receivers, clientMsgId);
    };

    /**
     * 缇ゅ彂瑙嗛锛坴ideo锛夋秷鎭�
     * 璇︽儏璇疯锛�<http://mp.weixin.qq.com/wiki/15/5380a4e6f02f2ffdc7981a8ed7a40753.html>
     * Examples:
     * ```
     * api.massSendVideo(mediaId, receivers);
     * ```
     * Result:
     * ```
     * {
     *  "errcode":0,
     *  "errmsg":"send job submission success",
     *  "msg_id":34182
     * }
     * ```
     * @param {String} mediaId 瑙嗛media id
     * @param {String|Array} clientMsgId 寮�鍙戣�呬晶缇ゅ彂msgid锛岄暱搴﹂檺鍒�64瀛楄妭锛屽涓嶅～锛屽垯鍚庡彴榛樿浠ョ兢鍙戣寖鍥村拰缇ゅ彂鍐呭鐨勬憳瑕佸�煎仛涓篶lientmsgid
     * @param {String|Array} receivers 鎺ユ敹浜恒�備竴涓粍锛屾垨鑰卭penid鍒楄〃
     */
    public JSONObject massSendVideo (String mediaId, Object receivers, List<String> clientMsgId) {
        Map<String, Object> opts = new HashMap<String, Object>();
        Map<String, Object> mpvideo = new HashMap<String, Object>();
        mpvideo.put("media_id", mediaId);
        opts.put("mpvideo", mpvideo);
        opts.put("msgtype", "mpvideo");
        return this.massSend(opts, receivers, clientMsgId);
    };

    /**
     * 缇ゅ彂瑙嗛锛坴ideo锛夋秷鎭紝鐩存帴閫氳繃涓婁紶鏂囦欢寰楀埌鐨刴edia id杩涜缇ゅ彂锛堣嚜鍔ㄧ敓鎴愮礌鏉愶級
     * 璇︽儏璇疯锛�<http://mp.weixin.qq.com/wiki/15/5380a4e6f02f2ffdc7981a8ed7a40753.html>
     * Examples:
     * ```
     * api.massSendMPVideo(data, receivers);
     * ```
     * Data:
     * ```
     * {
     *  "media_id": "rF4UdIMfYK3efUfyoddYRMU50zMiRmmt_l0kszupYh_SzrcW5Gaheq05p_lHuOTQ",
     *  "title": "TITLE",
     *  "description": "Description"
     * }
     * ```
     * Result:
     * ```
     * {
     *  "errcode":0,
     *  "errmsg":"send job submission success",
     *  "msg_id":34182
     * }
     * ```
     * @param {Object} data 瑙嗛鏁版嵁
     * @param {String|Array} clientMsgId 寮�鍙戣�呬晶缇ゅ彂msgid锛岄暱搴﹂檺鍒�64瀛楄妭锛屽涓嶅～锛屽垯鍚庡彴榛樿浠ョ兢鍙戣寖鍥村拰缇ゅ彂鍐呭鐨勬憳瑕佸�煎仛涓篶lientmsgid
     * @param {String|Array} receivers 鎺ユ敹浜恒�備竴涓粍锛屾垨鑰卭penid鍒楄〃
     */
    public JSONObject massSendMPVideo (Map<String, Object> data, Object receivers, List<String> clientMsgId) throws Exception {
        // 鑷姩甯浆瑙嗛鐨刴edia_id
        JSONObject result = this.uploadMPVideo(data);
        if(!result.containsKey("media_id")){
            throw new Exception("upload mpvideo faild");
        }
        String mediaId = result.getString("media_id");
        return this.massSendVideo(mediaId, receivers, clientMsgId);
    };

    /**
     * 鍒犻櫎缇ゅ彂娑堟伅
     * 璇︽儏璇疯锛�<http://mp.weixin.qq.com/wiki/15/5380a4e6f02f2ffdc7981a8ed7a40753.html>
     * Examples:
     * ```
     * api.deleteMass(message_id);
     * ```
     * Result:
     * ```
     * {
     *  "errcode":0,
     *  "errmsg":"ok"
     * }
     * ```
     * @param {String} messageId 寰呭垹闄ょ兢鍙戠殑娑堟伅id
     */
    public boolean deleteMass  (String messageId) {

        AccessToken token = this.ensureAccessToken();
        String accessToken = token.getAccessToken();

        String url = this.PREFIX + "message/mass/delete?access_token=" + accessToken;

        Map<String, Object> opts = new HashMap<String, Object>();
        opts.put("msg_id", messageId);

        String respStr = HttpUtils.sendPostJsonRequest(url, JSON.toJSONString(opts));
        JSONObject resp = JSON.parseObject(respStr);

        int errCode = resp.getIntValue("errcode");
        if(errCode == 0){
            return true;
        }else{
            return false;
        }
    };

    /**
     * 棰勮鎺ュ彛锛岄瑙堝浘鏂囨秷鎭�
     * 璇︽儏璇疯锛�<http://mp.weixin.qq.com/wiki/15/5380a4e6f02f2ffdc7981a8ed7a40753.html>
     * Examples:
     * ```
     * api.previewNews(openid, mediaId);
     * ```
     * Result:
     * ```
     * {
     *  "errcode":0,
     *  "errmsg":"send job submission success",
     *  "msg_id": 34182
     * }
     * ```
     * @param {String} openid 鐢ㄦ埛openid
     * @param {String} mediaId 鍥炬枃娑堟伅mediaId
     */
    public JSONObject previewNews (String openid, String mediaId) {

        AccessToken token = this.ensureAccessToken();
        String accessToken = token.getAccessToken();

        String url = this.PREFIX + "message/mass/preview?access_token=" + accessToken;

        Map<String, Object> opts = new HashMap<String, Object>();
        Map<String, Object> mpnews = new HashMap<String, Object>();
        mpnews.put("media_id", mediaId);
        opts.put("mpnews", mpnews);
        opts.put("msgtype", "mpnews");
        opts.put("touser", openid);

        String respStr = HttpUtils.sendPostJsonRequest(url, JSON.toJSONString(opts));
        JSONObject resp = JSON.parseObject(respStr);
        return resp;
    };

    /**
     * 棰勮鎺ュ彛锛岄瑙堟枃鏈秷鎭�
     * 璇︽儏璇疯锛�<http://mp.weixin.qq.com/wiki/15/5380a4e6f02f2ffdc7981a8ed7a40753.html>
     * Examples:
     * ```
     * api.previewText(openid, content);
     * ```
     * Result:
     * ```
     * {
     *  "errcode":0,
     *  "errmsg":"send job submission success",
     *  "msg_id": 34182
     * }
     * ```
     * @param {String} openid 鐢ㄦ埛openid
     * @param {String} content 鏂囨湰娑堟伅
     */
    public JSONObject previewText (String openid, String content) {

        AccessToken token = this.ensureAccessToken();
        String accessToken = token.getAccessToken();

        String url = this.PREFIX + "message/mass/preview?access_token=" + accessToken;

        Map<String, Object> opts = new HashMap<String, Object>();
        Map<String, Object> text = new HashMap<String, Object>();
        text.put("content", content);
        opts.put("text", text);
        opts.put("msgtype", "text");
        opts.put("touser", openid);

        String respStr = HttpUtils.sendPostJsonRequest(url, JSON.toJSONString(opts));
        JSONObject resp = JSON.parseObject(respStr);
        return resp;
    };

    /**
     * 棰勮鎺ュ彛锛岄瑙堣闊虫秷鎭�
     * 璇︽儏璇疯锛�<http://mp.weixin.qq.com/wiki/15/5380a4e6f02f2ffdc7981a8ed7a40753.html>
     * Examples:
     * ```
     * api.previewVoice(openid, mediaId);
     * ```
     * Result:
     * ```
     * {
     *  "errcode":0,
     *  "errmsg":"send job submission success",
     *  "msg_id": 34182
     * }
     * ```
     * @param {String} openid 鐢ㄦ埛openid
     * @param {String} mediaId 璇煶mediaId
     */
    public JSONObject previewVoice (String openid, String mediaId) {

        AccessToken token = this.ensureAccessToken();
        String accessToken = token.getAccessToken();

        String url = this.PREFIX + "message/mass/preview?access_token=" + accessToken;

        Map<String, Object> opts = new HashMap<String, Object>();
        Map<String, Object> voice = new HashMap<String, Object>();
        voice.put("media_id", mediaId);
        opts.put("voice", voice);
        opts.put("msgtype", "voice");
        opts.put("touser", openid);

        String respStr = HttpUtils.sendPostJsonRequest(url, JSON.toJSONString(opts));
        JSONObject resp = JSON.parseObject(respStr);

        return resp;
    };

    /**
     * 棰勮鎺ュ彛锛岄瑙堝浘鐗囨秷鎭�
     * 璇︽儏璇疯锛�<http://mp.weixin.qq.com/wiki/15/5380a4e6f02f2ffdc7981a8ed7a40753.html>
     * Examples:
     * ```
     * api.previewImage(openid, mediaId);
     * ```
     * Result:
     * ```
     * {
     *  "errcode":0,
     *  "errmsg":"send job submission success",
     *  "msg_id": 34182
     * }
     * ```
     * @param {String} openid 鐢ㄦ埛openid
     * @param {String} mediaId 鍥剧墖mediaId
     */
    public JSONObject previewImage (String openid, String mediaId) {

        AccessToken token = this.ensureAccessToken();
        String accessToken = token.getAccessToken();

        String url = this.PREFIX + "message/mass/preview?access_token=" + accessToken;

        Map<String, Object> opts = new HashMap<String, Object>();
        Map<String, Object> image = new HashMap<String, Object>();
        image.put("media_id", mediaId);
        opts.put("image", image);
        opts.put("msgtype", "image");
        opts.put("touser", openid);

        String respStr = HttpUtils.sendPostJsonRequest(url, JSON.toJSONString(opts));
        JSONObject resp = JSON.parseObject(respStr);

        return resp;
    };

    /**
     * 棰勮鎺ュ彛锛岄瑙堣棰戞秷鎭�
     * 璇︽儏璇疯锛�<http://mp.weixin.qq.com/wiki/15/5380a4e6f02f2ffdc7981a8ed7a40753.html>
     * Examples:
     * ```
     * api.previewVideo(openid, mediaId);
     * ```
     * Result:
     * ```
     * {
     *  "errcode":0,
     *  "errmsg":"send job submission success",
     *  "msg_id": 34182
     * }
     * ```
     * @param {String} openid 鐢ㄦ埛openid
     * @param {String} mediaId 瑙嗛mediaId
     */
    public JSONObject previewVideo (String openid, String mediaId) {

        AccessToken token = this.ensureAccessToken();
        String accessToken = token.getAccessToken();

        String url = this.PREFIX + "message/mass/preview?access_token=" + accessToken;

        Map<String, Object> opts = new HashMap<String, Object>();
        Map<String, Object> mpvideo = new HashMap<String, Object>();
        mpvideo.put("media_id", mediaId);
        opts.put("mpvideo", mpvideo);
        opts.put("msgtype", "mpvideo");
        opts.put("touser", openid);

        String respStr = HttpUtils.sendPostJsonRequest(url, JSON.toJSONString(opts));
        JSONObject resp = JSON.parseObject(respStr);

        return resp;
    };

    /**
     * 鏌ヨ缇ゅ彂娑堟伅鐘舵��
     * 璇︽儏璇疯锛�<http://mp.weixin.qq.com/wiki/15/5380a4e6f02f2ffdc7981a8ed7a40753.html>
     * Examples:
     * ```
     * api.getMassMessageStatus(messageId);
     * ```
     * Result:
     * ```
     * {
     *  "msg_id":201053012,
     *  "msg_status":"SEND_SUCCESS"
     * }
     * ```
     * @param {String} messageId 娑堟伅ID
     */
    public JSONObject getMassMessageStatus (String messageId) {

        AccessToken token = this.ensureAccessToken();
        String accessToken = token.getAccessToken();

        String url = this.PREFIX + "message/mass/get?access_token=" + accessToken;

        Map<String, Object> opts = new HashMap<String, Object>();
        opts.put("msg_id", messageId);

        String respStr = HttpUtils.sendPostJsonRequest(url, JSON.toJSONString(opts));
        JSONObject resp = JSON.parseObject(respStr);

        return resp;
    }





    /**
     * 涓婁紶姘镐箙绱犳潗锛屽垎鍒湁鍥剧墖锛坕mage锛夈�佽闊筹紙voice锛夈�佸拰缂╃暐鍥撅紙thumb锛�
     * 璇︽儏璇疯锛�<http://mp.weixin.qq.com/wiki/14/7e6c03263063f4813141c3e17dd4350a.html>
     * Examples:
     * ```
     * api.uploadMaterial('filepath', type);
     * ```
     * Result:
     * ```
     * {"type":"TYPE","media_id":"MEDIA_ID","created_at":123456789}
     * ```
     * Shortcut:
     * - `uploadImageMaterial(filepath);`
     * - `uploadVoiceMaterial(filepath);`
     * - `uploadThumbMaterial(filepath);`
     * @param {String} filepath 鏂囦欢璺緞
     * @param {String} type 濯掍綋绫诲瀷锛屽彲鐢ㄥ�兼湁image銆乿oice銆乿ideo銆乼humb
     */
    public JSONObject uploadMaterial (String filepath, MaterialType type) {

        AccessToken token = this.ensureAccessToken();
        String accessToken = token.getAccessToken();

        String url = this.PREFIX + "material/add_material?access_token=" + accessToken + "&type=" + type;

        Map<String, Object> data = new HashMap<String, Object>();
        data.put("media", new File(filepath));

        String respStr = HttpUtils.sendPostFormDataRequest(url, data);
        JSONObject resp = JSON.parseObject(respStr);

        return resp;

    };

    public JSONObject uploadImageMaterial (String filepath) {
        return uploadMaterial(filepath, MaterialType.image);
    }

    public JSONObject uploadVoiceMaterial (String filepath) {
        return uploadMaterial(filepath, MaterialType.voice);
    }

    public JSONObject uploadThumbMaterial (String filepath) {
        return uploadMaterial(filepath, MaterialType.thumb);
    }

    /**
     * 涓婁紶姘镐箙绱犳潗锛岃棰戯紙video锛�
     * 璇︽儏璇疯锛�<http://mp.weixin.qq.com/wiki/14/7e6c03263063f4813141c3e17dd4350a.html>
     * Examples:
     * ```
     * var description = {
     *   "title":VIDEO_TITLE,
     *   "introduction":INTRODUCTION
     * };
     * api.uploadVideoMaterial('filepath', description);
     * ```
     *
     * Result:
     * ```
     * {"media_id":"MEDIA_ID"}
     * ```
     * @param {String} filepath 瑙嗛鏂囦欢璺緞
     * @param {Object} description 鎻忚堪
     */
    public JSONObject uploadVideoMaterial (String filepath, Map<String, Object> description) {

        AccessToken token = this.ensureAccessToken();
        String accessToken = token.getAccessToken();

        String url = this.PREFIX + "material/add_material?access_token=" + accessToken + "&type=video";

        Map<String, Object> data = new HashMap<String, Object>();
        data.put("media", new File(filepath));
        data.put("description", JSON.toJSONString(description));

        String respStr = HttpUtils.sendPostFormDataRequest(url, data);
        JSONObject resp = JSON.parseObject(respStr);

        return resp;
    };

    /**
     * 鏂板姘镐箙鍥炬枃绱犳潗
     *
     * News:
     * ```
     * [
     *    {
     *      "title": TITLE,
     *      "thumb_media_id": THUMB_MEDIA_ID,
     *      "author": AUTHOR,
     *      "digest": DIGEST,
     *      "show_cover_pic": SHOW_COVER_PIC(0 / 1),
     *      "content": CONTENT,
     *      "content_source_url": CONTENT_SOURCE_URL
     *    },
     *    //鑻ユ柊澧炵殑鏄鍥炬枃绱犳潗锛屽垯姝ゅ搴旇繕鏈夊嚑娈礱rticles缁撴瀯
     *  ]
     * ```
     * Examples:
     * ```
     * api.uploadNewsMaterial(news);
     * ```
     *
     * Result:
     * ```
     * {"errcode":0,"errmsg":"ok"}
     * ```
     * @param {Object} news 鍥炬枃瀵硅薄
     */
    public boolean uploadNewsMaterial (List<Map<String, Object>> news) {

        AccessToken token = this.ensureAccessToken();
        String accessToken = token.getAccessToken();

        String url = this.PREFIX + "material/add_news?access_token=" + accessToken;

        Map<String, Object> data = new HashMap<String, Object>();
        data.put("articles", news);

        String respStr = HttpUtils.sendPostJsonRequest(url, JSON.toJSONString(data));
        JSONObject resp = JSON.parseObject(respStr);
        int errCode = resp.getIntValue("errcode");
        if(errCode == 0){
            return true;
        }else{
            return false;
        }
    }

    /**
     * 鏇存柊姘镐箙鍥炬枃绱犳潗
     * News:
     * ```
     * {
     *  "media_id":MEDIA_ID,
     *  "index":INDEX,
     *  "articles": [
     *    {
     *      "title": TITLE,
     *      "thumb_media_id": THUMB_MEDIA_ID,
     *      "author": AUTHOR,
     *      "digest": DIGEST,
     *      "show_cover_pic": SHOW_COVER_PIC(0 / 1),
     *      "content": CONTENT,
     *      "content_source_url": CONTENT_SOURCE_URL
     *    },
     *    //鑻ユ柊澧炵殑鏄鍥炬枃绱犳潗锛屽垯姝ゅ搴旇繕鏈夊嚑娈礱rticles缁撴瀯
     *  ]
     * }
     * ```
     * Examples:
     * ```
     * api.uploadNewsMaterial(news);
     * ```
     * Result:
     * ```
     * {"errcode":0,"errmsg":"ok"}
     * ```
     * @param {Object} news 鍥炬枃瀵硅薄
     */
    public boolean updateNewsMaterial (Map<String, Object> news) {

        AccessToken token = this.ensureAccessToken();
        String accessToken = token.getAccessToken();

        String url = this.PREFIX + "material/add_news?access_token=" + accessToken;

        String respStr = HttpUtils.sendPostJsonRequest(url, JSON.toJSONString(news));
        JSONObject resp = JSON.parseObject(respStr);
        int errCode = resp.getIntValue("errcode");
        if(errCode == 0){
            return true;
        }else{
            return false;
        }
    }

    /**
     * 鏍规嵁濯掍綋ID鑾峰彇姘镐箙绱犳潗
     * 璇︽儏璇疯锛�<http://mp.weixin.qq.com/wiki/4/b3546879f07623cb30df9ca0e420a5d0.html>
     * Examples:
     * ```
     * api.getMaterial('media_id');
     * ```
     *
     * - `result`, 璋冪敤姝ｅ父鏃跺緱鍒扮殑鏂囦欢Buffer瀵硅薄
     * - `res`, HTTP鍝嶅簲瀵硅薄
     * @param {String} mediaId 濯掍綋鏂囦欢鐨処D
     */
    public JSONObject getMaterial (String mediaId) {

        AccessToken token = this.ensureAccessToken();
        String accessToken = token.getAccessToken();

        String url = this.PREFIX + "material/get_material?access_token=" + accessToken;

        Map<String, Object> data = new HashMap<String, Object>();
        data.put("media_id", mediaId);

        String respStr = HttpUtils.sendPostJsonRequest(url, JSON.toJSONString(data));
        JSONObject resp = JSON.parseObject(respStr);

        return resp;
    }

    /**
     * 鍒犻櫎姘镐箙绱犳潗
     * 璇︽儏璇疯锛�<http://mp.weixin.qq.com/wiki/5/e66f61c303db51a6c0f90f46b15af5f5.html>
     * Examples:
     * ```
     * api.removeMaterial('media_id');
     * ```
     *
     * - `result`, 璋冪敤姝ｅ父鏃跺緱鍒扮殑鏂囦欢Buffer瀵硅薄
     * - `res`, HTTP鍝嶅簲瀵硅薄
     * @param {String} mediaId 濯掍綋鏂囦欢鐨処D
     */
    public JSONObject removeMaterial (String mediaId) {

        AccessToken token = this.ensureAccessToken();
        String accessToken = token.getAccessToken();

        String url = this.PREFIX + "material/del_material?access_token=" + accessToken;

        Map<String, Object> data = new HashMap<String, Object>();
        data.put("media_id", mediaId);

        String respStr = HttpUtils.sendPostJsonRequest(url, JSON.toJSONString(data));
        JSONObject resp = JSON.parseObject(respStr);

        return resp;
    }

    /**
     * 鑾峰彇绱犳潗鎬绘暟
     * 璇︽儏璇疯锛�<http://mp.weixin.qq.com/wiki/16/8cc64f8c189674b421bee3ed403993b8.html>
     * Examples:
     * ```
     * api.getMaterialCount();
     * ```
     *
     * - `result`, 璋冪敤姝ｅ父鏃跺緱鍒扮殑鏂囦欢Buffer瀵硅薄
     * - `res`, HTTP鍝嶅簲瀵硅薄 * Result:
     * ```
     * {
     *  "voice_count":COUNT,
     *  "video_count":COUNT,
     *  "image_count":COUNT,
     *  "news_count":COUNT
     * }
     * ```
     */
    public JSONObject getMaterialCount () {

        AccessToken token = this.ensureAccessToken();
        String accessToken = token.getAccessToken();

        String url = this.PREFIX + "material/get_materialcount?access_token=" + accessToken;

        String respStr = HttpUtils.sendGetRequest(url);
        JSONObject resp = JSON.parseObject(respStr);

        return resp;
    }

    /**
     * 鑾峰彇姘镐箙绱犳潗鍒楄〃
     * 璇︽儏璇疯锛�<http://mp.weixin.qq.com/wiki/12/2108cd7aafff7f388f41f37efa710204.html>
     * Examples:
     * ```
     * api.getMaterials(type, offset, count);
     * ```
     *
     * - `result`, 璋冪敤姝ｅ父鏃跺緱鍒扮殑鏂囦欢Buffer瀵硅薄
     * - `res`, HTTP鍝嶅簲瀵硅薄 * Result:
     * ```
     * {
     *  "total_count": TOTAL_COUNT,
     *  "item_count": ITEM_COUNT,
     *  "item": [{
     *    "media_id": MEDIA_ID,
     *    "name": NAME,
     *    "update_time": UPDATE_TIME
     *  },
     *  //鍙兘浼氭湁澶氫釜绱犳潗
     *  ]
     * }
     * ```
     * @param {String} type 绱犳潗鐨勭被鍨嬶紝鍥剧墖锛坕mage锛夈�佽棰戯紙video锛夈�佽闊� 锛坴oice锛夈�佸浘鏂囷紙news锛�
     * @param {Number} offset 浠庡叏閮ㄧ礌鏉愮殑璇ュ亸绉讳綅缃紑濮嬭繑鍥烇紝0琛ㄧず浠庣涓�涓礌鏉� 杩斿洖
     * @param {Number} count 杩斿洖绱犳潗鐨勬暟閲忥紝鍙栧�煎湪1鍒�20涔嬮棿
     */
    public JSONObject getMaterials (String type, int offset, int count) {

        AccessToken token = this.ensureAccessToken();
        String accessToken = token.getAccessToken();

        String url = this.PREFIX + "material/batchget_material?access_token=" + accessToken;

        Map<String, Object> data = new HashMap<String, Object>();
        data.put("type", type);
        data.put("offset", offset);
        data.put("count", count);

        String respStr = HttpUtils.sendPostJsonRequest(url, JSON.toJSONString(data));
        JSONObject resp = JSON.parseObject(respStr);

        return resp;
    }

    /**
     * 鍙戦�佽涔夌悊瑙ｈ姹�
     * 璇︾粏璇风湅锛歨ttp://mp.weixin.qq.com/wiki/index.php?title=%E8%AF%AD%E4%B9%89%E7%90%86%E8%A7%A3 * Opts:
     * ```
     * {
     *   "query":"鏌ヤ竴涓嬫槑澶╀粠鍖椾含鍒颁笂娴风殑鍗楄埅鏈虹エ",
     *   "city":"鍖椾含",
     *   "category": "flight,hotel"
     * }
     * ```
     * Examples:
     * ```
     * api.semantic(uid, opts);
     * ```
     * Result:
     * ```
     * {
     *   "errcode":0,
     *   "query":"鏌ヤ竴涓嬫槑澶╀粠鍖椾含鍒颁笂娴风殑鍗楄埅鏈虹エ",
     *   "type":"flight",
     *   "semantic":{
     *       "details":{
     *           "start_loc":{
     *               "type":"LOC_CITY",
     *               "city":"鍖椾含甯�",
     *               "city_simple":"鍖椾含",
     *               "loc_ori":"鍖椾含"
     *               },
     *           "end_loc": {
     *               "type":"LOC_CITY",
     *               "city":"涓婃捣甯�",
     *               "city_simple":"涓婃捣",
     *               "loc_ori":"涓婃捣"
     *             },
     *           "start_date": {
     *               "type":"DT_ORI",
     *               "date":"2014-03-05",
     *               "date_ori":"鏄庡ぉ"
     *             },
     *          "airline":"涓浗鍗楁柟鑸┖鍏徃"
     *       },
     *   "intent":"SEARCH"
     * }
     * ```
     * @param {String} openid 鐢ㄦ埛ID
     * @param {Object} opts 鏌ヨ鏉′欢
     */
    public JSONObject semantic (String openid, Map<String, Object> opts) {

        AccessToken token = this.ensureAccessToken();
        String accessToken = token.getAccessToken();

        // https://api.weixin.qq.com/semantic/semproxy/search?access_token=YOUR_ACCESS_TOKEN
        String url = "https://api.weixin.qq.com/semantic/semproxy/search?access_token=" + accessToken;
        opts.put("appid", this.appid);
        opts.put("uid", openid);

        String respStr = HttpUtils.sendPostJsonRequest(url, JSON.toJSONString(opts));
        JSONObject resp = JSON.parseObject(respStr);

        return resp;
    }




    /**
     * 鑾峰彇灏忕▼搴忎簩缁寸爜锛岄�傜敤浜庨渶瑕佺殑鐮佹暟閲忚緝灏戠殑涓氬姟鍦烘櫙
     * https://developers.weixin.qq.com/miniprogram/dev/api/createWXAQRCode.html
     * Examples:
     * ```
     * String path = 'index?foo=bar'; // 灏忕▼搴忛〉闈㈣矾寰�
     * api.createWXAQRCode(path, width);
     * ```
     * @param {String} path 鎵爜杩涘叆鐨勫皬绋嬪簭椤甸潰璺緞锛屾渶澶ч暱搴� 128 瀛楄妭锛屼笉鑳戒负绌�
     * @param {String} width 浜岀淮鐮佺殑瀹藉害锛屽崟浣� px銆傛渶灏� 280px锛屾渶澶� 1280px
     */
    public JSONObject createWXAQRCode (String path, int width) throws Exception {

        AccessToken token = this.ensureAccessToken();
        String accessToken = token.getAccessToken();

        if(width < 280 || width > 1280){
            if(width < 280){
                throw new Exception("the value of \"width\" is too small");
            }
            if(width < 1280){
                throw new Exception("the value of \"width\" is too large");
            }
        }

        String url = this.PREFIX + "wxaapp/createwxaqrcode?access_token=" + accessToken;
        Map<String, Object> data = new HashMap<String, Object>();
        data.put("path", path);
        data.put("width", width);

        String respStr = HttpUtils.sendPostJsonRequest(url, JSON.toJSONString(data));
        JSONObject resp = JSON.parseObject(respStr);

        return resp;
    }


    /**
     * 鑾峰彇灏忕▼搴忕爜锛岄�傜敤浜庨渶瑕佺殑鐮佹暟閲忚緝灏戠殑涓氬姟鍦烘櫙
     * https://developers.weixin.qq.com/miniprogram/dev/api/getWXACode.html
     * Examples:
     * ```
     * var path = 'index?foo=bar'; // 灏忕▼搴忛〉闈㈣矾寰�
     * api.getWXACode(path);
     * ```
     * @param {String} path 鎵爜杩涘叆鐨勫皬绋嬪簭椤甸潰璺緞锛屾渶澶ч暱搴� 128 瀛楄妭锛屼笉鑳戒负绌�
     * @param {String} width 浜岀淮鐮佺殑瀹藉害锛屽崟浣� px銆傛渶灏� 280px锛屾渶澶� 1280px
     * @param {String} auto_color 鑷姩閰嶇疆绾挎潯棰滆壊锛屽鏋滈鑹蹭緷鐒舵槸榛戣壊锛屽垯璇存槑涓嶅缓璁厤缃富鑹茶皟
     * @param {Object} line_color auto_color 涓� false 鏃剁敓鏁堬紝浣跨敤 rgb 璁剧疆棰滆壊 渚嬪 {"r":"xxx","g":"xxx","b":"xxx"} 鍗佽繘鍒惰〃绀�
     * @param {Bool} is_hyaline 鏄惁闇�瑕侀�忔槑搴曡壊锛屼负 true 鏃讹紝鐢熸垚閫忔槑搴曡壊鐨勫皬绋嬪簭鐮�
     */
    public JSONObject getWXACode (String path, int width, boolean auto_color, Map<String, Object> line_color, boolean is_hyaline) {

        if(width < 280 || width > 1280){
            width = 430;
        }

        if(line_color == null){
            line_color = new HashMap<String, Object>();
            line_color.put("r", 0);
            line_color.put("g", 0);
            line_color.put("b", 0);
        }

        AccessToken token = this.ensureAccessToken();
        String accessToken = token.getAccessToken();

        String url = this.WXA_PREFIX + "getwxacode?access_token=" + accessToken;
        Map<String, Object> data = new HashMap<String, Object>();
        data.put("path", path);
        data.put("width", width);
        data.put("auto_color", auto_color);
        data.put("line_color", line_color);
        data.put("is_hyaline", is_hyaline);

        String respStr = HttpUtils.sendPostJsonRequest(url, JSON.toJSONString(data));
        JSONObject resp = JSON.parseObject(respStr);

        return resp;
    };


    /**
     * 鑾峰彇灏忕▼搴忕爜锛岄�傜敤浜庨渶瑕佺殑鐮佹暟閲忔瀬澶氱殑涓氬姟鍦烘櫙
     * https://developers.weixin.qq.com/miniprogram/dev/api/getWXACodeUnlimit.html
     * Examples:
     * ```
     * var scene = 'foo=bar';
     * var page = 'pages/index/index'; // 灏忕▼搴忛〉闈㈣矾寰�
     * api.getWXACodeUnlimit(scene, page);
     * ```
     * @param {String} scene 鏈�澶�32涓彲瑙佸瓧绗︼紝鍙敮鎸佹暟瀛楋紝澶у皬鍐欒嫳鏂囦互鍙婇儴鍒嗙壒娈婂瓧绗︼細!#$&'()*+,/:;=?@-._~锛屽叾瀹冨瓧绗﹁鑷缂栫爜涓哄悎娉曞瓧绗︼紙鍥犱笉鏀寔%锛屼腑鏂囨棤娉曚娇鐢� urlencode 澶勭悊锛岃浣跨敤鍏朵粬缂栫爜鏂瑰紡锛�
     * @param {String} page 蹇呴』鏄凡缁忓彂甯冪殑灏忕▼搴忓瓨鍦ㄧ殑椤甸潰锛堝惁鍒欐姤閿欙級锛屼緥濡� pages/index/index, 鏍硅矾寰勫墠涓嶈濉姞 /,涓嶈兘鎼哄甫鍙傛暟锛堝弬鏁拌鏀惧湪scene瀛楁閲岋級锛屽鏋滀笉濉啓杩欎釜瀛楁锛岄粯璁よ烦涓婚〉闈�
     * @param {String} width 浜岀淮鐮佺殑瀹藉害锛屽崟浣� px銆傛渶灏� 280px锛屾渶澶� 1280px
     * @param {String} auto_color 鑷姩閰嶇疆绾挎潯棰滆壊锛屽鏋滈鑹蹭緷鐒舵槸榛戣壊锛屽垯璇存槑涓嶅缓璁厤缃富鑹茶皟
     * @param {Object} line_color auto_color 涓� false 鏃剁敓鏁堬紝浣跨敤 rgb 璁剧疆棰滆壊 渚嬪 {"r":"xxx","g":"xxx","b":"xxx"} 鍗佽繘鍒惰〃绀�
     * @param {Bool} is_hyaline 鏄惁闇�瑕侀�忔槑搴曡壊锛屼负 true 鏃讹紝鐢熸垚閫忔槑搴曡壊鐨勫皬绋嬪簭鐮�
     */
    public JSONObject getWXACodeUnlimit (String scene, String page, int width, boolean auto_color, Map<String, Object> line_color, boolean is_hyaline) {

        if(width < 280 || width > 1280){
            width = 430;
        }

        if(line_color == null){
            line_color = new HashMap<String, Object>();
            line_color.put("r", 0);
            line_color.put("g", 0);
            line_color.put("b", 0);
        }

        AccessToken token = this.ensureAccessToken();
        String accessToken = token.getAccessToken();

        String url = this.WXA_PREFIX + "getwxacodeunlimit?access_token=" + accessToken;
        Map<String, Object> data = new HashMap<String, Object>();
        data.put("scene", scene);
        data.put("page", page);
        data.put("width", width);
        data.put("auto_color", auto_color);
        data.put("line_color", line_color);
        data.put("is_hyaline", is_hyaline);

        String respStr = HttpUtils.sendPostJsonRequest(url, JSON.toJSONString(data));
        JSONObject resp = JSON.parseObject(respStr);

        return resp;
    }


    /**
     * 涓婁紶鍥剧墖
     * 璇︾粏璇风湅锛�<http://mp.weixin.qq.com/wiki/index.php?title=寰俊灏忓簵鎺ュ彛>
     * Examples:
     * ```
     * api.uploadPicture('/path/to/your/img.jpg');
     * ```
     *
     * Result:
     * ```
     * {
     *  "errcode": 0,
     *  "errmsg": "success"
     *  "image_url": "http://mmbiz.qpic.cn/mmbiz/4whpV1VZl2ibl4JWwwnW3icSJGqecVtRiaPxwWEIr99eYYL6AAAp1YBo12CpQTXFH6InyQWXITLvU4CU7kic4PcoXA/0"
     * }
     * ```
     * @param {String} filepath 鏂囦欢璺緞
     */
    public JSONObject uploadPicture (String filepath) {

        AccessToken token = this.ensureAccessToken();
        String accessToken = token.getAccessToken();

        File file = new File(filepath);
        String basename = file.getName();

        String url = this.MERCHANT_PREFIX + "common/upload_img?access_token=" +
                accessToken + "&filename=" + basename;

        String respStr = HttpUtils.sendPostFileRequest(url, file);
        JSONObject resp = JSON.parseObject(respStr);

        return resp;
    }





    /**
     * 璁剧疆鎵�灞炶涓�
     * Examples:
     * ```
     * Object industryIds = {
     *  "industry_id1":'1',
     *  "industry_id2":"4"
     * };
     * api.setIndustry(industryIds);
     * ```
     * @param {Object} industryIds 鍏紬鍙锋ā鏉挎秷鎭墍灞炶涓氱紪鍙�
     */
    public JSONObject setIndustry (Map<String, Object> industryIds) {

        AccessToken token = this.ensureAccessToken();
        String accessToken = token.getAccessToken();

        String url = this.PREFIX + "template/api_set_industry?access_token=" + accessToken;

        String respStr = HttpUtils.sendPostJsonRequest(url, JSON.toJSONString(industryIds));
        JSONObject resp = JSON.parseObject(respStr);

        return resp;
    };

    /**
     * 鑾峰緱妯℃澘ID
     * Examples:
     * ```
     * var templateIdShort = 'TM00015';
     * api.addTemplate(templateIdShort);
     * ```
     * @param {String} templateIdShort 妯℃澘搴撲腑妯℃澘鐨勭紪鍙凤紝鏈夆�淭M**鈥濆拰鈥淥PENTMTM**鈥濈瓑褰㈠紡
     */
    public JSONObject addTemplate (String templateIdShort) {

        AccessToken token = this.ensureAccessToken();
        String accessToken = token.getAccessToken();

        String url = this.PREFIX + "template/api_add_template?access_token=" + accessToken;

        Map<String, Object> data = new HashMap<String, Object>();
        data.put("template_id_short", templateIdShort);

        String respStr = HttpUtils.sendPostJsonRequest(url, JSON.toJSONString(data));
        JSONObject resp = JSON.parseObject(respStr);

        return resp;
    };

    /**
     * 鍙戦�佹ā鏉挎秷鎭�
     * Examples:
     * ```
     * String templateId: '妯℃澘id';
     * // URL缃┖锛屽垯鍦ㄥ彂閫佸悗,鐐瑰嚮妯℃澘娑堟伅浼氳繘鍏ヤ竴涓┖鐧介〉闈紙ios锛�, 鎴栨棤娉曠偣鍑伙紙android锛�
     * String url: 'http://weixin.qq.com/download';
     * String topcolor = '#FF0000'; // 椤堕儴棰滆壊
     * Object data = {
     *  user:{
     *    "value":'榛勫厛鐢�',
     *    "color":"#173177"
     *  }
     * };
     * api.sendTemplate('openid', templateId, url, topColor, data);
     * ```
     * @param {String} openid 鐢ㄦ埛鐨刼penid
     * @param {String} templateId 妯℃澘ID
     * @param {String} url URL缃┖锛屽垯鍦ㄥ彂閫佸悗锛岀偣鍑绘ā鏉挎秷鎭細杩涘叆涓�涓┖鐧介〉闈紙ios锛夛紝鎴栨棤娉曠偣鍑伙紙android锛�
     * @param {String} topColor 瀛椾綋棰滆壊
     * @param {Object} data 娓叉煋妯℃澘鐨勬暟鎹�
     * @param {Object} miniprogram 璺宠浆灏忕▼搴忔墍闇�鏁版嵁 {appid, pagepath}
     */
    public JSONObject sendTemplate (String openid,
                                    String templateId,
                                    String url,
                                    String topColor,
                                    Map<String, Object> data,
                                    Map<String, Object> miniprogram) {

        AccessToken token = this.ensureAccessToken();
        String accessToken = token.getAccessToken();

        String apiUrl = this.PREFIX + "message/template/send?access_token=" + accessToken;

        Map<String, Object> template = new HashMap<String, Object>();
        data.put("touser", openid);
        data.put("template_id", templateId);
        data.put("url", url);
        data.put("miniprogram", miniprogram);
        data.put("color", topColor);
        data.put("data", data);

        String respStr = HttpUtils.sendPostJsonRequest(apiUrl, JSON.toJSONString(template));
        JSONObject resp = JSON.parseObject(respStr);

        return resp;
    };

    /**
     * 鍙戦�佹ā鏉挎秷鎭敮鎸佸皬绋嬪簭
     * Examples:
     * ```
     * String templateId = '妯℃澘id';
     * String page = 'index?foo=bar'; // 灏忕▼搴忛〉闈㈣矾寰�
     * String formId = '鎻愪氦琛ㄥ崟id';
     * String color = '#FF0000'; // 瀛椾綋棰滆壊
     * Object data = {
     *  keyword1: {
     *    "value":'榛勫厛鐢�',
     *    "color":"#173177"
     *  }
     * var emphasisKeyword = 'keyword1.DATA'
     * };
     * api.sendMiniProgramTemplate('openid', templateId, page, formId, data, color, emphasisKeyword);
     * ```
     * @param {String} openid 鎺ユ敹鑰咃紙鐢ㄦ埛锛夌殑 openid
     * @param {String} templateId 鎵�闇�涓嬪彂鐨勬ā鏉挎秷鎭殑id
     * @param {String} page 鐐瑰嚮妯℃澘鍗＄墖鍚庣殑璺宠浆椤甸潰锛屼粎闄愭湰灏忕▼搴忓唴鐨勯〉闈€�傛敮鎸佸甫鍙傛暟,锛堢ず渚媔ndex?foo=bar锛夈�傝瀛楁涓嶅～鍒欐ā鏉挎棤璺宠浆
     * @param {String} formId 琛ㄥ崟鎻愪氦鍦烘櫙涓嬶紝涓� submit 浜嬩欢甯︿笂鐨� formId锛涙敮浠樺満鏅笅锛屼负鏈鏀粯鐨� prepay_id
     * @param {Object} data 妯℃澘鍐呭锛屼笉濉垯涓嬪彂绌烘ā鏉�
     * @param {String} color 妯℃澘鍐呭瀛椾綋鐨勯鑹诧紝涓嶅～榛樿榛戣壊 銆愬簾寮冦��
     * @param {String} emphasisKeyword 妯℃澘闇�瑕佹斁澶х殑鍏抽敭璇嶏紝涓嶅～鍒欓粯璁ゆ棤鏀惧ぇ
     */
    public JSONObject sendMiniProgramTemplate (String openid,
                                               String templateId,
                                               String page,
                                               String formId,
                                               Map<String, Object> data,
                                               String color,
                                               String emphasisKeyword) {

        AccessToken token = this.ensureAccessToken();
        String accessToken = token.getAccessToken();

        String apiUrl = this.PREFIX + "message/wxopen/template/send?access_token=" + accessToken;

        Map<String, Object> template = new HashMap<String, Object>();
        data.put("touser", openid);
        data.put("template_id", templateId);
        data.put("page", page);
        data.put("form_id", formId);
        data.put("data", data);
        data.put("color", color);
        data.put("emphasis_keyword", emphasisKeyword);

        String respStr = HttpUtils.sendPostJsonRequest(apiUrl, JSON.toJSONString(template));
        JSONObject resp = JSON.parseObject(respStr);

        return resp;
    }


    /**
     * 鏂板涓存椂绱犳潗锛屽垎鍒湁鍥剧墖锛坕mage锛夈�佽闊筹紙voice锛夈�佽棰戯紙video锛夊拰缂╃暐鍥撅紙thumb锛�
     * 璇︽儏璇疯锛�<http://mp.weixin.qq.com/wiki/5/963fc70b80dc75483a271298a76a8d59.html>
     * Examples:
     * ```
     * api.uploadMedia('filepath', type);
     * ```
     *
     * Result:
     * ```
     * {"type":"TYPE","media_id":"MEDIA_ID","created_at":123456789}
     * ```
     * Shortcut:
     * - `api.uploadImageMedia(filepath);`
     * - `api.uploadVoiceMedia(filepath);`
     * - `api.uploadVideoMedia(filepath);`
     * - `api.uploadThumbMedia(filepath);`
     *
     * @param {String|InputStream} filepath 鏂囦欢璺緞/鏂囦欢Buffer鏁版嵁
     * @param {String} type 濯掍綋绫诲瀷锛屽彲鐢ㄥ�兼湁image銆乿oice銆乿ideo銆乼humb
     */
    public JSONObject uploadMedia (String filepath, String type) {
        return uploadMedia(filepath, type);
    }
    public JSONObject uploadMedia (InputStream fileInputStream, String type) {
        return uploadMedia(fileInputStream, type);
    }

    public JSONObject uploadMedia (Object filepath, String type) {

        AccessToken token = this.ensureAccessToken();
        String accessToken = token.getAccessToken();

        String apiUrl = this.PREFIX + "media/upload?access_token=" + accessToken + "&type=" + type;

        Map<String, Object> data = new HashMap<String, Object>();

        if(filepath instanceof String) {
            String filepathStr = (String) filepath;
            data.put("media", new File(filepathStr));
        }else if(filepath instanceof InputStream){
            data.put("media", filepath);
        }

        String respStr = HttpUtils.sendPostFormDataRequest(apiUrl, data);
        JSONObject resp = JSON.parseObject(respStr);

        return resp;
    };

    public JSONObject uploadImageMedia (String filepath) {
        return uploadMedia(filepath, "image");
    }
    public JSONObject uploadImageMedia (InputStream fileInputStream) {
        return uploadMedia(fileInputStream, "image");
    }
    public JSONObject uploadImageMedia (Object filepath) {
        return uploadMedia(filepath, "image");
    }
    public JSONObject uploadVoiceMedia (String filepath) {
        return uploadMedia(filepath, "voice");
    }
    public JSONObject uploadVoiceMedia (InputStream fileInputStream) {
        return uploadMedia(fileInputStream, "voice");
    }
    public JSONObject uploadVoiceMedia (Object filepath) {
        return uploadMedia(filepath, "voice");
    }
    public JSONObject uploadVideoMedia (String filepath) {
        return uploadMedia(filepath, "video");
    }
    public JSONObject uploadVideoMedia (InputStream fileInputStream) {
        return uploadMedia(fileInputStream, "video");
    }
    public JSONObject uploadVideoMedia (Object filepath) {
        return uploadMedia(filepath, "video");
    }
    public JSONObject uploadThumbMedia (String filepath) {
        return uploadMedia(filepath, "thumb");
    }
    public JSONObject uploadThumbMedia (InputStream fileInputStream) {
        return uploadMedia(fileInputStream, "thumb");
    }
    public JSONObject uploadThumbMedia (Object filepath) {
        return uploadMedia(filepath, "thumb");
    }

    /**
     * 鑾峰彇涓存椂绱犳潗
     * 璇︽儏璇疯锛�<http://mp.weixin.qq.com/wiki/11/07b6b76a6b6e8848e855a435d5e34a5f.html>
     * Examples:
     * ```
     * api.getMedia('media_id');
     * ```
     * - `result`, 璋冪敤姝ｅ父鏃跺緱鍒扮殑鏂囦欢Buffer瀵硅薄
     * - `res`, HTTP鍝嶅簲瀵硅薄
     * @param {String} mediaId 濯掍綋鏂囦欢鐨処D
     */
    public JSONObject getMedia (String mediaId) {

        AccessToken token = this.ensureAccessToken();
        String accessToken = token.getAccessToken();

        String url = this.PREFIX + "media/get?access_token=" + accessToken + "&media_id=" + mediaId;

        String respStr = HttpUtils.sendGetRequest(url);
        JSONObject resp = JSON.parseObject(respStr);

        return resp;
    };
    /**
     * 涓婁紶鍥炬枃娑堟伅鍐呯殑鍥剧墖鑾峰彇URL
     * 璇︽儏璇疯锛�<http://mp.weixin.qq.com/wiki/15/5380a4e6f02f2ffdc7981a8ed7a40753.html>
     * Examples:
     * ```
     * api.uploadImage('filepath');
     * ```
     * Result:
     * ```
     * {"url":  "http://mmbiz.qpic.cn/mmbiz/gLO17UPS6FS2xsypf378iaNhWacZ1G1UplZYWEYfwvuU6Ont96b1roYsCNFwaRrSaKTPCUdBK9DgEHicsKwWCBRQ/0"}
     * ```
     * @param {String} filepath 鍥剧墖鏂囦欢璺緞
     */
    public JSONObject uploadImage (String filepath) {

        AccessToken token = this.ensureAccessToken();
        String accessToken = token.getAccessToken();

        String apiUrl = this.PREFIX + "media/uploadimg?access_token=" + accessToken;

        Map<String, Object> data = new HashMap<String, Object>();
        data.put("media", new File(filepath));

        String respStr = HttpUtils.sendPostFormDataRequest(apiUrl, data);
        JSONObject resp = JSON.parseObject(respStr);

        return resp;
    }


    /**
     * 鍒涘缓鑷畾涔夎彍鍗�
     * 璇︾粏璇风湅锛歨ttp://mp.weixin.qq.com/wiki/index.php?title=鑷畾涔夎彍鍗曞垱寤烘帴鍙� * Menu:
     * ```
     * {
     *  "button":[
     *    {
     *      "type":"click",
     *      "name":"浠婃棩姝屾洸",
     *      "key":"V1001_TODAY_MUSIC"
     *    },
     *    {
     *      "name":"鑿滃崟",
     *      "sub_button":[
     *        {
     *          "type":"view",
     *          "name":"鎼滅储",
     *          "url":"http://www.soso.com/"
     *        },
     *        {
     *          "type":"click",
     *          "name":"璧炰竴涓嬫垜浠�",
     *          "key":"V1001_GOOD"
     *        }]
     *      }]
     *    }
     *  ]
     * }
     * ```
     * Examples:
     * ```
     * var result = await api.createMenu(menu);
     * ```
     * Result:
     * ```
     * {"errcode":0,"errmsg":"ok"}
     * ```
     * @param {Object} menu 鑿滃崟瀵硅薄
     */
    public boolean createMenu (Map<String, Object> menu) {

        AccessToken token = this.ensureAccessToken();
        String accessToken = token.getAccessToken();

        String apiUrl = this.PREFIX + "menu/create?access_token=" + accessToken;

        String respStr = HttpUtils.sendPostJsonRequest(apiUrl, JSON.toJSONString(menu));
        JSONObject resp = JSON.parseObject(respStr);
        int errCode = resp.getIntValue("errcode");
        if(errCode == 0){
            return true;
        }else {
            return false;
        }
    };

    /**
     * 鑾峰彇鑿滃崟
     * 璇︾粏璇风湅锛�<http://mp.weixin.qq.com/wiki/index.php?title=鑷畾涔夎彍鍗曟煡璇㈡帴鍙�> * Examples:
     * ```
     * var result = await api.getMenu();
     * ```
     * Result:
     * ```
     * // 缁撴灉绀轰緥
     * {
     *  "menu": {
     *    "button":[
     *      {"type":"click","name":"浠婃棩姝屾洸","key":"V1001_TODAY_MUSIC","sub_button":[]},
     *      {"type":"click","name":"姝屾墜绠�浠�","key":"V1001_TODAY_SINGER","sub_button":[]},
     *      {"name":"鑿滃崟","sub_button":[
     *        {"type":"view","name":"鎼滅储","url":"http://www.soso.com/","sub_button":[]},
     *        {"type":"view","name":"瑙嗛","url":"http://v.qq.com/","sub_button":[]},
     *        {"type":"click","name":"璧炰竴涓嬫垜浠�","key":"V1001_GOOD","sub_button":[]}]
     *      }
     *    ]
     *  }
     * }
     * ```
     */
    public JSONObject getMenu () {

        AccessToken token = this.ensureAccessToken();
        String accessToken = token.getAccessToken();

        String apiUrl = this.PREFIX + "menu/get?access_token=" + accessToken;

        String respStr = HttpUtils.sendGetRequest(apiUrl);
        JSONObject resp = JSON.parseObject(respStr);
        JSONObject menu = resp.getJSONObject("menu");
        return menu;
    };

    /**
     * 鍒犻櫎鑷畾涔夎彍鍗�
     * 璇︾粏璇风湅锛�<http://mp.weixin.qq.com/wiki/index.php?title=鑷畾涔夎彍鍗曞垹闄ゆ帴鍙�>
     * Examples:
     * ```
     * var result = await api.removeMenu();
     * ```
     * Result:
     * ```
     * {"errcode":0,"errmsg":"ok"}
     * ```
     */
    public boolean removeMenu () {

        AccessToken token = this.ensureAccessToken();
        String accessToken = token.getAccessToken();

        String apiUrl = this.PREFIX + "menu/delete?access_token=" + accessToken;

        String respStr = HttpUtils.sendGetRequest(apiUrl);
        JSONObject resp = JSON.parseObject(respStr);
        int errcode = resp.getIntValue("errcode");
        if(errcode == 0){
            return true;
        }else {
            return false;
        }
    };

    /**
     * 鑾峰彇鑷畾涔夎彍鍗曢厤缃�
     * 璇︾粏璇风湅锛�<http://mp.weixin.qq.com/wiki/17/4dc4b0514fdad7a5fbbd477aa9aab5ed.html>
     * Examples:
     * ```
     * var result = await api.getMenuConfig();
     * ```
     * Result:
     * ```
     * {"errcode":0,"errmsg":"ok"}
     * ```
     */
    public JSONObject getMenuConfig () {

        AccessToken token = this.ensureAccessToken();
        String accessToken = token.getAccessToken();

        String apiUrl = this.PREFIX + "get_current_selfmenu_info?access_token=" + accessToken;

        String respStr = HttpUtils.sendGetRequest(apiUrl);
        JSONObject resp = JSON.parseObject(respStr);
        return resp;
    }

    /**
     * 鍒涘缓涓�у寲鑷畾涔夎彍鍗�
     * 璇︾粏璇风湅锛歨ttp://mp.weixin.qq.com/wiki/0/c48ccd12b69ae023159b4bfaa7c39c20.html * Menu:
     * ```
     * {
     *  "button":[
     *  {
     *      "type":"click",
     *      "name":"浠婃棩姝屾洸",
     *      "key":"V1001_TODAY_MUSIC"
     *  },
     *  {
     *    "name":"鑿滃崟",
     *    "sub_button":[
     *    {
     *      "type":"view",
     *      "name":"鎼滅储",
     *      "url":"http://www.soso.com/"
     *    },
     *    {
     *      "type":"view",
     *      "name":"瑙嗛",
     *      "url":"http://v.qq.com/"
     *    },
     *    {
     *      "type":"click",
     *      "name":"璧炰竴涓嬫垜浠�",
     *      "key":"V1001_GOOD"
     *    }]
     * }],
     * "matchrule":{
     *  "group_id":"2",
     *  "sex":"1",
     *  "country":"涓浗",
     *  "province":"骞夸笢",
     *  "city":"骞垮窞",
     *  "client_platform_type":"2"
     *  }
     * }
     * ```
     * Examples:
     * ```
     * var result = await api.addConditionalMenu(menu);
     * ```
     * Result:
     * ```
     * {"errcode":0,"errmsg":"ok"}
     * ```
     * @param {Object} menu 鑿滃崟瀵硅薄
     */
    public boolean addConditionalMenu (Map<String, Object> menu) {

        AccessToken token = this.ensureAccessToken();
        String accessToken = token.getAccessToken();

        // https://api.weixin.qq.com/cgi-bin/menu/addconditional?access_token=ACCESS_TOKEN
        String apiUrl = this.PREFIX + "menu/addconditional?access_token=" + accessToken;

        String respStr = HttpUtils.sendPostJsonRequest(apiUrl, JSON.toJSONString(menu));
        JSONObject resp = JSON.parseObject(respStr);
        int errCode = resp.getIntValue("errcode");
        if(errCode == 0){
            return true;
        }else{
            return false;
        }
    };

    /**
     * 鍒犻櫎涓�у寲鑷畾涔夎彍鍗�
     * 璇︾粏璇风湅锛歨ttp://mp.weixin.qq.com/wiki/0/c48ccd12b69ae023159b4bfaa7c39c20.html * Menu:
     * ```
     * {
     *  "menuid":"208379533"
     * }
     * ```
     * Examples:
     * ```
     * var result = await api.delConditionalMenu(menuid);
     * ```
     * Result:
     * ```
     * {"errcode":0,"errmsg":"ok"}
     * ```
     * @param {String} menuid 鑿滃崟id
     */
    public boolean delConditionalMenu (String menuid) {

        AccessToken token = this.ensureAccessToken();
        String accessToken = token.getAccessToken();

        // https://api.weixin.qq.com/cgi-bin/menu/delconditional?access_token=ACCESS_TOKEN
        String apiUrl = this.PREFIX + "menu/delconditional?access_token=" + accessToken;

        Map<String, Object> data = new HashMap<String, Object>();
        data.put("menuid", menuid);

        String respStr = HttpUtils.sendPostJsonRequest(apiUrl, JSON.toJSONString(data));
        JSONObject resp = JSON.parseObject(respStr);
        int errCode = resp.getIntValue("errcode");
        if(errCode == 0){
            return true;
        }else{
            return false;
        }
    };

    /**
     * 娴嬭瘯涓�у寲鑷畾涔夎彍鍗�
     * 璇︾粏璇风湅锛歨ttp://mp.weixin.qq.com/wiki/0/c48ccd12b69ae023159b4bfaa7c39c20.html * Menu:
     * ```
     * {
     *  "user_id":"nickma"
     * }
     * ```
     * Examples:
     * ```
     * var result = await api.tryConditionalMenu(user_id);
     * ```
     * Result:
     * ```
     * {
     *    "button": [
     *        {
     *            "type": "view",
     *            "name": "tx",
     *            "url": "http://www.qq.com/",
     *            "sub_button": [ ]
     *        },
     *        {
     *            "type": "view",
     *            "name": "tx",
     *            "url": "http://www.qq.com/",
     *            "sub_button": [ ]
     *        },
     *        {
     *            "type": "view",
     *            "name": "tx",
     *            "url": "http://www.qq.com/",
     *            "sub_button": [ ]
     *        }
     *    ]
     * }
     * ```
     * @param {String} user_id user_id鍙互鏄矇涓濈殑OpenID锛屼篃鍙互鏄矇涓濈殑寰俊鍙枫��
     */
    public JSONObject tryConditionalMenu (String user_id) {

        AccessToken token = this.ensureAccessToken();
        String accessToken = token.getAccessToken();

        // https://api.weixin.qq.com/cgi-bin/menu/trymatch?access_token=ACCESS_TOKEN
        String apiUrl = this.PREFIX + "menu/trymatch?access_token=" + accessToken;

        Map<String, Object> data = new HashMap<String, Object>();
        data.put("user_id", user_id);

        String respStr = HttpUtils.sendPostJsonRequest(apiUrl, JSON.toJSONString(data));
        JSONObject resp = JSON.parseObject(respStr);
        return resp;
    };


    /**
     * 瀹㈡湇娑堟伅锛屽彂閫佹枃瀛楁秷鎭�
     * 璇︾粏缁嗚妭 http://mp.weixin.qq.com/wiki/index.php?title=鍙戦�佸鏈嶆秷鎭�
     * Examples:
     * ```
     * api.sendText('openid', 'Hello world');
     * ```
     * @param {String} openid 鐢ㄦ埛鐨刼penid
     * @param {String} text 鍙戦�佺殑娑堟伅鍐呭
     */
    public JSONObject sendText (String openid, String text) {

        AccessToken token = this.ensureAccessToken();
        String accessToken = token.getAccessToken();

        String apiUrl = this.PREFIX + "message/custom/send?access_token=" + accessToken;

        Map<String, Object> data = new HashMap<String, Object>();
        Map<String, Object> textMap = new HashMap<String, Object>();
        textMap.put("content", text);
        data.put("touser", openid);
        data.put("msgtype", "text");
        data.put("text", textMap);

        String respStr = HttpUtils.sendPostJsonRequest(apiUrl, JSON.toJSONString(data));
        JSONObject resp = JSON.parseObject(respStr);
        return resp;
    };

    /**
     * 瀹㈡湇娑堟伅锛屽彂閫佸浘鐗囨秷鎭�
     * 璇︾粏缁嗚妭 http://mp.weixin.qq.com/wiki/index.php?title=鍙戦�佸鏈嶆秷鎭�
     * Examples:
     * ```
     * api.sendImage('openid', 'media_id');
     * ```
     * @param {String} openid 鐢ㄦ埛鐨刼penid
     * @param {String} mediaId 濯掍綋鏂囦欢鐨処D锛屽弬瑙乽ploadMedia鏂规硶
     */
    public JSONObject sendImage (String openid, String mediaId) {

        AccessToken token = this.ensureAccessToken();
        String accessToken = token.getAccessToken();

        String apiUrl = this.PREFIX + "message/custom/send?access_token=" + accessToken;

        Map<String, Object> data = new HashMap<String, Object>();
        Map<String, Object> imageMap = new HashMap<String, Object>();
        imageMap.put("media_id", mediaId);
        data.put("touser", openid);
        data.put("msgtype", "image");
        data.put("image", imageMap);

        String respStr = HttpUtils.sendPostJsonRequest(apiUrl, JSON.toJSONString(data));
        JSONObject resp = JSON.parseObject(respStr);
        return resp;
    }

    /**
     * 瀹㈡湇娑堟伅锛屽彂閫佸崱鍒�
     * 璇︾粏缁嗚妭 http://mp.weixin.qq.com/wiki/index.php?title=鍙戦�佸鏈嶆秷鎭�
     * Examples:
     * ```
     * api.sendCard('openid', 'card_id');
     * ```
     * @param {String} openid 鐢ㄦ埛鐨刼penid
     * @param {String} card_id 鍗″埜鐨処D
     */
    public JSONObject sendCard (String openid, String cardid) {

        AccessToken token = this.ensureAccessToken();
        String accessToken = token.getAccessToken();

        String apiUrl = this.PREFIX + "message/custom/send?access_token=" + accessToken;

        Map<String, Object> data = new HashMap<String, Object>();
        Map<String, Object> wxcardMap = new HashMap<String, Object>();
        wxcardMap.put("card_id", cardid);
        data.put("touser", openid);
        data.put("msgtype", "wxcard");
        data.put("wxcard", wxcardMap);

        String respStr = HttpUtils.sendPostJsonRequest(apiUrl, JSON.toJSONString(data));
        JSONObject resp = JSON.parseObject(respStr);
        return resp;
    }

    /**
     * 瀹㈡湇娑堟伅锛屽彂閫佽闊虫秷鎭�
     * 璇︾粏缁嗚妭 http://mp.weixin.qq.com/wiki/index.php?title=鍙戦�佸鏈嶆秷鎭�
     * Examples:
     * ```
     * api.sendVoice('openid', 'media_id');
     * ```
     * @param {String} openid 鐢ㄦ埛鐨刼penid
     * @param {String} mediaId 濯掍綋鏂囦欢鐨処D
     */
    public JSONObject sendVoice (String openid, String mediaId) {

        AccessToken token = this.ensureAccessToken();
        String accessToken = token.getAccessToken();

        String apiUrl = this.PREFIX + "message/custom/send?access_token=" + accessToken;

        Map<String, Object> data = new HashMap<String, Object>();
        Map<String, Object> voiceMap = new HashMap<String, Object>();
        voiceMap.put("media_id", mediaId);
        data.put("touser", openid);
        data.put("msgtype", "voice");
        data.put("voice", voiceMap);

        String respStr = HttpUtils.sendPostJsonRequest(apiUrl, JSON.toJSONString(data));
        JSONObject resp = JSON.parseObject(respStr);
        return resp;
    };

    /**
     * 瀹㈡湇娑堟伅锛屽彂閫佽棰戞秷鎭�
     * 璇︾粏缁嗚妭 http://mp.weixin.qq.com/wiki/index.php?title=鍙戦�佸鏈嶆秷鎭�
     * Examples:
     * ```
     * api.sendVideo('openid', 'media_id', 'thumb_media_id');
     * ```
     * @param {String} openid 鐢ㄦ埛鐨刼penid
     * @param {String} mediaId 濯掍綋鏂囦欢鐨処D
     * @param {String} thumbMediaId 缂╃暐鍥炬枃浠剁殑ID
     */
    public JSONObject sendVideo (String openid, String mediaId, String thumbMediaId) {

        AccessToken token = this.ensureAccessToken();
        String accessToken = token.getAccessToken();

        String apiUrl = this.PREFIX + "message/custom/send?access_token=" + accessToken;

        Map<String, Object> data = new HashMap<String, Object>();
        Map<String, Object> videoMap = new HashMap<String, Object>();
        videoMap.put("media_id", mediaId);
        videoMap.put("thumb_media_id", thumbMediaId);
        data.put("touser", openid);
        data.put("msgtype", "video");
        data.put("video", videoMap);

        String respStr = HttpUtils.sendPostJsonRequest(apiUrl, JSON.toJSONString(data));
        JSONObject resp = JSON.parseObject(respStr);
        return resp;
    };

    /**
     * 瀹㈡湇娑堟伅锛屽彂閫侀煶涔愭秷鎭�
     * 璇︾粏缁嗚妭 http://mp.weixin.qq.com/wiki/index.php?title=鍙戦�佸鏈嶆秷鎭�
     * Examples:
     * ```
     * var music = {
     *  title: '闊充箰鏍囬', // 鍙��
     *  description: '鎻忚堪鍐呭', // 鍙��
     *  musicurl: 'http://url.cn/xxx', 闊充箰鏂囦欢鍦板潃
     *  hqmusicurl: "HQ_MUSIC_URL",
     *  thumb_media_id: "THUMB_MEDIA_ID"
     * };
     * api.sendMusic('openid', music);
     * ```
     * @param {String} openid 鐢ㄦ埛鐨刼penid
     * @param {Object} music 闊充箰鏂囦欢
     */
    public JSONObject sendMusic (String openid, Map<String, Object> music) {
        AccessToken token = this.ensureAccessToken();
        String accessToken = token.getAccessToken();

        String apiUrl = this.PREFIX + "message/custom/send?access_token=" + accessToken;

        Map<String, Object> data = new HashMap<String, Object>();
        data.put("touser", openid);
        data.put("msgtype", "music");
        data.put("music", music);

        String respStr = HttpUtils.sendPostJsonRequest(apiUrl, JSON.toJSONString(data));
        JSONObject resp = JSON.parseObject(respStr);
        return resp;
    };

    /**
     * 瀹㈡湇娑堟伅锛屽彂閫佸浘鏂囨秷鎭�
     * 璇︾粏缁嗚妭 http://mp.weixin.qq.com/wiki/index.php?title=鍙戦�佸鏈嶆秷鎭�
     * Examples:
     * ```
     * var articles = [
     *  {
     *    "title":"Happy Day",
     *    "description":"Is Really A Happy Day",
     *    "url":"URL",
     *    "picurl":"PIC_URL"
     *  },
     *  {
     *    "title":"Happy Day",
     *    "description":"Is Really A Happy Day",
     *    "url":"URL",
     *    "picurl":"PIC_URL"
     *  }];
     * api.sendNews('openid', articles);
     * ```
     * @param {String} openid 鐢ㄦ埛鐨刼penid
     * @param {Array} articles 鍥炬枃鍒楄〃
     */
    public JSONObject sendNews (String openid, List<Map<String, Object>> articles) {

        AccessToken token = this.ensureAccessToken();
        String accessToken = token.getAccessToken();

        String apiUrl = this.PREFIX + "message/custom/send?access_token=" + accessToken;

        Map<String, Object> data = new HashMap<String, Object>();
        Map<String, Object> newsMap = new HashMap<String, Object>();
        newsMap.put("articles", articles);
        data.put("touser", openid);
        data.put("msgtype", "news");
        data.put("news", newsMap);

        String respStr = HttpUtils.sendPostJsonRequest(apiUrl, JSON.toJSONString(data));
        JSONObject resp = JSON.parseObject(respStr);
        return resp;
    };

    /**
     * 瀹㈡湇娑堟伅锛屽彂閫佸浘鏂囨秷鎭紙鐐瑰嚮璺宠浆鍒板浘鏂囨秷鎭〉闈級
     * 璇︾粏缁嗚妭 https://mp.weixin.qq.com/wiki?t=resource/res_main&id=mp1421140547
     * Examples:
     * ```
     * api.sendMpNews('openid', 'mediaId');
     * ```
     * @param {String} openid 鐢ㄦ埛鐨刼penid
     * @param {String} mediaId 鍥炬枃娑堟伅濯掍綋鏂囦欢鐨処D
     */
    public JSONObject sendMpNews (String openid, String mediaId) {

        AccessToken token = this.ensureAccessToken();
        String accessToken = token.getAccessToken();

        String apiUrl = this.PREFIX + "message/custom/send?access_token=" + accessToken;

        Map<String, Object> data = new HashMap<String, Object>();
        Map<String, Object> mpnewsMap = new HashMap<String, Object>();
        mpnewsMap.put("media_id", mediaId);
        data.put("touser", openid);
        data.put("msgtype", "mpnews");
        data.put("mpnews", mpnewsMap);

        String respStr = HttpUtils.sendPostJsonRequest(apiUrl, JSON.toJSONString(data));
        JSONObject resp = JSON.parseObject(respStr);
        return resp;
    };

    /**
     * 瀹㈡湇娑堟伅锛屽彂閫佸皬绋嬪簭鍗＄墖锛堣姹傚皬绋嬪簭涓庡叕浼楀彿宸插叧鑱旓級
     * 璇︾粏缁嗚妭 https://mp.weixin.qq.com/wiki?t=resource/res_main&id=mp1421140547
     * Examples:
     * ```
     * var miniprogram = {
     *  title: '灏忕▼搴忔爣棰�', // 蹇呭～
     *  appid: '灏忕▼搴廰ppid', // 蹇呭～
     *  pagepath: 'pagepath', // 鎵撳紑鍚庡皬绋嬪簭鐨勫湴鍧�锛屽彲浠ュ甫query
     *  thumb_media_id: "THUMB_MEDIA_ID"
     * };
     * api.sendMiniProgram('openid', miniprogram);
     * ```
     * @param {String} openid 鐢ㄦ埛鐨刼penid
     * @param {Object} miniprogram 灏忕▼搴忎俊鎭�
     */
    public JSONObject sendMiniProgram (String openid, Map<String, Object> miniprogram) {

        AccessToken token = this.ensureAccessToken();
        String accessToken = token.getAccessToken();

        String apiUrl = this.PREFIX + "message/custom/send?access_token=" + accessToken;

        Map<String, Object> data = new HashMap<String, Object>();
        data.put("touser", openid);
        data.put("msgtype", "miniprogrampage");
        data.put("miniprogrampage", miniprogram);

        String respStr = HttpUtils.sendPostJsonRequest(apiUrl, JSON.toJSONString(data));
        JSONObject resp = JSON.parseObject(respStr);
        return resp;
    };

    /**
     * 鑾峰彇鑷姩鍥炲瑙勫垯
     * 璇︾粏璇风湅锛�<http://mp.weixin.qq.com/wiki/19/ce8afc8ae7470a0d7205322f46a02647.html>
     * Examples:
     * ```
     * var result = await api.getAutoreply();
     * ```
     * Result:
     * ```
     * {
     * "is_add_friend_reply_open": 1,
     * "is_autoreply_open": 1,
     * "add_friend_autoreply_info": {
     *     "type": "text",
     *     "content": "Thanks for your attention!"
     * },
     * "message_default_autoreply_info": {
     *     "type": "text",
     *     "content": "Hello, this is autoreply!"
     * },
     * "keyword_autoreply_info": {
     *     "list": [
     *         {
     *             "rule_name": "autoreply-news",
     *             "create_time": 1423028166,
     *             "reply_mode": "reply_all",
     *             "keyword_list_info": [
     *                 {
     *                     "type": "text",
     *                     "match_mode": "contain",
     *                     "content": "news娴嬭瘯"//姝ゅcontent鍗充负鍏抽敭璇嶅唴瀹�
     *                 }
     *             ],
     *             "reply_list_info": [
     *                 {
     *                     "type": "news",
     *                     "news_info": {
     *                         "list": [
     *                             {
     *                                 "title": "it's news",
     *                                 "author": "jim",
     *                                 "digest": "it's digest",
     *                                 "show_cover": 1,
     *                                 "cover_url": "http://mmbiz.qpic.cn/mmbiz/GE7et87vE9vicuCibqXsX9GPPLuEtBfXfKbE8sWdt2DDcL0dMfQWJWTVn1N8DxI0gcRmrtqBOuwQHeuPKmFLK0ZQ/0",
     *                                 "content_url": "http://mp.weixin.qq.com/s?__biz=MjM5ODUwNTM3Ng==&mid=203929886&idx=1&sn=628f964cf0c6d84c026881b6959aea8b#rd",
     *                                 "source_url": "http://www.url.com"
     *                             }
     *                         ]
     *                     }
     *                 },
     *                 {
     *                     ....
     *                 }
     *             ]
     *         },
     *         {
     *             "rule_name": "autoreply-voice",
     *             "create_time": 1423027971,
     *             "reply_mode": "random_one",
     *             "keyword_list_info": [
     *                 {
     *                     "type": "text",
     *                     "match_mode": "contain",
     *                     "content": "voice娴嬭瘯"
     *                 }
     *             ],
     *             "reply_list_info": [
     *                 {
     *                     "type": "voice",
     *                     "content": "NESsxgHEvAcg3egJTtYj4uG1PTL6iPhratdWKDLAXYErhN6oEEfMdVyblWtBY5vp"
     *                 }
     *             ]
     *         },
     *         ...
     *     ]
     * }
     * }
     * ```
     */
    public JSONObject getAutoreply () {

        AccessToken token = this.ensureAccessToken();
        String accessToken = token.getAccessToken();

        String apiUrl = this.PREFIX + "get_current_autoreply_info?access_token=" + accessToken;

        String respStr = HttpUtils.sendGetRequest(apiUrl);
        JSONObject resp = JSON.parseObject(respStr);
        return resp;
    }


    /**
     * 鍒涘缓闂ㄥ簵
     * Tips:
     * - 鍒涘缓闂ㄥ簵鎺ュ彛璋冪敤鎴愬姛鍚庝笉浼氬疄鏃惰繑鍥瀙oi_id銆�
     * - 鎴愬姛鍒涘缓鍚庯紝闂ㄥ簵淇℃伅浼氱粡杩囧鏍革紝瀹℃牳閫氳繃鍚庢柟鍙娇鐢ㄥ苟鑾峰彇poi_id銆�
     * - 鍥剧墖photo_url蹇呴』涓轰笂浼犲浘鐗囨帴鍙�(api.uploadLogo锛屽弬瑙佸崱鍒告帴鍙�)鐢熸垚鐨剈rl銆�
     * - 闂ㄥ簵绫荤洰categories璇峰弬鑰冨井淇″叕浼楀彿鍚庡彴鐨勯棬搴楃鐞嗛儴鍒嗐�� * Poi:
     * ```
     * {
     *   "sid": "5794560",
     *   "business_name": "鑲墦楦�",
     *   "branch_name": "涓滄柟璺簵",
     *   "province": "涓婃捣甯�",
     *   "city": "涓婃捣甯�",
     *   "district": "娴︿笢鏂板尯",
     *   "address": "涓滄柟璺�88鍙�",
     *   "telephone": "021-5794560",
     *   "categories": ["缇庨,蹇灏忓悆"],
     *   "offset_type": 1,
     *   "longitude": 125.5794560,
     *   "latitude": 45.5794560,
     *   "photo_list": [{
     *     "photo_url": "https://5794560.qq.com/1"
     *   }, {
     *     "photo_url": "https://5794560.qq.com/2"
     *   }],
     *   "recommend": "鑴夊楦¤吙鍫″椁�,鑴変箰楦�,鍏ㄥ鎹�",
     *   "special": "鍏嶈垂WIFE,澶栧崠鏈嶅姟",
     *   "introduction": "鑲墦楦℃槸鍏ㄧ悆澶у瀷璺ㄥ浗杩為攣椁愬巺,2015骞村垱绔嬩簬绫冲浗,鍦ㄤ笘鐣屼笂澶х害鎷ユ湁3 浜块棿鍒嗗簵,涓昏鍞崠鑲墦楦＄瓑鍨冨溇椋熷搧",
     *   "open_time": "10:00-18:00",
     *   "avg_price": 88
     * }
     * ```
     * Examples:
     * ```
     * api.addPoi(poi);
     * ```
     * Result:
     * ```
     * {"errcode":0,"errmsg":"ok"}
     * ```
     * @name addPoi
     * @param {Object} poi 闂ㄥ簵瀵硅薄
     */
    public boolean addPoi (Map<String, Object> poi) {

        AccessToken token = this.ensureAccessToken();
        String accessToken = token.getAccessToken();

        String apiUrl = this.PREFIX + "poi/addpoi?access_token=" + accessToken;

        Map<String, Object> data = new HashMap<String, Object>();
        Map<String, Object> businessMap = new HashMap<String, Object>();
        data.put("base_info", poi);
        data.put("business", businessMap);

        String respStr = HttpUtils.sendPostJsonRequest(apiUrl, JSON.toJSONString(data));
        JSONObject resp = JSON.parseObject(respStr);
        int errCode = resp.getIntValue("errcode");
        if(errCode == 0){
            return true;
        }else{
            return false;
        }
    };

    /**
     * 鑾峰彇闂ㄥ簵淇℃伅
     * Examples:
     * ```
     * api.getPoi(POI_ID);
     * ```
     * Result:
     * ```
     * {
     *   "sid": "5794560",
     *   "business_name": "鑲墦楦�",
     *   "branch_name": "涓滄柟璺簵",
     *   "province": "涓婃捣甯�",
     *   "city": "涓婃捣甯�",
     *   "district": "娴︿笢鏂板尯",
     *   "address": "涓滄柟璺�88鍙�",
     *   "telephone": "021-5794560",
     *   "categories": ["缇庨,蹇灏忓悆"],
     *   "offset_type": 1,
     *   "longitude": 125.5794560,
     *   "latitude": 45.5794560,
     *   "photo_list": [{
     *     "photo_url": "https://5794560.qq.com/1"
     *   }, {
     *     "photo_url": "https://5794560.qq.com/2"
     *   }],
     *   "recommend": "鑴夊楦¤吙鍫″椁�,鑴変箰楦�,鍏ㄥ鎹�",
     *   "special": "鍏嶈垂WIFE,澶栧崠鏈嶅姟",
     *   "introduction": "鑲墦楦℃槸鍏ㄧ悆澶у瀷璺ㄥ浗杩為攣椁愬巺,2015骞村垱绔嬩簬绫冲浗,鍦ㄤ笘鐣屼笂澶х害鎷ユ湁3 浜块棿鍒嗗簵,涓昏鍞崠鑲墦楦＄瓑鍨冨溇椋熷搧",
     *   "open_time": "10:00-18:00",
     *   "avg_price": 88,
     *   "available_state": 3,
     *   "update_status": 0
     * }
     * ```
     * @name getPoi
     * @param {Number} poiId 闂ㄥ簵ID
     */
    public JSONObject getPoi (String poiId) {

        AccessToken token = this.ensureAccessToken();
        String accessToken = token.getAccessToken();

        String apiUrl = this.PREFIX + "poi/getpoi?access_token=" + accessToken;

        Map<String, Object> data = new HashMap<String, Object>();
        data.put("poi_id", poiId);

        String respStr = HttpUtils.sendPostJsonRequest(apiUrl, JSON.toJSONString(data));
        JSONObject resp = JSON.parseObject(respStr);
        return resp;
    }

    /**
     * 鑾峰彇闂ㄥ簵鍒楄〃
     * Examples:
     * ```
     * api.getPois(0, 20);
     * ```
     * Result:
     * ```
     * {
     *   "errcode": 0,
     *   "errmsg": "ok"
     *   "business_list": [{
     *     "base_info": {
     *       "sid": "100",
     *       "poi_id": "5794560",
     *       "business_name": "鑲墦楦�",
     *       "branch_name": "涓滄柟璺簵",
     *       "address": "涓滄柟璺�88鍙�",
     *       "available_state": 3
     *     }
     *   }, {
     *     "base_info": {
     *       "sid": "101",
     *       "business_name": "鑲墦楦�",
     *       "branch_name": "瑗挎柟璺簵",
     *       "address": "瑗挎柟璺�88鍙�",
     *       "available_state": 4
     *     }
     *   }],
     *   "total_count": "2",
     * }
     * ```
     * @name getPois
     * @param {Number} begin 寮�濮嬩綅缃紝0鍗充负浠庣涓�鏉″紑濮嬫煡璇�
     * @param {Number} limit 杩斿洖鏁版嵁鏉℃暟锛屾渶澶у厑璁�50锛岄粯璁や负20
     */
    public JSONObject getPois (Long begin) {
        return getPois(begin, 20);
    }
    public JSONObject getPois (Long begin, Integer limit) {

        AccessToken token = this.ensureAccessToken();
        String accessToken = token.getAccessToken();

        String apiUrl = this.PREFIX + "poi/getpoilist?access_token=" + accessToken;

        Map<String, Object> data = new HashMap<String, Object>();
        data.put("begin", begin);
        data.put("limit", limit);

        String respStr = HttpUtils.sendPostJsonRequest(apiUrl, JSON.toJSONString(data));
        JSONObject resp = JSON.parseObject(respStr);
        return resp;
    }

    /**
     * 鍒犻櫎闂ㄥ簵
     * Tips:
     * - 寰呭鏍搁棬搴椾笉鍏佽鍒犻櫎
     * Examples:
     * ```
     * api.delPoi(POI_ID);
     * ```
     * @name delPoi
     * @param {Number} poiId 闂ㄥ簵ID
     */
    public JSONObject delPoi (String poiId) {

        AccessToken token = this.ensureAccessToken();
        String accessToken = token.getAccessToken();

        String apiUrl = this.PREFIX + "poi/delpoi?access_token=" + accessToken;

        Map<String, Object> data = new HashMap<String, Object>();
        data.put("poi_id", poiId);

        String respStr = HttpUtils.sendPostJsonRequest(apiUrl, JSON.toJSONString(data));
        JSONObject resp = JSON.parseObject(respStr);
        return resp;
    };

    /**
     * 淇敼闂ㄥ簵鏈嶅姟淇℃伅
     * Tips:
     * - 寰呭鏍搁棬搴椾笉鍏佽淇敼
     * Poi:
     * ```
     * {
     *   "poi_id": "5794560",
     *   "telephone": "021-5794560",
     *   "photo_list": [{
     *     "photo_url": "https://5794560.qq.com/1"
     *   }, {
     *     "photo_url": "https://5794560.qq.com/2"
     *   }],
     *   "recommend": "鑴夊楦¤吙鍫″椁�,鑴変箰楦�,鍏ㄥ鎹�",
     *   "special": "鍏嶈垂WIFE,澶栧崠鏈嶅姟",
     *   "introduction": "鑲墦楦℃槸鍏ㄧ悆澶у瀷璺ㄥ浗杩為攣椁愬巺,2015骞村垱绔嬩簬绫冲浗,鍦ㄤ笘鐣屼笂澶х害鎷ユ湁3 浜块棿鍒嗗簵,涓昏鍞崠鑲墦楦＄瓑鍨冨溇椋熷搧",
     *   "open_time": "10:00-18:00",
     *   "avg_price": 88
     * }
     * ```
     * 鐗瑰埆娉ㄦ剰锛屼互涓�7涓瓧娈碉紝鑻ユ湁濉啓鍐呭鍒欎负瑕嗙洊鏇存柊锛岃嫢鏃犲唴瀹瑰垯瑙嗕负涓嶄慨鏀癸紝缁存寔鍘熸湁鍐呭銆�
     * photo_list瀛楁涓哄叏鍒楄〃瑕嗙洊锛岃嫢闇�瑕佸鍔犲浘鐗囷紝闇�灏嗕箣鍓嶅浘鐗囧悓鏍锋斁鍏ist涓紝鍦ㄥ叾鍚庡鍔犳柊澧炲浘鐗囥�� * Examples:
     * ```
     * api.updatePoi(poi);
     * ```
     * Result:
     * ```
     * {"errcode":0,"errmsg":"ok"}
     * ```
     * @name updatePoi
     * @param {Object} poi 闂ㄥ簵瀵硅薄
     */
    public boolean updatePoi (Map<String, Object> poi) {

        AccessToken token = this.ensureAccessToken();
        String accessToken = token.getAccessToken();

        String apiUrl = this.PREFIX + "poi/updatepoi?access_token=" + accessToken;

        Map<String, Object> data = new HashMap<String, Object>();
        Map<String, Object> businessMap = new HashMap<String, Object>();
        businessMap.put("base_info", poi);
        data.put("business", businessMap);

        String respStr = HttpUtils.sendPostJsonRequest(apiUrl, JSON.toJSONString(data));
        JSONObject resp = JSON.parseObject(respStr);
        int errCode = resp.getIntValue("errcode");
        if(errCode == 0){
            return true;
        }else{
            return false;
        }
    }



    /**
     * 鑾峰彇鐢ㄦ埛鍩烘湰淇℃伅銆傚彲浠ヨ缃甽ang锛屽叾涓瓃h_CN 绠�浣擄紝zh_TW 绻佷綋锛宔n 鑻辫銆傞粯璁や负en
     * 璇︽儏璇疯锛�<http://mp.weixin.qq.com/wiki/index.php?title=鑾峰彇鐢ㄦ埛鍩烘湰淇℃伅>
     * Examples:
     * ```
     * api.getUser(openid);
     * api.getUser({openid: 'openid', lang: 'en'});
     * ```
     *
     * Result:
     * ```
     * {
     *  "subscribe": 1,
     *  "openid": "o6_bmjrPTlm6_2sgVt7hMZOPfL2M",
     *  "nickname": "Band",
     *  "sex": 1,
     *  "language": "zh_CN",
     *  "city": "骞垮窞",
     *  "province": "骞夸笢",
     *  "country": "涓浗",
     *  "headimgurl": "http://wx.qlogo.cn/mmopen/g3MonUZtNHkdmzicIlibx6iaFqAc56vxLSUfpb6n5WKSYVY0ChQKkiaJSgQ1dZuTOgvLLrhJbERQQ4eMsv84eavHiaiceqxibJxCfHe/0",
     *  "subscribe_time": 1382694957
     * }
     * ```
     * Param:
     * - openid {String} 鐢ㄦ埛鐨刼penid銆�
     * - language {String} 璇█
     */
    public JSONObject getUser (String openid) {
        return getUser(openid, "zh_CN");
    }

    public JSONObject getUser (String openid, String language) {

        AccessToken token = this.ensureAccessToken();
        String accessToken = token.getAccessToken();

        // https://api.weixin.qq.com/cgi-bin/user/info?access_token=ACCESS_TOKEN&openid=OPENID
        String url = this.PREFIX + "user/info?openid=" + openid
                + "&lang=" + language
                + "&access_token=" + accessToken;

        String respStr = HttpUtils.sendGetRequest(url);
        JSONObject resp = JSON.parseObject(respStr);

        return resp;
    }

    /**
     * 鎵归噺鑾峰彇鐢ㄦ埛鍩烘湰淇℃伅
     * Example:
     * ```
     * api.batchGetUsers(['openid1', 'openid2'])
     * api.batchGetUsers(['openid1', 'openid2'], 'en')
     * ```
     * Result:
     * ```
     * {
     *   "user_info_list": [{
     *     "subscribe": 1,
     *     "openid": "otvxTs4dckWG7imySrJd6jSi0CWE",
     *     "nickname": "iWithery",
     *     "sex": 1,
     *     "language": "zh_CN",
     *     "city": "Jieyang",
     *     "province": "Guangdong",
     *     "country": "China",
     *     "headimgurl": "http://wx.qlogo.cn/mmopen/xbIQx1GRqdvyqkMMhEaGOX802l1CyqMJNgUzKP8MeAeHFicRDSnZH7FY4XB7p8XHXIf6uJA2SCunTPicGKezDC4saKISzRj3nz/0",
     *     "subscribe_time": 1434093047,
     *     "unionid": "oR5GjjgEhCMJFyzaVZdrxZ2zRRF4",
     *     "remark": "",
     *     "groupid": 0
     *   }, {
     *     "subscribe": 0,
     *     "openid": "otvxTs_JZ6SEiP0imdhpi50fuSZg",
     *     "unionid": "oR5GjjjrbqBZbrnPwwmSxFukE41U",
     *   }]
     * }
     * ```
     * @param {Array} openids 鐢ㄦ埛鐨刼penid鏁扮粍銆�
     * @param {String} lang  璇█(zh_CN, zh_TW, en),榛樿绠�浣撲腑鏂�(zh_CN)
     */
    public JSONArray batchGetUsers (List<String> openids) {
        return batchGetUsers(openids, "zh_CN");
    }
    public JSONArray batchGetUsers (List<String> openids, String language) {

        AccessToken token = this.ensureAccessToken();
        String accessToken = token.getAccessToken();

        String url = this.PREFIX + "user/info/batchget?access_token=" + accessToken;

        Map<String, Object> data = new HashMap<String, Object>();
            List<Map<String, String>> user_list = new ArrayList<Map<String, String>>();
            for(String openid : openids){
                Map<String, String> openItem = new HashMap<String, String>();
                openItem.put("openid", openid);
                openItem.put("lang", language);
                user_list.add(openItem);
            }
        data.put("user_list", user_list);

        String respStr = HttpUtils.sendPostJsonRequest(url, JSON.toJSONString(data));
        JSONObject resp = JSON.parseObject(respStr);

        JSONArray userInfoList = resp.getJSONArray("user_info_list");

        return userInfoList;
    };

    /**
     * 鑾峰彇鍏虫敞鑰呭垪琛�
     * 璇︾粏缁嗚妭 http://mp.weixin.qq.com/wiki/index.php?title=鑾峰彇鍏虫敞鑰呭垪琛�
     * Examples:
     * ```
     * api.getFollowers();
     * // or
     * api.getFollowers(nextOpenid);
     * ```
     * Result:
     * ```
     * {
     *  "total":2,
     *  "count":2,
     *  "data":{
     *    "openid":["","OPENID1","OPENID2"]
     *  },
     *  "next_openid":"NEXT_OPENID"
     * }
     * ```
     * @param {String} nextOpenid 璋冪敤涓�娆′箣鍚庯紝浼犻�掑洖鏉ョ殑nextOpenid銆傜涓�娆¤幏鍙栨椂鍙笉濉�
     */
    public JSONObject getFollowers () {
        return getFollowers(null);
    }
    public JSONObject getFollowers (String nextOpenid) {

        AccessToken token = this.ensureAccessToken();
        String accessToken = token.getAccessToken();

        if(nextOpenid == null){
            nextOpenid = "";
        }

        // https://api.weixin.qq.com/cgi-bin/user/get?access_token=ACCESS_TOKEN&next_openid=NEXT_OPENID
        String url = this.PREFIX + "user/get?next_openid=" + nextOpenid + "&access_token=" + accessToken;

        String respStr = HttpUtils.sendGetRequest(url);
        JSONObject resp = JSON.parseObject(respStr);

        return resp;
    };

    /**
     * 璁剧疆鐢ㄦ埛澶囨敞鍚�
     * 璇︾粏缁嗚妭 http://mp.weixin.qq.com/wiki/index.php?title=璁剧疆鐢ㄦ埛澶囨敞鍚嶆帴鍙�
     * Examples:
     * ```
     * api.updateRemark(openid, remark);
     * ```
     * Result:
     * ```
     * {
     *  "errcode":0,
     *  "errmsg":"ok"
     * }
     * ```
     * @param {String} openid 鐢ㄦ埛鐨刼penid
     * @param {String} remark 鏂扮殑澶囨敞鍚嶏紝闀垮害蹇呴』灏忎簬30瀛楃
     */
    public boolean updateRemark (String openid, String remark) {

        AccessToken token = this.ensureAccessToken();
        String accessToken = token.getAccessToken();

        // https://api.weixin.qq.com/cgi-bin/user/info/updateremark?access_token=ACCESS_TOKEN
        String url = this.PREFIX + "user/info/updateremark?access_token=" + accessToken;

        Map<String, Object> data = new HashMap<String, Object>();
        data.put("openid", openid);
        data.put("remark", remark);

        String respStr = HttpUtils.sendPostJsonRequest(url, JSON.toJSONString(data));
        JSONObject resp = JSON.parseObject(respStr);
        int errCode = resp.getIntValue("errcode");
        if(errCode == 0){
            return true;
        }else{
            return false;
        }
    };

    /**
     * 鍒涘缓鏍囩
     * 璇︾粏缁嗚妭 https://mp.weixin.qq.com/wiki?t=resource/res_main&id=mp1421140837&token=&lang=zh_CN
     * Examples:
     * ```
     * api.createTags(name);
     * ```
     * Result:
     * ```
     * {
     *  "id":tagId,
     *  "name":tagName
     * }
     * ```
     * @param {String} name 鏍囩鍚�
     */
    public JSONObject createTags (String name){

        AccessToken token = this.ensureAccessToken();
        String accessToken = token.getAccessToken();

        // https://api.weixin.qq.com/cgi-bin/tags/create?access_token=ACCESS_TOKEN
        String url = this.PREFIX + "tags/create?access_token=" + accessToken;

        Map<String, Object> data = new HashMap<String, Object>();
            Map<String, Object> tagMap = new HashMap<String, Object>();
            tagMap.put("name", name);
        data.put("tag", tagMap);

        String respStr = HttpUtils.sendPostJsonRequest(url, JSON.toJSONString(data));
        JSONObject resp = JSON.parseObject(respStr);

        return resp;
    };

    /**
     * 鑾峰彇鍏紬鍙峰凡鍒涘缓鐨勬爣绛�
     * 璇︾粏缁嗚妭 https://mp.weixin.qq.com/wiki?t=resource/res_main&id=mp1421140837&token=&lang=zh_CN
     * Examples:
     * ```
     * api.getTags();
     * ```
     * Result:
     * ```
     *  {
     *    "tags":[{
     *        "id":1,
     *        "name":"姣忓ぉ涓�缃愬彲涔愭槦浜�",
     *        "count":0 //姝ゆ爣绛句笅绮変笣鏁�
     *  },{
     *    "id":2,
     *    "name":"鏄熸爣缁�",
     *    "count":0
     *  },{
     *    "id":127,
     *    "name":"骞夸笢",
     *    "count":5
     *  }
     *    ]
     *  }
     * ```
     */
    public JSONArray getTags (){

        AccessToken token = this.ensureAccessToken();
        String accessToken = token.getAccessToken();

        // https://api.weixin.qq.com/cgi-bin/tags/get?access_token=ACCESS_TOKEN
        String url = this.PREFIX + "tags/get?access_token=" + accessToken;

        String respStr = HttpUtils.sendGetRequest(url);
        JSONObject resp = JSON.parseObject(respStr);
        JSONArray tags = resp.getJSONArray("tags");

        return tags;
    };

    /**
     * 缂栬緫鏍囩
     * 璇︾粏缁嗚妭 https://mp.weixin.qq.com/wiki?t=resource/res_main&id=mp1421140837&token=&lang=zh_CN
     * Examples:
     * ```
     * api.updateTag(id,name);
     * ```
     * Result:
     * ```
     *  {
     *    "errcode":0,
     *    "errmsg":"ok"
     *  }
     * ```
     * @param {String} id 鏍囩id
     * @param {String} name 鏍囩鍚�
     */
    public boolean updateTag (String id, String name) {

        AccessToken token = this.ensureAccessToken();
        String accessToken = token.getAccessToken();

        // https://api.weixin.qq.com/cgi-bin/tags/update?access_token=ACCESS_TOKEN
        String url = this.PREFIX + "tags/update?access_token=" + accessToken;

        Map<String, Object> data = new HashMap<String, Object>();
            Map<String, Object> tagMap = new HashMap<String, Object>();
            tagMap.put("id", id);
            tagMap.put("name", name);
        data.put("tag", tagMap);

        String respStr = HttpUtils.sendPostJsonRequest(url, JSON.toJSONString(data));
        JSONObject resp = JSON.parseObject(respStr);
        int errCode = resp.getIntValue("errcode");
        if(errCode == 0){
            return true;
        }else{
            return false;
        }
    }

    /**
     * 鍒犻櫎鏍囩
     * 璇︾粏缁嗚妭 https://mp.weixin.qq.com/wiki?t=resource/res_main&id=mp1421140837&token=&lang=zh_CN
     * Examples:
     * ```
     * api.deleteTag(id);
     * ```
     * Result:
     * ```
     *  {
     *    "errcode":0,
     *    "errmsg":"ok"
     *  }
     * ```
     * @param tagId {String} 鏍囩id
     */
    public boolean deleteTag (String tagId){

        AccessToken token = this.ensureAccessToken();
        String accessToken = token.getAccessToken();

        // https://api.weixin.qq.com/cgi-bin/tags/delete?access_token=ACCESS_TOKEN
        String url = this.PREFIX + "tags/delete?access_token=" + accessToken;

        Map<String, Object> data = new HashMap<String, Object>();
        Map<String, Object> tagMap = new HashMap<String, Object>();
        tagMap.put("id", tagId);
        data.put("tag", tagMap);

        String respStr = HttpUtils.sendPostJsonRequest(url, JSON.toJSONString(data));
        JSONObject resp = JSON.parseObject(respStr);
        int errCode = resp.getIntValue("errcode");
        if(errCode == 0){
            return true;
        }else{
            return false;
        }
    }

    /**
     * 鑾峰彇鏍囩涓嬬矇涓濆垪琛�
     * 璇︾粏缁嗚妭 https://mp.weixin.qq.com/wiki?t=resource/res_main&id=mp1421140837&token=&lang=zh_CN
     * Examples:
     * ```
     * api.getUsersFromTag(tagId,nextOpenId);
     * ```
     * Result:
     * ```
     *  {
     *  "count":2,//杩欐鑾峰彇鐨勭矇涓濇暟閲�
     *  "data":{//绮変笣鍒楄〃
     *    "openid":[
     *       "ocYxcuAEy30bX0NXmGn4ypqx3tI0",
     *       "ocYxcuBt0mRugKZ7tGAHPnUaOW7Y"
     *     ]
     *   },
     * ```
     * @param {String} tagId 鏍囩id
     * @param {String} nextOpenId 绗竴涓媺鍙栫殑OPENID锛屼笉濉粯璁や粠澶村紑濮嬫媺鍙�
     */
    public JSONObject getUsersFromTag (String tagId){
        return getUsersFromTag(tagId, null);
    }

    public JSONObject getUsersFromTag (String tagId, String nextOpenId){

        AccessToken token = this.ensureAccessToken();
        String accessToken = token.getAccessToken();

        if(nextOpenId == null){
            nextOpenId = "";
        }

        // https://api.weixin.qq.com/cgi-bin/user/tag/get?access_token=ACCESS_TOKEN
        String url = this.PREFIX + "user/tag/get?access_token=" + accessToken;

        Map<String, Object> data = new HashMap<String, Object>();
        data.put("tagid", tagId);
        data.put("next_openid", nextOpenId);

        String respStr = HttpUtils.sendPostJsonRequest(url, JSON.toJSONString(data));
        JSONObject resp = JSON.parseObject(respStr);
        return resp;
    }

    /**
     * 鎵归噺涓虹敤鎴锋墦鏍囩
     * 璇︾粏缁嗚妭 https://mp.weixin.qq.com/wiki?t=resource/res_main&id=mp1421140837&token=&lang=zh_CN
     * Examples:
     * ```
     * api.batchTagging(openIds,tagId);
     * ```
     * Result:
     * ```
     *  {
     *    "errcode":0,
     *    "errmsg":"ok"
     *  }
     * ```
     * @param {Array} openIds openId鍒楄〃
     * @param {String} tagId 鏍囩id
     */
    public boolean batchTagging (List<String> openIds, String tagId) {

        AccessToken token = this.ensureAccessToken();
        String accessToken = token.getAccessToken();

        if(openIds == null){
            openIds = new ArrayList<String>();
        }

        // https://api.weixin.qq.com/cgi-bin/tags/members/batchtagging?access_token=ACCESS_TOKEN
        String url = this.PREFIX + "tags/members/batchtagging?access_token=" + accessToken;

        Map<String, Object> data = new HashMap<String, Object>();
        data.put("tagid", tagId);
        data.put("openid_list", openIds);

        String respStr = HttpUtils.sendPostJsonRequest(url, JSON.toJSONString(data));
        JSONObject resp = JSON.parseObject(respStr);
        int errCode = resp.getIntValue("errcode");
        if(errCode == 0){
            return true;
        }else{
            return false;
        }
    }

    /**
     * 鎵归噺涓虹敤鎴峰彇娑堟爣绛�
     * 璇︾粏缁嗚妭 https://mp.weixin.qq.com/wiki?t=resource/res_main&id=mp1421140837&token=&lang=zh_CN
     * Examples:
     * ```
     * api.batchUnTagging(openIds,tagId);
     * ```
     * Result:
     * ```
     *  {
     *    "errcode":0,
     *    "errmsg":"ok"
     *  }
     * ```
     * @param {Array} openIds openId鍒楄〃
     * @param {String} tagId 鏍囩id
     */
    public boolean batchUnTagging (List<String> openIds, String tagId) {

        AccessToken token = this.ensureAccessToken();
        String accessToken = token.getAccessToken();

        if(openIds == null){
            openIds = new ArrayList<String>();
        }

        // https://api.weixin.qq.com/cgi-bin/tags/members/batchuntagging?access_token=ACCESS_TOKEN
        String url = this.PREFIX + "tags/members/batchuntagging?access_token=" + accessToken;

        Map<String, Object> data = new HashMap<String, Object>();
        data.put("tagid", tagId);
        data.put("openid_list", openIds);

        String respStr = HttpUtils.sendPostJsonRequest(url, JSON.toJSONString(data));
        JSONObject resp = JSON.parseObject(respStr);
        int errCode = resp.getIntValue("errcode");
        if(errCode == 0){
            return true;
        }else{
            return false;
        }

    };

    /**
     * 鑾峰彇鐢ㄦ埛韬笂鐨勬爣绛惧垪琛�
     * 璇︾粏缁嗚妭 https://mp.weixin.qq.com/wiki?t=resource/res_main&id=mp1421140837&token=&lang=zh_CN
     * Examples:
     * ```
     * api.getUserTagList(openId);
     * ```
     * Result:
     * ```
     *  {
     *    "tagid_list":[//琚疆涓婄殑鏍囩鍒楄〃 134,2]
     *   }
     * ```
     */
    public JSONArray getUserTagList (String openId) {

        AccessToken token = this.ensureAccessToken();
        String accessToken = token.getAccessToken();

        // https://api.weixin.qq.com/cgi-bin/tags/getidlist?access_token=ACCESS_TOKEN
        String url = this.PREFIX + "tags/getidlist?access_token=" + accessToken;

        Map<String, Object> data = new HashMap<String, Object>();
        data.put("openid", openId);

        String respStr = HttpUtils.sendPostJsonRequest(url, JSON.toJSONString(data));
        JSONObject resp = JSON.parseObject(respStr);
        JSONArray list = resp.getJSONArray("tagid_list");
        return list;
    }



    /**
     * 娉ㄥ唽鎽囦竴鎽囪处鍙凤紝鐢宠寮�閫氬姛鑳�
     * 鎺ュ彛璇存槑:
     * 鐢宠寮�閫氭憞涓�鎽囧懆杈瑰姛鑳姐�傛垚鍔熸彁浜ょ敵璇疯姹傚悗锛屽伐浣滀汉鍛樹細鍦ㄤ笁涓伐浣滄棩鍐呭畬鎴愬鏍搞�傝嫢瀹℃牳涓嶉�氳繃锛屽彲浠ラ噸鏂版彁浜ょ敵璇疯姹傘��
     * 鑻ユ槸瀹℃牳涓紝璇疯�愬績绛夊緟宸ヤ綔浜哄憳瀹℃牳锛屽湪瀹℃牳涓姸鎬佷笉鑳藉啀鎻愪氦鐢宠璇锋眰銆�
     * 璇︽儏璇峰弬瑙侊細<http://mp.weixin.qq.com/wiki/13/025f1d471dc999928340161c631c6635.html>
     * Options:
     * ```
     * {
     *  "name": "zhang_san",
     *  "phone_number": "13512345678",
     *  "email": "weixin123@qq.com",
     *  "industry_id": "0118",
     *  "qualification_cert_urls": [
     *    "http://shp.qpic.cn/wx_shake_bus/0/1428565236d03d864b7f43db9ce34df5f720509d0e/0",
     *    "http://shp.qpic.cn/wx_shake_bus/0/1428565236d03d864b7f43db9ce34df5f720509d0e/0"
     *  ],
     *  "apply_reason": "test"
     * }
     * ```
     * Examples:
     * ```
     * api.registerShakeAccount(options);
     * ```
     * Result:
     * ```
     * {
     *   "data" : { },
     *   "errcode": 0,
     *   "errmsg": "success."
     * }
     * ```
     * @name registerShakeAccount
     * @param {Object} options 璇锋眰鍙傛暟
     */
    public JSONObject registerShakeAccount (Map<String, Object> options) {

        AccessToken token = this.ensureAccessToken();
        String accessToken = token.getAccessToken();

        String url = "https://api.weixin.qq.com/shakearound/account/register?access_token=" + accessToken;

        String respStr = HttpUtils.sendPostJsonRequest(url, JSON.toJSONString(options));
        JSONObject resp = JSON.parseObject(respStr);

        return resp;
    }

    /**
     * 鏌ヨ瀹℃牳鐘舵��
     * 鎺ュ彛璇存槑锛�
     * 鏌ヨ宸茬粡鎻愪氦鐨勫紑閫氭憞涓�鎽囧懆杈瑰姛鑳界敵璇风殑瀹℃牳鐘舵�併�傚湪鐢宠鎻愪氦鍚庯紝宸ヤ綔浜哄憳浼氬湪涓変釜宸ヤ綔鏃ュ唴瀹屾垚瀹℃牳銆�
     * 璇︽儏璇峰弬瑙侊細http://mp.weixin.qq.com/wiki/13/025f1d471dc999928340161c631c6635.html
     * Examples:
     * ```
     * api.checkShakeAccountStatus();
     * ```
     * Result:
     * ```
     * {
     *   "data" : {
     *     "apply_time": 1432026025,
     *     "audit_comment": "test",
     *     "audit_status": 1,       //瀹℃牳鐘舵�併��0锛氬鏍告湭閫氳繃銆�1锛氬鏍镐腑銆�2锛氬鏍稿凡閫氳繃锛涘鏍镐細鍦ㄤ笁涓伐浣滄棩鍐呭畬鎴�
     *     "audit_time": 0
     *   },
     *   "errcode": 0,
     *   "errmsg": "success."
     * }
     * ```
     * @name checkShakeAccountStatus
     */
    public JSONObject checkShakeAccountStatus () {

        AccessToken token = this.ensureAccessToken();
        String accessToken = token.getAccessToken();

        String url = "https://api.weixin.qq.com/shakearound/account/auditstatus?access_token=" + accessToken;

        String respStr = HttpUtils.sendGetRequest(url);
        JSONObject resp = JSON.parseObject(respStr);

        return resp;
    }

    /**
     * 璁惧绠＄悊: 鐢宠璁惧ID銆�
     * 鎺ュ彛璇存槑:
     * 鐢宠閰嶇疆璁惧鎵�闇�鐨刄UID銆丮ajor銆丮inor銆傝嫢婵�娲荤巼灏忎簬50%锛屼笉鑳芥柊澧炶澶囥�傚崟娆℃柊澧炶澶囪秴杩�500涓紝
     * 闇�璧颁汉宸ュ鏍告祦绋嬨�傚鏍搁�氳繃鍚庯紝鍙敤杩斿洖鐨勬壒娆D鐢ㄢ�滄煡璇㈣澶囧垪琛ㄢ�濇帴鍙ｆ媺鍙栨湰娆＄敵璇风殑璁惧ID銆�
     * 璇︽儏璇峰弬瑙侊細<http://mp.weixin.qq.com/wiki/15/b9e012f917e3484b7ed02771156411f3.html> * Options:
     * ```
     * {
     *   "quantity":3,
     *   "apply_reason":"娴嬭瘯",
     *   "comment":"娴嬭瘯涓撶敤",
     *   "poi_id":1234
     * }
     * ```
     * Examples:
     * ```
     * api.applyBeacons(options);
     * ```
     * Result:
     * ```
     * {
     *   "data" : { ... },
     *   "errcode": 0,
     *   "errmsg": "success."
     * }
     * ```
     * @name applyBeacons
     * @param {Object} options 璇锋眰鍙傛暟
     */
    public JSONObject applyBeacons (Map<String, Object> options) {

        AccessToken token = this.ensureAccessToken();
        String accessToken = token.getAccessToken();

        String url = "https://api.weixin.qq.com/shakearound/device/applyid?access_token=" + accessToken;

        String respStr = HttpUtils.sendPostJsonRequest(url, JSON.toJSONString(options));
        JSONObject resp = JSON.parseObject(respStr);

        return resp;
    }

    /**
     * 璁惧绠＄悊: 缂栬緫璁惧鐨勫娉ㄤ俊鎭��
     * 鎺ュ彛璇存槑:
     * 鍙敤璁惧ID鎴栧畬鏁寸殑UUID銆丮ajor銆丮inor鎸囧畾璁惧锛屼簩鑰呴�夊叾涓�銆�
     * 璇︽儏璇峰弬瑙侊細http://mp.weixin.qq.com/wiki/15/b9e012f917e3484b7ed02771156411f3.html
     * Options:
     * ```
     * {
     *  "device_identifier": {
     *    // 璁惧缂栧彿锛岃嫢濉簡UUID銆乵ajor銆乵inor锛屽垯鍙笉濉澶囩紪鍙凤紝鑻ヤ簩鑰呴兘濉紝鍒欎互璁惧缂栧彿涓轰紭鍏�
     *    "device_id": 10011,
     *    "uuid": "FDA50693-A4E2-4FB1-AFCF-C6EB07647825",
     *    "major": 1002,
     *    "minor": 1223
     *  },
     *  "comment": "test"
     * }
     * ```
     * Examples:
     * ```
     * api.updateBeacon(options);
     * ```
     * Result:
     * ```
     * {
     *   "data" : {
     *   },
     *   "errcode": 0,
     *   "errmsg": "success."
     * }
     * ```
     * @name updateBeacon
     * @param {Object} options 璇锋眰鍙傛暟
     */
    public JSONObject updateBeacon (Map<String, Object> options) {

        AccessToken token = this.ensureAccessToken();
        String accessToken = token.getAccessToken();

        String url = "https://api.weixin.qq.com/shakearound/device/update?access_token=" + accessToken;

        String respStr = HttpUtils.sendPostJsonRequest(url, JSON.toJSONString(options));
        JSONObject resp = JSON.parseObject(respStr);

        return resp;
    }

    /**
     * 璁惧绠＄悊: 閰嶇疆璁惧涓庨棬搴楃殑鍏宠仈鍏崇郴銆�
     * 鎺ュ彛璇存槑:
     * 淇敼璁惧鍏宠仈鐨勯棬搴桰D銆佽澶囩殑澶囨敞淇℃伅銆傚彲鐢ㄨ澶嘔D鎴栧畬鏁寸殑UUID銆丮ajor銆丮inor鎸囧畾璁惧锛屼簩鑰呴�夊叾涓�銆�
     * 璇︽儏璇峰弬瑙侊細http://mp.weixin.qq.com/wiki/15/b9e012f917e3484b7ed02771156411f3.html
     * Options:
     * ```
     * {
     *   "device_identifier": {
     *     "device_id": 10011,
     *     "uuid": "FDA50693-A4E2-4FB1-AFCF-C6EB07647825",
     *     "major": 1002,
     *     "minor": 1223
     *   },
     *   "poi_id": 1231
     * }
     * ```
     * Examples:
     * ```
     * api.bindBeaconLocation(options);
     * ```
     * Result:
     * ```
     * {
     *   "data" : {
     *   },
     *   "errcode": 0,
     *   "errmsg": "success."
     * }
     * ```
     * @name bindBeaconLocation
     * @param {Object} options 璇锋眰鍙傛暟
     */
    public JSONObject bindBeaconLocation (Map<String, Object> options) {

        AccessToken token = this.ensureAccessToken();
        String accessToken = token.getAccessToken();

        String url = "https://api.weixin.qq.com/shakearound/device/bindlocation?access_token=" + accessToken;

        String respStr = HttpUtils.sendPostJsonRequest(url, JSON.toJSONString(options));
        JSONObject resp = JSON.parseObject(respStr);

        return resp;
    }

    /**
     * 璁惧绠＄悊: 鏌ヨ璁惧鍒楄〃
     * 鎺ュ彛璇存槑:
     * 鏌ヨ宸叉湁鐨勮澶嘔D銆乁UID銆丮ajor銆丮inor銆佹縺娲荤姸鎬併�佸娉ㄤ俊鎭�佸叧鑱旈棬搴椼�佸叧鑱旈〉闈㈢瓑淇℃伅銆�
     * 鍙寚瀹氳澶嘔D鎴栧畬鏁寸殑UUID銆丮ajor銆丮inor鏌ヨ锛屼篃鍙壒閲忔媺鍙栬澶囦俊鎭垪琛ㄣ��
     * 璇︽儏璇峰弬瑙侊細http://mp.weixin.qq.com/wiki/15/b9e012f917e3484b7ed02771156411f3.html
     * Options:
     * 1) 鏌ヨ鎸囧畾璁惧鏃讹細
     * ```
     * {
     *  "device_identifier": [
     *    {
     *      "device_id":10011,
     *      "uuid":"FDA50693-A4E2-4FB1-AFCF-C6EB07647825",
     *      "major":1002,
     *      "minor":1223
     *    }
     *  ]
     * }
     * ```
     * 2) 闇�瑕佸垎椤垫煡璇㈡垨鑰呮寚瀹氳寖鍥村唴鐨勮澶囨椂锛�
     * ```
     * {
     *   "begin": 0,
     *   "count": 3
     * }
     * ```
     * 3) 褰撻渶瑕佹牴鎹壒娆D鏌ヨ鏃讹細
     * ```
     * {
     *   "apply_id": 1231,
     *   "begin": 0,
     *   "count": 3
     * }
     * ```
     * Examples:
     * ```
     * api.getBeacons(options);
     * ```
     * Result:
     * ```
     * {
     *   "data": {
     *     "devices": [
     *       {
     *         "comment": "",
     *         "device_id": 10097,
     *         "major": 10001,
     *         "minor": 12102,
     *         "page_ids": "15369",
     *         "status": 1, //婵�娲荤姸鎬侊紝0锛氭湭婵�娲伙紝1锛氬凡婵�娲伙紙浣嗕笉娲昏穬锛夛紝2锛氭椿璺�
     *         "poi_id": 0,
     *         "uuid": "FDA50693-A4E2-4FB1-AFCF-C6EB07647825"
     *       },
     *       {
     *         "comment": "",
     *         "device_id": 10098,
     *         "major": 10001,
     *         "minor": 12103,
     *         "page_ids": "15368",
     *         "status": 1,
     *         "poi_id": 0,
     *         "uuid": "FDA50693-A4E2-4FB1-AFCF-C6EB07647825"
     *       }
     *      ],
     *      "total_count": 151
     *    },
     *    "errcode": 0,
     *    "errmsg": "success."
     * }
     * ```
     * @name getBeacons
     * @param {Object} options 璇锋眰鍙傛暟
     */
    public JSONObject getBeacons (Map<String, Object> options) {

        AccessToken token = this.ensureAccessToken();
        String accessToken = token.getAccessToken();

        String url = "https://api.weixin.qq.com/shakearound/device/search?access_token=" + accessToken;

        String respStr = HttpUtils.sendPostJsonRequest(url, JSON.toJSONString(options));
        JSONObject resp = JSON.parseObject(respStr);

        return resp;

    };

    /**
     * 椤甸潰绠＄悊: 鏂板椤甸潰
     * 鎺ュ彛璇存槑:
     * 鏂板鎽囦竴鎽囧嚭鏉ョ殑椤甸潰淇℃伅锛屽寘鎷湪鎽囦竴鎽囬〉闈㈠嚭鐜扮殑涓绘爣棰樸�佸壇鏍囬銆佸浘鐗囧拰鐐瑰嚮杩涘幓鐨勮秴閾炬帴銆�
     * 鍏朵腑锛屽浘鐗囧繀椤讳负鐢ㄧ礌鏉愮鐞嗘帴鍙ｏ紙uploadPageIcon鍑芥暟锛変笂浼犺嚦寰俊渚ф湇鍔″櫒鍚庤繑鍥炵殑閾炬帴銆�
     * 璇︽儏璇峰弬瑙侊細http://mp.weixin.qq.com/wiki/5/6626199ea8757c752046d8e46cf13251.html
     * Page:
     * ```
     * {
     *   "title":"涓绘爣棰�",
     *   "description":"鍓爣棰�",
     *   "page_url":" https://zb.weixin.qq.com",
     *   "comment":"鏁版嵁绀轰緥",
     *   "icon_url":"http://shp.qpic.cn/wx_shake_bus/0/14288351768a23d76e7636b56440172120529e8252/120"
     *   //璋冪敤uploadPageIcon鍑芥暟鑾峰彇鍒拌URL
     * }
     * ```
     * Examples:
     * ```
     * api.createPage(page);
     * ```
     * Result:
     * ```
     * {
     *   "data" : {
     *     "page_id": 28840
     *   },
     *   "errcode": 0,
     *   "errmsg": "success."
     * }
     * ```
     * @name createPage
     * @param {Object} page 璇锋眰鍙傛暟
     */
    public JSONObject createPage (Map<String, Object> page) {

        AccessToken token = this.ensureAccessToken();
        String accessToken = token.getAccessToken();

        String url = "https://api.weixin.qq.com/shakearound/page/add?access_token=" + accessToken;

        String respStr = HttpUtils.sendPostJsonRequest(url, JSON.toJSONString(page));
        JSONObject resp = JSON.parseObject(respStr);

        return resp;

    };

    /**
     * 椤甸潰绠＄悊: 缂栬緫椤甸潰淇℃伅
     * 鎺ュ彛璇存槑:
     * 缂栬緫鎽囦竴鎽囧嚭鏉ョ殑椤甸潰淇℃伅锛屽寘鎷湪鎽囦竴鎽囬〉闈㈠嚭鐜扮殑涓绘爣棰樸�佸壇鏍囬銆佸浘鐗囧拰鐐瑰嚮杩涘幓鐨勮秴閾炬帴銆�
     * 璇︽儏璇峰弬瑙侊細http://mp.weixin.qq.com/wiki/5/6626199ea8757c752046d8e46cf13251.html
     * Page:
     * ```
     * {
     *   "page_id":12306,
     *   "title":"涓绘爣棰�",
     *   "description":"鍓爣棰�",
     *   "page_url":" https://zb.weixin.qq.com",
     *   "comment":"鏁版嵁绀轰緥",
     *   "icon_url":"http://shp.qpic.cn/wx_shake_bus/0/14288351768a23d76e7636b56440172120529e8252/120"
     *   //璋冪敤uploadPageIcon鍑芥暟鑾峰彇鍒拌URL
     * }
     * ```
     * Examples:
     * ```
     * api.updatePage(page);
     * ```
     * Result:
     * ```
     * {
     *   "data" : {
     *     "page_id": 28840
     *   },
     *   "errcode": 0,
     *   "errmsg": "success."
     * }
     * ```
     * @name updatePage
     * @param {Object} page 璇锋眰鍙傛暟
     */
    public JSONObject updatePage (Map<String, Object> page) {

        AccessToken token = this.ensureAccessToken();
        String accessToken = token.getAccessToken();

        String url = "https://api.weixin.qq.com/shakearound/page/update?access_token=" + accessToken;

        String respStr = HttpUtils.sendPostJsonRequest(url, JSON.toJSONString(page));
        JSONObject resp = JSON.parseObject(respStr);

        return resp;
    };

    /**
     * 椤甸潰绠＄悊: 鍒犻櫎椤甸潰
     * 鎺ュ彛璇存槑:
     * 鍒犻櫎宸叉湁鐨勯〉闈紝鍖呮嫭鍦ㄦ憞涓�鎽囬〉闈㈠嚭鐜扮殑涓绘爣棰樸�佸壇鏍囬銆佸浘鐗囧拰鐐瑰嚮杩涘幓鐨勮秴閾炬帴銆�
     * 鍙湁椤甸潰涓庤澶囨病鏈夊叧鑱斿叧绯绘椂锛屾墠鍙鍒犻櫎銆�
     * 璇︽儏璇峰弬瑙侊細http://mp.weixin.qq.com/wiki/5/6626199ea8757c752046d8e46cf13251.html
     * Page_ids:
     * ```
     * {
     *   "page_ids":[12345,23456,34567]
     * }
     * ```
     * Examples:
     * ```
     * api.deletePages(options);
     * ```
     * Result:
     * ```
     * {
     *   "data" : {
     *   },
     *   "errcode": 0,
     *   "errmsg": "success."
     * }
     * ```
     * @name deletePages
     * @param {Object} pageIds 璇锋眰鍙傛暟
     */
    public JSONObject deletePages (List<Long> pageIds) {

        AccessToken token = this.ensureAccessToken();
        String accessToken = token.getAccessToken();

        String url = "https://api.weixin.qq.com/shakearound/page/delete?access_token=" + accessToken;

        Map<String, Object> data = new HashMap<String, Object>();
        data.put("page_ids", pageIds);

        String respStr = HttpUtils.sendPostJsonRequest(url, JSON.toJSONString(data));
        JSONObject resp = JSON.parseObject(respStr);

        return resp;
    };

    /**
     * 椤甸潰绠＄悊: 鏌ヨ椤甸潰鍒楄〃
     * 鎺ュ彛璇存槑:
     * 鏌ヨ宸叉湁鐨勯〉闈紝鍖呮嫭鍦ㄦ憞涓�鎽囬〉闈㈠嚭鐜扮殑涓绘爣棰樸�佸壇鏍囬銆佸浘鐗囧拰鐐瑰嚮杩涘幓鐨勮秴閾炬帴銆傛彁渚涗袱绉嶆煡璇㈡柟寮忥紝鍙寚瀹氶〉闈D鏌ヨ锛屼篃鍙壒閲忔媺鍙栭〉闈㈠垪琛ㄣ��
     * 璇︽儏璇峰弬瑙侊細http://mp.weixin.qq.com/wiki/5/6626199ea8757c752046d8e46cf13251.html
     * Options:
     * 1) 闇�瑕佹煡璇㈡寚瀹氶〉闈㈡椂锛�
     * ```
     * {
     *   "page_ids":[12345, 23456, 34567]
     * }
     * ```
     * 2) 闇�瑕佸垎椤垫煡璇㈡垨鑰呮寚瀹氳寖鍥村唴鐨勯〉闈㈡椂锛�
     * ```
     * {
     *   "begin": 0,
     *   "count": 3
     * }
     * ``` * Examples:
     * ```
     * api.getBeacons(options);
     * ```
     * Result:
     * ```
     * {
     *   "data": {
     *     "pages": [
     *        {
     *          "comment": "just for test",
     *          "description": "test",
     *          "icon_url": "https://www.baidu.com/img/bd_logo1",
     *          "page_id": 28840,
     *          "page_url": "http://xw.qq.com/testapi1",
     *          "title": "娴嬭瘯1"
     *        },
     *        {
     *          "comment": "just for test",
     *          "description": "test",
     *          "icon_url": "https://www.baidu.com/img/bd_logo1",
     *          "page_id": 28842,
     *          "page_url": "http://xw.qq.com/testapi2",
     *          "title": "娴嬭瘯2"
     *        }
     *      ],
     *      "total_count": 2
     *    },
     *    "errcode": 0,
     *    "errmsg": "success."
     * }
     * ```
     * @name getPages
     * @param {Object} options 璇锋眰鍙傛暟
     */
    public JSONObject getPages (Map<String, Object> options) {

        AccessToken token = this.ensureAccessToken();
        String accessToken = token.getAccessToken();

        String url = "https://api.weixin.qq.com/shakearound/page/search?access_token=" + accessToken;

        String respStr = HttpUtils.sendPostJsonRequest(url, JSON.toJSONString(options));
        JSONObject resp = JSON.parseObject(respStr);

        return resp;
    };

    /**
     * 涓婁紶鍥剧墖绱犳潗
     * 鎺ュ彛璇存槑锛�
     * 涓婁紶鍦ㄦ憞涓�鎽囬〉闈㈠睍绀虹殑鍥剧墖绱犳潗锛岀礌鏉愪繚瀛樺湪寰俊渚ф湇鍔″櫒涓娿��
     * 鏍煎紡闄愬畾涓猴細jpg,jpeg,png,gif锛屽浘鐗囧ぇ灏忓缓璁�120px*120 px锛岄檺鍒朵笉瓒呰繃200 px *200 px锛屽浘鐗囬渶涓烘鏂瑰舰銆�
     * 璇︽儏璇峰弬瑙侊細http://mp.weixin.qq.com/wiki/5/e997428269ff189d8f9a4b9e177be2d9.html
     * Examples:
     * ```
     * api.uploadPageIcon('filepath');
     * ```
     * Result:
     * ```
     * {
     *   "data" : {
     *     "pic_url": "http://shp.qpic.cn/wechat_shakearound_pic/0/1428377032e9dd2797018cad79186e03e8c5aec8dc/120"
     *   },
     *   "errcode": 0,
     *   "errmsg": "success."
     * }
     * ```
     * @name uploadPageIcon
     * @param {String} filepath 鏂囦欢璺緞
     */
    public JSONObject uploadPageIcon (String filepath) {

        AccessToken token = this.ensureAccessToken();
        String accessToken = token.getAccessToken();

        String url = "https://api.weixin.qq.com/shakearound/material/add?access_token=" + accessToken;

        File file = new File(filepath);

        Map<String, Object> data = new HashMap<String, Object>();
        data.put("media", file);

        String respStr = HttpUtils.sendPostFormDataRequest(url, data);
        JSONObject resp = JSON.parseObject(respStr);

        return resp;
    };

    /**
     * 閰嶇疆璁惧涓庨〉闈㈢殑鍏宠仈鍏崇郴
     * 鎺ュ彛璇存槑:
     * 閰嶇疆璁惧涓庨〉闈㈢殑鍏宠仈鍏崇郴銆傛敮鎸佸缓绔嬫垨瑙ｉ櫎鍏宠仈鍏崇郴锛屼篃鏀寔鏂板椤甸潰鎴栬鐩栭〉闈㈢瓑鎿嶄綔銆�
     * 閰嶇疆瀹屾垚鍚庯紝鍦ㄦ璁惧鐨勪俊鍙疯寖鍥村唴锛屽嵆鍙憞鍑哄叧鑱旂殑椤甸潰淇℃伅銆傝嫢璁惧閰嶇疆澶氫釜椤甸潰锛屽垯闅忔満鍑虹幇椤甸潰淇℃伅銆�
     * 璇︽儏璇峰弬瑙侊細http://mp.weixin.qq.com/wiki/6/c449687e71510db19564f2d2d526b6ea.html
     * Options:
     * ```
     * {
     *  "device_identifier": {
     *    // 璁惧缂栧彿锛岃嫢濉簡UUID銆乵ajor銆乵inor锛屽垯鍙笉濉澶囩紪鍙凤紝鑻ヤ簩鑰呴兘濉紝鍒欎互璁惧缂栧彿涓轰紭鍏�
     *    "device_id":10011,
     *    "uuid":"FDA50693-A4E2-4FB1-AFCF-C6EB07647825",
     *    "major":1002,
     *    "minor":1223
     *  },
     *  "page_ids":[12345, 23456, 334567]
     * }
     * ```
     * Examples:
     * ```
     * api.bindBeaconWithPages(options);
     * ```
     * Result:
     * ```
     * {
     *   "data" : {
     *   },
     *   "errcode": 0,
     *   "errmsg": "success."
     * }
     * ```
     * @name bindBeaconWithPages
     * @param {Object} options 璇锋眰鍙傛暟
     */
    public JSONObject bindBeaconWithPages (Map<String, Object> options) {

        AccessToken token = this.ensureAccessToken();
        String accessToken = token.getAccessToken();

        String url = "https://api.weixin.qq.com/shakearound/device/bindpage?access_token=" + accessToken;

        String respStr = HttpUtils.sendPostJsonRequest(url, JSON.toJSONString(options));
        JSONObject resp = JSON.parseObject(respStr);

        return resp;
    }

    /**
     * 鏌ヨ璁惧涓庨〉闈㈢殑鍏宠仈鍏崇郴
     * 鎺ュ彛璇存槑:
     * 鏌ヨ璁惧涓庨〉闈㈢殑鍏宠仈鍏崇郴銆傛彁渚涗袱绉嶆煡璇㈡柟寮忥紝鍙寚瀹氶〉闈D鍒嗛〉鏌ヨ璇ラ〉闈㈡墍鍏宠仈鐨勬墍鏈夌殑璁惧淇℃伅锛�
     * 涔熷彲鏍规嵁璁惧ID鎴栧畬鏁寸殑UUID銆丮ajor銆丮inor鏌ヨ璇ヨ澶囨墍鍏宠仈鐨勬墍鏈夐〉闈俊鎭��
     * 璇︽儏璇峰弬瑙侊細http://mp.weixin.qq.com/wiki/6/c449687e71510db19564f2d2d526b6ea.html
     * Options:
     * 1) 褰撴煡璇㈡寚瀹氳澶囨墍鍏宠仈鐨勯〉闈㈡椂锛�
     * ```
     * {
     *  "type": 1,
     *  "device_identifier": {
     *    // 璁惧缂栧彿锛岃嫢濉簡UUID銆乵ajor銆乵inor锛屽垯鍙笉濉澶囩紪鍙凤紝鑻ヤ簩鑰呴兘濉紝鍒欎互璁惧缂栧彿涓轰紭鍏�
     *    "device_id":10011,
     *    "uuid":"FDA50693-A4E2-4FB1-AFCF-C6EB07647825",
     *    "major":1002,
     *    "minor":1223
     *  }
     * }
     * ```
     * 2) 褰撴煡璇㈤〉闈㈡墍鍏宠仈鐨勮澶囨椂锛�
     * {
     *  "type": 2,
     *  "page_id": 11101,
     *  "begin": 0,
     *  "count": 3
     * }
     * Examples:
     * ```
     * api.searchBeaconPageRelation(options);
     * ```
     * Result:
     * ```
     * {
     *  "data": {
     *    "relations": [
     *      {
     *        "device_id": 797994,
     *        "major": 10001,
     *        "minor": 10023,
     *        "page_id": 50054,
     *        "uuid": "FDA50693-A4E2-4FB1-AFCF-C6EB07647825"
     *      },
     *      {
     *        "device_id": 797994,
     *        "major": 10001,
     *        "minor": 10023,
     *        "page_id": 50055,
     *        "uuid": "FDA50693-A4E2-4FB1-AFCF-C6EB07647825"
     *      }
     *    ],
     *    "total_count": 2
     *  },
     *  "errcode": 0,
     *  "errmsg": "success."
     * }
     * ```
     * @name searchBeaconPageRelation
     * @param {Object} options 璇锋眰鍙傛暟
     */
    public JSONObject searchBeaconPageRelation (Map<String, Object> options) {

        AccessToken token = this.ensureAccessToken();
        String accessToken = token.getAccessToken();

        String url = "https://api.weixin.qq.com/shakearound/relation/search?access_token=" + accessToken;

        String respStr = HttpUtils.sendPostJsonRequest(url, JSON.toJSONString(options));
        JSONObject resp = JSON.parseObject(respStr);

        return resp;

    };

    /**
     * 鑾峰彇鎽囧懆杈圭殑璁惧鍙婄敤鎴蜂俊鎭�
     * 鎺ュ彛璇存槑:
     * 鑾峰彇璁惧淇℃伅锛屽寘鎷琔UID銆乵ajor銆乵inor锛屼互鍙婅窛绂汇�乷penID绛変俊鎭��
     * 璇︽儏璇峰弬瑙侊細http://mp.weixin.qq.com/wiki/3/34904a5db3d0ec7bb5306335b8da1faf.html
     * Ticket:
     * ```
     * {
     *   "ticket":鈥�6ab3d8465166598a5f4e8c1b44f44645鈥�
     * }
     * ```
     * Examples:
     * ```
     * api.getShakeInfo(ticket);
     * ```
     * Result:
     * ```
     * {
     *   "data" : {
     *   },
     *   "errcode": 0,
     *   "errmsg": "success."
     * }
     * ```
     * @name getShakeInfo
     * @param {String} ticket 鎽囧懆杈逛笟鍔＄殑ticket锛屽彲鍦ㄦ憞鍒扮殑URL涓緱鍒帮紝ticket鐢熸晥鏃堕棿涓�30鍒嗛挓
     */
    public JSONObject getShakeInfo (String ticket) {

        AccessToken token = this.ensureAccessToken();
        String accessToken = token.getAccessToken();

        String url = "https://api.weixin.qq.com/shakearound/user/getshakeinfo?access_token=" + accessToken;

        Map<String, Object> data = new HashMap<String, Object>();
        data.put("ticket", ticket);

        String respStr = HttpUtils.sendPostJsonRequest(url, JSON.toJSONString(data));
        JSONObject resp = JSON.parseObject(respStr);

        return resp;
    };

    /**
     * 鏁版嵁缁熻: 浠ヨ澶囦负缁村害鐨勬暟鎹粺璁℃帴鍙�
     * 鎺ュ彛璇存槑:
     * 鏌ヨ鍗曚釜璁惧杩涜鎽囧懆杈规搷浣滅殑浜烘暟銆佹鏁帮紝鐐瑰嚮鎽囧懆杈规秷鎭殑浜烘暟銆佹鏁帮紱鏌ヨ鐨勬渶闀挎椂闂磋法搴︿负30澶┿��
     * 璇︽儏璇峰弬瑙侊細http://mp.weixin.qq.com/wiki/0/8a24bcacad40fe7ee98d1573cb8a6764.html
     * Options:
     * ```
     * {
     *   "device_identifier": {
     *     "device_id":10011,  //璁惧缂栧彿锛岃嫢濉簡UUID銆乵ajor銆乵inor锛屽垯鍙笉濉澶囩紪鍙凤紝鑻ヤ簩鑰呴兘濉紝鍒欎互璁惧缂栧彿涓轰紭鍏�
     *     "uuid":"FDA50693-A4E2-4FB1-AFCF-C6EB07647825", //UUID銆乵ajor銆乵inor锛屼笁涓俊鎭渶濉啓瀹屾暣锛岃嫢濉簡璁惧缂栧彿锛屽垯鍙笉濉淇℃伅銆�
     *     "major":1002,
     *     "minor":1223
     *   },
     *   "begin_date": 12313123311,
     *   "end_date": 123123131231
     * }
     * ```
     * Examples:
     * ```
     * api.getDeviceStatistics(options);
     * ```
     * Result:
     * ```
     * {
     *   "data" : {
     *     {
     *       "click_pv": 0,
     *       "click_uv": 0,
     *       "ftime": 1425052800,
     *       "shake_pv": 0,
     *       "shake_uv": 0
     *     },
     *     {
     *       "click_pv": 0,
     *       "click_uv": 0,
     *       "ftime": 1425139200,
     *       "shake_pv": 0,
     *       "shake_uv": 0
     *     }
     *   },
     *   "errcode": 0,
     *   "errmsg": "success."
     * }
     * ```
     * @name getDeviceStatistics
     * @param {Object} options 璇锋眰鍙傛暟
     */
    public JSONObject getDeviceStatistics (Map<String, Object> options) {

        AccessToken token = this.ensureAccessToken();
        String accessToken = token.getAccessToken();

        String url = "https://api.weixin.qq.com/shakearound/statistics/device?access_token=" + accessToken;

        String respStr = HttpUtils.sendPostJsonRequest(url, JSON.toJSONString(options));
        JSONObject resp = JSON.parseObject(respStr);

        return resp;
    };

    /**
     * 鏁版嵁缁熻: 浠ラ〉闈负缁村害鐨勬暟鎹粺璁℃帴鍙�
     * 鎺ュ彛璇存槑:
     * 鏌ヨ鍗曚釜椤甸潰閫氳繃鎽囧懆杈规憞鍑烘潵鐨勪汉鏁般�佹鏁帮紝鐐瑰嚮鎽囧懆杈归〉闈㈢殑浜烘暟銆佹鏁帮紱鏌ヨ鐨勬渶闀挎椂闂磋法搴︿负30澶┿��
     * 璇︽儏璇峰弬瑙侊細http://mp.weixin.qq.com/wiki/0/8a24bcacad40fe7ee98d1573cb8a6764.html
     * Options:
     * ```
     * {
     *   "page_id": 12345,
     *   "begin_date": 12313123311,
     *   "end_date": 123123131231
     * }
     * ```
     * Examples:
     * ```
     * api.getPageStatistics(options);
     * ```
     * Result:
     * ```
     * {
     *   "data" : {
     *     {
     *       "click_pv": 0,
     *       "click_uv": 0,
     *       "ftime": 1425052800,
     *       "shake_pv": 0,
     *       "shake_uv": 0
     *     },
     *     {
     *       "click_pv": 0,
     *       "click_uv": 0,
     *       "ftime": 1425139200,
     *       "shake_pv": 0,
     *       "shake_uv": 0
     *     }
     *   },
     *   "errcode": 0,
     *   "errmsg": "success."
     * }
     * ```
     * @name getPageStatistics
     * @param {Object} options 璇锋眰鍙傛暟
     */
    public JSONObject getPageStatistics (Map<String, Object> options) {

        AccessToken token = this.ensureAccessToken();
        String accessToken = token.getAccessToken();

        String url = "https://api.weixin.qq.com/shakearound/statistics/page?access_token=" + accessToken;

        String respStr = HttpUtils.sendPostJsonRequest(url, JSON.toJSONString(options));
        JSONObject resp = JSON.parseObject(respStr);

        return resp;

    };


    /**
     * 澧炲姞閭垂妯℃澘
     * 璇︾粏璇风湅锛�<http://mp.weixin.qq.com/wiki/index.php?title=寰俊灏忓簵鎺ュ彛>
     * Examples:
     * ```
     * api.addExpress(express);
     * ```
     * Express:
     * ```
     * {
     *  "delivery_template": {
     *    "Name": "testexpress",
     *    "Assumer": 0,
     *    "Valuation": 0,
     *    "TopFee": [
     *      {
     *        "Type": 10000027,
     *        "Normal": {
     *          "StartStandards": 1,
     *          "StartFees": 2,
     *          "AddStandards": 3,
     *          "AddFees": 1
     *        },
     *        "Custom": [
     *          {
     *            "StartStandards": 1,
     *            "StartFees": 100,
     *            "AddStandards": 1,
     *            "AddFees": 3,
     *            "DestCountry": "涓浗",
     *            "DestProvince": "骞夸笢鐪�",
     *            "DestCity": "骞垮窞甯�"
     *          }
     *        ]
     *      },
     *      {
     *        "Type": 10000028,
     *        "Normal": {
     *          "StartStandards": 1,
     *          "StartFees": 3,
     *          "AddStandards": 3,
     *          "AddFees": 2
     *        },
     *        "Custom": [
     *          {
     *            "StartStandards": 1,
     *            "StartFees": 10,
     *            "AddStandards": 1,
     *            "AddFees": 30,
     *            "DestCountry": "涓浗",
     *            "DestProvince": "骞夸笢鐪�",
     *            "DestCity": "骞垮窞甯�"
     *          }
     *        ]
     *      },
     *      {
     *        "Type": 10000029,
     *        "Normal": {
     *          "StartStandards": 1,
     *          "StartFees": 4,
     *          "AddStandards": 3,
     *          "AddFees": 3
     *        },
     *        "Custom": [
     *          {
     *            "StartStandards": 1,
     *            "StartFees": 8,
     *            "AddStandards": 2,
     *            "AddFees": 11,
     *            "DestCountry": "涓浗",
     *            "DestProvince": "骞夸笢鐪�",
     *            "DestCity": "骞垮窞甯�"
     *          }
     *        ]
     *      }
     *    ]
     *  }
     * }
     * ```
     * Result:
     * ```
     * {
     *  "errcode": 0,
     *  "errmsg": "success"
     *  "template_id": 123456
     * }
     * ```
     * @param {Object} express 閭垂妯＄増
     */
    public JSONObject addExpressTemplate (Map<String, Object> express) {

        AccessToken token = this.ensureAccessToken();
        String accessToken = token.getAccessToken();

        String url = this.MERCHANT_PREFIX + "express/add?access_token=" + accessToken;

        String respStr = HttpUtils.sendPostJsonRequest(url, JSON.toJSONString(express));
        JSONObject resp = JSON.parseObject(respStr);

        return resp;
    }

    /**
     * 淇敼閭垂妯℃澘
     * 璇︾粏璇风湅锛�<http://mp.weixin.qq.com/wiki/index.php?title=寰俊灏忓簵鎺ュ彛>
     * Examples:
     * ```
     * api.deleteExpressTemplate(templateId);
     * ```
     * Result:
     * ```
     * {
     *  "errcode": 0,
     *  "errmsg": "success"
     * }
     * ```
     * @param {Number} templateId 閭垂妯＄増ID
     */
    public boolean deleteExpressTemplate (Long templateId) {

        AccessToken token = this.ensureAccessToken();
        String accessToken = token.getAccessToken();

        String url = this.MERCHANT_PREFIX + "express/del?access_token=" + accessToken;

        Map<String, Object> data = new HashMap<String, Object>();
        data.put("template_id", templateId);

        String respStr = HttpUtils.sendPostJsonRequest(url, JSON.toJSONString(data));
        JSONObject resp = JSON.parseObject(respStr);
        int errCode = resp.getIntValue("errcode");
        if(errCode == 0){
            return true;
        }else {
            return false;
        }
    };

    /**
     * 淇敼閭垂妯℃澘
     * 璇︾粏璇风湅锛�<http://mp.weixin.qq.com/wiki/index.php?title=寰俊灏忓簵鎺ュ彛>
     * Examples:
     * ```
     * api.updateExpressTemplate(template);
     * ```
     * Express:
     * ```
     * {
     *  "template_id": 123456,
     *  "delivery_template": ...
     * }
     * ```
     * Result:
     * ```
     * {
     *  "errcode": 0,
     *  "errmsg": "success"
     * }
     * ```
     * @param {Object} template 閭垂妯＄増
     */
    public boolean updateExpressTemplate (Map<String, Object> template) {

        AccessToken token = this.ensureAccessToken();
        String accessToken = token.getAccessToken();

        String url = this.MERCHANT_PREFIX + "express/del?access_token=" + accessToken;

        String respStr = HttpUtils.sendPostJsonRequest(url, JSON.toJSONString(template));
        JSONObject resp = JSON.parseObject(respStr);
        int errCode = resp.getIntValue("errcode");
        if(errCode == 0){
            return true;
        }else {
            return false;
        }

    };

    /**
     * 鑾峰彇鎸囧畾ID鐨勯偖璐规ā鏉�
     * 璇︾粏璇风湅锛�<http://mp.weixin.qq.com/wiki/index.php?title=寰俊灏忓簵鎺ュ彛>
     * Examples:
     * ```
     * api.getExpressTemplateById(templateId);
     * ```
     * Result:
     * ```
     * {
     *  "errcode": 0,
     *  "errmsg": "success",
     *  "template_info": {
     *    "Id": 103312916,
     *    "Name": "testexpress",
     *    "Assumer": 0,
     *    "Valuation": 0,
     *    "TopFee": [
     *      {
     *        "Type": 10000027,
     *        "Normal": {
     *          "StartStandards": 1,
     *          "StartFees": 2,
     *          "AddStandards": 3,
     *          "AddFees": 1
     *        },
     *        "Custom": [
     *          {
     *            "StartStandards": 1,
     *            "StartFees": 1000,
     *            "AddStandards": 1,
     *            "AddFees": 3,
     *            "DestCountry": "涓浗",
     *            "DestProvince": "骞夸笢鐪�",
     *            "DestCity": "骞垮窞甯�"
     *          }
     *        ]
     *      },
     *      ...
     *    ]
     *  }
     * }
     * ```
     * @param {Number} templateId 閭垂妯＄増Id
     */
    public JSONObject getExpressTemplateById (Long templateId) {

        AccessToken token = this.ensureAccessToken();
        String accessToken = token.getAccessToken();

        String url = this.MERCHANT_PREFIX + "express/getbyid?access_token=" + accessToken;

        Map<String, Object> data = new HashMap<String, Object>();
        data.put("template_id", templateId);

        String respStr = HttpUtils.sendPostJsonRequest(url, JSON.toJSONString(data));
        JSONObject resp = JSON.parseObject(respStr);
        return resp;
    };

    /**
     * 鑾峰彇鎵�鏈夐偖璐规ā鏉跨殑鏈皝瑁呯増鏈�
     * 璇︾粏璇风湅锛�<http://mp.weixin.qq.com/wiki/index.php?title=寰俊灏忓簵鎺ュ彛>
     * Examples:
     * ```
     * api.getAllExpressTemplates();
     * ```
     * Result:
     * ```
     * {
     *  "errcode": 0,
     *  "errmsg": "success",
     *  "templates_info": [
     *    {
     *      "Id": 103312916,
     *      "Name": "testexpress1",
     *      "Assumer": 0,
     *      "Valuation": 0,
     *      "TopFee": [...],
     *    },
     *    {
     *      "Id": 103312917,
     *      "Name": "testexpress2",
     *      "Assumer": 0,
     *      "Valuation": 2,
     *      "TopFee": [...],
     *    },
     *    {
     *      "Id": 103312918,
     *      "Name": "testexpress3",
     *      "Assumer": 0,
     *      "Valuation": 1,
     *      "TopFee": [...],
     *    }
     *  ]
     * }
     * ```
     */
    public JSONObject getAllExpressTemplates () {

        AccessToken token = this.ensureAccessToken();
        String accessToken = token.getAccessToken();

        String url = this.MERCHANT_PREFIX + "express/getall?access_token=" + accessToken;

        String respStr = HttpUtils.sendGetRequest(url);
        JSONObject resp = JSON.parseObject(respStr);
        return resp;
    }


    /**
     * 澧炲姞鍟嗗搧
     * 璇︾粏璇风湅锛�<http://mp.weixin.qq.com/wiki/index.php?title=寰俊灏忓簵鎺ュ彛>
     * Examples:
     * ```
     * api.createGoods(goods);
     * ```
     * Goods:
     * ```
     * {
     *  "product_base":{
     *    "category_id":[
     *      "537074298"
     *    ],
     *    "property":[
     *      {"id":"1075741879","vid":"1079749967"},
     *      {"id":"1075754127","vid":"1079795198"},
     *      {"id":"1075777334","vid":"1079837440"}
     *    ],
     *    "name":"testaddproduct",
     *    "sku_info":[
     *      {
     *        "id":"1075741873",
     *        "vid":["1079742386","1079742363"]
     *      }
     *    ],
     *    "main_img": "http://mmbiz.qpic.cn/mmbiz/4whpV1VZl2iccsvYbHvnphkyGtnvjD3ulEKogfsiaua49pvLfUS8Ym0GSYjViaLic0FD3vN0V8PILcibEGb2fPfEOmw/0",
     *    "img":[
     *      "http://mmbiz.qpic.cn/mmbiz/4whpV1VZl2iccsvYbHvnphkyGtnvjD3ulEKogfsiaua49pvLfUS8Ym0GSYjViaLic0FD3vN0V8PILcibEGb2fPfEOmw/0"
     *    ],
     *    "detail":[
     *      {"text":"testfirst"},
     *      {"img": 4whpV1VZl2iccsvYbHvnphkyGtnvjD3ul1UcLcwxrFdwTKYhH9Q5YZoCfX4Ncx655ZK6ibnlibCCErbKQtReySaVA/0"},
     *      {"text":"testagain"}
     *    ],
     *    "buy_limit":10
     *  },
     *  "sku_list":[
     *    {
     *      "sku_id":"1075741873:1079742386",
     *      "price":30,
     *      "icon_url": "http://mmbiz.qpic.cn/mmbiz/4whpV1VZl28bJj62XgfHPibY3ORKicN1oJ4CcoIr4BMbfA8LqyyjzOZzqrOGz3f5K Wq1QGP3fo6TOTSYD3TBQjuw/0",
     *      "product_code":"testing",
     *      "ori_price":9000000,
     *      "quantity":800
     *    },
     *    {
     *      "sku_id":"1075741873:1079742363",
     *      "price":30,
     *      "icon_url": "http://mmbiz.qpic.cn/mmbiz/4whpV1VZl28bJj62XgfHPibY3ORKicN1oJ4CcoIr4BMbfA8LqyyjzOZzqrOGz3f5K Wq1QGP3fo6TOTSYD3TBQjuw/0",
     *      "product_code":"testingtesting",
     *      "ori_price":9000000,
     *      "quantity":800
     *    }
     *  ],
     *  "attrext":{
     *    "location":{
     *      "country":"涓浗",
     *      "province":"骞夸笢鐪�",
     *      "city":"骞垮窞甯�",
     *      "address":"T.I.T鍒涙剰鍥�"
     *    },
     *    "isPostFree":0,
     *    "isHasReceipt":1,
     *    "isUnderGuaranty":0,
     *    "isSupportReplace":0
     *  },
     *  "delivery_info":{
     *    "delivery_type":0,
     *    "template_id":0,
     *    "express":[
     *      {"id":10000027,"price":100},
     *      {"id":10000028,"price":100},
     *      {"id":10000029,"price":100}
     *    ]
     *  }
     * }
     * ```
     * Result:
     * ```
     * {
     *  "errcode": 0,
     *  "errmsg": "success",
     *  "product_id": "pDF3iYwktviE3BzU3BKiSWWi9Nkw"
     * }
     * ```
     * @param {Object} goods 鍟嗗搧淇℃伅
     */
    public JSONObject createGoods (Map<String, Object> goods) {

        AccessToken token = this.ensureAccessToken();
        String accessToken = token.getAccessToken();

        String url = this.MERCHANT_PREFIX + "create?access_token=" + accessToken;

        String respStr = HttpUtils.sendPostJsonRequest(url, JSON.toJSONString(goods));
        JSONObject resp = JSON.parseObject(respStr);
        return resp;
    };

    /**
     * 鍒犻櫎鍟嗗搧
     * 璇︾粏璇风湅锛�<http://mp.weixin.qq.com/wiki/index.php?title=寰俊灏忓簵鎺ュ彛>
     * Examples:
     * ```
     * api.deleteGoods(productId);
     * ```
     * Result:
     * ```
     * {
     *  "errcode": 0,
     *  "errmsg": "success",
     * }
     * ```
     * @param {String} productId 鍟嗗搧Id
     */
    public boolean deleteGoods (String productId) {

        AccessToken token = this.ensureAccessToken();
        String accessToken = token.getAccessToken();

        String url = this.MERCHANT_PREFIX + "del?access_token=" + accessToken;

        Map<String, Object> data = new HashMap<String, Object>();
        data.put("product_id", productId);

        String respStr = HttpUtils.sendPostJsonRequest(url, JSON.toJSONString(data));
        JSONObject resp = JSON.parseObject(respStr);
        int errCode = resp.getIntValue("errcode");
        if(errCode == 0){
            return true;
        }else{
            return false;
        }
    }

    /**
     * 淇敼鍟嗗搧
     * 璇︾粏璇风湅锛�<http://mp.weixin.qq.com/wiki/index.php?title=寰俊灏忓簵鎺ュ彛>
     * Examples:
     * ```
     * api.updateGoods(goods);
     * ```
     * Goods:
     * ```
     * {
     *  "product_id":"pDF3iY6Kr_BV_CXaiYysoGqJhppQ",
     *  "product_base":{
     *    "category_id":[
     *      "537074298"
     *    ],
     *    "property":[
     *      {"id":"1075741879","vid":"1079749967"},
     *      {"id":"1075754127","vid":"1079795198"},
     *      {"id":"1075777334","vid":"1079837440"}
     *    ],
     *    "name":"testaddproduct",
     *    "sku_info":[
     *      {
     *        "id":"1075741873",
     *        "vid":["1079742386","1079742363"]
     *      }
     *    ],
     *    "main_img": "http://mmbiz.qpic.cn/mmbiz/4whpV1VZl2iccsvYbHvnphkyGtnvjD3ulEKogfsiaua49pvLfUS8Ym0GSYjViaLic0FD3vN0V8PILcibEGb2fPfEOmw/0",
     *    "img":[
     *      "http://mmbiz.qpic.cn/mmbiz/4whpV1VZl2iccsvYbHvnphkyGtnvjD3ulEKogfsiaua49pvLfUS8Ym0GSYjViaLic0FD3vN0V8PILcibEGb2fPfEOmw/0"
     *    ],
     *    "detail":[
     *      {"text":"testfirst"},
     *      {"img": 4whpV1VZl2iccsvYbHvnphkyGtnvjD3ul1UcLcwxrFdwTKYhH9Q5YZoCfX4Ncx655ZK6ibnlibCCErbKQtReySaVA/0"},
     *      {"text":"testagain"}
     *    ],
     *    "buy_limit":10
     *  },
     *  "sku_list":[
     *    {
     *      "sku_id":"1075741873:1079742386",
     *      "price":30,
     *      "icon_url": "http://mmbiz.qpic.cn/mmbiz/4whpV1VZl28bJj62XgfHPibY3ORKicN1oJ4CcoIr4BMbfA8LqyyjzOZzqrOGz3f5K Wq1QGP3fo6TOTSYD3TBQjuw/0",
     *      "product_code":"testing",
     *      "ori_price":9000000,
     *      "quantity":800
     *    },
     *    {
     *      "sku_id":"1075741873:1079742363",
     *      "price":30,
     *      "icon_url": "http://mmbiz.qpic.cn/mmbiz/4whpV1VZl28bJj62XgfHPibY3ORKicN1oJ4CcoIr4BMbfA8LqyyjzOZzqrOGz3f5K Wq1QGP3fo6TOTSYD3TBQjuw/0",
     *      "product_code":"testingtesting",
     *      "ori_price":9000000,
     *      "quantity":800
     *    }
     *  ],
     *  "attrext":{
     *    "location":{
     *      "country":"涓浗",
     *      "province":"骞夸笢鐪�",
     *      "city":"骞垮窞甯�",
     *      "address":"T.I.T鍒涙剰鍥�"
     *    },
     *    "isPostFree":0,
     *    "isHasReceipt":1,
     *    "isUnderGuaranty":0,
     *    "isSupportReplace":0
     *  },
     *  "delivery_info":{
     *    "delivery_type":0,
     *    "template_id":0,
     *    "express":[
     *      {"id":10000027,"price":100},
     *      {"id":10000028,"price":100},
     *      {"id":10000029,"price":100}
     *    ]
     *  }
     * }
     * ```
     * Result:
     * ```
     * {
     *  "errcode": 0,
     *  "errmsg": "success"
     * }
     * ```
     * @param {Object} goods 鍟嗗搧淇℃伅
     */
    public boolean updateGoods (Map<String, Object> goods) {

        AccessToken token = this.ensureAccessToken();
        String accessToken = token.getAccessToken();

        String url = this.MERCHANT_PREFIX + "update?access_token=" + accessToken;

        String respStr = HttpUtils.sendPostJsonRequest(url, JSON.toJSONString(goods));
        JSONObject resp = JSON.parseObject(respStr);
        int errCode = resp.getIntValue("errcode");
        if(errCode == 0){
            return true;
        }else{
            return false;
        }
    }

    /**
     * 鏌ヨ鍟嗗搧
     * 璇︾粏璇风湅锛�<http://mp.weixin.qq.com/wiki/index.php?title=寰俊灏忓簵鎺ュ彛>
     * Examples:
     * ```
     * api.getGoods(productId);
     * ```
     * Result:
     * ```
     * {
     *  "errcode": 0,
     *  "errmsg": "success",
     *  "product_info":{
     *    "product_id":"pDF3iY6Kr_BV_CXaiYysoGqJhppQ",
     *    "product_base":{
     *      "name":"testaddproduct",
     *      "category_id":[537074298],
     *      "img":[
     *        "http://mmbiz.qpic.cn/mmbiz/4whpV1VZl2iccsvYbHvnphkyGtnvjD3ulEKogfsiaua49pvLfUS8Ym0GSYjViaLic0FD3vN0V8PILcibEGb2fPfEOmw/0"
     *      ],
     *      "property":[
     *        {"id":"鍝佺墝","vid":"Fujifilm/瀵屸紶澹�"},
     *        {"id":"灞忓箷灏衡绩瀵�","vid":"1.8鑻扁绩瀵�"},
     *        {"id":"闃叉姈鎬ц兘","vid":"CCD闃叉姈"}
     *      ],
     *      "sku_info":[
     *        {
     *          "id":"1075741873",
     *          "vid":[
     *            "1079742386",
     *            "1079742363"
     *          ]
     *        }
     *      ],
     *      "buy_limit":10,
     *      "main_img": 4whpV1VZl2iccsvYbHvnphkyGtnvjD3ulEKogfsiaua49pvLfUS8Ym0GSYjViaLic 0FD3vN0V8PILcibEGb2fPfEOmw/0",
     *      "detail_html": "<div class=\"item_pic_wrp\" style= \"margin-bottom:8px;font-size:0;\"><img class=\"item_pic\" style= \"width:100%;\" alt=\"\" src=\"http://mmbiz.qpic.cn/mmbiz/ 4whpV1VZl2iccsvYbHvnphkyGtnvjD3ulEKogfsiaua49pvLfUS8Ym0GSYjViaLic 0FD3vN0V8PILcibEGb2fPfEOmw/0\" ></div><p style=\"margin-bottom: 11px;margin-top:11px;\">test</p><div class=\"item_pic_wrp\" style=\"margin-bottom:8px;font-size:0;\"><img class=\"item_pic\" style=\"width:100%;\" alt=\"\" src=\"http://mmbiz.qpic.cn/mmbiz/ 4whpV1VZl2iccsvYbHvnphkyGtnvjD3ul1UcLcwxrFdwTKYhH9Q5YZoCfX4Ncx655 ZK6ibnlibCCErbKQtReySaVA/0\" ></div><p style=\"margin-bottom: 11px;margin-top:11px;\">test again</p>"
     *    },
     *    "sku_list":[
     *      {
     *        "sku_id":"1075741873:1079742386",
     *        "price":30,
     *        "icon_url": "http://mmbiz.qpic.cn/mmbiz/4whpV1VZl2iccsvYbHvnphkyGtnvjD3ulEKogfsiaua49pvLfUS8Ym0GSYjViaLic0FD3vN0V8PILcibEGb2fPfEOmw/0",
     *        "quantity":800,
     *        "product_code":"testing",
     *        "ori_price":9000000
     *      },
     *      {
     *        "sku_id":"1075741873:1079742363",
     *        "price":30,
     *        "icon_url": "http://mmbiz.qpic.cn/mmbiz/4whpV1VZl28bJj62XgfHPibY3ORKicN1oJ4CcoIr4BMbfA8LqyyjzOZzqrOGz3f5KWq1QGP3fo6TOTSYD3TBQjuw/0",
     *        "quantity":800,
     *        "product_code":"testingtesting",
     *        "ori_price":9000000
     *      }
     *    ],
     *    "attrext":{
     *      "isPostFree":0,
     *      "isHasReceipt":1,
     *      "isUnderGuaranty":0,
     *      "isSupportReplace":0,
     *      "location":{
     *        "country":"涓浗",
     *        "province":"骞夸笢鐪�",
     *        "city":"饧插窞甯�",
     *        "address":"T.I.T鍒涙剰鍥�"
     *      }
     *    },
     *    "delivery_info":{
     *      "delivery_type":1,
     *      "template_id":103312920
     *    }
     *  }
     * }
     * ```
     * @param {String} productId 鍟嗗搧Id
     */
    public JSONObject getGoods (String productId) {

        AccessToken token = this.ensureAccessToken();
        String accessToken = token.getAccessToken();

        String url = this.MERCHANT_PREFIX + "get?product_id=" + productId + "&access_token=" + accessToken;

        String respStr = HttpUtils.sendGetRequest(url);
        JSONObject resp = JSON.parseObject(respStr);
        return resp;
    }

    /**
     * 鑾峰彇鎸囧畾鐘舵�佺殑鎵�鏈夊晢鍝�
     * 璇︾粏璇风湅锛�<http://mp.weixin.qq.com/wiki/index.php?title=寰俊灏忓簵鎺ュ彛>
     * Examples:
     * ```
     * api.getGoodsByStatus(GoodsStatus.ALL);           // 鍏ㄩ儴
     * api.getGoodsByStatus(GoodsStatus.UPPER_SHELF);   // 涓婃灦
     * api.getGoodsByStatus(GoodsStatus.LOWER_SHELF);   // 涓嬫灦
     * ```
     * Result:
     * ```
     * {
     *  "errcode": 0,
     *  "errmsg": "success",
     *  "products_info": [
     *    {
     *      "product_base": ...,
     *      "sku_list": ...,
     *      "attrext": ...,
     *      "delivery_info": ...,
     *      "product_id": "pDF3iY-mql6CncpbVajaB_obC321",
     *      "status": 1
     *    }
     *  ]
     * }
     * ```
     * @param {Number} status 鐘舵�佺爜銆�(0-鍏ㄩ儴, 1-涓婃灦, 2-涓嬫灦)
     */
    public JSONObject getGoodsByStatus (Integer status) {

        AccessToken token = this.ensureAccessToken();
        String accessToken = token.getAccessToken();

        String url = this.MERCHANT_PREFIX + "getbystatus?access_token=" + accessToken;

        Map<String, Object> data = new HashMap<String, Object>();
        data.put("status", status);

        String respStr = HttpUtils.sendPostJsonRequest(url, JSON.toJSONString(data));
        JSONObject resp = JSON.parseObject(respStr);
        return resp;
    };

    /**
     * 鍟嗗搧涓婁笅鏋�
     * 璇︾粏璇风湅锛�<http://mp.weixin.qq.com/wiki/index.php?title=寰俊灏忓簵鎺ュ彛>
     * Examples:
     * ```
     * api.updateGoodsStatus(productId, GoodsStatus.ALL);
     * ```
     * Result:
     * ```
     * {
     *  "errcode": 0,
     *  "errmsg": "success"
     * }
     * ```
     * @param {String} productId 鍟嗗搧Id
     * @param {Number} status 鐘舵�佺爜銆�(0-鍏ㄩ儴, 1-涓婃灦, 2-涓嬫灦)
     */
    public boolean updateGoodsStatus (String productId, Integer status) {

        AccessToken token = this.ensureAccessToken();
        String accessToken = token.getAccessToken();

        String url = this.MERCHANT_PREFIX + "modproductstatus?access_token=" + accessToken;

        Map<String, Object> data = new HashMap<String, Object>();
        data.put("product_id", productId);
        data.put("status", status);

        String respStr = HttpUtils.sendPostJsonRequest(url, JSON.toJSONString(data));
        JSONObject resp = JSON.parseObject(respStr);
        int errCode = resp.getIntValue("errcode");
        if(errCode == 0){
            return true;
        }else{
            return false;
        }
    };

    /**
     * 鑾峰彇鎸囧畾鍒嗙被鐨勬墍鏈夊瓙鍒嗙被
     * 璇︾粏璇风湅锛�<http://mp.weixin.qq.com/wiki/index.php?title=寰俊灏忓簵鎺ュ彛>
     * Examples:
     * ```
     * api.getSubCats(catId);
     * ```
     * Result:
     * ```
     * {
     *  "errcode": 0,
     *  "errmsg": "success"
     *  "cate_list": [!
     *    {"id": "537074292","name": "鏁扮爜鐩告満"},
     *    {"id": "537074293","name": "瀹垛饯鐢ㄦ憚鍍忔満"},
     *    {"id": "537074298",! "name": "鍗曞弽鐩告満"}
     *  ]
     * }
     * ```
     * @param {Number} catId 澶у垎绫籌D
     */
    public JSONObject getSubCats (Long cateId) {

        AccessToken token = this.ensureAccessToken();
        String accessToken = token.getAccessToken();

        String url = this.MERCHANT_PREFIX + "category/getsub?access_token=" + accessToken;

        Map<String, Object> data = new HashMap<String, Object>();
        data.put("cate_id", cateId);

        String respStr = HttpUtils.sendPostJsonRequest(url, JSON.toJSONString(data));
        JSONObject resp = JSON.parseObject(respStr);
        return resp;
    };

    /**
     * 鑾峰彇鎸囧畾瀛愬垎绫荤殑鎵�鏈塖KU
     * 璇︾粏璇风湅锛�<http://mp.weixin.qq.com/wiki/index.php?title=寰俊灏忓簵鎺ュ彛>
     * Examples:
     * ```
     * api.getSKUs(catId);
     * ```
     * Result:
     * ```
     * {
     *  "errcode": 0,
     *  "errmsg": "success"
     *  "sku_table": [
     *    {
     *      "id": "1075741873",
     *      "name": "棰溾緤鑹�",
     *      "value_list": [
     *        {"id": "1079742375", "name": "鎾炩緤鑹�"},
     *        {"id": "1079742376","name": "妗斺緤鑹�"}
     *      ]
     *    }
     *  ]
     * }
     * ```
     * @param {Number} catId 澶у垎绫籌D
     */
    public JSONObject getSKUs (Long cateId) {

        AccessToken token = this.ensureAccessToken();
        String accessToken = token.getAccessToken();

        String url = this.MERCHANT_PREFIX + "category/getsku?access_token=" + accessToken;

        Map<String, Object> data = new HashMap<String, Object>();
        data.put("cate_id", cateId);

        String respStr = HttpUtils.sendPostJsonRequest(url, JSON.toJSONString(data));
        JSONObject resp = JSON.parseObject(respStr);
        return resp;
    };

    /**
     * 鑾峰彇鎸囧畾鍒嗙被鐨勬墍鏈夊睘鎬�
     * 璇︾粏璇风湅锛�<http://mp.weixin.qq.com/wiki/index.php?title=寰俊灏忓簵鎺ュ彛>
     * Examples:
     * ```
     * api.getProperties(catId);
     * ```
     * Result:
     * ```
     * {
     *  "errcode": 0,
     *  "errmsg": "success"
     *  "properties": [
     *    {
     *      "id": "1075741879",
     *      "name": "鍝佺墝",
     *      "property_value": [
     *        {"id": "200050867","name": "VIC&#38"},
     *        {"id": "200050868","name": "Kate&#38"},
     *        {"id": "200050971","name": "M&#38"},
     *        {"id": "200050972","name": "Black&#38"}
     *      ]
     *    },
     *    {
     *      "id": "123456789",
     *      "name": "棰溾緤鑹�",
     *      "property_value": ...
     *    }
     *  ]
     * }
     * ```
     * @param {Number} catId 鍒嗙被ID
     */
    public JSONObject getProperties (Long cateId) {

        AccessToken token = this.ensureAccessToken();
        String accessToken = token.getAccessToken();

        String url = this.MERCHANT_PREFIX + "category/getproperty?access_token=" + accessToken;

        Map<String, Object> data = new HashMap<String, Object>();
        data.put("cate_id", cateId);

        String respStr = HttpUtils.sendPostJsonRequest(url, JSON.toJSONString(data));
        JSONObject resp = JSON.parseObject(respStr);
        return resp;
    }




    /**
     * 鍒涘缓鍟嗗搧鍒嗙粍
     * 璇︾粏璇风湅锛�<http://mp.weixin.qq.com/wiki/index.php?title=寰俊灏忓簵鎺ュ彛>
     * Examples:
     * ```
     * api.createGoodsGroup(groupName, productList);
     * ```
     * Result:
     * ```
     * {
     *  "errcode": 0,
     *  "errmsg": "success",
     *  "group_id": 19
     * }
     * ```
     * @param {String}      groupName 鍒嗙粍鍚�
     * @param {Array}       productList 璇ョ粍鍟嗗搧鍒楄〃
     */
    public JSONObject createGoodsGroup (String groupName, List<String> productList) {

        AccessToken token = this.ensureAccessToken();
        String accessToken = token.getAccessToken();

        if(productList == null){
            productList = new ArrayList<String>();
        }

        String url = this.MERCHANT_PREFIX + "group/add?access_token=" + accessToken;

        Map<String, Object> data = new HashMap<String, Object>();
        Map<String, Object> groupDetail = new HashMap<String, Object>();
        groupDetail.put("group_name", groupName);
        groupDetail.put("product_list", productList);
        data.put("group_detail", groupDetail);

        String respStr = HttpUtils.sendPostJsonRequest(url, JSON.toJSONString(data));
        JSONObject resp = JSON.parseObject(respStr);
        return resp;
    }

    /**
     * 鍒犻櫎鍟嗗搧鍒嗙粍
     * 璇︾粏璇风湅锛�<http://mp.weixin.qq.com/wiki/index.php?title=寰俊灏忓簵鎺ュ彛>
     * Examples:
     * ```
     * api.deleteGoodsGroup(groupId);
     * ```
     * Result:
     * ```
     * {
     *  "errcode": 0,
     *  "errmsg": "success"
     * }
     * ```
     * @param {String}      groupId 鍒嗙粍ID
     */
    public boolean deleteGoodsGroup (String groupId) {

        AccessToken token = this.ensureAccessToken();
        String accessToken = token.getAccessToken();

        String url = this.MERCHANT_PREFIX + "group/del?access_token=" + accessToken;

        Map<String, Object> data = new HashMap<String, Object>();
        data.put("group_id", groupId);

        String respStr = HttpUtils.sendPostJsonRequest(url, JSON.toJSONString(data));
        JSONObject resp = JSON.parseObject(respStr);
        int errCode = resp.getIntValue("errcode");
        if(errCode == 0){
            return true;
        }else{
            return false;
        }
    }

    /**
     * 淇敼鍟嗗搧鍒嗙粍灞炴��
     * 璇︾粏璇风湅锛�<http://mp.weixin.qq.com/wiki/index.php?title=寰俊灏忓簵鎺ュ彛>
     * Examples:
     * ```
     * api.updateGoodsGroup(groupId, groupName);
     * ```
     * Result:
     * ```
     * {
     *  "errcode": 0,
     *  "errmsg": "success"
     * }
     * ```
     * @param {String}      groupId 鍒嗙粍ID
     * @param {String}      groupName 鍒嗙粍鍚�
     */
    public boolean updateGoodsGroup (String groupId, String groupName) {

        AccessToken token = this.ensureAccessToken();
        String accessToken = token.getAccessToken();

        String url = this.MERCHANT_PREFIX + "group/propertymod?access_token=" + accessToken;

        Map<String, Object> data = new HashMap<String, Object>();
        data.put("group_id", groupId);
        data.put("group_name", groupName);

        String respStr = HttpUtils.sendPostJsonRequest(url, JSON.toJSONString(data));
        JSONObject resp = JSON.parseObject(respStr);
        int errCode = resp.getIntValue("errcode");
        if(errCode == 0){
            return true;
        }else{
            return false;
        }
    }

    /**
     * 淇敼鍟嗗搧鍒嗙粍鍐呯殑鍟嗗搧
     * 璇︾粏璇风湅锛�<http://mp.weixin.qq.com/wiki/index.php?title=寰俊灏忓簵鎺ュ彛>
     * Examples:
     * ```
     * api.updateGoodsForGroup(groupId, addProductList, delProductList);
     * ```
     * Result:
     * ```
     * {
     *  "errcode": 0,
     *  "errmsg": "success"
     * }
     * ```
     * @param {Object}      groupId 鍒嗙粍ID
     * @param {Array}       addProductList 寰呮坊鍔犵殑鍟嗗搧鏁扮粍
     * @param {Array}       delProductList 寰呭垹闄ょ殑鍟嗗搧鏁扮粍
     */
    public boolean updateGoodsForGroup (String groupId,
                                        List<String> addProductList,
                                        List<String> delProductList) {

        if(addProductList == null){
            addProductList = new ArrayList<String>();
        }

        if(delProductList == null){
            delProductList = new ArrayList<String>();
        }

        List<Map<String, Object>> productList = new ArrayList<Map<String, Object>>();

        for(String productId : addProductList){
            Map<String, Object> item = new HashMap<String, Object>();

            item.put("product_id", productId);
            item.put("mod_action", 1);

            productList.add(item);
        }

        for(String productId : delProductList){
            Map<String, Object> item = new HashMap<String, Object>();

            item.put("product_id", productId);
            item.put("mod_action", 0);

            productList.add(item);
        }

        AccessToken token = this.ensureAccessToken();
        String accessToken = token.getAccessToken();

        String url = this.MERCHANT_PREFIX + "group/productmod?access_token=" + accessToken;

        Map<String, Object> data = new HashMap<String, Object>();
        data.put("group_id", groupId);
        data.put("product", productList);

        String respStr = HttpUtils.sendPostJsonRequest(url, JSON.toJSONString(data));
        JSONObject resp = JSON.parseObject(respStr);
        int errCode = resp.getIntValue("errcode");
        if(errCode == 0){
            return true;
        }else{
            return false;
        }
    }

    /**
     * 鑾峰彇鎵�鏈夊晢鍝佸垎缁�
     * 璇︾粏璇风湅锛�<http://mp.weixin.qq.com/wiki/index.php?title=寰俊灏忓簵鎺ュ彛>
     * Examples:
     * ```
     * api.getAllGroups();
     * ```
     * Result:
     * ```
     * {
     *  "errcode": 0,
     *  "errmsg": "success"
     *  "groups_detail": [
     *    {
     *      "group_id": 200077549,
     *      "group_name": "鏂板搧涓婃灦"
     *    },{
     *      "group_id": 200079772,
     *      "group_name": "鍏ㄧ悆鐑崠"
     *    }
     *  ]
     * }
     * ```
     */
    public JSONObject getAllGroups () {

        AccessToken token = this.ensureAccessToken();
        String accessToken = token.getAccessToken();

        String url = this.MERCHANT_PREFIX + "group/getall?access_token=" + accessToken;

        String respStr = HttpUtils.sendGetRequest(url);
        JSONObject resp = JSON.parseObject(respStr);
        return resp;
    }

    /**
     * 鏍规嵁ID鑾峰彇鍟嗗搧鍒嗙粍
     * 璇︾粏璇风湅锛�<http://mp.weixin.qq.com/wiki/index.php?title=寰俊灏忓簵鎺ュ彛>
     * Examples:
     * ```
     * api.getGroupById(groupId);
     * ```
     * Result:
     * ```
     * {
     *  "errcode": 0,
     *  "errmsg": "success"
     *  "group_detail": {
     *    "group_id": 200077549,
     *    "group_name": "鏂板搧涓婃灦",
     *    "product_list": [
     *      "pDF3iYzZoY-Budrzt8O6IxrwIJAA",
     *      "pDF3iY3pnWSGJcO2MpS2Nxy3HWx8",
     *      "pDF3iY33jNt0Dj3M3UqiGlUxGrio"
     *    ]
     *  }
     * }
     * ```
     * @param {String}      groupId 鍒嗙粍ID
     */
    public JSONObject getGroupById (String groupId) {

        AccessToken token = this.ensureAccessToken();
        String accessToken = token.getAccessToken();

        String url = this.MERCHANT_PREFIX + "group/getbyid?access_token=" + accessToken;

        Map<String, Object> data = new HashMap<String, Object>();
        data.put("group_id", groupId);

        String respStr = HttpUtils.sendPostJsonRequest(url,JSON.toJSONString(data));
        JSONObject resp = JSON.parseObject(respStr);
        return resp;
    }





    /**
     * 澧炲姞搴撳瓨
     * 璇︾粏璇风湅锛�<http://mp.weixin.qq.com/wiki/index.php?title=寰俊灏忓簵鎺ュ彛>
     * Examples:
     * ```
     * api.updateStock(10, productId, sku); // 澧炲姞10浠跺簱瀛�
     * api.updateStock(-10, productId, sku); // 鍑忓皯10浠跺簱瀛�
     * ```
     * Result:
     * ```
     * {
     *  "errcode": 0,
     *  "errmsg": "success"
     * }
     * ```
     * @param {Number} number 澧炲姞鎴栬�呭垹闄ょ殑鏁伴噺
     * @param {String} productId 鍟嗗搧ID
     * @param {String} sku SKU淇℃伅
     */
    public boolean updateStock (Integer number, String productId, String sku) {

        AccessToken token = this.ensureAccessToken();
        String accessToken = token.getAccessToken();

        String url = null;
        if(number > 0) {
            url = this.MERCHANT_PREFIX + "stock/add?access_token=" + accessToken;
        }else{
            url = this.MERCHANT_PREFIX + "stock/reduce?access_token=" + accessToken;
        }

        Map<String, Object> data = new HashMap<String, Object>();
        data.put("product_id", productId);
        data.put("sku_info", sku);
        data.put("quantity", Math.abs(number));

        String respStr = HttpUtils.sendPostJsonRequest(url,JSON.toJSONString(data));
        JSONObject resp = JSON.parseObject(respStr);
        int errCode = resp.getIntValue("errcode");
        if(errCode == 0){
            return true;
        }else{
            return false;
        }
    };


    /**
     * 澧炲姞璐ф灦
     * 璇︾粏璇风湅锛�<http://mp.weixin.qq.com/wiki/index.php?title=寰俊灏忓簵鎺ュ彛>
     * Examples:
     * ```
     * api.createShelf(shelf);
     * ```
     * Shelf:
     * ```
     * {
     *   "shelf_data": {
     *     "module_infos": [
     *     {
     *       "group_info": {
     *         "filter": {
     *           "count": 2
     *         },
     *         "group_id": 50
     *       },
     *       "eid": 1
     *     },
     *     {
     *       "group_infos": {
     *         "groups": [
     *           {
     *             "group_id": 49
     *           },
     *           {
     *             "group_id": 50
     *           },
     *           {
     *             "group_id": 51
     *           }
     *         ]
     *       },
     *       "eid": 2
     *     },
     *     {
     *       "group_info": {
     *         "group_id": 52,
     *         "img": "http://mmbiz.qpic.cn/mmbiz/4whpV1VZl29nqqObBwFwnIX3licVPnFV5Jm64z4I0TTicv0TjN7Vl9bykUUibYKIOjicAwIt6Oy0Y6a1Rjp5Tos8tg/0"
     *       },
     *       "eid": 3
     *     },
     *     {
     *       "group_infos": {
     *         "groups": [
     *           {
     *             "group_id": 49,
     *             "img": "http://mmbiz.qpic.cn/mmbiz/4whpV1VZl29nqqObBwFwnIX3licVPnFV5uUQx7TLx4tB9qZfbe3JmqR4NkkEmpb5LUWoXF1ek9nga0IkeSSFZ8g/0"
     *           },
     *           {
     *             "group_id": 50,
     *             "img": "http://mmbiz.qpic.cn/mmbiz/4whpV1VZl29nqqObBwFwnIX3licVPnFV5G1kdy3ViblHrR54gbCmbiaMnl5HpLGm5JFeENyO9FEZAy6mPypEpLibLA/0"
     *           },
     *           {
     *             "group_id": 52,
     *             "img": "http://mmbiz.qpic.cn/mmbiz/4whpV1VZl29nqqObBwFwnIX3licVPnFV5uUQx7TLx4tB9qZfbe3JmqR4NkkEmpb5LUWoXF1ek9nga0IkeSSFZ8g/0"
     *           }
     *         ]
     *       },
     *       "eid": 4
     *     },
     *     {
     *       "group_infos": {
     *         "groups": [
     *           {
     *             "group_id": 43
     *           },
     *           {
     *             "group_id": 44
     *           },
     *           {
     *             "group_id": 45
     *           },
     *           {
     *             "group_id": 46
     *           }
     *         ],
     *       "img_background": "http://mmbiz.qpic.cn/mmbiz/4whpV1VZl29nqqObBwFwnIX3licVPnFV5uUQx7TLx4tB9qZfbe3JmqR4NkkEmpb5LUWoXF1ek9nga0IkeSSFZ8g/0"
     *       },
     *       "eid": 5
     *     }
     *     ]
     *   },
     *   "shelf_banner": "http://mmbiz.qpic.cn/mmbiz/4whpV1VZl2ibrWQn8zWFUh1YznsMV0XEiavFfLzDWYyvQOBBszXlMaiabGWzz5B2KhNn2IDemHa3iarmCyribYlZYyw/0",
     *   "shelf_name": "娴嬭瘯璐ф灦"
     * }
     * ```
     * Result:
     * ```
     * {
     *   "errcode": 0,
     *   "errmsg": "success",
     *   "shelf_id": 12
     * }
     * ```
     * @param {Object} shelf 璐ф灦淇℃伅
     */
    public JSONObject createShelf (Map<String, Object> shelf) {

        AccessToken token = this.ensureAccessToken();
        String accessToken = token.getAccessToken();

        String url = this.MERCHANT_PREFIX + "shelf/add?access_token=" + accessToken;

        String respStr = HttpUtils.sendPostJsonRequest(url, JSON.toJSONString(shelf));
        JSONObject resp = JSON.parseObject(respStr);
        return resp;
    };

    /**
     * 鍒犻櫎璐ф灦
     * 璇︾粏璇风湅锛�<http://mp.weixin.qq.com/wiki/index.php?title=寰俊灏忓簵鎺ュ彛>
     * Examples:
     * ```
     * api.deleteShelf(shelfId);
     * ```
     * Result:
     * ```
     * {
     *   "errcode": 0,
     *   "errmsg": "success"
     * }
     * ```
     * @param {String} shelfId 璐ф灦Id
     */
    public boolean deleteShelf (String shelfId) {

        AccessToken token = this.ensureAccessToken();
        String accessToken = token.getAccessToken();

        String url = this.MERCHANT_PREFIX + "shelf/del?access_token" + accessToken;

        Map<String, Object> data = new HashMap<String, Object>();
        data.put("shelf_id", shelfId);

        String respStr = HttpUtils.sendPostJsonRequest(url, JSON.toJSONString(data));
        JSONObject resp = JSON.parseObject(respStr);
        int errCode = resp.getIntValue("errcode");
        if(errCode == 0){
            return true;
        }else{
            return false;
        }
    };

    /**
     * 淇敼璐ф灦
     * 璇︾粏璇风湅锛�<http://mp.weixin.qq.com/wiki/index.php?title=寰俊灏忓簵鎺ュ彛>
     * Examples:
     * ```
     * api.updateShelf(shelf);
     * ```
     * Shelf:
     * ```
     * {
     *   "shelf_id": 12345,
     *   "shelf_data": ...,
     *   "shelf_banner": "http://mmbiz.qpic.cn/mmbiz/ 4whpV1VZl2ibrWQn8zWFUh1YznsMV0XEiavFfLzDWYyvQOBBszXlMaiabGWzz5B2K hNn2IDemHa3iarmCyribYlZYyw/0",
     *   "shelf_name": "璐ф灦鍚嶇О"
     * }
     * ```
     * Result:
     * ```
     * {
     *   "errcode": 0,
     *   "errmsg": "success"
     * }
     * ```
     * @param {Object} shelf 璐ф灦淇℃伅
     */
    public boolean updateShelf (Map<String, Object> shelf) {

        AccessToken token = this.ensureAccessToken();
        String accessToken = token.getAccessToken();

        String url = this.MERCHANT_PREFIX + "shelf/mod?access_token=" + accessToken;

        String respStr = HttpUtils.sendPostJsonRequest(url, JSON.toJSONString(shelf));
        JSONObject resp = JSON.parseObject(respStr);
        int errCode = resp.getIntValue("errcode");
        if(errCode == 0){
            return true;
        }else{
            return false;
        }
    };

    /**
     * 鑾峰彇鎵�鏈夎揣鏋�
     * 璇︾粏璇风湅锛�<http://mp.weixin.qq.com/wiki/index.php?title=寰俊灏忓簵鎺ュ彛>
     * Examples:
     * ```
     * api.getAllShelf();
     * ```
     * Result:
     * ```
     * {
     *   "errcode": 0,
     *   "errmsg": "success",
     *   "shelves": [
     *     {
     *       "shelf_info": {
     *       "module_infos": [
     *         {
     *         "group_infos": {
     *           "groups": [
     *           {
     *             "group_id": 200080093
     *           },
     *           {
     *             "group_id": 200080118
     *           },
     *           {
     *             "group_id": 200080119
     *           },
     *           {
     *             "group_id": 200080135
     *           }
     *           ],
     *           "img_background": "http://mmbiz.qpic.cn/mmbiz/4whpV1VZl294FzPwnf9dAcaN7ButStztAZyy2yHY8pW6sTQKicIhAy5F0a2CqmrvDBjMFLtc2aEhAQ7uHsPow9A/0"
     *         },
     *         "eid": 5
     *         }
     *       ]
     *       },
     *       "shelf_banner": "http://mmbiz.qpic.cn/mmbiz/4whpV1VZl294FzPwnf9dAcaN7ButStztAZyy2yHY8pW6sTQKicIhAy5F0a2CqmrvDBjMFLtc2aEhAQ7uHsPow9A/0",
     *       "shelf_name": "鏂版柊浜虹被",
     *       "shelf_id": 22
     *     },
     *     {
     *       "shelf_info": {
     *       "module_infos": [
     *         {
     *           "group_info": {
     *             "group_id": 200080119,
     *             "filter": {
     *               "count": 4
     *             }
     *           },
     *           "eid": 1
     *         }
     *       ]
     *       },
     *       "shelf_banner": "http://mmbiz.qpic.cn/mmbiz/4whpV1VZl294FzPwnf9dAcaN7ButStztAZyy2yHY8pW6sTQKicIhAy5F0a2CqmrvDBjMFLtc2aEhAQ7uHsPow9A/0",
     *       "shelf_name": "搴楅摵",
     *       "shelf_id": 23
     *     }
     *   ]
     * }
     * ```
     */
    public JSONObject getAllShelves () {

        AccessToken token = this.ensureAccessToken();
        String accessToken = token.getAccessToken();

        String url = this.MERCHANT_PREFIX + "shelf/getall?access_token=" + accessToken;

        String respStr = HttpUtils.sendGetRequest(url);
        JSONObject resp = JSON.parseObject(respStr);

        return resp;
    };

    /**
     * 鏍规嵁璐ф灦ID鑾峰彇璐ф灦淇℃伅
     * 璇︾粏璇风湅锛�<http://mp.weixin.qq.com/wiki/index.php?title=寰俊灏忓簵鎺ュ彛>
     * Examples:
     * ```
     * api.getShelfById(shelfId);
     * ```
     * Result:
     * ```
     * {
     *   "errcode": 0,
     *   "errmsg": "success",
     *   "shelf_info": {
     *     "module_infos": [...]
     *   },
     *   "shelf_banner": "http://mmbiz.qpic.cn/mmbiz/4whpV1VZl2ibp2DgDXiaic6WdflMpNdInS8qUia2BztlPu1gPlCDLZXEjia2qBdjoLiaCGUno9zbs1UyoqnaTJJGeEew/0",
     *   "shelf_name": "鏂板缓璐ф灦",
     *   "shelf_id": 97
     * }
     * ```
     * @param {String} shelfId 璐ф灦Id
     */
    public JSONObject getShelfById (String shelfId) {

        AccessToken token = this.ensureAccessToken();
        String accessToken = token.getAccessToken();

        String url = this.MERCHANT_PREFIX + "shelf/getbyid?access_token=" + accessToken;

        Map<String, Object> data = new HashMap<String, Object>();
        data.put("shelf_id", shelfId);

        String respStr = HttpUtils.sendPostJsonRequest(url, JSON.toJSONString(data));
        JSONObject resp = JSON.parseObject(respStr);

        return resp;
    }



    /**
     * 鏍规嵁璁㈠崟Id鑾峰彇璁㈠崟璇︽儏
     * 璇︾粏璇风湅锛�<http://mp.weixin.qq.com/wiki/index.php?title=寰俊灏忓簵鎺ュ彛>
     * Examples:
     * ```
     * api.getOrderById(orderId);
     * ```
     * Result:
     * ```
     * {
     *   "errcode": 0,
     *   "errmsg": "success",
     *   "order": {
     *     "order_id": "7197417460812533543",
     *     "order_status": 6,
     *     "order_total_price": 6,
     *     "order_create_time": 1394635817,
     *     "order_express_price": 5,
     *     "buyer_openid": "oDF3iY17NsDAW4UP2qzJXPsz1S9Q",
     *     "buyer_nick": "likeacat",
     *     "receiver_name": "寮犲皬鐚�",
     *     "receiver_province": "骞夸笢鐪�",
     *     "receiver_city": "骞垮窞甯�",
     *     "receiver_address": "鍗庢櫙璺竴鍙峰崡鏂归�氫俊澶у帵5妤�",
     *     "receiver_mobile": "123456789",
     *     "receiver_phone": "123456789",
     *     "product_id": "pDF3iYx7KDQVGzB7kDg6Tge5OKFo",
     *     "product_name": "瀹夎帀鑺矱-BRA涓撴煖濂冲＋鑸掗�傚唴琛ｈ暰涓�3/4钖勬澂鑱氭嫝涓婃墭鎬ф劅鏂囪兏KB0716",
     *     "product_price": 1,
     *     "product_sku": "10000983:10000995;10001007:10001010",
     *     "product_count": 1,
     *     "product_img": "http://img2.paipaiimg.com/00000000/item-52B87243-63CCF66C00000000040100003565C1EA.0.300x300.jpg",
     *     "delivery_id": "1900659372473",
     *     "delivery_company": "059Yunda",
     *     "trans_id": "1900000109201404103172199813"
     *   }
     * }
     * ```
     * @param {String} orderId 璁㈠崟Id
     */
    public JSONObject getOrderById (String orderId) {

        AccessToken token = this.ensureAccessToken();
        String accessToken = token.getAccessToken();

        String url = this.MERCHANT_PREFIX + "order/getbyid?access_token=" + accessToken;

        Map<String, Object> data = new HashMap<String, Object>();
        data.put("order_id", orderId);

        String respStr = HttpUtils.sendPostJsonRequest(url, JSON.toJSONString(data));
        JSONObject resp = JSON.parseObject(respStr);

        return resp;
    };

    /**
     * 鏍规嵁璁㈠崟鐘舵��/鍒涘缓鏃堕棿鑾峰彇璁㈠崟璇︽儏
     * 璇︾粏璇风湅锛�<http://mp.weixin.qq.com/wiki/index.php?title=寰俊灏忓簵鎺ュ彛>
     * Examples:
     * ```
     * api.getOrdersByStatus([status,] [beginTime,] [endTime,]);
     * ```
     * Usage:
     * 褰撳彧浼犲叆callback鍙傛暟鏃讹紝鏌ヨ鎵�鏈夌姸鎬侊紝鎵�鏈夋椂闂寸殑璁㈠崟
     * 褰撲紶鍏ヤ竴涓弬鏁帮紝鍙傛暟涓篘umber绫诲瀷锛屾煡璇㈡寚瀹氱姸鎬侊紝鎵�鏈夋椂闂寸殑璁㈠崟
     * 褰撲紶鍏ヤ竴涓弬鏁帮紝鍙傛暟涓篋ate绫诲瀷锛屾煡璇㈡墍鏈夌姸鎬侊紝鎸囧畾璁㈠崟鍒涘缓璧峰鏃堕棿鐨勮鍗�(寰呮祴璇�)
     * 褰撲紶鍏ヤ簩涓弬鏁帮紝绗竴鍙傛暟涓鸿鍗曠姸鎬佺爜锛岀浜屽弬鏁颁负璁㈠崟鍒涘缓璧峰鏃堕棿
     * 褰撲紶鍏ヤ笁涓弬鏁帮紝绗竴鍙傛暟涓鸿鍗曠姸鎬佺爜锛岀浜屽弬鏁颁负璁㈠崟鍒涘缓璧峰鏃堕棿锛岀涓夊弬鏁颁负璁㈠崟鍒涘缓缁堟鏃堕棿
     * Result:
     * ```
     * {
     *   "errcode": 0,
     *   "errmsg": "success",
     *   "order_list": [
     *     {
     *       "order_id": "7197417460812533543",
     *       "order_status": 6,
     *       "order_total_price": 6,
     *       "order_create_time": 1394635817,
     *       "order_express_price": 5,
     *       "buyer_openid": "oDF3iY17NsDAW4UP2qzJXPsz1S9Q",
     *       "buyer_nick": "likeacat",
     *       "receiver_name": "寮犲皬鐚�",
     *       "receiver_province": "骞夸笢鐪�",
     *       "receiver_city": "骞垮窞甯�",
     *       "receiver_address": "鍗庢櫙璺竴鍙峰崡鏂归�氫俊澶у帵5妤�",
     *       "receiver_mobile": "123456",
     *       "receiver_phone": "123456",
     *       "product_id": "pDF3iYx7KDQVGzB7kDg6Tge5OKFo",
     *       "product_name": "瀹夎帀鑺矱-BRA涓撴煖濂冲＋鑸掗�傚唴琛ｈ暰涓�3/4钖勬澂鑱氭嫝涓婃墭鎬ф劅鏂囪兏KB0716",
     *       "product_price": 1,
     *       "product_sku": "10000983:10000995;10001007:10001010",
     *       "product_count": 1,
     *       "product_img": "http://mmbiz.qpic.cn/mmbiz/4whpV1VZl2icND8WwMThBEcehjhDv2icY4GrDSG5RLM3B2qd9kOicWGVJcsAhvXfibhWRNoGOvCfMC33G9z5yQr2Qw/0",
     *       "delivery_id": "1900659372473",
     *       "delivery_company": "059Yunda",
     *       "trans_id": "1900000109201404103172199813"
     *     },
     *     {
     *       "order_id": "7197417460812533569",
     *       "order_status": 8,
     *       "order_total_price": 1,
     *       "order_create_time": 1394636235,
     *       "order_express_price": 0,
     *       "buyer_openid": "oDF3iY17NsDAW4UP2qzJXPsz1S9Q",
     *       "buyer_nick": "likeacat",
     *       "receiver_name": "寮犲皬鐚�",
     *       "receiver_province": "骞夸笢鐪�",
     *       "receiver_city": "骞垮窞甯�",
     *       "receiver_address": "鍗庢櫙璺竴鍙峰崡鏂归�氫俊澶у帵5妤�",
     *       "receiver_mobile": "123456",
     *       "receiver_phone": "123456",
     *       "product_id": "pDF3iYx7KDQVGzB7kDg6Tge5OKFo",
     *       "product_name": "椤瑰潬333",
     *       "product_price": 1,
     *       "product_sku": "1075741873:1079742377",
     *       "product_count": 1,
     *       "product_img": "http://mmbiz.qpic.cn/mmbiz/4whpV1VZl2icND8WwMThBEcehjhDv2icY4GrDSG5RLM3B2qd9kOicWGVJcsAhvXfibhWRNoGOvCfMC33G9z5yQr2Qw/0",
     *       "delivery_id": "1900659372473",
     *       "delivery_company": "059Yunda",
     *       "trans_id": "1900000109201404103172199813"
     *     }
     *   ]
     * }
     * ```
     * @param {Number} status 鐘舵�佺爜銆�(鏃犳鍙傛暟-鍏ㄩ儴鐘舵��, 2-寰呭彂璐�, 3-宸插彂璐�, 5-宸插畬鎴�, 8-缁存潈涓�)
     * @param {Date} beginTime 璁㈠崟鍒涘缓鏃堕棿璧峰鏃堕棿銆�(鏃犳鍙傛暟鍒欎笉鎸夌収鏃堕棿鍋氱瓫閫�)
     * @param {Date} endTime 璁㈠崟鍒涘缓鏃堕棿缁堟鏃堕棿銆�(鏃犳鍙傛暟鍒欎笉鎸夌収鏃堕棿鍋氱瓫閫�)
     */
    public JSONObject getOrdersByStatus (Integer status) {
        return getOrdersByStatus(status, null, null);
    }
    public JSONObject getOrdersByStatus (Date beginTime) {
        return getOrdersByStatus(null, beginTime, null);
    }
    public JSONObject getOrdersByStatus (Integer status, Date beginTime) {
        return getOrdersByStatus(status, beginTime, null);
    }
    public JSONObject getOrdersByStatus (Integer status, Date beginTime, Date endTime) {

        int argumentLength = 0;
        if(status != null){ argumentLength++; };
        if(beginTime != null) { argumentLength++; };
        if(endTime != null) { argumentLength++; };


        AccessToken token = this.ensureAccessToken();
        String accessToken = token.getAccessToken();

        String url = this.MERCHANT_PREFIX + "'order/getbyfilter?access_token=" + accessToken;

        Map<String, Object> data = new HashMap<String, Object>();

        if(argumentLength == 1){
            if(status != null && beginTime == null && endTime == null){
                data.put("status", status);
            }else if(status == null && beginTime != null && endTime == null){
                data.put("begintime", Math.round(beginTime.getTime() / 1000));
                data.put("endtime", Math.round(new Date().getTime() / 1000));
            }else{
                throw new Error("the parameter must be 'status' or 'beginTime'");
            }
        }else if(argumentLength == 2){
            if(status != null && beginTime != null && endTime == null){
                data.put("status", status);
                data.put("begintime", Math.round(beginTime.getTime() / 1000));
                data.put("endtime", Math.round(new Date().getTime() / 1000));
            }else{
                throw new Error("the parameter must be 'status' and 'beginTime'");
            }
        }else if(argumentLength == 3){
            data.put("status", status);
            data.put("begintime", Math.round(beginTime.getTime() / 1000));
            data.put("endtime", Math.round(endTime.getTime() / 1000));
        }

        String respStr = HttpUtils.sendPostJsonRequest(url, JSON.toJSONString(data));
        JSONObject resp = JSON.parseObject(respStr);

        return resp;
    };

    /**
     * 璁剧疆璁㈠崟鍙戣揣淇℃伅
     * 璇︾粏璇风湅锛�<http://mp.weixin.qq.com/wiki/index.php?title=寰俊灏忓簵鎺ュ彛>
     * Examples:
     * ```
     * api.setExpressForOrder(orderId, deliveryCompany, deliveryTrackNo, isOthers);
     * ```
     * Result:
     * ```
     * {
     *  "errcode": 0,
     *  "errmsg": "success"
     * }
     * ```
     * @param {String} orderId 璁㈠崟Id
     * @param {String} deliveryCompany 鐗╂祦鍏徃 (鐗╂祦鍏徃Id璇峰弬鑰冨井淇″皬搴桝PI鎵嬪唽)
     * @param {String} deliveryTrackNo 杩愬崟Id
     * @param {Boolean} isOthers 鏄惁涓�6.4.5琛ㄤ箣澶栫殑鍏跺畠鐗╂祦鍏徃(0-鍚︼紝1-鏄紝鏃犺瀛楁榛樿涓轰笉鏄叾瀹冪墿娴佸叕鍙�)
     */
    public boolean setExpressForOrder (String orderId, String deliveryCompany, String deliveryTrackNo) {
        return setExpressForOrder(orderId, deliveryCompany, deliveryTrackNo, false);
    }
    public boolean setExpressForOrder (String orderId, String deliveryCompany, String deliveryTrackNo, boolean isOthers) {

        AccessToken token = this.ensureAccessToken();
        String accessToken = token.getAccessToken();

        String url = this.MERCHANT_PREFIX + "order/setdelivery?access_token=" + accessToken;

        Map<String, Object> data = new HashMap<String, Object>();
        data.put("order_id", orderId);
        data.put("delivery_company", deliveryCompany);
        data.put("delivery_track_no", deliveryTrackNo);
        data.put("is_others", isOthers);

        String respStr = HttpUtils.sendPostJsonRequest(url, JSON.toJSONString(data));
        JSONObject resp = JSON.parseObject(respStr);
        int errCode = resp.getIntValue("errcode");
        if(errCode == 0){
            return true;
        }else{
            return false;
        }
    }

    /**
     * 璁剧疆璁㈠崟鍙戣揣淇℃伅锛嶄笉闇�瑕佺墿娴侀厤閫�
     * 閫傜敤浜庝笉闇�瑕佸疄浣撶墿娴侀厤閫佺殑铏氭嫙鍟嗗搧锛屽畬鎴愭湰鎿嶄綔鍚庤鍗曞嵆瀹屾垚銆�
     * 璇︾粏璇风湅锛�<http://mp.weixin.qq.com/wiki/index.php?title=寰俊灏忓簵鎺ュ彛>
     * Examples:
     * ```
     * api.setNoDeliveryForOrder(orderId);
     * ```
     * Result:
     * ```
     * {
     *  "errcode": 0,
     *  "errmsg": "success"
     * }
     * ```
     * @param {String} orderId 璁㈠崟Id
     */
    public boolean setNoDeliveryForOrder (String orderId) {

        AccessToken token = this.ensureAccessToken();
        String accessToken = token.getAccessToken();

        String url = this.MERCHANT_PREFIX + "order/setdelivery?access_token=" + accessToken;

        Map<String, Object> data = new HashMap<String, Object>();
        data.put("order_id", orderId);
        data.put("need_delivery", 0);

        String respStr = HttpUtils.sendPostJsonRequest(url, JSON.toJSONString(data));
        JSONObject resp = JSON.parseObject(respStr);
        int errCode = resp.getIntValue("errcode");
        if(errCode == 0){
            return true;
        }else{
            return false;
        }
    };

    /**
     * 鍏抽棴璁㈠崟
     * 璇︾粏璇风湅锛�<http://mp.weixin.qq.com/wiki/index.php?title=寰俊灏忓簵鎺ュ彛>
     * Examples:
     * ```
     * api.closeOrder(orderId);
     * ```
     * Result:
     * ```
     * {
     *  "errcode": 0,
     *  "errmsg": "success"
     * }
     * ```
     * @param {String} orderId 璁㈠崟Id
     */
    public boolean closeOrder (String orderId) {

        AccessToken token = this.ensureAccessToken();
        String accessToken = token.getAccessToken();

        String url = this.MERCHANT_PREFIX + "order/close?access_token=" + accessToken;

        Map<String, Object> data = new HashMap<String, Object>();
        data.put("order_id", orderId);

        String respStr = HttpUtils.sendPostJsonRequest(url, JSON.toJSONString(data));
        JSONObject resp = JSON.parseObject(respStr);
        int errCode = resp.getIntValue("errcode");
        if(errCode == 0){
            return true;
        }else{
            return false;
        }
    }


}

