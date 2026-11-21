package com.arcane.dam.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class AssetResponseDTO {
    private String id;
    private String fileName;
    private String fileType;
    private Long fileSize;
    private String fileUrl;
    private Instant uploadTimestamp;
    private String spaceId;
    private String userId;
}
