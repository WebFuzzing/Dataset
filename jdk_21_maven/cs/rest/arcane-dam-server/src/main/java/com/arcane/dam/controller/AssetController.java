package com.arcane.dam.controller;

import com.arcane.dam.dto.AssetResponseDTO;
import com.arcane.dam.exception.CustomException;
import com.arcane.dam.service.AssetService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping (value = "/api/v1/asset")
@CrossOrigin
@RequiredArgsConstructor
public class AssetController {
    private final AssetService assetService;

    @PreAuthorize("hasRole('admin') or hasRole('team_member')")
    @PostMapping("/upload")
    public ResponseEntity<List<AssetResponseDTO>> uploadFiles(@RequestParam("file") List<MultipartFile> files,
                                                              @RequestParam ("spaceId") String spaceId,
                                                              @RequestParam ("userId") String userId) {
        if (files.isEmpty()) {
            throw new CustomException("No files provided", HttpStatus.BAD_REQUEST);
        }
        return new ResponseEntity<>(assetService.uploadFiles(files, spaceId, userId), HttpStatus.OK);
    }

    @PreAuthorize("hasRole('admin')")
    @GetMapping("/download/{fileId}")
    public ResponseEntity<byte[]> downloadFile(@PathVariable String fileId) {
        byte[] fileContent = assetService.downloadFile(fileId);

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=" + fileId)
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .body(fileContent);
    }

    @PreAuthorize("hasRole('admin') or hasRole('team_member')")
    @GetMapping("/view/{fileId}")
    public ResponseEntity<byte[]> viewFile(@PathVariable String fileId) {
        byte[] fileContent = assetService.downloadFile(fileId);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=" + fileId)
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .body(fileContent);
    }

    @PreAuthorize("hasRole('admin') or hasRole('team_member')")
    @PutMapping("/update/{fileId}")
    public ResponseEntity<AssetResponseDTO> updateFile(@PathVariable String fileId, @RequestBody MultipartFile file) {
        if (file.isEmpty()) {
            throw new CustomException("File is empty", HttpStatus.BAD_REQUEST);
        }
        return new ResponseEntity<>(assetService.updateFile(fileId, file),HttpStatus.OK);
    }

    @PreAuthorize("hasRole('admin') or hasRole('team_member')")
    @DeleteMapping("/{fileId}")
    public ResponseEntity<Map<String, String>> deleteFile(@PathVariable String fileId) {
        if (assetService.deleteFile(fileId)) {
            Map<String, String> response = new HashMap<>();
            response.put("message", "File deleted successfully");
            return new ResponseEntity<>(response, HttpStatus.OK);
        }
        throw new CustomException("File not found", HttpStatus.NOT_FOUND);
    }


    @PreAuthorize("hasRole('admin') or hasRole('team_member')")
    @GetMapping("/space/{spaceId}")
    public ResponseEntity<List<AssetResponseDTO>> getAssetsBySpaceId(@PathVariable String spaceId) {
        return new ResponseEntity<>(assetService.getAssetsBySpaceId(spaceId), HttpStatus.OK);
    }
}
