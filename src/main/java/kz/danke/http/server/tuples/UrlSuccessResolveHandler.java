package kz.danke.http.server.tuples;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class UrlSuccessResolveHandler {
    
    private PathHttpMethodKey pathHttpMethodKey;
    private MethodObject methodObject;
    private Map<Integer, String> indicesVariableNameMap;

    public UrlSuccessResolveHandler() {
        this.indicesVariableNameMap = new ConcurrentHashMap<>();
    }

    public UrlSuccessResolveHandler(PathHttpMethodKey pathHttpMethodKey, MethodObject methodObject) {
        this.pathHttpMethodKey = pathHttpMethodKey;
        this.methodObject = methodObject;
        this.indicesVariableNameMap = new ConcurrentHashMap<>();
    }

    public void addIndicesMap(Map<Integer, String> map) {
        this.indicesVariableNameMap.putAll(map);
    }

    public void addIndicesVariable(Integer index, String name) {
        this.indicesVariableNameMap.put(index, name);
    }

    public Map<Integer, String> getIndicesVariableNameMap() {
        return indicesVariableNameMap;
    }

    public void setIndicesVariableNameMap(Map<Integer, String> indicesVariableNameMap) {
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
