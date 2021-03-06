package kz.danke.http.server.factory.impl;

import kz.danke.http.server.exception.PathNotFoundException;
import kz.danke.http.server.exception.StaticElementException;
import kz.danke.http.server.factory.HttpAnnotationHandlerFactory;
import kz.danke.http.server.http.HttpMethod;
import kz.danke.http.server.tuples.MethodObject;
import kz.danke.http.server.tuples.PathHttpMethodKey;
import kz.danke.http.server.tuples.UrlSuccessResolveHandler;

import java.lang.reflect.Method;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class HttpAnnotationHandlerFactoryImpl implements HttpAnnotationHandlerFactory {

    private final Map<PathHttpMethodKey, MethodObject> pathMethodObjectMap = new ConcurrentHashMap<>();

    public HttpAnnotationHandlerFactoryImpl() {
    }

    public HttpAnnotationHandlerFactoryImpl(Map<PathHttpMethodKey, MethodObject> pathMethodObjectMap) {
        this.pathMethodObjectMap.putAll(pathMethodObjectMap);
    }

    @Override
    public void addHandler(HttpMethod httpMethod, Object obj, String methodPath, Method method) {
        this.pathMethodObjectMap.put(new PathHttpMethodKey(methodPath, httpMethod), new MethodObject(obj, method));
    }

    @Override
    public void addHandlers(Map<PathHttpMethodKey, MethodObject> handlerMap) {
        this.pathMethodObjectMap.putAll(handlerMap);
    }

    @Override
    public UrlSuccessResolveHandler getHandler(PathHttpMethodKey methodPath) {
        Map<String, String> indicesMap = new ConcurrentHashMap<>();

        if (this.pathMethodObjectMap.containsKey(methodPath)) {
            return new UrlSuccessResolveHandler(
                    methodPath, this.pathMethodObjectMap.get(methodPath)
            );
        }

        return this.pathMethodObjectMap.keySet()
                .parallelStream()
                .filter(pathHttpMethodKey -> this.comparing(pathHttpMethodKey, methodPath, indicesMap))
                .findAny()
                .map(pathHttpMethodKey -> {
                    UrlSuccessResolveHandler urlSuccessResolveHandler = new UrlSuccessResolveHandler(
                            pathHttpMethodKey, this.pathMethodObjectMap.get(pathHttpMethodKey)
                    );
                    urlSuccessResolveHandler.addIndicesMap(indicesMap);
                    return urlSuccessResolveHandler;
                })
                .orElseThrow(() -> new PathNotFoundException(
                        String.format("Path %s with method %s not found", methodPath.getPath(), methodPath.getHttpMethod().name())
                ));
    }

    private boolean comparing(PathHttpMethodKey handlerPath, PathHttpMethodKey incomingPath, Map<String, String> map) {
        if (!handlerPath.getHttpMethod().equals(incomingPath.getHttpMethod())) {
            return false;
        }
        if (handlerPath.getPath().equalsIgnoreCase(incomingPath.getPath())) {
            return true;
        }
        if (incomingPath.getPath().contains("ico")) {
            throw new StaticElementException("Static element processing");
        }
        String[] handlerPathSplit = handlerPath.getPath().split("/");
        String[] incomingPathSplit = incomingPath.getPath().split("/");

        if (incomingPathSplit.length == handlerPathSplit.length) {
            for (int i = 0; i < handlerPathSplit.length; i++) {
                if (handlerPathSplit[i].contains("#")) {
                    map.put(handlerPathSplit[i].replace("#", ""), incomingPathSplit[i]);
                    continue;
                }
                if (!handlerPathSplit[i].equals(incomingPathSplit[i])) {
                    return false;
                }
            }
        } else {
            return false;
        }
        return true;
    }



}
