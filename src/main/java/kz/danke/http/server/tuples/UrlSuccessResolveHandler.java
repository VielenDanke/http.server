package kz.danke.http.server.tuples;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class UrlSuccessResolveHandler {

    private PathHttpMethodKey pathHttpMethodKey;
    private MethodObject methodObject;
    private Map<String, String> indicesVariableNameMap;

    public UrlSuccessResolveHandler() {
        this.indicesVariableNameMap = new ConcurrentHashMap<>();
    }

    public UrlSuccessResolveHandler(PathHttpMethodKey pathHttpMethodKey, MethodObject methodObject) {
        this.pathHttpMethodKey = pathHttpMethodKey;
        this.methodObject = methodObject;
        this.indicesVariableNameMap = new ConcurrentHashMap<>();
    }

    public void addIndicesMap(Map<String, String> map) {
        this.indicesVariableNameMap.putAll(map);
    }

    public void addIndicesVariable(String key, String name) {
        this.indicesVariableNameMap.put(key, name);
    }

    public Map<String, String> getIndicesVariableNameMap() {
        return indicesVariableNameMap;
    }

    public void setIndicesVariableNameMap(Map<String, String> indicesVariableNameMap) {
        this.indicesVariableNameMap = indicesVariableNameMap;
    }

    public PathHttpMethodKey getPathHttpMethodKey() {
        return pathHttpMethodKey;
    }

    public void setPathHttpMethodKey(PathHttpMethodKey pathHttpMethodKey) {
        this.pathHttpMethodKey = pathHttpMethodKey;
    }

    public MethodObject getMethodObject() {
        return methodObject;
    }

    public void setMethodObject(MethodObject methodObject) {
        this.methodObject = methodObject;
    }
}
