package org.movies.xml.controller;

import org.movies.xml.dto.MovieDto;
import org.springframework.core.MethodParameter;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpInputMessage;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.servlet.mvc.method.annotation.RequestBodyAdvice;

import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Type;
import java.util.ArrayDeque;
import java.util.Arrays;
import java.util.Collections;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

/**
 * Enforces the single accepted shape of an incoming movie document before Jackson
 * gets to bind it.
 *
 * <p>Jackson makes no distinction between an attribute and an element on the way in,
 * so on its own it would happily accept a document that spells id or year as elements,
 * which is exactly the mistake this case study needs to catch. This advice walks the
 * raw document first and rejects anything that is not the one documented form.
 *
 * <p>As a side effect it records which names were literally present, which is what
 * lets PATCH tell an absent cast apart from an empty one.
 */
@ControllerAdvice
public class StrictXmlRequestBodyAdvice implements RequestBodyAdvice {

    /** What every known element is allowed to carry. */
    private record ElementSpec(Set<String> attributes, Set<String> children, Set<String> repeatableChildren) {

        static ElementSpec leaf() {
            return new ElementSpec(Set.of(), Set.of(), Set.of());
        }
    }

    private static final String ROOT = "movie";

    private static final Map<String, ElementSpec> SHAPE = shape();

    private static Map<String, ElementSpec> shape() {
        Map<String, ElementSpec> shape = new HashMap<>();
        shape.put("movie", new ElementSpec(
                Set.of("id", "year"),
                Set.of("title", "genre", "rating", "director", "cast"),
                Set.of()));
        shape.put("director", new ElementSpec(
                Set.of("nationality"),
                Set.of("name"),
                Set.of()));
        shape.put("cast", new ElementSpec(
                Set.of(),
                Set.of("actor"),
                Set.of("actor")));
        shape.put("actor", new ElementSpec(
                Set.of("billing"),
                Set.of("name"),
                Set.of()));
        for (String leaf : Arrays.asList("title", "genre", "rating", "name")) {
            shape.put(leaf, ElementSpec.leaf());
        }
        return Collections.unmodifiableMap(shape);
    }

    private static XMLInputFactory newFactory() {
        XMLInputFactory factory = XMLInputFactory.newInstance();
        factory.setProperty(XMLInputFactory.SUPPORT_DTD, Boolean.FALSE);
        factory.setProperty(XMLInputFactory.IS_SUPPORTING_EXTERNAL_ENTITIES, Boolean.FALSE);
        factory.setProperty(XMLInputFactory.IS_COALESCING, Boolean.TRUE);
        return factory;
    }

    @Override
    public boolean supports(MethodParameter methodParameter,
                            Type targetType,
                            Class<? extends HttpMessageConverter<?>> converterType) {
        return MovieDto.class.equals(targetType);
    }

    @Override
    public HttpInputMessage beforeBodyRead(HttpInputMessage inputMessage,
                                           MethodParameter parameter,
                                           Type targetType,
                                           Class<? extends HttpMessageConverter<?>> converterType) throws IOException {
        byte[] body = inputMessage.getBody().readAllBytes();
        BufferedInputMessage buffered = new BufferedInputMessage(inputMessage.getHeaders(), body);
        buffered.setPresentNames(check(buffered));
        return buffered;
    }

    @Override
    public Object afterBodyRead(Object body,
                                HttpInputMessage inputMessage,
                                MethodParameter parameter,
                                Type targetType,
                                Class<? extends HttpMessageConverter<?>> converterType) {
        if (body instanceof MovieDto dto && inputMessage instanceof BufferedInputMessage buffered) {
            dto.setPresentNames(buffered.getPresentNames());
        }
        return body;
    }

    @Override
    public Object handleEmptyBody(Object body,
                                  HttpInputMessage inputMessage,
                                  MethodParameter parameter,
                                  Type targetType,
                                  Class<? extends HttpMessageConverter<?>> converterType) {
        return body;
    }

    /**
     * Walks the document and returns the names present at movie level, both attributes
     * and child elements.
     *
     * @throws HttpMessageNotReadableException on malformed XML or on any deviation from
     *                                         the accepted shape
     */
    private Set<String> check(BufferedInputMessage message) {
        Set<String> present = new LinkedHashSet<>();
        Deque<String> path = new ArrayDeque<>();
        Map<String, Set<String>> seenChildren = new HashMap<>();
        boolean rootSeen = false;

        XMLStreamReader reader = null;
        try (InputStream in = message.getBody()) {
            reader = newFactory().createXMLStreamReader(in);
            while (reader.hasNext()) {
                int event = reader.next();
                if (event == XMLStreamConstants.START_ELEMENT) {
                    String name = localName(message, reader);
                    if (path.isEmpty()) {
                        if (rootSeen || !ROOT.equals(name)) {
                            throw shapeError(message,
                                    "the root element must be <" + ROOT + ">, found <" + name + ">");
                        }
                        rootSeen = true;
                    } else {
                        String parent = path.peek();
                        ElementSpec parentSpec = SHAPE.get(parent);
                        if (parentSpec.children().isEmpty()) {
                            throw shapeError(message,
                                    "<" + parent + "> must not contain child elements, found <" + name + ">");
                        }
                        if (!parentSpec.children().contains(name)) {
                            throw shapeError(message, "<" + name + "> is not a valid child of <" + parent + ">");
                        }
                        Set<String> seen = seenChildren.computeIfAbsent(key(path), k -> new HashSet<>());
                        if (!seen.add(name) && !parentSpec.repeatableChildren().contains(name)) {
                            throw shapeError(message,
                                    "<" + name + "> must appear at most once inside <" + parent + ">");
                        }
                        if (path.size() == 1) {
                            present.add(name);
                        }
                    }
                    checkAttributes(message, reader, name, present, path.isEmpty());
                    path.push(name);
                } else if (event == XMLStreamConstants.END_ELEMENT) {
                    seenChildren.remove(key(path));
                    path.pop();
                } else if (event == XMLStreamConstants.CHARACTERS || event == XMLStreamConstants.CDATA) {
                    String parent = path.peek();
                    if (parent != null
                            && !SHAPE.get(parent).children().isEmpty()
                            && !reader.getText().isBlank()) {
                        throw shapeError(message, "<" + parent + "> must not carry text");
                    }
                }
            }
            if (!rootSeen) {
                throw shapeError(message, "the document is empty");
            }
        } catch (XMLStreamException e) {
            throw new HttpMessageNotReadableException("malformed XML: " + rootCause(e), e, message);
        } catch (IOException e) {
            throw new HttpMessageNotReadableException("could not read the request body", e, message);
        } finally {
            closeQuietly(reader);
        }
        return present;
    }

    private void checkAttributes(BufferedInputMessage message,
                                 XMLStreamReader reader,
                                 String element,
                                 Set<String> present,
                                 boolean isRoot) {
        ElementSpec spec = SHAPE.get(element);
        for (int i = 0; i < reader.getAttributeCount(); i++) {
            String namespace = reader.getAttributeNamespace(i);
            String name = reader.getAttributeLocalName(i);
            if (namespace != null && !namespace.isEmpty()) {
                throw shapeError(message,
                        "namespaced attributes are not accepted, found " + namespace + ":" + name);
            }
            if (!spec.attributes().contains(name)) {
                throw shapeError(message, name + " is not a valid attribute of <" + element + ">");
            }
            if (isRoot) {
                present.add(name);
            }
        }
    }

    private static String localName(BufferedInputMessage message, XMLStreamReader reader) {
        String namespace = reader.getNamespaceURI();
        if (namespace != null && !namespace.isEmpty()) {
            throw shapeError(message, "namespaced elements are not accepted, found " + namespace);
        }
        return reader.getLocalName();
    }

    private static String key(Deque<String> path) {
        return String.join("/", path);
    }

    private static HttpMessageNotReadableException shapeError(BufferedInputMessage message, String detail) {
        return new HttpMessageNotReadableException(detail, null, message);
    }

    private static String rootCause(Throwable e) {
        Throwable cause = e;
        while (cause.getCause() != null) {
            cause = cause.getCause();
        }
        String message = cause.getMessage();
        return (message == null) ? cause.getClass().getSimpleName() : message.replace('\n', ' ').trim();
    }

    private static void closeQuietly(XMLStreamReader reader) {
        if (reader == null) {
            return;
        }
        try {
            reader.close();
        } catch (XMLStreamException ignored) {
            // nothing useful to do while unwinding
        }
    }

    /**
     * Lets the buffered bytes be read a second time, and carries the present names across.
     * Spring hands this same instance to afterBodyRead, so no ThreadLocal is needed: nothing
     * clears one when the converter throws in between.
     */
    private static final class BufferedInputMessage implements HttpInputMessage {

        private final HttpHeaders headers;
        private final byte[] body;
        private Set<String> presentNames = Collections.emptySet();

        BufferedInputMessage(HttpHeaders headers, byte[] body) {
            this.headers = headers;
            this.body = body;
        }

        Set<String> getPresentNames() {
            return presentNames;
        }

        void setPresentNames(Set<String> presentNames) {
            this.presentNames = presentNames;
        }

        @Override
        public InputStream getBody() {
            return new ByteArrayInputStream(body);
        }

        @Override
        public HttpHeaders getHeaders() {
            return headers;
        }
    }
}
