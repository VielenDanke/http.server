package kz.danke.http.server.tuples;

import kz.danke.http.server.http.HttpMethod;

import java.lang.reflect.Method;

public class MethodObject {

    private Object object;
    private Method method;

    public MethodObject() {
    }

    public MethodObject(Object object, Method method) {
        this.object = object;
        this.method = method;
    }

    public Object getObject() {
        return object;
    }

    public void setObject(Object object) {
        this.object = object;
    }

    public Method getMethod() {
        return method;
    }

    public void setMethod(Method method) {
        this.method = method;
    }
}
