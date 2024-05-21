package com.xuecheng.base.exception;

import lombok.Data;
import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;

/**
 * 错误响应参数包装
 */
@Getter @Setter
public class RestErrorResponse implements Serializable {
    private String errCode;
    private String errMessage;

    public RestErrorResponse(String errMessage){
        this.errMessage= errMessage;
    }

    public RestErrorResponse(String errCode, String errMessage){
        this.errCode = errCode;
        this.errMessage = errMessage;
    }
}
