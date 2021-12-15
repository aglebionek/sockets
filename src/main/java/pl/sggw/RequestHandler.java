package pl.sggw;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.HashMap;
import java.util.concurrent.ConcurrentHashMap;

import pl.sggw.book.Book;

public class RequestHandler {
    private static final String REQUEST_METHOD_HEADER_KEY = "Method";
    private static final String REQUEST_URL_HEADER_KEY = "Url";
    private static final String REQUEST_CONTENT_LENGTH_HEADER_KEY = "Content-Length";
    private static final String REQUEST_BODY_KEY = "Body";
    private static final Charset CHARSET = StandardCharsets.UTF_8;

    public static HashMap<String, String> returnRequestHeaders(InputStream inputStream) throws IOException {
        HashMap<String, String> result = new HashMap<String, String>();
        BufferedReader in = new BufferedReader(new InputStreamReader(inputStream));

        // Get request headers and turn them into a hashmap
        String line = in.readLine();
        String[] methodAndUrl = new String[3];

        try {
            methodAndUrl = line.split(" ");
        } catch (Exception e) {
            return result;
        }
        result.put(REQUEST_METHOD_HEADER_KEY, methodAndUrl[0]);
        result.put(REQUEST_URL_HEADER_KEY, methodAndUrl[1].substring(1, methodAndUrl[1].length()));
        while (!(line = in.readLine()).equals("")) {
            String[] keyValuePair = line.split(":", 2);
            result.put(keyValuePair[0].trim(), keyValuePair[1].trim());
        }

        // If it's a post request and a body is present, add it to the resulting hashmap
        if (result.get(REQUEST_METHOD_HEADER_KEY).toLowerCase().equals("post")) {
            StringBuilder body = new StringBuilder();
            int c = 0;
            for (int i = 0; i < Integer.parseInt(result.get(REQUEST_CONTENT_LENGTH_HEADER_KEY)); i++) {
                c = in.read();
                body.append((char) c);
            }
            result.put(REQUEST_BODY_KEY, body.toString());
        }

        return result;
    }

    public static HashMap<String, String> returnRequestBody(String body) throws IOException {
        HashMap<String, String> requestBody = new HashMap<String, String>();
        if (body != null) {
            final String[] rawRequestParameters = body.split("[&]");
            for (final String rawRequestParameter : rawRequestParameters) {
                final String[] requestParameter = rawRequestParameter.split("=", 2);
                final String requestParameterName = decodeUrlComponent(requestParameter[0]);
                final String requestParameterValue = requestParameter.length > 1
                        ? decodeUrlComponent(requestParameter[1])
                        : null;
                requestBody.put(requestParameterName, requestParameterValue);
            }
        }
        return requestBody;
    }

    private static String decodeUrlComponent(final String urlComponent) {
        try {
            return URLDecoder.decode(urlComponent, CHARSET.name());
        } catch (final UnsupportedEncodingException ex) {
            throw new InternalError(ex);
        }
    }

    static class Post {
        public static String returnViewForRoute(String route, HashMap<String, String> requestBody,
                ConcurrentHashMap<Integer, Book> booksDictionary) throws IOException {
            String view = ViewsReader.defaultView();
            if (route.isEmpty()) {
                return view;
            } else if (route.equals("addBookAction")) {
                try {
                    int highestBookId = Collections.max(booksDictionary.keySet());
                    Book book = new Book(requestBody.get("title"), requestBody.get("authorName"),
                            requestBody.get("authorSurname"));
                    booksDictionary.put(highestBookId + 1, book);
                } catch (Exception e) {
                    e.printStackTrace();
                }
                // view = Get.returnViewForRoute("books.html", booksDictionary);
            } else if (route.equals("clearBooksAction")) {
                booksDictionary.clear();
                view = Get.returnViewForRoute("books.html", booksDictionary);
            } else if (route.equals("updateBookAction")) {
                Book book = new Book(requestBody.get("title"), requestBody.get("authorName"),
                        requestBody.get("authorSurname"));
                booksDictionary.put(Integer.parseInt(requestBody.get("id")), book);
            }
            return view;
        }

    }

    static class Get {
        public static String returnViewForRoute(String route, ConcurrentHashMap<Integer, Book> booksDictionary)
                throws IOException {
            String view = ViewsReader.defaultView();
            if (route.isEmpty())
                return view;
            else {
                try {
                    view = ViewsReader.fromFile(route.split("\\?", 2)[0]);
                } catch (Exception e) {
                    System.out.println(route);
                    e.printStackTrace();
                    System.out.println("No such view, returning default view.");
                    return view;
                }

                if (route.equals("books.html")) {
                    if (booksDictionary.isEmpty()) {
                        view = String.format(view, "<p>Brak książek</p>");
                    }
                    String booksAsHtml = PrintingDatabase.databaseToHtml(booksDictionary);
                    view = String.format(view, booksAsHtml);
                } else if (route.startsWith("updateBook.html")) {
                    String url = route.split("\\?", 2)[1];
                    HashMap<String, String> parameters = urlParameters(url);
                    if (parameters.containsKey("id")) {
                        Book book = booksDictionary.get(Integer.parseInt(parameters.get("id")));
                        view = ViewsReader.fromFile("updateBook.html");
                        view = String.format(view, parameters.get("id"), book.getTitle(), book.getAuthorName(),
                                book.getAuthorSurname());
                    }
                }
            }
            return view;
        }

    }

    public static HashMap<String, String> urlParameters(String url) throws IOException {
        return returnRequestBody(url);
    }
}
