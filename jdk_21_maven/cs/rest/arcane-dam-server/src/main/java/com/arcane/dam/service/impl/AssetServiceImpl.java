package com.arcane.dam.service.impl;

import com.arcane.dam.dto.AssetResponseDTO;
import com.arcane.dam.entity.AssetMetaData;
import com.arcane.dam.entity.Users;
import com.arcane.dam.exception.CustomException;
import com.arcane.dam.repository.AssetRepository;
import com.arcane.dam.repository.UserRepository;
import com.arcane.dam.service.AssetService;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.core.ResponseBytes;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.S3ClientBuilder;
import software.amazon.awssdk.services.s3.model.*;

import java.io.IOException;
import java.net.URI;
import java.time.Instant;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;


@Service
@RequiredArgsConstructor
@Slf4j
public class AssetServiceImpl implements AssetService {
    private final UserRepository userRepository;
    @Value("${aws.s3.bucket-name}")
    private String bucketName;

    @Value("${aws.access_key}")
    private String accessKey;

    @Value("${aws.secret_key}")
    private String secretKey;

    // MODIFIED: allow overriding S3 endpoint for LocalStack
    @Value("${aws.endpoint:}")
    private String endpoint;

    @Value("${aws.region:us-east-1}")
    private String region;

    private S3Client s3Client;
    private final AssetRepository assetRepository;
    private final ModelMapper mapper;

    @PostConstruct
    private void initialize() {
        AwsBasicCredentials awsCredentials = AwsBasicCredentials.create(accessKey, secretKey);
        S3ClientBuilder builder = S3Client.builder()
                .credentialsProvider(StaticCredentialsProvider.create(awsCredentials))
                .region(software.amazon.awssdk.regions.Region.of(region));
        if (endpoint != null && !endpoint.isBlank()) {
            builder.endpointOverride(URI.create(endpoint)).forcePathStyle(true);
        }
        s3Client = builder.build();
    }

    @Override
    public List<AssetResponseDTO> uploadFiles(List<MultipartFile> multipartFiles, String spaceId, String userId) {
        DateTimeFormatter dateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
        String todayDate = dateTimeFormatter.format(LocalDate.now());

        List<AssetResponseDTO> responses = new ArrayList<>();

        for (MultipartFile multipartFile : multipartFiles) {
            if (multipartFile.isEmpty()) {
                throw new CustomException("File is empty", HttpStatus.BAD_REQUEST);
            }

            String filePath = todayDate + "/" + multipartFile.getOriginalFilename();
            String fileId = UUID.randomUUID().toString();

            try {
                PutObjectRequest putObjectRequest = PutObjectRequest.builder()
                        .bucket(bucketName)
                        .key(filePath)
                        .contentType(multipartFile.getContentType())
                        .contentLength(multipartFile.getSize())
                        .build();

                s3Client.putObject(putObjectRequest, RequestBody.fromInputStream(multipartFile.getInputStream(), multipartFile.getSize()));

                String fileUrl = String.format("https://%s.s3.amazonaws.com/%s", bucketName, filePath);

                Users user = userRepository.getUsersById(userId);
                if (user == null) {
                    throw new CustomException("User not found", HttpStatus.NOT_FOUND);
                }

                // Save metadata to DynamoDB
                AssetMetaData metadata = new AssetMetaData(
                        fileId,
                        multipartFile.getOriginalFilename(),
                        multipartFile.getContentType(),
                        multipartFile.getSize(),
                        fileUrl,
                        Instant.now(),
                        spaceId,
                        user.getId()
                );
                assetRepository.save(metadata);

                AssetResponseDTO response = mapper.map(metadata, AssetResponseDTO.class);
                responses.add(response);

            } catch (IOException e) {
                log.error("Error occurred ==> {}", e.getMessage());
                throw new CustomException("Error occurred in file upload: " + e.getMessage(), HttpStatus.BAD_REQUEST);
            }
        }

        return responses;
    }


    @Override
    public byte[] downloadFile(String fileId) {
        AssetMetaData assetMetaData = assetRepository.findById(fileId)
                .orElseThrow(() -> new CustomException("File not found", HttpStatus.NOT_FOUND));

        String fileUrl = assetMetaData.getFileUrl();
        if (fileUrl == null || fileUrl.isEmpty()) {
            throw new CustomException("File URL is empty", HttpStatus.BAD_REQUEST);
        }

        String filePath = fileUrl.replace("https://arcane-bucket.s3.amazonaws.com/", "");

        if (filePath.isEmpty()) {
            throw new CustomException("File path is empty", HttpStatus.BAD_REQUEST);
        }

        GetObjectRequest getObjectRequest = GetObjectRequest.builder()
                .bucket(bucketName)
                .key(filePath)
                .build();

        ResponseBytes<GetObjectResponse> objectBytes = s3Client.getObjectAsBytes(getObjectRequest);

        return objectBytes.asByteArray();
    }


    @Override
    public AssetResponseDTO updateFile(String fileId, MultipartFile multipartFile) {
        AssetMetaData assetMetaData = assetRepository.findById(fileId)
                .orElseThrow(() -> new CustomException("File not found", HttpStatus.NOT_FOUND));

        DateTimeFormatter dateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
        String todayDate = dateTimeFormatter.format(LocalDate.now());
        String filePath = todayDate + "/" + multipartFile.getOriginalFilename();

        try {
            PutObjectRequest putObjectRequest = PutObjectRequest.builder()
                    .bucket(bucketName)
                    .key(filePath)
                    .contentType(multipartFile.getContentType())
                    .contentLength(multipartFile.getSize())
                    .build();

            s3Client.putObject(putObjectRequest, RequestBody.fromInputStream(multipartFile.getInputStream(), multipartFile.getSize()));

            String fileUrl = String.format("https://%s.s3.amazonaws.com/%s", bucketName, filePath);

            // Update metadata in DynamoDB
            assetMetaData.setFileName(multipartFile.getOriginalFilename());
            assetMetaData.setFileUrl(fileUrl);
            assetMetaData.setFileSize(multipartFile.getSize());
            assetMetaData.setUploadTimestamp(Instant.now());

            if(assetRepository.update(assetMetaData.getId(),assetMetaData)){
                return mapper.map(assetMetaData, AssetResponseDTO.class);
            }

        } catch (IOException e) {
            throw new CustomException("Error occurred in file upload: " + e.getMessage(), HttpStatus.BAD_REQUEST);
        }
        throw new CustomException("Error occurred in file upload", HttpStatus.BAD_REQUEST);
    }

    @Override
    public boolean deleteFile(String fileId) {
        AssetMetaData assetMetaData = assetRepository.findById(fileId)
                .orElseThrow(() -> new CustomException("File not found", HttpStatus.NOT_FOUND));

        String filePath = assetMetaData.getFileUrl().replace("https://arcane-bucket.s3.amazonaws.com/", "");

        try {
            DeleteObjectRequest deleteObjectRequest = DeleteObjectRequest.builder()
                    .bucket(bucketName)
                    .key(filePath)
                    .build();

            s3Client.deleteObject(deleteObjectRequest);

            return assetRepository.delete(assetMetaData);

        } catch (Exception e) {
            throw new CustomException("Error occurred while deleting the file: " + e.getMessage(), HttpStatus.BAD_REQUEST);
        }
    }

    @Override
    public List<AssetResponseDTO> getAssetsBySpaceId(String spaceId) {
        if (spaceId == null || spaceId.isEmpty()) {
            throw new CustomException("Space ID cannot be null or empty", HttpStatus.BAD_REQUEST);
        }
        List<AssetMetaData> assets = assetRepository.findBySpaceId(spaceId);

        if (assets == null || assets.isEmpty()) {
            throw new CustomException("No assets found for the provided Space ID", HttpStatus.NOT_FOUND);
        }

        return assets.stream()
                .map(metaData -> mapper.map(metaData, AssetResponseDTO.class))
                .collect(Collectors.toList());

    }

}
