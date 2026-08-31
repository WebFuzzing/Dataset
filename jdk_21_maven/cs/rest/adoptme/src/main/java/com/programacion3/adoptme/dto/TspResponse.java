package com.programacion3.adoptme.dto;

import lombok.*;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TspResponse {
    private List<String> route;     
    private Integer totalDistanceKm; 
}
