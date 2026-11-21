package com.arcane.dam.service;

import com.arcane.dam.dto.AssetResponseDTO;
import com.arcane.dam.entity.AssetMetaData;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@Service
public interface AssetService {
    List<AssetResponseDTO> uploadFiles(List<MultipartFile> multipartFiles, String spaceId, String userId);
    byte[] downloadFile(String fileId);
    AssetResponseDTO updateFile(String fileId, MultipartFile multipartFile);
    boolean deleteFile(String fileId);
    List<AssetResponseDTO> getAssetsBySpaceId(String spaceId);
}
