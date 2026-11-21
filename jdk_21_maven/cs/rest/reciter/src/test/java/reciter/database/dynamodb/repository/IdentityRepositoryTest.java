package reciter.database.dynamodb.repository;


import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import reciter.database.dynamodb.model.Identity;
import reciter.model.identity.Gender;
import reciter.model.identity.GenderEnum;

@ExtendWith(MockitoExtension.class)
public class IdentityRepositoryTest {

    @Mock
    private IdentityRepository repository;

    private Identity identity1;
    private Identity identity2;

    @BeforeEach
    public void setUp() {
        // Create common test data
        identity1 = new Identity();
        Gender gender1 = new Gender("vr67890",  "jag", GenderEnum.M, 5.0);
        identity1.setUid("vr12345");
        reciter.model.identity.Identity identityData1 = new reciter.model.identity.Identity();
        identityData1.setUid("vr12345");
        identityData1.setGender(gender1);
        identity1.setIdentity(identityData1);

        identity2 = new Identity();
        Gender gender2 = new Gender("vr67890", "jag", GenderEnum.M, 5.0);
        identity2.setUid("vr67890");
        reciter.model.identity.Identity identityData2 = new reciter.model.identity.Identity();
        identityData2.setGender(gender2);
        identity2.setIdentity(identityData2);
    }

    @Test
    public void testSave() {
        // Setup mock behavior
        when(repository.findById("vr12345")).thenReturn(Optional.of(identity1));

        // Call the repository method
        repository.save(identity1);

        // Verify the interaction
        verify(repository).save(identity1);

        // Retrieve the saved entity by ID (uid)
        Optional<Identity> retrievedIdentity = repository.findById("vr12345");

        // Assert that the entity is not null and matches the saved values
        assertTrue(retrievedIdentity.isPresent());
        assertEquals(identity1.getUid(), retrievedIdentity.get().getUid());
    }

    @Test
    public void testFindById() {
        // Setup mock behavior
        when(repository.findById("vr67890")).thenReturn(Optional.of(identity2));

        // Call the repository method
        Optional<Identity> retrievedIdentity = repository.findById("vr67890");

        // Verify the interaction
        verify(repository).findById("vr67890");

        // Assert that the entity is present and its fields match
        assertTrue(retrievedIdentity.isPresent());
        assertEquals(identity2.getUid(), retrievedIdentity.get().getUid());
    }

    @Test
    public void testFindAll() {
        // Setup mock behavior
        List<Identity> expectedIdentities = Arrays.asList(identity1, identity2);
        when(repository.findAll()).thenReturn(expectedIdentities);
        when(repository.count()).thenReturn(2L);

        // Call the repository method
        Iterable<Identity> allIdentities = repository.findAll();
        long count = repository.count();

        // Verify the interaction
        verify(repository).findAll();
        verify(repository).count();

        // Assert that the count matches the expected value
        assertEquals(2, count);
        
        // Convert iterable to list for easier assertion
        List<Identity> identityList = new java.util.ArrayList<>();
        allIdentities.forEach(identityList::add);
        
        assertEquals(2, identityList.size());
        assertTrue(identityList.contains(identity1));
        assertTrue(identityList.contains(identity2));
    }

    @Test
    public void testDeleteById() {
        // Setup mock behavior
        doNothing().when(repository).deleteById("vr12345");
        when(repository.findById("vr12345")).thenReturn(Optional.empty());

        // Call the repository method
        repository.deleteById("vr12345");

        // Verify the interaction
        verify(repository).deleteById("vr12345");

        // Attempt to retrieve the entity by ID
        Optional<Identity> deletedIdentity = repository.findById("vr12345");

        // Assert that the entity no longer exists
        assertFalse(deletedIdentity.isPresent());
    }

    @Test
    public void testDeleteAll() {
        // Setup mock behavior
        doNothing().when(repository).deleteAll();
        when(repository.count()).thenReturn(0L);

        // Call the repository method
        repository.deleteAll();

        // Verify the interaction
        verify(repository).deleteAll();

        // Assert that no identities remain in the repository
        long count = repository.count();
        assertEquals(0, count);
    }

    @Test
    public void testCount() {
        // Setup mock behavior
        when(repository.count()).thenReturn(2L);

        // Call the repository method
        long count = repository.count();

        // Verify the interaction
        verify(repository).count();

        // Assert that the count matches the expected value
        assertEquals(2, count);
    }

    @Test
    public void testFindAllById() {
        // Setup mock behavior
        List<String> uids = Arrays.asList("vr12345", "vr67890");
        List<Identity> expectedIdentities = Arrays.asList(identity1, identity2);
        when(repository.findAllById(uids)).thenReturn(expectedIdentities);

        // Call the repository method
        List<Identity> identities = repository.findAllById(uids);

        // Verify the interaction
        verify(repository).findAllById(uids);

        // Assert that the retrieved identities match the expected ones
        assertEquals(2, identities.size());
        assertTrue(identities.stream().anyMatch(i -> i.getUid().equals("vr12345")));
        assertTrue(identities.stream().anyMatch(i -> i.getUid().equals("vr67890")));
    }
}
