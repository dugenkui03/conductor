package com.netflix.conductor.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import static java.lang.annotation.RetentionPolicy.RUNTIME;


// Use the annotation to perform some operations when Service layer method are invoked
// 当调用服务层方法的时候，使用该注解执行一些操作：指令？
@Target(ElementType.METHOD)
@Retention(RUNTIME)
public @interface Service {

}