package reciter.database.dynamodb.repository;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import reciter.database.dynamodb.model.ESearchCount;

@ExtendWith(MockitoExtension.class)
public class ESearchCountRepositoryTest {

    @Mock
    private ESearchCountRepository eSearchCountRepository;

    private ESearchCount eSearchCount1;
    private ESearchCount eSearchCount2;

    @BeforeEach
    public void setUp() {
        eSearchCount1 = new ESearchCount();
        eSearchCount1.setUid("uid1");

        eSearchCount2 = new ESearchCount();
        eSearchCount2.setUid("uid2");
    }

    @Test
    public void testSave() {
        doNothing().when(eSearchCountRepository).save(eSearchCount1);

        eSearchCountRepository.save(eSearchCount1);

        verify(eSearchCountRepository).save(eSearchCount1);
    }

    @Test
    public void testFindById_Found() {
        when(eSearchCountRepository.findById("uid1")).thenReturn(Optional.of(eSearchCount1));

        Optional<ESearchCount> result = eSearchCountRepository.findById("uid1");

        verify(eSearchCountRepository).findById("uid1");
        assertTrue(result.isPresent());
        assertEquals("uid1", result.get().getUid());
    }

    @Test
    public void testFindById_NotFound() {
        when(eSearchCountRepository.findById("uid_missing")).thenReturn(Optional.empty());

        Optional<ESearchCount> result = eSearchCountRepository.findById("uid_missing");

        verify(eSearchCountRepository).findById("uid_missing");
        assertFalse(result.isPresent());
    }
}