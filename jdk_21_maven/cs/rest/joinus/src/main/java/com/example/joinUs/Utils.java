package com.example.joinUs;

import com.example.joinUs.model.mongodb.User;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.server.ResponseStatusException;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;


public class Utils {

    public static <T> boolean isNullOrEmpty(T object) {
        if (object == null) return true;

        else if (object instanceof String str) {
            return str.isEmpty() || str.isBlank();
        } else if (object instanceof Collection<?>) {
            Collection collection = (Collection) object;
            return collection.isEmpty();
        }

        return false;
    }

    public static User getUserFromContext() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "User is not authenticated in the application");
        }
        return (User) authentication.getPrincipal();
    }

    public static void checkPropertiesForCreateUpdate(Object... objects) {
        for (Object object : objects) {
            if (Utils.isNullOrEmpty(object)) {
                throw new ResponseStatusException(HttpStatus.CONFLICT, "property not  provided");
            }
        }
    }

    private static <T> Iterable<T> intersectionOf2(Iterable<T> first, Iterable<T> second) {

        ArrayList<T> result = new ArrayList<>();
        Iterator<T> firstIterator = first.iterator();
        while (firstIterator.hasNext()) {
            T firstElement = firstIterator.next();
            Iterator<T> secondIterator = second.iterator();
            while (secondIterator.hasNext()) {
                T secondElement = secondIterator.next();
                if (secondElement == firstElement) {
                    result.add(firstElement);
                    break;
                }
            }
        }

        return result;
    }

    public static <T> ArrayList<T> intersection(Iterable<T>... iterable) {

        Iterable<T> result = iterable[0];
        ArrayList<T> list = new ArrayList<>();

        for (int i = 1; i < iterable.length; i++) {
            result = intersectionOf2(result, iterable[i]);
        }
        Iterator<T> iterator = result.iterator();
        while (iterator.hasNext()) list.add(iterator.next());

        return list;
    }

    public static String getEventProjection() {
        return "{'event_id':1,'event_name':1,'venue.city.name':1,'member_count':1,'creator_group':1,'event_time':1,'fee.amount':1}";
    }

    public static String getGroupProjection() {
        return "{'group_id':1,'group_name':1,'upcoming_events':1,'city.name':1','category.name':1,'member_count':1,'event_count':1}";
    }

    public static <T, R> R wrapFunction(Function<T, R> method, T input) {
        try {
            return method.apply(input);
        } catch (Exception e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, e.getMessage());
        }
    }

    public static <T> T wrapFunction(Supplier<T> method) {
        try {
            return method.get();
        } catch (Exception e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, e.getMessage());
        }
    }

    public static <T> void wrapFunction(Consumer<T> method, T input) {
        try {
            method.accept(input);
        } catch (Exception e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, e.getMessage());
        }
    }

    public static <T, U, R> R wrapFunction(BiFunction<T, U, R> method, T firstArg, U secondArg) {
        try {
            return method.apply(firstArg, secondArg);
        } catch (Exception e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, e.getMessage());
        }
    }

}
