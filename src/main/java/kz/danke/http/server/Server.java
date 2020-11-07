package kz.danke.http.server;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.xml.XmlMapper;
import kz.danke.http.server.annotation.MethodHandler;
import kz.danke.http.server.exception.PathNotFoundException;
import kz.danke.http.server.exception.UnsupportedContentTypeException;
import kz.danke.http.server.factory.HttpAnnotationHandlerFactory;
import kz.danke.http.server.http.ContentType;
import kz.danke.http.server.http.HttpRequest;
import kz.danke.http.server.http.HttpResponse;
import kz.danke.http.server.tuples.MethodObject;

import java.io.IOException;
import java.lang.reflect.Method;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.channels.AsynchronousServerSocketChannel;
import java.nio.channels.AsynchronousSocketChannel;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.*;

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
                Future<AsynchronousSocketChannel> channelFuture = server.accept();
                AsynchronousSocketChannel channel = channelFuture.join();
                EXECUTOR_SERVICE.execute(() -> {
                    try {
                        handleClient(channel);
                    } catch (Exception e) {
                        throw new RuntimeException(e);
                    }
                });
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void handleClient(AsynchronousSocketChannel clientChannel) throws InterruptedException, java.util.concurrent.ExecutionException, java.util.concurrent.TimeoutException, IOException {
        while (clientChannel != null && clientChannel.isOpen()) {
            ByteBuffer buffer = ByteBuffer.allocate(BUFFER_SIZE);
            StringBuffer stringBuffer = new StringBuffer();
            boolean keepReading = true;

            while (keepReading) {
                int readResult = clientChannel.read(buffer).get();

                keepReading = readResult == BUFFER_SIZE;

                buffer.flip();

                CharBuffer charBuffer = StandardCharsets.UTF_8.decode(buffer);

                stringBuffer.append(charBuffer);

                buffer.clear();
            }
            if (stringBuffer.isEmpty()) {
                break;
            }
            HttpRequest request = new HttpRequest(stringBuffer.toString());
            HttpResponse response = new HttpResponse();

            String requestMethodPath = String.format("%s %s", request.getUri(), request.getMethod().name());

            if (requestMethodPath.contains("ico")) {
                break;
            }
            try {
                MethodObject handler = this.httpFactory.getHandler(requestMethodPath);

                Method e = handler.getMethod();
                Object t = handler.getObject();

                MethodHandler annotation = e.getAnnotation(MethodHandler.class);
                if (request.getHeaders().get("Content-Type") == null && !annotation.consumes().isBlank()) {
                    throw new UnsupportedContentTypeException();
                } else if (request.getHeaders().get("Content-Type") != null &&
                        !request.getHeaders().get("Content-Type").equalsIgnoreCase(annotation.consumes()) &&
                        !annotation.consumes().isBlank()) {
                    throw new UnsupportedContentTypeException();
                }
                Object invoke = e.invoke(t);

                switch (annotation.produces()) {
                    case ContentType.TEXT_PLAIN_VALUE -> response.setBody((String) invoke);
                    case ContentType.APPLICATION_XML_VALUE -> response.setBody(XML_MAPPER.writeValueAsString(invoke));
                    case ContentType.APPLICATION_JSON_VALUE -> response.setBody(OBJECT_MAPPER.writeValueAsString(invoke));
                }
                response.addHeader("Content-Type", annotation.produces());
            } catch (PathNotFoundException e) {
                createResponseNotFound(response);
            } catch (UnsupportedContentTypeException e) {
                createUnsupportedContentTypeError(response);
            } catch (Exception e) {
                createResponseInternalServerError(response, e);
            }
            ByteBuffer resp = ByteBuffer.wrap(response.getBytes());

            clientChannel.write(resp);

            clientChannel.close();
        }
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
}
