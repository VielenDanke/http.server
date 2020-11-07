package kz.danke.http.server.factory.impl;

import kz.danke.http.server.annotation.MethodHandler;
import kz.danke.http.server.annotation.WebHandler;
import kz.danke.http.server.exception.PathConflictException;
import kz.danke.http.server.factory.FactoryPacker;
import kz.danke.http.server.factory.HttpAnnotationHandlerFactory;
import kz.danke.http.server.tuples.MethodObject;
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

    private final Map<String, MethodObject> pathMethodObjectMap = new ConcurrentHashMap<>();
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
                            MethodHandler annotation = m.getAnnotation(MethodHandler.class);

                            String methodPath = String.format("%s %s", annotation.path(), annotation.method().name());

                            if (this.pathMethodObjectMap.containsKey(methodPath)) {
                                throw new PathConflictException("Path conflict");
                            }
                            this.pathMethodObjectMap.put(methodPath, new MethodObject(obj, m));
                        })
                );

        httpFactory.addHandlers(this.pathMethodObjectMap);
    }
}
