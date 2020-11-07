package kz.danke.http.server.factory;

import kz.danke.http.server.http.HttpMethod;
import kz.danke.http.server.tuples.MethodObject;
import kz.danke.http.server.tuples.PathHttpMethodKey;
import kz.danke.http.server.tuples.PathMethodObject;

import java.lang.reflect.Method;
import java.util.Map;
import java.util.Set;

public interface HttpAnnotationHandlerFactory {

    void addHandler(HttpMethod httpMethod, Object obj, String methodPath, Method method);

    void addHandlers(Map<PathHttpMethodKey, MethodObject> handlerMap);

    MethodObject getHandler(PathHttpMethodKey methodPath);
}
