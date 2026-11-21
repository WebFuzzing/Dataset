package it.pagopa.pn.mandate.mapper;

public interface BaseMapperInterface<T,S> {
    S toEntity(T source);
    T toDto(S source);
}
