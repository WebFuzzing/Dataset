package com.example.gdprkv.access;

import com.example.gdprkv.models.Subject;
import java.util.Optional;

public interface SubjectAccess {

    Optional<Subject> findBySubjectId(String subjectId);

    /**
     * Creates a new subject. Will fail if subject already exists.
     */
    Subject save(Subject subject);

    /**
     * Updates an existing subject. Will fail if subject does not exist.
     */
    Subject update(Subject subject);
}
