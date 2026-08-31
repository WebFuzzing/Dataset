package com.programacion3.adoptme.dto;

import java.util.List;

public record PathResponse(
        boolean exists,
        String method,
        List<String> path,
        int steps,
        double totalWeight
) {}
