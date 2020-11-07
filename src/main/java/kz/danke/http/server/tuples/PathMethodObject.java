package kz.danke.http.server.tuples;

import kz.danke.http.server.http.HttpMethod;

import java.lang.reflect.Method;
import java.util.Objects;

public class PathMethodObject {

    private String path;
    private HttpMethod httpMethod;
    private Object object;
    private Method method;

    public PathMethodObject() {
    }

    public PathMethodObject(String path, HttpMethod httpMethod, Object object, Method method) {
        this.path = path;
        this.object = object;
        this.method = method;
        this.httpMethod = httpMethod;
    }

    public HttpMethod getHttpMethod() {
        return httpMethod;
    }

    public void setHttpMethod(HttpMethod httpMethod) {
        this.httpMethod = httpMethod;
    }

    public String getPath() {
        return path;
    }

    public void setPath(String path) {
        this.path = path;
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


    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        PathMethodObject that = (PathMethodObject) o;
        return Objects.equals(path, that.path);
    }

    @Override
    public int hashCode() {
        return Objects.hash(path);
    }
}
