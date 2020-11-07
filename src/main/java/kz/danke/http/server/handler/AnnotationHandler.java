package kz.danke.http.server.handler;

import kz.danke.http.server.annotation.MethodHandler;
import kz.danke.http.server.annotation.WebHandler;
import kz.danke.http.server.http.ContentType;
import kz.danke.http.server.http.HttpMethod;

@WebHandler
public class AnnotationHandler {

    @MethodHandler(path = "/get", method = HttpMethod.GET, produces = ContentType.APPLICATION_XML)
    public String getHandler() {
        return "Get string";
    }

    @MethodHandler(path = "/post", method = HttpMethod.POST, produces = ContentType.APPLICATION_JSON)
    public String postHandler() {
        return "Post string";
    }
}
