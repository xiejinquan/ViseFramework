package com.vise.base.net.exception;

import android.net.ParseException;

import com.google.gson.JsonParseException;
import com.vise.base.net.ApiCode;
import com.vise.base.net.ApiResult;

import org.apache.http.conn.ConnectTimeoutException;
import org.json.JSONException;

import java.net.ConnectException;

import retrofit2.adapter.rxjava.HttpException;

import static com.vise.base.net.ApiCode.*;

/**
 * @Description:
 * @author: <a href="http://www.xiaoyaoyou1212.com">DAWI</a>
 * @date: 2016-12-30 17:59
 */
public class ApiException extends Exception {


    private final ApiCode code;
    private String displayMessage;
    public String message;


    public ApiException(Throwable throwable, ApiCode code) {
        super(throwable);
        this.code = code;
        this.message = throwable.getMessage();
    }

    public ApiCode getCode() {
        return code;
    }

    public String getDisplayMessage() {
        return displayMessage;
    }

    public void setDisplayMessage(String msg) {
        this.displayMessage = msg + "(code:" + code + ")";
    }

    public static boolean isOk(ApiResult apiResult) {
        if (apiResult == null)
            return false;
//        if (apiResult.isOk() || ignoreSomeIssue(apiResult.getCode()))
//            return true;
        else
            return false;
    }

    /*private static boolean ignoreSomeIssue(int code) {
        switch (code) {
            case TIMESTAMP_ERROR.code://时间戳过期
            case ACCESS_TOKEN_EXPIRED.code://AccessToken错误或已过期
            case REFRESH_TOKEN_EXPIRED.code://RefreshToken错误或已过期
            case OTHER_PHONE_LOGINED.code: //帐号在其它手机已登录
            case SIGN_ERROR.code://签名错误
                return true;
            default:
                return false;

        }
    }*/


    /*public static ApiException handleException(Throwable e) {
        ApiException ex;
        if (e instanceof HttpException) {
            HttpException httpException = (HttpException) e;
            ex = new ApiException(e, HTTP_ERROR);
            switch (httpException.code()) {
                case UNAUTHORIZED.getCode():
                case FORBIDDEN:
                case NOT_FOUND:
                case REQUEST_TIMEOUT:
                case GATEWAY_TIMEOUT:
                case INTERNAL_SERVER_ERROR:
                case BAD_GATEWAY:
                case SERVICE_UNAVAILABLE:
                default:
                    ex.message = "网络错误";
                    break;
            }
            return ex;
        } else if (e instanceof ServerException) {
            ServerException resultException = (ServerException) e;
            ex = new ApiException(resultException, resultException.errCode);
            ex.message = resultException.message;
            return ex;
        } else if (e instanceof JsonParseException
                || e instanceof JSONException
                || e instanceof ParseException) {
            ex = new ApiException(e, PARSE_ERROR);
            ex.message = "解析错误";
            return ex;
        } else if (e instanceof ConnectException) {
            ex = new ApiException(e, NETWORD_ERROR);
            ex.message = "连接失败";
            return ex;
        } else if (e instanceof javax.net.ssl.SSLHandshakeException) {
            ex = new ApiException(e, SSL_ERROR);
            ex.message = "证书验证失败";
            return ex;
        } else if (e instanceof ConnectTimeoutException) {
            ex = new ApiException(e, TIMEOUT_ERROR);
            ex.message = "连接超时";
            return ex;
        } else if (e instanceof java.net.SocketTimeoutException) {
            ex = new ApiException(e, TIMEOUT_ERROR);
            ex.message = "连接超时";
            return ex;
        } else {
            ex = new ApiException(e, UNKNOWN);
            ex.message = "未知错误";
            return ex;
        }
    }*/

}
