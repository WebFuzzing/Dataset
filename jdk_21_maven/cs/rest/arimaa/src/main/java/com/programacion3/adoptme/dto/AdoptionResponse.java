package com.programacion3.adoptme.dto;

import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AdoptionResponse {
    private String adopterId;  
    private String dogId;      
    private String message;    
}
