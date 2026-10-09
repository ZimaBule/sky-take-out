package com.sky.aspect;

import com.sky.annotation.AutoFill;
import com.sky.context.BaseContext;
import com.sky.enumeration.OperationType;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.stereotype.Component;

import java.lang.reflect.Method;
import java.time.LocalDateTime;

@Component
@Aspect
@Slf4j
public class AutoFillAspect {
    @Before("@annotation(com.sky.annotation.AutoFill)") //在“增改”操作时自动填充修改时间和修改人
                                                        //删除不用，因为对象都没了，没有储存了，哪还有相应的属性和方法
    public void autoFill(JoinPoint joinPoint)  {
        log.info("公共字段自动填充...");
        /* 1.准备工作：先获取各种需要的值 */
        //1.1 获取当前切入点(即mapper层的某个insert或update方法)的数据库操作类型(即其@AutoFill注解的value属性值)
        MethodSignature signature = (MethodSignature) joinPoint.getSignature(); //获取切入点签名
        AutoFill autoFill = signature.getMethod().getAnnotation(AutoFill.class);//获取方法上的AutoFill注解
        OperationType operationType = autoFill.value();                         //获取注解的value属性值，即数据库操作类型

        //1.2 获取当前切入点的形参--即实体对象(employee,category等)
        Object[] args = joinPoint.getArgs();
        Object entity = args[0];//mapper层方法的形参中实体的位置约定好放第一

        //1.3 获取准备自动填充的字段数据
        LocalDateTime now = LocalDateTime.now();
        Long currentId = BaseContext.getCurrentId();

        /* 2.执行自动填充 */
        if (operationType == OperationType.INSERT){
            log.info("执行插入操作的自动填充...");
            try {
                //2.1 获取实体对象的set方法
                Method setCreateTime = entity.getClass().getMethod("setCreateTime", LocalDateTime.class);
                Method setUpdateTime = entity.getClass().getMethod("setUpdateTime", LocalDateTime.class);
                Method setCreateUser = entity.getClass().getMethod("setCreateUser", Long.class);
                Method setUpdateUser = entity.getClass().getMethod("setUpdateUser", Long.class);

                //2.2 通过’反射 ‘调用set方法给字段赋值
                setCreateTime.invoke(entity, now);
                setUpdateTime.invoke(entity, now);
                setCreateUser.invoke(entity, currentId);
                setUpdateUser.invoke(entity, currentId);
            }catch (Exception e){
                e.printStackTrace();
            }
        }else if(operationType == OperationType.UPDATE){
            log.info("执行更新操作的自动填充...");
            try {
                Method setUpdateTime = entity.getClass().getMethod("setUpdateTime", LocalDateTime.class);
                Method setUpdateUser = entity.getClass().getMethod("setUpdateUser", Long.class);

                setUpdateTime.invoke(entity, now);
                setUpdateUser.invoke(entity, currentId);
            }catch (Exception e){
                e.printStackTrace();
            }
        }

    }
}
