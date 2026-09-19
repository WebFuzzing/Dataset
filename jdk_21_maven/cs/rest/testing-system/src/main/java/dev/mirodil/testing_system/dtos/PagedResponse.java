package dev.mirodil.testing_system.dtos;

import org.springframework.data.domain.Page;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class PagedResponse<T> {
    private final int size;
    private final int pageNumber;
    private final long totalElements;
    private final int totalPages;

    private final List<T> content;
    private final Map<String, Object> page;
    private final Map<String, String> links;

    public PagedResponse(Page<T> pageData, String fullUrl) {
        this.content = pageData.getContent();
        this.pageNumber = pageData.getNumber();
        this.size = pageData.getSize();
        this.totalElements = pageData.getTotalElements();
        this.totalPages = pageData.getTotalPages();
        this.links = generateLinks(fullUrl, pageData);
        page = generatePage();
    }

    private Map<String, Object> generatePage() {
        Map<String, Object> page = new HashMap<>(4);
        page.put("size", size);
        page.put("number", pageNumber);
        page.put("totalElements", totalElements);
        page.put("totalPages", totalPages);

        return page;
    }

    private Map<String, String> generateLinks(String fullUrl, Page<T> pageData) {
        Map<String, String> links = new HashMap<>();

        int currentPage = pageData.getNumber();
        int totalPages = pageData.getTotalPages();

        // Remove the existing page parameter from the URL
        String baseUrl = fullUrl.replaceAll("[&?]page=\\d+", "");

        if (baseUrl.contains("?")) {
            baseUrl = baseUrl.endsWith("?") || baseUrl.endsWith("&") ? baseUrl.substring(0, baseUrl.length() - 1) : baseUrl;
        }

        // Append page parameters dynamically
        String self = baseUrl + (baseUrl.contains("?") ? "&" : "?") + "page=" + currentPage;
        String first = baseUrl + (baseUrl.contains("?") ? "&" : "?") + "page=0";
        String last = baseUrl + (baseUrl.contains("?") ? "&" : "?") + "page=" + (totalPages - 1);

        links.put("self", self);
        links.put("first", first);
        links.put("last", last);

        if (pageData.hasNext()) {
            String next = baseUrl + (baseUrl.contains("?") ? "&" : "?") + "page=" + (currentPage + 1);
            links.put("next", next);
        }

        if (pageData.hasPrevious()) {
            String previous = baseUrl + (baseUrl.contains("?") ? "&" : "?") + "page=" + (currentPage - 1);
            links.put("previous", previous);
        }

        return links;
    }

    public List<T> getContent() {
        return content;
    }

    public Map<String, Object> getPage() {
        return page;
    }

    public Map<String, String> getLinks() {
        return links;
    }
}