package com.example.arimaabackend.services.neo4j;
import com.example.arimaabackend.enums.UserRole;
import java.time.Instant;
import com.example.arimaabackend.dto.UserCreateRequest;
import com.example.arimaabackend.dto.UserResponse;
import com.example.arimaabackend.dto.UserUpdateRequest;
import com.example.arimaabackend.model.neo4j.UserNode;
import com.example.arimaabackend.repository.neo4j.UserNeo4jRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Service
public class UserNeo4jServiceImpl implements UserNeo4jService {

    private final UserNeo4jRepository userNeo4jRepository;

    public UserNeo4jServiceImpl(UserNeo4jRepository userNeo4jRepository) {
        this.userNeo4jRepository = userNeo4jRepository;
    }

    @Override
    public UserResponse create(UserCreateRequest request) {
        UserNode node = new UserNode();
        // Since we removed @GeneratedValue, we need a way to generate IDs for new users.
        // For simplicity, we'll use a timestamp-based ID or similar if no ID is provided.
        // But better is to find the max ID and increment, or use a UUID.
        // For now, let's use current time millis to avoid collisions in this simple implementation.
        node.setId(System.currentTimeMillis()); 
        node.setUsername(request.username());
        node.setEmail(request.email());
        node.setRole(UserRole.USER);
        Instant now = Instant.now();
        node.setCreatedAt(now);
        node.setUpdatedAt(now);
        
        UserNode saved = userNeo4jRepository.save(node);
        return toResponse(saved);
    }

    @Override
    public UserResponse getById(Long id) {
        return userNeo4jRepository.findById(id)
                .map(this::toResponse)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "User not found with id: " + id));
    }

    @Override
    public UserResponse update(Long id, UserUpdateRequest request) {
        UserNode node = userNeo4jRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "User not found with id: " + id));
        
        node.setUsername(request.username());
        node.setEmail(request.email());
        node.setUpdatedAt(Instant.now());
        
        UserNode updated = userNeo4jRepository.save(node);
        return toResponse(updated);
    }

    @Override
    public void deleteById(Long id) {
        if (!userNeo4jRepository.existsById(id)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found with id: " + id);
        }
        userNeo4jRepository.deleteById(id);
    }

    private UserResponse toResponse(UserNode node) {
        return new UserResponse(
                node.getId(),
                node.getUsername(),
                node.getEmail(),
                node.getRole() != null ? node.getRole().name() : null,
                node.getCreatedAt(),
                node.getUpdatedAt()
        );
    }
}
