package kz.danke.http.server.http;

public enum ContentType {

    APPLICATION_JSON("application/json"),
    TEXT_PLAIN("text/plain"),
    APPLICATION_XML("application/xml");

    private final String value;

    ContentType(String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }
}
