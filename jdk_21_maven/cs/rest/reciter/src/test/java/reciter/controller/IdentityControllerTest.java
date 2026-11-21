package reciter.controller;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.util.ReflectionTestUtils;

import reciter.model.identity.AuthorName;
import reciter.model.identity.Identity;
import reciter.service.IdentityService;

@ExtendWith(MockitoExtension.class)
public class IdentityControllerTest {

    @Mock
    private IdentityService identityService;

    @InjectMocks
    private IdentityController identityController;

    private Identity validIdentity;
    private List<Identity> validIdentities;

    @BeforeEach
    public void setUp() {
        // Set the mandatory fields property
         ReflectionTestUtils.setField(identityController, "mandatoryFields", "firstName,lastName,firstInitial");
        
        // Create a valid identity for testing
        validIdentity = new Identity();
        validIdentity.setUid("test123");
        
        List<AuthorName> alternateNames = new ArrayList<>();
        AuthorName authorName = new AuthorName();
        authorName.setFirstName("John");
        authorName.setLastName("Smith");
        authorName.setFirstInitial("J");
        alternateNames.add(authorName);
        
        validIdentity.setAlternateNames(alternateNames);
        
        // Create a list of valid identities
        validIdentities = new ArrayList<>();
        validIdentities.add(validIdentity);
        
        Identity anotherIdentity = new Identity();
        anotherIdentity.setUid("test456");
        
        List<AuthorName> alternateNames2 = new ArrayList<>();
        AuthorName authorName2 = new AuthorName();
        authorName2.setFirstName("Jane");
        authorName2.setLastName("Doe");
        authorName2.setFirstInitial("J");
        alternateNames2.add(authorName2);
        
        anotherIdentity.setAlternateNames(alternateNames2);
        validIdentities.add(anotherIdentity);
    }

    @Test
    public void testAddIdentitySuccess() {
        // Arrange
        doNothing().when(identityService).save(any(Identity.class));
        
        // Act
        ResponseEntity<?> response = identityController.addIdentity(validIdentity);
        
        // Assert
        assertEquals(HttpStatus.OK, response.getStatusCode());
        verify(identityService, times(1)).save(validIdentity);
    }
    
    @Test
    public void testAddIdentityMissingUid() {
        // Arrange
        Identity invalidIdentity = new Identity();
        // Copy alternateNames from valid identity
        invalidIdentity.setAlternateNames(validIdentity.getAlternateNames());
        // UID missing
        
        // Act
        ResponseEntity<?> response = identityController.addIdentity(invalidIdentity);
        
        // Assert
        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
        assertTrue(response.getBody().toString().contains("Uid"));
        verify(identityService, never()).save(any(Identity.class));
    }
    
   // @Test
   /* public void testAddIdentityMissingAlternateNames() {
        // Arrange
        Identity invalidIdentity = new Identity();
        invalidIdentity.setUid("test123");
        // alternateNames missing
        
        // Act
        ResponseEntity<?> response = identityController.addIdentity(invalidIdentity);
        
        // Assert
        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
        assertTrue(response.getBody().toString().contains("alternateNames"));
        verify(identityService, never()).save(any(Identity.class));
    }*/
    
    
    
    @Test
    public void testAddIdentityMissingFirstName() {
        // Arrange
        Identity invalidIdentity = new Identity();
        invalidIdentity.setUid("test123");
        
        List<AuthorName> alternateNames = new ArrayList<>();
        AuthorName authorName = new AuthorName();
        // firstName missing
        authorName.setLastName("Smith");
        authorName.setFirstInitial("J");
        alternateNames.add(authorName);
        
        invalidIdentity.setAlternateNames(alternateNames);
        
        // Act
        ResponseEntity<?> response = identityController.addIdentity(invalidIdentity);
        
        // Assert
        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
        assertTrue(response.getBody().toString().contains("firstName"));
        verify(identityService, never()).save(any(Identity.class));
    }
    
    @Test
    public void testAddIdentityMissingLastName() {
        // Arrange
        Identity invalidIdentity = new Identity();
        invalidIdentity.setUid("test123");
        
        List<AuthorName> alternateNames = new ArrayList<>();
        AuthorName authorName = new AuthorName();
        authorName.setFirstName("John");
        // lastName missing
        authorName.setFirstInitial("J");
        alternateNames.add(authorName);
        
        invalidIdentity.setAlternateNames(alternateNames);
        
        // Act
        ResponseEntity<?> response = identityController.addIdentity(invalidIdentity);
        
        // Assert
        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
        assertTrue(response.getBody().toString().contains("lastName"));
        verify(identityService, never()).save(any(Identity.class));
    }
    
    @Test
    public void testAddIdentityMissingFirstInitial() {
        // Arrange
        Identity invalidIdentity = new Identity();
        invalidIdentity.setUid("test123");
        
        List<AuthorName> alternateNames = new ArrayList<>();
        AuthorName authorName = new AuthorName();
        authorName.setFirstName("John");
        authorName.setLastName("Smith");
        // firstInitial missing
        alternateNames.add(authorName);
        
        invalidIdentity.setAlternateNames(alternateNames);
        
        // Act
        ResponseEntity<?> response = identityController.addIdentity(invalidIdentity);
        
        // Assert
        assertEquals(HttpStatus.OK, response.getStatusCode());
        verify(identityService, times(1)).save(validIdentity);
    }
    
    @Test
    public void testSaveIdentitiesSuccess() {
        // Arrange
        doNothing().when(identityService).save(anyList());
        
        // Act
        identityController.saveIdentities(validIdentities);
        
        // Assert
        verify(identityService, times(1)).save(validIdentities);
    }
    
    @Test
    public void testFindByUidSuccess() {
        // Arrange
        when(identityService.findByUid("test123")).thenReturn(validIdentity);
        
        // Act
        ResponseEntity<?> response = identityController.findByUid("test123");
        
        // Assert
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(validIdentity, response.getBody());
        verify(identityService, times(1)).findByUid("test123");
    }
    
    @Test
    public void testFindByUidNotFound() {
        // Arrange
        when(identityService.findByUid("nonexistent")).thenReturn(null);
        
        // Act
        ResponseEntity<?> response = identityController.findByUid("nonexistent");
        
        // Assert
        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
        assertTrue(response.getBody().toString().contains("nonexistent"));
        verify(identityService, times(1)).findByUid("nonexistent");
    }
    
    @Test
    public void testFindByUidsSuccess() {
        // Arrange
        List<String> uids = Arrays.asList("test123", "test456");
        when(identityService.findByUids(uids)).thenReturn(validIdentities);
        
        // Act
        ResponseEntity<?> response = identityController.findByUids(uids);
        
        // Assert
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(validIdentities, response.getBody());
        verify(identityService, times(1)).findByUids(uids);
    }
    
    @Test
    public void testFindByUidsNotFound() {
        // Arrange
        List<String> uids = Collections.singletonList("nonexistent");
        when(identityService.findByUids(uids)).thenReturn(new ArrayList<>());
        
        // Act
        ResponseEntity<?> response = identityController.findByUids(uids);
        
        // Assert
        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
        assertTrue(response.getBody().toString().contains("nonexistent"));
        verify(identityService, times(1)).findByUids(uids);
    }
    
    @Test
    public void testFindByUidsNullPointerException() {
        // Arrange
        List<String> uids = Collections.singletonList("test123");
        when(identityService.findByUids(uids)).thenThrow(new NullPointerException());
        
        // Act
        ResponseEntity<?> response = identityController.findByUids(uids);
        
        // Assert
        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
        assertTrue(response.getBody().toString().contains("test123"));
        verify(identityService, times(1)).findByUids(uids);
    }
    
    @Test
    public void testFindAllSuccess() {
        // Arrange
        when(identityService.findAll()).thenReturn(validIdentities);
        
        // Act
        ResponseEntity<?> response = identityController.findAll();
        
        // Assert
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(validIdentities, response.getBody());
        verify(identityService, times(1)).findAll();
    }
    
    @Test
    public void testFindAllEmpty() {
        // Arrange
        when(identityService.findAll()).thenReturn(new ArrayList<>());
        
        // Act
        ResponseEntity<?> response = identityController.findAll();
        
        // Assert
        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
        assertEquals("Identity table is empty", response.getBody());
        verify(identityService, times(1)).findAll();
    }
    
    @Test
    public void testFindAllException() {
        // Arrange
        when(identityService.findAll()).thenThrow(new RuntimeException("Database error"));
        
        // Act
        ResponseEntity<?> response = identityController.findAll();
        
        // Assert
        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
        assertTrue(response.getBody().toString().contains("Issue with the request"));
        verify(identityService, times(1)).findAll();
    }
    
    @Test
    public void testGetMandatoryFields() {
        // Act
        String[] fields = identityController.getMandatoryFields();
        
        // Assert
        assertArrayEquals(new String[]{"firstName", "lastName", "firstInitial"}, fields);
    }
}
