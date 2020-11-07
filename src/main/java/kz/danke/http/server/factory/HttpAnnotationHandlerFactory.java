package kz.danke.http.server.factory;

import kz.danke.http.server.tuples.MethodObject;
import kz.danke.http.server.tuples.PathMethodObject;

import java.lang.reflect.Method;
import java.util.Map;
import java.util.Set;

public interface HttpAnnotationHandlerFactory {

    void addHandler(Object obj, String methodPath, Method method);

    void addHandlers(Map<String, MethodObject> handlerMap);

    MethodObject getHandler(String methodPath);
}
