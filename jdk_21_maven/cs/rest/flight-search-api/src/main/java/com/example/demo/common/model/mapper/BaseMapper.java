package com.example.demo.common.model.mapper;

import java.util.Collection;
import java.util.List;

/**
 * A generic interface for mapping between two types, {@code S} (source) and {@code T} (target).
 * This interface provides methods to map a single object or a collection of objects from type {@code S}
 * to type {@code T}.
 *
 * @param <S> the source type
 * @param <T> the target type
 */
public interface BaseMapper<S, T> {

    /**
     * Maps a single object of type {@code S} to an object of type {@code T}.
     *
     * @param source the source object to be mapped
     * @return the mapped target object of type {@code T}
     */
    T map(S source);

    /**
     * Maps a collection of objects of type {@code S} to a list of objects of type {@code T}.
     *
     * @param sources the collection of source objects to be mapped
     * @return a list of mapped target objects of type {@code T}
     */
    List<T> map(Collection<S> sources);

}
