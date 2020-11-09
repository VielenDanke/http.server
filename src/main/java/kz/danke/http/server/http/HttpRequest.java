package kz.danke.http.server.http;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class HttpRequest {
    private static final String DELIMITER = "\r\n\r\n";
    private static final String NEW_LINE = "\r\n";
    private static final String HEADER_DELIMITER = ":";

    private final String message;
    private final HttpMethod method;
    private final String uri;
    private final Map<String, String> headers = new ConcurrentHashMap<>();
    private final String body;

    public HttpRequest(StringBuffer message) {
        this(message.toString());
    }

    public HttpRequest(String message) {
        this.message = message;

        String[] parts = message.split(DELIMITER);

        String head = parts[0];

        String[] headers = head.split(NEW_LINE);

        String[] firstLine = headers[0].split(" ");

        this.method = HttpMethod.valueOf(firstLine[0]);
        this.uri = firstLine[1];

        for (int i = 1; i < headers.length; i++) {
            String[] headerPart = headers[i].split(HEADER_DELIMITER, 2);
            this.headers.put(headerPart[0].trim(), headerPart[1].trim());
        }

        String bodyLength = this.headers.get("Content-Length");
        int contentLength = bodyLength != null ? Integer.parseInt(bodyLength) : 0;
        this.body = parts.length > 1 ? parts[1].trim().substring(0, contentLength) : "";
    }

    public String getMessage() {
        return message;
    }

    public HttpMethod getMethod() {
        return method;
    }

    public String getUri() {
        return uri;
    }

    public Map<String, String> getHeaders() {
        return headers;
    }

    public String getBody() {
        return body;
    }
}
