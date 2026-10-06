package com.example.demo.auth.model.mapper;

import com.example.demo.auth.model.User;
import com.example.demo.auth.model.entity.UserEntity;
import com.example.demo.auth.model.enums.UserStatus;
import com.example.demo.auth.model.enums.UserType;
import org.junit.jupiter.api.Test;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit test class for {@link UserEntityToUserMapper}.
 * This class tests the mapping functionality between a {@link UserEntity} and a {@link User}
 * to ensure proper data transformation from entity to domain model.
 */
class UserEntityToUserMapperTest {

    private final UserEntityToUserMapper mapper = UserEntityToUserMapper.initialize();

    @Test
    void testMapUserEntityNull() {

        User result = mapper.map((UserEntity) null);

        assertNull(result);

    }

    @Test
    void testMapUserEntityCollectionNull() {

        List<User> result = mapper.map((Collection<UserEntity>) null);

        assertNull(result);

    }

    @Test
    void testMapUserEntityListEmpty() {

        List<User> result = mapper.map(Collections.emptyList());

        assertNotNull(result);
        assertTrue(result.isEmpty());

    }

    @Test
    void testMapUserEntityListWithNullElements() {

        UserEntity userEntity = UserEntity.builder()
                .id(UUID.randomUUID().toString())
                .email("test@example.com")
                .firstName("UserFirstName")
                .lastName("UserLastName")
                .phoneNumber("1234567890")
                .userType(UserType.USER)
                .userStatus(UserStatus.ACTIVE)
                .build();

        List<UserEntity> userEntities = Arrays.asList(userEntity, null);

        List<User> result = mapper.map(userEntities);

        assertNotNull(result);
        assertEquals(2, result.size());
        assertNotNull(result.get(0));
        assertNull(result.get(1));

    }

    @Test
    void testMapSingleUserEntity() {

        UserEntity userEntity = UserEntity.builder()
                .id(UUID.randomUUID().toString())
                .email("test@example.com")
                .firstName("UserFirstName")
                .lastName("UserLastName")
                .phoneNumber("1234567890")
                .userType(UserType.USER)
                .userStatus(UserStatus.ACTIVE)
                .build();

        User result = mapper.map(userEntity);

        assertNotNull(result);
        assertEquals(userEntity.getId(), result.getId());
        assertEquals(userEntity.getEmail(), result.getEmail());
        assertEquals(userEntity.getFirstName(), result.getFirstName());
        assertEquals(userEntity.getLastName(), result.getLastName());
        assertEquals(userEntity.getPhoneNumber(), result.getPhoneNumber());
        assertEquals(userEntity.getUserType(), result.getUserType());
        assertEquals(userEntity.getUserStatus(), result.getUserStatus());
        assertEquals(userEntity.getCreatedAt(), result.getCreatedAt());
        assertEquals(userEntity.getCreatedBy(), result.getCreatedBy());
        assertEquals(userEntity.getUpdatedAt(), result.getUpdatedAt());
        assertEquals(userEntity.getUpdatedBy(), result.getUpdatedBy());

    }

    @Test
    void testMapUserEntityListWithValues() {

        UserEntity userEntity1 = UserEntity.builder()
                .id(UUID.randomUUID().toString())
                .email("user1@example.com")
                .firstName("UserFirstName1")
                .lastName("UserLastName1")
                .phoneNumber("1111111111")
                .userType(UserType.USER)
                .userStatus(UserStatus.ACTIVE)
                .build();

        UserEntity userEntity2 = UserEntity.builder()
                .id(UUID.randomUUID().toString())
                .email("user2@example.com")
                .firstName("UserFirstName2")
                .lastName("UserLastName2")
                .phoneNumber("2222222222")
                .userType(UserType.USER)
                .userStatus(UserStatus.ACTIVE)
                .build();

        List<UserEntity> userEntities = Arrays.asList(userEntity1, userEntity2);

        List<User> result = mapper.map(userEntities);

        assertNotNull(result);
        assertEquals(2, result.size());

        assertEquals(userEntity1.getId(), result.get(0).getId());
        assertEquals(userEntity1.getEmail(), result.get(0).getEmail());
        assertEquals(userEntity1.getFirstName(), result.get(0).getFirstName());

        assertEquals(userEntity2.getId(), result.get(1).getId());
        assertEquals(userEntity2.getEmail(), result.get(1).getEmail());
        assertEquals(userEntity2.getFirstName(), result.get(1).getFirstName());
    }

    @Test
    void testMapWithEdgeCaseValues() {

        UserEntity userEntity = UserEntity.builder()
                .id(null)
                .email("")
                .firstName(null)
                .lastName("")
                .phoneNumber(null)
                .userType(null)
                .userStatus(null)
                .createdAt(null)
                .createdBy(null)
                .updatedAt(null)
                .updatedBy(null)
                .build();

        User result = mapper.map(userEntity);

        assertNotNull(result);
        assertNull(result.getId());
        assertEquals("", result.getEmail());
        assertNull(result.getFirstName());
        assertEquals("", result.getLastName());
        assertNull(result.getPhoneNumber());
        assertNull(result.getUserType());
        assertNull(result.getUserStatus());
        assertNull(result.getCreatedAt());
        assertNull(result.getCreatedBy());
        assertNull(result.getUpdatedAt());
        assertNull(result.getUpdatedBy());

    }

}