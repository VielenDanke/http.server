package kz.danke.http.server;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.xml.XmlMapper;
import kz.danke.http.server.annotation.MethodBody;
import kz.danke.http.server.annotation.MethodHandler;
import kz.danke.http.server.annotation.MethodParam;
import kz.danke.http.server.annotation.MethodVariable;
import kz.danke.http.server.exception.BodyNotFoundException;
import kz.danke.http.server.exception.PathNotFoundException;
import kz.danke.http.server.exception.UnsupportedContentTypeException;
import kz.danke.http.server.exception.UnsupportedHttpMethodException;
import kz.danke.http.server.factory.HttpAnnotationHandlerFactory;
import kz.danke.http.server.http.ContentType;
import kz.danke.http.server.http.HttpMethod;
import kz.danke.http.server.http.HttpRequest;
import kz.danke.http.server.http.HttpResponse;
import kz.danke.http.server.tuples.InvokeProduces;
import kz.danke.http.server.tuples.PathHttpMethodKey;
import kz.danke.http.server.tuples.UrlSuccessResolveHandler;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.channels.AsynchronousServerSocketChannel;
import java.nio.channels.AsynchronousSocketChannel;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Map;
import java.util.concurrent.*;
import java.util.stream.Collectors;

public class Server {
    private static final int BUFFER_SIZE = 1024;
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
    private static final XmlMapper XML_MAPPER = new XmlMapper();
    private static final ExecutorService EXECUTOR_SERVICE = Executors.newVirtualThreadExecutor();

    private final HttpAnnotationHandlerFactory httpFactory;
    private final String host;
    private final int port;

    private AsynchronousServerSocketChannel server;

    public Server(HttpAnnotationHandlerFactory httpFactory, String host, int port) {
        this.httpFactory = httpFactory;
        this.host = host;
        this.port = port;
    }

    public void bootstrap() {
        try {
            server = AsynchronousServerSocketChannel.open();
            server.bind(new InetSocketAddress(this.host, this.port));
            while (server.isOpen()) {
                AsynchronousSocketChannel asynchronousSocketChannel = server.accept().join();
                handleClient(asynchronousSocketChannel);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void handleClient(AsynchronousSocketChannel clientChannel) throws InterruptedException, java.util.concurrent.ExecutionException, java.util.concurrent.TimeoutException, IOException {
        if (clientChannel != null && clientChannel.isOpen()) {
            ByteBuffer buffer = ByteBuffer.allocate(BUFFER_SIZE);
            StringBuffer stringBuffer = new StringBuffer();

            boolean keepReading = true;

            while (keepReading) {
                int readResult = 0;

                readResult = clientChannel.read(buffer).get();

                keepReading = readResult == BUFFER_SIZE;

                buffer.flip();

                CharBuffer charBuffer = StandardCharsets.UTF_8.decode(buffer);

                stringBuffer.append(charBuffer);

                buffer.clear();
            }

            if (stringBuffer.isEmpty()) {
                return;
            }
            if (stringBuffer.toString().contains("ico")) {
                return;
            }

            Mono<HttpResponse> httpResponseMono = Mono.just(new HttpResponse());

            Mono<HttpRequest> httpRequestMono = Mono.just(stringBuffer)
                    .map(HttpRequest::new)
                    .publishOn(Schedulers.fromExecutorService(EXECUTOR_SERVICE));

            Mono<UrlSuccessResolveHandler> urlSuccessResolveHandlerMono = httpRequestMono
                    .map(httpRequest -> {
                        String uri = httpRequest.getUri();
                        if (uri.contains("?")) {
                            int indexQuestion = uri.indexOf("?");
                            return new PathHttpMethodKey(uri.substring(0, indexQuestion), httpRequest.getMethod());
                        } else {
                            return new PathHttpMethodKey(uri, httpRequest.getMethod());
                        }
                    })
                    .map(this.httpFactory::getHandler)
                    .publishOn(Schedulers.fromExecutorService(EXECUTOR_SERVICE));

            urlSuccessResolveHandlerMono
                    .map(handler -> handler.getMethodObject().getMethod())
                    .map(method -> method.getAnnotation(MethodHandler.class))
                    .zipWith(httpRequestMono)
                    .doOnNext(tuple -> {
                        MethodHandler annotation = tuple.getT1();
                        HttpRequest request = tuple.getT2();

                        if (!annotation.method().equals(request.getMethod())) {
                            throw new UnsupportedHttpMethodException();
                        }
                        String contentTypeHeader = request.getHeaders().get("Content-Type");

                        if (contentTypeHeader == null && !annotation.consumes().isBlank()) {
                            throw new UnsupportedContentTypeException();
                        } else if (contentTypeHeader != null &&
                                !contentTypeHeader.equalsIgnoreCase(annotation.consumes()) &&
                                !annotation.consumes().isBlank()) {
                            throw new UnsupportedContentTypeException();
                        }
                    })
                    .publishOn(Schedulers.fromExecutorService(EXECUTOR_SERVICE));

            urlSuccessResolveHandlerMono
                    .zipWith(httpRequestMono)
                    .map(tuple -> {
                        UrlSuccessResolveHandler handler = tuple.getT1();
                        HttpRequest request = tuple.getT2();

                        Map<String, String> indicesVariableNameMap = handler.getIndicesVariableNameMap();
                        Method method = handler.getMethodObject().getMethod();
                        Object object = handler.getMethodObject().getObject();

                        MethodHandler annotation = method.getAnnotation(MethodHandler.class);

                        Parameter[] parameters = method.getParameters();

                        Object[] objects = injectAnnotationObjectToParameter(request, parameters, indicesVariableNameMap);

                        Object invoke;
                        try {
                            invoke = method.invoke(object, objects);
                            return new InvokeProduces(invoke, annotation.produces());
                        } catch (Exception e) {
                            throw new RuntimeException();
                        }
                    })
                    .zipWith(httpResponseMono)
                    .map(tuple -> {
                        InvokeProduces invokeProduces = tuple.getT1();
                        HttpResponse response = tuple.getT2();
                        try {
                            Object invoke = invokeProduces.getInvokeResult();
                            String contentTypeProduces = invokeProduces.getContentTypeProduces();
                            switch (contentTypeProduces) {
                                case ContentType.TEXT_PLAIN_VALUE -> response.setBody((String) invoke);
                                case ContentType.APPLICATION_XML_VALUE -> response.setBody(XML_MAPPER.writeValueAsString(invoke));
                                case ContentType.APPLICATION_JSON_VALUE -> response.setBody(OBJECT_MAPPER.writeValueAsString(invoke));
                            }
                            response.addHeader("Content-Type", contentTypeProduces);
                        } catch (Exception e) {
                            throw new RuntimeException();
                        }
                        return response;
                    })
                    .map(response -> ByteBuffer.wrap(response.getBytes()))
                    .doOnNext(clientChannel::write)
                    .subscribeOn(Schedulers.fromExecutorService(EXECUTOR_SERVICE))
                    .subscribe();
        }
    }

    private Object[] injectAnnotationObjectToParameter(HttpRequest request,
                                                       Parameter[] parameters,
                                                       Map<String, String> indicesVariableNameMap) {
        final String contentType = "Content-Type";
        final String contentTypeHeader = request.getHeaders().get(contentType);

        return Arrays.stream(parameters)
                .parallel()
                .filter(parameter -> parameter.getDeclaredAnnotation(MethodVariable.class) != null ||
                        parameter.getDeclaredAnnotation(MethodBody.class) != null ||
                        parameter.getDeclaredAnnotation(MethodParam.class) != null)
                .map(parameter -> {
                    if (parameter.getAnnotation(MethodVariable.class) != null) {
                        MethodVariable methodVariable = parameter.getAnnotation(MethodVariable.class);

                        String key = methodVariable.name();

                        return indicesVariableNameMap.get(key);
                    } else if (parameter.getAnnotation(MethodParam.class) != null) {
                        MethodParam methodParam = parameter.getAnnotation(MethodParam.class);

                        String key = methodParam.name();

                        String uriToParse = request.getUri();

                        boolean isContainsParam = uriToParse.contains(key);

                        if (isContainsParam) {
                            int indexQuestion = uriToParse.indexOf("?");

                            String paramUriPart = uriToParse.substring(indexQuestion);

                            if (paramUriPart.contains("&")) {
                                String[] paramPairs = paramUriPart.split("&");

                                return Arrays.stream(paramPairs)
                                        .filter(param -> param.contains(key))
                                        .map(param -> {
                                            int equalIndex = param.indexOf("=");

                                            return param.substring(equalIndex + 1);
                                        })
                                        .collect(Collectors.joining(","));
                            }
                            return paramUriPart.split("=")[1];
                        }
                    }
                    MethodBody methodBody = parameter.getAnnotation(MethodBody.class);

                    if (!methodBody.name().isBlank()) {
                        if (!request.getBody().contains(methodBody.name())) {
                            throw new BodyNotFoundException();
                        }
                    }
                    if (contentTypeHeader != null) {
                        final Class<?> type = parameter.getType();

                        switch (contentTypeHeader) {
                            case ContentType.APPLICATION_JSON_VALUE -> {
                                try {
                                    return OBJECT_MAPPER.readValue(request.getBody(), type);
                                } catch (JsonProcessingException jsonProcessingException) {
                                    throw new RuntimeException();
                                }
                            }
                            case ContentType.APPLICATION_XML_VALUE -> {
                                try {
                                    return XML_MAPPER.readValue(request.getBody(), type);
                                } catch (JsonProcessingException jsonProcessingException) {
                                    throw new RuntimeException();
                                }
                            }
                            case ContentType.TEXT_PLAIN_VALUE -> {
                                return request.getBody();
                            }
                        }
                    }
                    return request.getBody();
                })
                .toArray(Object[]::new);
    }

    private void createResponseNotFound(HttpResponse response) {
        response.setRawStatusCode(400);
        response.setStatus("Not Found");
        response.addHeader("Content-Type", ContentType.APPLICATION_JSON_VALUE);
    }

    private void createResponseInternalServerError(HttpResponse response, Exception e) {
        response.setRawStatusCode(500);
        response.setStatus("Internal Server Error");
        response.addHeader("Content-Type", ContentType.APPLICATION_JSON_VALUE);
        response.setBody(e.toString());
    }

    private void createUnsupportedContentTypeError(HttpResponse response) {
        response.setRawStatusCode(415);
        response.setStatus("Unsupported Media Type");
        response.addHeader("Content-Type", ContentType.APPLICATION_JSON_VALUE);
    }

    private void createMethodNotAllowedError(HttpResponse response) {
        response.setRawStatusCode(405);
        response.setStatus("Method Not Allowed");
        response.addHeader("Content-Type", ContentType.APPLICATION_JSON_VALUE);
    }
}
