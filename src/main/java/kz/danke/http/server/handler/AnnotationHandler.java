package kz.danke.http.server.handler;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import kz.danke.http.server.annotation.*;
import kz.danke.http.server.http.ContentType;
import kz.danke.http.server.http.HttpMethod;
import kz.danke.http.server.http.HttpRequest;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@WebHandler(path = "/books")
public class AnnotationHandler {

    @MethodHandler(produces = ContentType.APPLICATION_JSON_VALUE)
    public List<Book> getBooks() {
        return Arrays.asList(
            new Book(1, "first"),
            new Book(2, "second"),
            new Book(3, "third")
        );
    }

    @MethodHandler(path = "/#id", produces = ContentType.APPLICATION_JSON_VALUE)
    public Book getBookById(@MethodVariable(name = "id") String id) {
        return new Book(Integer.parseInt(id), "Book by id");
    }

    @MethodHandler(path = "/param", produces = ContentType.APPLICATION_JSON_VALUE, method = HttpMethod.GET)
    public String getParam(@MethodParam(name = "key") String key) {
        return key;
    }

    @MethodHandler(path = "/post", consumes = ContentType.APPLICATION_JSON_VALUE, produces = ContentType.APPLICATION_JSON_VALUE, method = HttpMethod.POST)
    public Book postBook(@MethodBody Book book) {
        return book;
    }

    @MethodHandler(path = "/multi-param/#name", produces = ContentType.APPLICATION_JSON_VALUE, method = HttpMethod.POST)
    public Map<String, String> getMultiParam(@MethodVariable(name = "name") String name,
                                             String boom,
                                             @MethodBody Book book,
                                             @MethodParam(name = "key") String value,
                                             HttpRequest request) {
        System.out.println(boom);
        return new HashMap<>() {{
            put("path variable", name);
            put("book", book.toString());
            put("param", value);
            put("request", request.getUri());
        }};
    }

    @MethodHandler(path = "/#id/book/#name", produces = ContentType.APPLICATION_JSON_VALUE, method = HttpMethod.GET)
    public Map<String, String> producing(
            @MethodVariable(name = "id") String id,
            @MethodVariable(name = "name") String name
    ) {
        return new HashMap<>() {{
            put("id", id);
            put("name", name);
        }};
    }
}
