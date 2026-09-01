package com.example.arimaabackend.services.mongo;

import com.example.arimaabackend.dto.UserCreateRequest;
import com.example.arimaabackend.dto.UserResponse;
import com.example.arimaabackend.dto.UserUpdateRequest;
import com.example.arimaabackend.model.mongo.UserDocument;
import com.example.arimaabackend.repository.mongo.UserMongoRepository;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;

@Service
public class UserMongoServiceImpl implements UserMongoService {

    private final UserMongoRepository userMongoRepository;
    private final PasswordEncoder passwordEncoder;
    private final SequenceGeneratorService sequenceGeneratorService;

    public UserMongoServiceImpl(UserMongoRepository userMongoRepository, PasswordEncoder passwordEncoder, SequenceGeneratorService sequenceGeneratorService) {
        this.userMongoRepository = userMongoRepository;
        this.passwordEncoder = passwordEncoder;
        this.sequenceGeneratorService = sequenceGeneratorService;
    }

    @Override
    public UserResponse create(UserCreateRequest request) {
        UserDocument doc = new UserDocument();
        doc.setId(sequenceGeneratorService.generateSequence(UserDocument.SEQUENCE_NAME));
        doc.setUsername(request.username());
        doc.setEmail(request.email());
        doc.setPasswordHash(passwordEncoder.encode(request.password()));
        doc.setRole("USER"); // Default role
        doc.setCreatedAt(Instant.now());
        doc.setUpdatedAt(Instant.now());
        
        doc = userMongoRepository.save(doc);
        return toResponse(doc);
    }

    @Override
    public UserResponse getById(Long id) {
        return userMongoRepository.findById(id)
                .map(this::toResponse)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User %d not found in Mongo".formatted(id)));
    }

    @Override
    public UserResponse update(Long id, UserUpdateRequest request) {
        UserDocument doc = userMongoRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User %d not found in Mongo".formatted(id)));

        doc.setUsername(request.username());
        doc.setEmail(request.email());
        if (request.password() != null && !request.password().isBlank()) {
            doc.setPasswordHash(passwordEncoder.encode(request.password()));
        }
        doc.setUpdatedAt(Instant.now());

        doc = userMongoRepository.save(doc);
        return toResponse(doc);
    }

    @Override
    public UserResponse getByUsername(String username) {
        return userMongoRepository.findByUsername(username)
                .map(this::toResponse)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User %s not found in Mongo".formatted(username)));
    }

    @Override
    public void deleteById(Long id) {
        if (!userMongoRepository.existsById(id)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "User %d not found in Mongo".formatted(id));
        }
        userMongoRepository.deleteById(id);
    }

    private UserResponse toResponse(UserDocument doc) {
        return new UserResponse(
                doc.getId(),
                doc.getUsername(),
                doc.getEmail(),
                doc.getRole(),
                doc.getCreatedAt(),
                doc.getUpdatedAt()
        );
    }
}
