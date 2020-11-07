package kz.danke.http.server.tuples;

import kz.danke.http.server.http.HttpMethod;

import java.util.Objects;

public class PathHttpMethodKey {

    private String path;
    private HttpMethod httpMethod;

    public PathHttpMethodKey() {
    }

    public PathHttpMethodKey(String path, HttpMethod httpMethod) {
        this.path = path;
        this.httpMethod = httpMethod;
    }

    public String getPath() {
        return path;
    }

    public void setPath(String path) {
        this.path = path;
    }

    public HttpMethod getHttpMethod() {
        return httpMethod;
    }

    public void setHttpMethod(HttpMethod httpMethod) {
        this.httpMethod = httpMethod;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        PathHttpMethodKey that = (PathHttpMethodKey) o;
        return path.equals(that.path) &&
                httpMethod == that.httpMethod;
    }

    @Override
    public int hashCode() {
        return Objects.hash(path, httpMethod);
    }
}
