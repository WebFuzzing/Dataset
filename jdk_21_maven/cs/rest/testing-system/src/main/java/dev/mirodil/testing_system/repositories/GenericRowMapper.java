package dev.mirodil.testing_system.repositories;

import org.springframework.jdbc.core.RowMapper;

import java.sql.ResultSet;
import java.sql.SQLException;

public class GenericRowMapper<T> implements RowMapper<T> {

    private final ResultSetExtractor<T> extractor;

    public GenericRowMapper(ResultSetExtractor<T> extractor) {
        this.extractor = extractor;
    }

    @Override
    public T mapRow(ResultSet rs, int rowNum) throws SQLException {
        return extractor.extractData(rs);
    }

    @FunctionalInterface
    public interface ResultSetExtractor<T> {
        T extractData(ResultSet rs) throws SQLException;
    }
}
