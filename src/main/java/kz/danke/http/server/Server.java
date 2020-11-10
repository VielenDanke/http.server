package kz.danke.http.server;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.xml.XmlMapper;
import kz.danke.http.server.annotation.MethodBody;
import kz.danke.http.server.annotation.MethodHandler;
import kz.danke.http.server.annotation.MethodParam;
import kz.danke.http.server.annotation.MethodVariable;
import kz.danke.http.server.exception.*;
import kz.danke.http.server.factory.HttpAnnotationHandlerFactory;
import kz.danke.http.server.http.ContentType;
import kz.danke.http.server.http.HttpRequest;
import kz.danke.http.server.http.HttpResponse;
import kz.danke.http.server.tuples.InvokeProduces;
import kz.danke.http.server.tuples.PathHttpMethodKey;
import kz.danke.http.server.tuples.UrlSuccessResolveHandler;

import java.io.IOException;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.AsynchronousServerSocketChannel;
import java.nio.channels.AsynchronousSocketChannel;
import java.nio.channels.CompletionHandler;
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
                handleClient(asynchronousSocketChannel, httpFactory);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void handleClient(AsynchronousSocketChannel clientChannel, HttpAnnotationHandlerFactory httpFactory) throws InterruptedException, java.util.concurrent.ExecutionException, java.util.concurrent.TimeoutException, IOException {
        CompletableFuture.runAsync(() -> {
            if (clientChannel != null && clientChannel.isOpen()) {
                ByteBuffer buffer = ByteBuffer.allocate(BUFFER_SIZE);

                clientChannel.read(buffer, null, new CompletionHandler<>() {

                    @Override
                    public void completed(Integer result, Object attachment) {
                        CompletableFuture<HttpResponse> httpResponseCompletableFuture = CompletableFuture.supplyAsync(HttpResponse::new, EXECUTOR_SERVICE);

                        CompletableFuture<HttpRequest> httpRequestCompletableFuture = CompletableFuture
                                .supplyAsync(() -> buffer, EXECUTOR_SERVICE)
                                .thenApplyAsync(ByteBuffer::flip, EXECUTOR_SERVICE)
                                .thenApplyAsync(StandardCharsets.UTF_8::decode, EXECUTOR_SERVICE)
                                .thenApplyAsync(StringBuffer::new, EXECUTOR_SERVICE)
                                .thenApplyAsync(HttpRequest::new, EXECUTOR_SERVICE);

                        CompletableFuture<UrlSuccessResolveHandler> urlSuccessResolveHandlerCompletableFuture = httpRequestCompletableFuture
                                .thenApplyAsync(httpRequest -> {
                                    String uri = httpRequest.getUri();
                                    if (uri.contains("?")) {
                                        int indexQuestion = uri.indexOf("?");
                                        return new PathHttpMethodKey(uri.substring(0, indexQuestion), httpRequest.getMethod());
                                    } else {
                                        return new PathHttpMethodKey(uri, httpRequest.getMethod());
                                    }
                                }, EXECUTOR_SERVICE)
                                .thenApplyAsync(httpFactory::getHandler, EXECUTOR_SERVICE);

                        CompletableFuture<MethodHandler> methodHandlerCompletableFuture = urlSuccessResolveHandlerCompletableFuture
                                .thenApplyAsync(handler -> handler.getMethodObject().getMethod(), EXECUTOR_SERVICE)
                                .thenApplyAsync(method -> method.getAnnotation(MethodHandler.class), EXECUTOR_SERVICE)
                                .thenCombineAsync(httpRequestCompletableFuture, (annotation, request) -> {
                                    if (!annotation.method().equals(request.getMethod())) {
                                        throw new UnsupportedHttpMethodException(
                                                String.format(
                                                        "Method %s is not supported", request.getMethod().name()
                                                )
                                        );
                                    }
                                    String contentTypeHeader = request.getHeaders().get("Content-Type");

                                    if (contentTypeHeader == null && !annotation.consumes().isBlank()) {
                                        throw new UnsupportedContentTypeException(
                                                String.format("Content-Type expected to be %s", annotation.consumes())
                                        );
                                    } else if (contentTypeHeader != null &&
                                            !contentTypeHeader.equalsIgnoreCase(annotation.consumes()) &&
                                            !annotation.consumes().isBlank()) {
                                        throw new UnsupportedContentTypeException(
                                                String.format(
                                                        "Content-Type %s is not supported, need %s",
                                                        contentTypeHeader,
                                                        annotation.consumes()
                                                )
                                        );
                                    }
                                    return annotation;
                                }, EXECUTOR_SERVICE);

                        urlSuccessResolveHandlerCompletableFuture
                                .thenCombineAsync(httpRequestCompletableFuture, (handler, request) -> {
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
                                        throw new MethodInvocationException(
                                                String.format("Method %s cannot be invoked", method.getName())
                                        );
                                    }
                                }, EXECUTOR_SERVICE)
                                .thenCombineAsync(httpResponseCompletableFuture, (invokeProduces, response) -> {
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
                                }, EXECUTOR_SERVICE)
                                .thenApplyAsync(httpResponse -> ByteBuffer.wrap(httpResponse.getBytes()), EXECUTOR_SERVICE)
                                .thenAcceptAsync(byteBuffer -> {
                                    clientChannel.write(byteBuffer);

                                    try {
                                        clientChannel.close();
                                    } catch (IOException e) {
                                        throw new ConnectionCloseException("Connection close failed");
                                    }
                                }, EXECUTOR_SERVICE)
                                .exceptionallyAsync(ex -> {
                                    HttpResponse response = new HttpResponse();

                                    this.createResponseDependsOnException(ex, response);

                                    ByteBuffer errorByteBuffer = ByteBuffer.wrap(response.getBytes());

                                    clientChannel.write(errorByteBuffer);

                                    try {
                                        clientChannel.close();
                                    } catch (IOException e) {
                                        throw new ConnectionCloseException("Connection close failed");
                                    }
                                    return null;
                                }, EXECUTOR_SERVICE);
                    }

                    @Override
                    public void failed(Throwable exc, Object attachment) {

                    }

                    private Object[] injectAnnotationObjectToParameter(HttpRequest request,
                                                                       Parameter[] parameters,
                                                                       Map<String, String> indicesVariableNameMap) {
                        final String contentType = "Content-Type";
                        final String contentTypeHeader = request.getHeaders().get(contentType);

                        return Arrays.stream(parameters)
                                .parallel()
                                .map(parameter -> {
                                    if (parameter.getAnnotation(MethodVariable.class) != null) {
                                        MethodVariable methodVariable = parameter.getAnnotation(MethodVariable.class);

                                        String key = methodVariable.name();

                                        String methodVariableValue = indicesVariableNameMap.get(key);

                                        if (methodVariableValue == null) {
                                            throw new MethodPathVariableNameNotMatchesException(
                                                    String.format("Method variable with name %s not found", key)
                                            );
                                        }
                                        return methodVariableValue;
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
                                    } else if (parameter.getType().isAssignableFrom(HttpRequest.class)) {
                                        return request;
                                    } else if (parameter.getAnnotation(MethodBody.class) != null) {
                                        MethodBody methodBody = parameter.getAnnotation(MethodBody.class);

                                        if (!methodBody.name().isBlank()) {
                                            if (!request.getBody().contains(methodBody.name())) {
                                                throw new BodyNotFoundException(
                                                        String.format("Body with name %s not found", methodBody.name())
                                                );
                                            }
                                        }
                                        if (contentTypeHeader != null) {
                                            final Class<?> type = parameter.getType();

                                            switch (contentTypeHeader) {
                                                case ContentType.APPLICATION_JSON_VALUE -> {
                                                    try {
                                                        return OBJECT_MAPPER.readValue(request.getBody(), type);
                                                    } catch (JsonProcessingException ex) {
                                                        throw new RequestBodyConvertingException(
                                                                String.format(
                                                                        "Cannot convert body %s to parameter %s",
                                                                        request.getBody(),
                                                                        methodBody.annotationType().getName()
                                                                )
                                                        );
                                                    }
                                                }
                                                case ContentType.APPLICATION_XML_VALUE -> {
                                                    try {
                                                        return XML_MAPPER.readValue(request.getBody(), type);
                                                    } catch (JsonProcessingException ex) {
                                                        throw new RequestBodyConvertingException(
                                                                String.format(
                                                                        "Cannot convert body %s to parameter %s",
                                                                        request.getBody(),
                                                                        methodBody.annotationType().getName()
                                                                )
                                                        );
                                                    }
                                                }
                                                case ContentType.TEXT_PLAIN_VALUE -> {
                                                    return request.getBody();
                                                }
                                            }
                                        }
                                    }
                                    return null;
                                })
                                .toArray(Object[]::new);
                    }

                    private void createResponseNotFound(HttpResponse response, Throwable e) {
                        response.setRawStatusCode(400);
                        response.setStatus("Not Found");
                        response.addHeader("Content-Type", ContentType.APPLICATION_JSON_VALUE);

                        if (e != null) {
                            response.setBody(
                                    e.getLocalizedMessage() != null && !e.getLocalizedMessage().isBlank() ?
                                            e.getLocalizedMessage() :
                                            "Something went wrong"
                            );
                        }
                    }

                    private void createResponseInternalServerError(HttpResponse response, Throwable e) {
                        response.setRawStatusCode(500);
                        response.setStatus("Internal Server Error");
                        response.addHeader("Content-Type", ContentType.APPLICATION_JSON_VALUE);
                        if (e != null) {
                            response.setBody(
                                    e.getLocalizedMessage() != null && !e.getLocalizedMessage().isBlank() ?
                                            e.getLocalizedMessage() :
                                            "Something went wrong"
                            );
                        }
                    }

                    private void createUnsupportedContentTypeError(HttpResponse response, Throwable e) {
                        response.setRawStatusCode(415);
                        response.setStatus("Unsupported Media Type");
                        response.addHeader("Content-Type", ContentType.APPLICATION_JSON_VALUE);
                        if (e != null) {
                            response.setBody(
                                    e.getLocalizedMessage() != null && !e.getLocalizedMessage().isBlank() ?
                                            e.getLocalizedMessage() :
                                            "Something went wrong"
                            );
                        }
                    }

                    private void createMethodNotAllowedError(HttpResponse response, Throwable e) {
                        response.setRawStatusCode(405);
                        response.setStatus("Method Not Allowed");
                        response.addHeader("Content-Type", ContentType.APPLICATION_JSON_VALUE);
                        if (e != null) {
                            response.setBody(
                                    e.getLocalizedMessage() != null && !e.getLocalizedMessage().isBlank() ?
                                            e.getLocalizedMessage() :
                                            "Something went wrong"
                            );
                        }
                    }

                    private void createResponseDependsOnException(Throwable e, HttpResponse response) {
                        Throwable ex = e.getCause();

                        if (ex.getClass().isAssignableFrom(PathNotFoundException.class)) {
                            this.createResponseNotFound(response, ex);
                        } else if (ex.getClass().isAssignableFrom(UnsupportedHttpMethodException.class)) {
                            this.createMethodNotAllowedError(response, ex);
                        } else if (ex.getClass().isAssignableFrom(UnsupportedContentTypeException.class)) {
                            this.createUnsupportedContentTypeError(response, e);
                        } else {
                            this.createResponseInternalServerError(response, ex);
                        }
                    }
                });
            }
        }, EXECUTOR_SERVICE);
    }
}
