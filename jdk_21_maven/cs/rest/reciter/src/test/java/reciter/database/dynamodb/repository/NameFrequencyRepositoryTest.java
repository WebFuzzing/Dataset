package reciter.database.dynamodb.repository;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Arrays;
import java.util.Collections;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import reciter.database.dynamodb.model.NameFrequency;

@ExtendWith(MockitoExtension.class)
public class NameFrequencyRepositoryTest {

    @Mock
    private NameFrequencyRepository nameFrequencyRepository;

    private NameFrequency nameFrequency1;
    private NameFrequency nameFrequency2;

    @BeforeEach
    public void setUp() {
        nameFrequency1 = new NameFrequency();
        nameFrequency1.setName("John");

        nameFrequency2 = new NameFrequency();
        nameFrequency2.setName("Jane");
    }

    @Test
    public void testSave() {
        doNothing().when(nameFrequencyRepository).save(nameFrequency1);

        nameFrequencyRepository.save(nameFrequency1);

        verify(nameFrequencyRepository).save(nameFrequency1);
    }

    @Test
    public void testSaveAll() {
        doNothing().when(nameFrequencyRepository).saveAll(Arrays.asList(nameFrequency1, nameFrequency2));

        nameFrequencyRepository.saveAll(Arrays.asList(nameFrequency1, nameFrequency2));

        verify(nameFrequencyRepository).saveAll(Arrays.asList(nameFrequency1, nameFrequency2));
    }

    @Test
    public void testFindById_Found() {
        when(nameFrequencyRepository.findById("John")).thenReturn(Optional.of(nameFrequency1));

        Optional<NameFrequency> result = nameFrequencyRepository.findById("John");

        verify(nameFrequencyRepository).findById("John");
        assertTrue(result.isPresent());
        assertEquals("John", result.get().getName());
    }

    @Test
    public void testFindById_NotFound() {
        when(nameFrequencyRepository.findById("Unknown")).thenReturn(Optional.empty());

        Optional<NameFrequency> result = nameFrequencyRepository.findById("Unknown");

        verify(nameFrequencyRepository).findById("Unknown");
        assertFalse(result.isPresent());
    }

    @Test
    public void testFindAll() {
        when(nameFrequencyRepository.findAll()).thenReturn(Arrays.asList(nameFrequency1, nameFrequency2));

        Iterable<NameFrequency> results = nameFrequencyRepository.findAll();

        verify(nameFrequencyRepository).findAll();

        int count = 0;
        for (NameFrequency item : results) {
            count++;
        }
        assertEquals(2, count);
    }

    @Test
    public void testDeleteById() {
        when(nameFrequencyRepository.findById("John")).thenReturn(Optional.of(nameFrequency1));

        nameFrequencyRepository.deleteById("John");

        verify(nameFrequencyRepository).deleteById("John");

        when(nameFrequencyRepository.findById("John")).thenReturn(Optional.empty());
        Optional<NameFrequency> deletedItem = nameFrequencyRepository.findById("John");

        assertFalse(deletedItem.isPresent());
    }

    @Test
    public void testDeleteAll() {
        when(nameFrequencyRepository.findAll()).thenReturn(Arrays.asList(nameFrequency1, nameFrequency2));

        nameFrequencyRepository.deleteAll();

        verify(nameFrequencyRepository).deleteAll();

        when(nameFrequencyRepository.findAll()).thenReturn(Collections.emptyList());

        Iterable<NameFrequency> results = nameFrequencyRepository.findAll();

        int count = 0;
        for (NameFrequency item : results) {
            count++;
        }
        assertEquals(0, count);
    }

    @Test
    public void testCount() {
        when(nameFrequencyRepository.count()).thenReturn(2L);

        long itemCount = nameFrequencyRepository.count();

        verify(nameFrequencyRepository).count();
        assertEquals(2L, itemCount);
    }

    @Test
    public void testExistsById_Found() {
        when(nameFrequencyRepository.existsById("John")).thenReturn(true);

        boolean exists = nameFrequencyRepository.existsById("John");

        verify(nameFrequencyRepository).existsById("John");
        assertTrue(exists);
    }

    @Test
    public void testExistsById_NotFound() {
        when(nameFrequencyRepository.existsById("Unknown")).thenReturn(false);

        boolean exists = nameFrequencyRepository.existsById("Unknown");

        verify(nameFrequencyRepository).existsById("Unknown");
        assertFalse(exists);
    }
}