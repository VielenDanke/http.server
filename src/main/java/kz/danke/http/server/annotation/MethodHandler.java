package kz.danke.http.server.annotation;

import kz.danke.http.server.http.ContentType;
import kz.danke.http.server.http.HttpMethod;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target(value = {ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
public @interface MethodHandler {

    String path() default "";

    HttpMethod method() default HttpMethod.GET;

    String consumes() default "";

    String produces() default "";
}
