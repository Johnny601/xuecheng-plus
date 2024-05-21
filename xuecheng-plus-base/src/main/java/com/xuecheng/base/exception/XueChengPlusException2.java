package com.xuecheng.base.exception;

import lombok.Getter;

@Getter
public class XueChengPlusException2 extends RuntimeException {
    private String errCode;
    private String errMessage;

    public XueChengPlusException2() {
        super();
    }

    public XueChengPlusException2(String errCode, String errMessage) {
        super(errMessage);

        this.errCode = errCode;
        this.errMessage = errMessage;
    }


    public static void cast(String errCode, CommonError commonError){
        throw new XueChengPlusException2(errCode, commonError.getErrMessage());
    }
    public static void cast(String errCode, String errMessage){
        throw new XueChengPlusException2(errCode, errMessage);
    }
}
