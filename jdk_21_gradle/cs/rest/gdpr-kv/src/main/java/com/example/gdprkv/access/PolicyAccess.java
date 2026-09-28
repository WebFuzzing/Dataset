package com.example.gdprkv.access;

import com.example.gdprkv.models.Policy;
import java.util.Optional;

public interface PolicyAccess {
    Optional<Policy> findByPurpose(String purpose);
}
