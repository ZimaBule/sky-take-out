package com.sky.annotation;

import com.sky.enumeration.OperationType;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface AutoFill { //在“增改”操作时自动填充修改时间和修改人，
                            // 删除不用，因为对象都没了，没有储存了，哪还有相应的属性和方法
    OperationType value();
}
