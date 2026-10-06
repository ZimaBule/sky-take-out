package com.sky.handler;

import com.sky.exception.BaseException;
import com.sky.result.Result;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * 全局异常处理器，处理项目中抛出的业务异常
 */
@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    //捕获业务异常
    @ExceptionHandler
    public Result exceptionHandler(BaseException ex){
        log.error("异常信息：{}", ex.getMessage());
        return Result.error(ex.getMessage());
    }

    //捕获Sql异常: Duplicate entry 'zhaojiawei' for key 'employee.idx_username'
    @ExceptionHandler
    public Result exceptionHandler(Exception ex){
        String message=ex.getMessage();
        if(message.contains("Duplicate entry")){
            return Result.error("该用户名(账号)已存在");
        }else{
            return Result.error("未知错误");
        }

    }

}
