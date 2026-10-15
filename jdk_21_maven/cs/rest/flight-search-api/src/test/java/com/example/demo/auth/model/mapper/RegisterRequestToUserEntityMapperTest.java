package com.example.demo.auth.model.mapper;

import com.example.demo.auth.model.dto.request.RegisterRequest;
import com.example.demo.auth.model.entity.UserEntity;
import com.example.demo.auth.model.enums.UserType;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit test class for {@link RegisterRequestToUserEntityMapper}.
 * This class tests the mapping functionality between a {@link RegisterRequest} and a
 * {@link UserEntity} to ensure the correct transformation of data during user registration.
 */
class RegisterRequestToUserEntityMapperTest {

    private final RegisterRequestToUserEntityMapper mapper = RegisterRequestToUserEntityMapper.initialize();

    @Test
    void testMapRegisterRequestNull() {

        UserEntity result = mapper.map((RegisterRequest) null);

        assertNull(result);

    }

    @Test
    void testMapRegisterRequestCollectionNull() {

        List<UserEntity> result = mapper.map((Collection<RegisterRequest>) null);

        assertNull(result);

    }

    @Test
    void testMapRegisterRequestListEmpty() {

        List<UserEntity> result = mapper.map(Collections.emptyList());

        assertNotNull(result);
        assertTrue(result.isEmpty());

    }

    @Test
    void testMapRegisterRequestListWithNullElements() {

        List<RegisterRequest> requests = Arrays.asList(
                new RegisterRequest("test1@example.com",
                        "password1",
                        "UserFirstName",
                        "UserLastName",
                        "1234567890",
                        UserType.USER),
                null
        );

        List<UserEntity> result = mapper.map(requests);

        assertNotNull(result);
        assertEquals(2, result.size());
        assertNotNull(result.get(0));
        assertNull(result.get(1));

    }

    @Test
    void testMapSingleRegisterRequest() {

        RegisterRequest request = new RegisterRequest(
                "test@example.com", "password",
                "UserFirstName", "UserLastName", "1234567890",
                UserType.ADMIN
        );

        UserEntity result = mapper.map(request);

        assertNotNull(result);
        assertEquals(request.getEmail(), result.getEmail());
        assertEquals(request.getPassword(), result.getPassword());
        assertEquals(request.getFirstName(), result.getFirstName());
        assertEquals(request.getLastName(), result.getLastName());
        assertEquals(request.getPhoneNumber(), result.getPhoneNumber());
        assertEquals(request.getUserType(), result.getUserType());

    }

}