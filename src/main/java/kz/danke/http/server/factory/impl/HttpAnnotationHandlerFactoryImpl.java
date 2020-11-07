package kz.danke.http.server.factory.impl;

import kz.danke.http.server.factory.HttpAnnotationHandlerFactory;
import kz.danke.http.server.tuples.MethodObject;

import java.lang.reflect.Method;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class HttpAnnotationHandlerFactoryImpl implements HttpAnnotationHandlerFactory {

    private final Map<String, MethodObject> pathMethodObjectMap = new ConcurrentHashMap<>();

    public HttpAnnotationHandlerFactoryImpl() {
    }

    public HttpAnnotationHandlerFactoryImpl(Map<String, MethodObject> pathMethodObjectMap) {
        this.pathMethodObjectMap.putAll(pathMethodObjectMap);
    }

    @Override
    public void addHandler(Object obj, String methodPath, Method method) {
        this.pathMethodObjectMap.put(methodPath, new MethodObject(obj, method));
    }

    @Override
    public void addHandlers(Map<String, MethodObject> handlerMap) {
        this.pathMethodObjectMap.putAll(handlerMap);
    }

    @Override
    public MethodObject getHandler(String methodPath) {
        boolean isPathContains = this.pathMethodObjectMap.containsKey(methodPath);

        if (isPathContains) {
            return this.pathMethodObjectMap.get(methodPath);
        }
        return null;
    }
}
