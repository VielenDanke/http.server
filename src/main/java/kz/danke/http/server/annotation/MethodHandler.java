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

    String path();

    HttpMethod method() default HttpMethod.GET;

    ContentType consumes() default ContentType.TEXT_PLAIN;

    ContentType produces() default ContentType.TEXT_PLAIN;
}
