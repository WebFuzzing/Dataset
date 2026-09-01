package com.example.arimaabackend.migration.steps.mongodb;

import java.util.EnumSet;
import java.util.List;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;

import com.example.arimaabackend.migration.MigrationContext;
import com.example.arimaabackend.migration.spi.MigrationStep;
import com.example.arimaabackend.migration.spi.MigrationTarget;
import com.example.arimaabackend.model.mongo.DatabaseSequence;
import com.example.arimaabackend.model.mongo.UserDocument;
import com.example.arimaabackend.model.sql.UserEntity;
import com.example.arimaabackend.repository.mongo.UserMongoRepository;
import com.example.arimaabackend.repository.sql.UserJpaRepository;
import org.springframework.data.mongodb.core.MongoOperations;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;

@Service
@Profile("migration")
public class UserMongoMigration implements MigrationStep {

    private static final Logger log = LoggerFactory.getLogger(UserMongoMigration.class);

    private final UserJpaRepository userJpaRepository;
    private final UserMongoRepository userMongoRepository;
    private final MongoOperations mongoOperations;

    public UserMongoMigration(UserJpaRepository userJpaRepository, UserMongoRepository userMongoRepository, MongoOperations mongoOperations) {
        this.userJpaRepository = userJpaRepository;
        this.userMongoRepository = userMongoRepository;
        this.mongoOperations = mongoOperations;
    }

    @Override
    public String stepName() {
        return "user-mongo";
    }

    @Override
    public Set<MigrationTarget> targets() {
        return EnumSet.of(MigrationTarget.MONGODB);
    }

    @Override
    public int getOrder() {
        return 121;
    }

    @Override
    public void migrate(MigrationContext context) {
        if (context.dryRun()) {
            log.info("[{}] dry-run: would migrate {} rows", stepName(), userJpaRepository.count());
            return;
        }
        List<UserDocument> documents = userJpaRepository.findAll().stream().map(this::toDocument).toList();
        userMongoRepository.saveAll(documents);
        log.info("[{}] migrated {} users", stepName(), documents.size());

        // Update sequence counter
        long maxId = documents.stream().mapToLong(UserDocument::getId).max().orElse(0L);
        if (maxId > 0) {
            mongoOperations.upsert(
                    Query.query(Criteria.where("_id").is(UserDocument.SEQUENCE_NAME)),
                    new Update().set("seq", maxId),
                    DatabaseSequence.class
            );
            log.info("[{}] updated sequence {} to {}", stepName(), UserDocument.SEQUENCE_NAME, maxId);
        }
    }

    private UserDocument toDocument(UserEntity entity) {
        var document = new UserDocument();
        document.setId(entity.getId());
        document.setUsername(entity.getUsername());
        document.setEmail(entity.getEmail());
        document.setPasswordHash(entity.getPasswordHash());
        document.setRole(entity.getRole() != null ? entity.getRole().name() : null);
        document.setCreatedAt(entity.getCreatedAt());
        document.setUpdatedAt(entity.getUpdatedAt());
        return document;
    }
}
