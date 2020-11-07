package kz.danke.http.server.factory.impl;

import kz.danke.http.server.annotation.MethodHandler;
import kz.danke.http.server.annotation.WebHandler;
import kz.danke.http.server.exception.PathConflictException;
import kz.danke.http.server.factory.FactoryPacker;
import kz.danke.http.server.factory.HttpAnnotationHandlerFactory;
import kz.danke.http.server.tuples.MethodObject;
import kz.danke.http.server.tuples.PathHttpMethodKey;
import org.reflections.Configuration;
import org.reflections.Reflections;
import org.reflections.scanners.*;
import org.reflections.util.ConfigurationBuilder;

import java.lang.reflect.Constructor;
import java.net.URL;
import java.util.Arrays;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class AnnotationFactoryPacker implements FactoryPacker {

    private final Map<PathHttpMethodKey, MethodObject> pathMethodObjectMap = new ConcurrentHashMap<>();
    private final String packageToScan;

    public AnnotationFactoryPacker(String packageToScan) {
        this.packageToScan = packageToScan;
    }

    @Override
    public void packaging(HttpAnnotationHandlerFactory httpFactory) throws Exception {
        Configuration configuration = new ConfigurationBuilder().forPackages(packageToScan).setScanners(
                new TypeAnnotationsScanner(),
                new FieldAnnotationsScanner(),
                new MethodParameterNamesScanner(),
                new MethodAnnotationsScanner(),
                new MethodParameterScanner(),
                new SubTypesScanner()
        );

        Reflections reflections = new Reflections(configuration);

        reflections.getTypesAnnotatedWith(WebHandler.class)
                .parallelStream()
                .map(aClass -> {
                    try {
                        Constructor<?> constructor = aClass.getConstructor();

                        return constructor.newInstance();
                    } catch (Exception e) {
                        throw new RuntimeException();
                    }
                })
                .forEach(obj -> Arrays.stream(obj.getClass().getDeclaredMethods())
                        .filter(m -> m.isAnnotationPresent(MethodHandler.class))
                        .forEach(m -> {
                            MethodHandler methodAnnotation = m.getAnnotation(MethodHandler.class);

                            WebHandler objectAnnotation = obj.getClass().getAnnotation(WebHandler.class);

                            String path = methodAnnotation.path();

                            if (!objectAnnotation.path().isBlank()) {
                                path = objectAnnotation.path() + path;
                            }

                            PathHttpMethodKey key = new PathHttpMethodKey(path, methodAnnotation.method());

                            if (this.pathMethodObjectMap.containsKey(key)) {
                                throw new PathConflictException("Path conflict");
                            }
                            this.pathMethodObjectMap.put(key, new MethodObject(obj, m));
                        })
                );

        httpFactory.addHandlers(this.pathMethodObjectMap);
    }
}
