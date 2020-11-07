package kz.danke.http.server;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.xml.XmlMapper;
import kz.danke.http.server.annotation.MethodHandler;
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
import java.util.concurrent.Future;

public class Server {
    private static final int BUFFER_SIZE = 1024;
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
    private static final XmlMapper XML_MAPPER = new XmlMapper();

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
                handleClient(channelFuture);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void handleClient(Future<AsynchronousSocketChannel> channelFuture) throws InterruptedException, java.util.concurrent.ExecutionException, java.util.concurrent.TimeoutException, IOException {
        AsynchronousSocketChannel clientChannel = channelFuture.get();

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
            MethodObject handler = this.httpFactory.getHandler(requestMethodPath);

            if (handler != null) {
                try {
                    Method e = handler.getMethod();
                    Object t = handler.getObject();

                    Object invoke = e.invoke(t);

                    MethodHandler annotation = e.getAnnotation(MethodHandler.class);

                    switch (annotation.produces()) {
                        case TEXT_PLAIN -> response.setBody((String) invoke);
                        case APPLICATION_XML -> response.setBody(XML_MAPPER.writeValueAsString(invoke));
                        case APPLICATION_JSON -> response.setBody(OBJECT_MAPPER.writeValueAsString(invoke));
                    }
                    response.addHeader("Content-Type", annotation.produces().getValue());
                } catch (Exception e) {
                    e.printStackTrace();

                    createResponseInternalServerError(response, e);
                }
            } else {
                createResponseNotFound(response);
            }
            ByteBuffer resp = ByteBuffer.wrap(response.getBytes());

            clientChannel.write(resp);

            clientChannel.close();
        }
    }

    private void createResponseNotFound(HttpResponse response) {
        response.setRawStatusCode(400);
        response.setStatus("Not found");
        response.addHeader("Content-Type", "text/html; charset=utf-8");
        response.setBody("<html><body><h1>Resource not found</h1></body></html>");
    }

    private void createResponseInternalServerError(HttpResponse response, Exception e) {
        response.setRawStatusCode(500);
        response.setStatus("Internal server error");
        response.addHeader("Content-Type", ContentType.APPLICATION_JSON.getValue());
        response.setBody(e.toString());
    }
}
