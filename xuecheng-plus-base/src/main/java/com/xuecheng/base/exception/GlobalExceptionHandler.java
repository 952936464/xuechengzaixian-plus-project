package com.xuecheng.base.exception;


import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.ResponseStatus;

import org.apache.commons.lang3.StringUtils;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@ControllerAdvice//增强
//@RestControllerAdvice
public class GlobalExceptionHandler {
    /**
     * 针对用户自定义异常
     * @param e
     * @return
     */
    @ResponseBody
    @ExceptionHandler(XueChengPlusException.class)//捕获指定异常
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public RestErrorResponse customException(XueChengPlusException e){
        //异常信息打印
        log.error("系统异常{}",e.getErrMessage(),e);

        String errMessage = e.getErrMessage();
        RestErrorResponse restErrorResponse = new RestErrorResponse(errMessage);
        return restErrorResponse;
    }

    /**
     * 针对系统自定义异常
     * @param e
     * @return
     */
    @ResponseBody
    @ExceptionHandler(Exception.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public RestErrorResponse exception(Exception e){
        //异常信息打印
        log.error("系统异常{}",e.getMessage(),e);
        RestErrorResponse restErrorResponse = new RestErrorResponse(CommonError.UNKOWN_ERROR.getErrMessage());
        return restErrorResponse;
    }

    /**
     * JSR303校验
     * @param e
     * @return
     */
    @ResponseBody
    @ExceptionHandler(MethodArgumentNotValidException.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public RestErrorResponse methodArgumentNotValidException(MethodArgumentNotValidException e){
        BindingResult bindingResult = e.getBindingResult();

        //异常信息打印
        List<String> errors = new ArrayList<>();
        bindingResult.getFieldErrors().stream().forEach(item ->{
            errors.add(item.getDefaultMessage());
        });
        String errMessage = StringUtils.join(errors,",");
        log.error("系统异常{}",e.getMessage(),errMessage);
        RestErrorResponse restErrorResponse = new RestErrorResponse(errMessage);
        return restErrorResponse;
    }

}
