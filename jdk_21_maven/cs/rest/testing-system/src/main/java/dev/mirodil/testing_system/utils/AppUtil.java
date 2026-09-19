package dev.mirodil.testing_system.utils;

import jakarta.servlet.http.HttpServletRequest;

public class AppUtil {
    public static String getUrlWithQueryParams(HttpServletRequest request) {
        String fullUrl = request.getRequestURL().toString();
        String queryParams = request.getQueryString();
        if (queryParams != null) {
            fullUrl += "?" + queryParams;
        }
        return fullUrl;
    }
}
