package com.xuecheng.base.exception;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.http.HttpStatus;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.List;

/**
 * @description 全局异常处理器
 * @author Mr.M
 * @date 2022/9/6 11:29
 * @version 1.0
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(XueChengPlusException.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public RestErrorResponse customException(XueChengPlusException e) {
        String errMessage = e.getErrMessage();

        log.error("【系统异常】{}",errMessage,e);
        return new RestErrorResponse(errMessage);
    }

    @ExceptionHandler(XueChengPlusException2.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public RestErrorResponse customException(XueChengPlusException2 e) {
        String errCode = e.getErrCode();
        String errMessage = e.getErrMessage();

        log.error("【系统异常】{}",errMessage,e);
        return new RestErrorResponse(errCode, errMessage);
    }

    @ResponseBody
    @ExceptionHandler(Exception.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public RestErrorResponse exception(Exception e) {
        log.error("【系统异常】{}",e.getMessage(),e);
        e.printStackTrace();
        if(e.getMessage().equals("Access is denied")) {
            return new RestErrorResponse("没有操作此功能的权限");
        }
        return new RestErrorResponse(CommonError.UNKNOWN_ERROR.getErrMessage());
    }


    @ExceptionHandler(MethodArgumentNotValidException.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public RestErrorResponse methodArgumentNotValidException(MethodArgumentNotValidException e) {
        BindingResult bindingResult = e.getBindingResult();
        List<String> errors = new ArrayList<>();
        bindingResult.getFieldErrors().stream().forEach(fieldError -> {
            errors.add(fieldError.getDefaultMessage());
        });

        String errMessage = StringUtils.join(errors, ",");

        log.error("【系统异常】{}",e.getMessage(), errMessage);

        return new RestErrorResponse(errMessage);
    }
}

