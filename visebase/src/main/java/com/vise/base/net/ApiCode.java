package com.vise.base.net;

/**
 * @Description:
 * @author: <a href="http://xiaoyaoyou1212.360doc.com">DAWI</a>
 * @date: 2016-12-30 18:11
 */
public enum ApiCode {
    /*==========对应HTTP的状态码=================*/
    UNAUTHORIZED(401),
    FORBIDDEN(403),
    NOT_FOUND(404),
    REQUEST_TIMEOUT(408),
    INTERNAL_SERVER_ERROR(500),
    BAD_GATEWAY(502),
    SERVICE_UNAVAILABLE(503),
    GATEWAY_TIMEOUT(504),
    /*===========================*/

    /*===========Request请求码================*/
    //未知错误
    UNKNOWN(1000),
    //解析错误
    PARSE_ERROR(1001),
    //网络错误
    NETWORK_ERROR(1002),
    //协议出错
    HTTP_ERROR(1003),
    //证书出错
    SSL_ERROR(1005),
    //连接超时
    TIMEOUT_ERROR(1006),
    //调用错误
    INVOKE_ERROR(1007),
    //类转换错误
    CANNOT_ERROR(1008),
    /*===========================*/

    /*===========Response响应码================*/
    //http请求成功状态码
    HTTP_SUCCESS(0),
    //AccessToken错误或已过期
    ACCESS_TOKEN_EXPIRED(10001),
    //RefreshToken错误或已过期
    REFRESH_TOKEN_EXPIRED(10002),
    //帐号在其它手机已登录
    OTHER_PHONE_LOGINED(10003),
    //时间戳过期
    TIMESTAMP_ERROR(10004),
    //缺少授权信息,没有AccessToken
    NO_ACCESS_TOKEN(10005),
    //签名错误
    SIGN_ERROR(10006);
    /*===========================*/

    public int code;

    ApiCode(int code) {
        this.code = code;
    }
}
