package kz.danke.http.server.handler;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import kz.danke.http.server.annotation.*;
import kz.danke.http.server.http.ContentType;
import kz.danke.http.server.http.HttpMethod;

import java.util.Arrays;
import java.util.List;

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

    @MethodHandler(path = "/post", produces = ContentType.APPLICATION_JSON_VALUE, method = HttpMethod.POST)
    public Book postBook(@MethodBody Book book) throws JsonProcessingException {
        return book;
    }
}
