package dev.mirodil.testing_system.utils;

import java.util.List;

public record FilterCriteria<T>(String attribute, List<String> operators, List<T> values) {

    @Override
    public String toString() {
        return attribute + " " + operators + " " + values;
    }
}