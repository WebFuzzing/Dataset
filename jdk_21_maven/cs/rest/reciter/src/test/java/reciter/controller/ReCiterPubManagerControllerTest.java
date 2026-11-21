package reciter.controller;


import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import reciter.database.dynamodb.model.ApplicationUser;
import reciter.service.ApplicationUserService;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class ReCiterPubManagerControllerTest {

    @Mock
    private ApplicationUserService applicationUserService;

    @InjectMocks
    private ReCiterPubManagerController reCiterPubManagerController;

    private String validUid;
    private String validUsername;
    private String validPassword;
    private ApplicationUser validUser;

    @BeforeEach
    public void setUp() {
        // Setup test data
        validUid = "user123";
        validUsername = "John Doe";
        validPassword = "securePassword123";
        validUser = new ApplicationUser(validUid, validUsername, validPassword);
    }

    @Test
    public void testAuthenticateSuccess() {
        // Arrange
        when(applicationUserService.authenticateUser(any(ApplicationUser.class))).thenReturn(true);

        // Act
        boolean result = reCiterPubManagerController.authenticate(validUid, validPassword);

        // Assert
        assertTrue(result);
        
        // Verify service was called with correct parameters
        verify(applicationUserService, times(1)).authenticateUser(argThat(user -> 
            user.getId().equals(validUid) && 
            user.getPassword().equals(validPassword)
        ));
    }

    @Test
    public void testAuthenticateFailure() {
        // Arrange
        when(applicationUserService.authenticateUser(any(ApplicationUser.class))).thenReturn(false);

        // Act
        boolean result = reCiterPubManagerController.authenticate(validUid, "wrongPassword");

        // Assert
        assertFalse(result);
        
        // Verify service was called
        verify(applicationUserService, times(1)).authenticateUser(any(ApplicationUser.class));
    }
    
    @Test
    public void testAuthenticateEmptyCredentials() {
        // Arrange
        when(applicationUserService.authenticateUser(any(ApplicationUser.class))).thenReturn(false);

        // Act
        boolean result = reCiterPubManagerController.authenticate("", "");

        // Assert
        assertFalse(result);
        
        // Verify service was called with empty credentials
        verify(applicationUserService, times(1)).authenticateUser(argThat(user -> 
            user.getId().equals("") && 
            user.getPassword().equals("")
        ));
    }

    @Test
    public void testCreateUserSuccess() {
        // Arrange
        when(applicationUserService.createUser(any(ApplicationUser.class))).thenReturn(true);

        // Act
        boolean result = reCiterPubManagerController.createUser(validUid, validUsername, validPassword);

        // Assert
        assertTrue(result);
        
        // Verify service was called with correct parameters
        verify(applicationUserService, times(1)).createUser(argThat(user -> 
            user.getId().equals(validUid) && 
            user.getUsername().equals(validUsername) && 
            user.getPassword().equals(validPassword)
        ));
    }

    @Test
    public void testCreateUserFailure() {
        // Arrange
        when(applicationUserService.createUser(any(ApplicationUser.class))).thenReturn(false);

        // Act
        boolean result = reCiterPubManagerController.createUser(validUid, validUsername, validPassword);

        // Assert
        assertFalse(result);
        
        // Verify service was called
        verify(applicationUserService, times(1)).createUser(any(ApplicationUser.class));
    }
    
    @Test
    public void testCreateUserDuplicateUser() {
        // Arrange - Simulate attempt to create a duplicate user
        when(applicationUserService.createUser(any(ApplicationUser.class))).thenReturn(false);

        // Act
        boolean result = reCiterPubManagerController.createUser("existingUser", validUsername, validPassword);

        // Assert
        assertFalse(result);
        
        // Verify service was called
        verify(applicationUserService, times(1)).createUser(any(ApplicationUser.class));
    }
    
    @Test
    public void testCreateUserEmptyCredentials() {
        // Arrange
        when(applicationUserService.createUser(any(ApplicationUser.class))).thenReturn(false);

        // Act
        boolean result = reCiterPubManagerController.createUser("", "", "");

        // Assert
        assertFalse(result);
        
        // Verify service was called with empty credentials
        verify(applicationUserService, times(1)).createUser(argThat(user -> 
            user.getId().equals("") && 
            user.getUsername().equals("") && 
            user.getPassword().equals("")
        ));
    }
    
    @Test
    public void testAuthenticateExceptionInService() {
        // Arrange
        when(applicationUserService.authenticateUser(any(ApplicationUser.class)))
            .thenThrow(new RuntimeException("Database connection error"));

        // Act & Assert
        assertThrows(RuntimeException.class, () -> {
            reCiterPubManagerController.authenticate(validUid, validPassword);
        });
        
        // Verify service was called
        verify(applicationUserService, times(1)).authenticateUser(any(ApplicationUser.class));
    }
    
    @Test
    public void testCreateUserExceptionInService() {
        // Arrange
        when(applicationUserService.createUser(any(ApplicationUser.class)))
            .thenThrow(new RuntimeException("Database connection error"));

        // Act & Assert
        assertThrows(RuntimeException.class, () -> {
            reCiterPubManagerController.createUser(validUid, validUsername, validPassword);
        });
        
        // Verify service was called
        verify(applicationUserService, times(1)).createUser(any(ApplicationUser.class));
    }
}
