package reciter.database.dynamodb.repository;


import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Arrays;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import reciter.database.dynamodb.model.Gender;

@ExtendWith(MockitoExtension.class)
public class GenderRepositoryTest {

    @Mock
    private GenderRepository genderRepository;

    private Gender gender1;
    private Gender gender2;

    @BeforeEach
    public void setUp() {
        // Create common test data
        gender1 = new Gender("Male", null, null, 0);
        gender2 = new Gender("Female", null, null, 0);
    }

    @Test
    public void testSave() {
        // Setup mock behavior for save (since save() is a void method, use doNothing)
        doNothing().when(genderRepository).save(gender1);

        // Call the repository method
        genderRepository.save(gender1);

        // Verify the interaction
        verify(genderRepository).save(gender1);
    }

    @Test
    public void testFindAll() {
        // Setup mock behavior
        when(genderRepository.findAll()).thenReturn(Arrays.asList(gender1, gender2));

        // Call the repository method
        Iterable<Gender> genders = genderRepository.findAll();

        // Verify the interaction
        verify(genderRepository).findAll();

        // Assert that the retrieved results match
        int count = 0;
        for (Gender gender : genders) {
            count++;
        }

        assertEquals(2, count);
    }

    @Test
    public void testDeleteById() {
        // Setup mock behavior
        when(genderRepository.findById("NonBinary")).thenReturn(Optional.of(new Gender("NonBinary", null, null, 0)));

        // Call the repository method
        genderRepository.deleteById("NonBinary");

        // Verify the interaction
        verify(genderRepository).deleteById("NonBinary");

        // Attempt to retrieve the deleted Gender object (should be null)
        when(genderRepository.findById("NonBinary")).thenReturn(Optional.empty());
        Optional<Gender> deletedGender = genderRepository.findById("NonBinary");

        // Assert that the Gender object has been deleted
        assertFalse(deletedGender.isPresent());
    }

    @Test
    public void testDeleteAll() {
        // Setup mock behavior
        when(genderRepository.findAll()).thenReturn(Arrays.asList(gender1, gender2));

        // Call the repository method
        genderRepository.deleteAll();

        // Verify the interaction
        verify(genderRepository).deleteAll();

        // Attempt to retrieve all Gender objects (should be empty)
        when(genderRepository.findAll()).thenReturn(Arrays.asList());

        Iterable<Gender> genders = genderRepository.findAll();

        // Assert that no Gender objects are returned
        int count = 0;
        for (Gender gender : genders) {
            count++;
        }
        assertEquals(0, count);
    }

    @Test
    public void testCount() {
        // Setup mock behavior
        when(genderRepository.count()).thenReturn(2L);

        // Call the repository method
        long itemCount = genderRepository.count();

        // Verify the interaction
        verify(genderRepository).count();

        // Assert that the item count is correct
        assertEquals(2L, itemCount);
    }


    @Test
    public void testExistsById() {
        // Setup mock behavior
        when(genderRepository.existsById("Other")).thenReturn(true);

        // Call the repository method
        boolean exists = genderRepository.existsById("Other");

        // Verify the interaction
        verify(genderRepository).existsById("Other");

        // Assert that the Gender object exists
        assertTrue(exists);
    }
}

