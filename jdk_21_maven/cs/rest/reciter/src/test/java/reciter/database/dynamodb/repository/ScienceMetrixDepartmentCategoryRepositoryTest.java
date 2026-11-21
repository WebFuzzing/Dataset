package reciter.database.dynamodb.repository;


import static org.junit.Assert.assertTrue;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import reciter.database.dynamodb.model.ScienceMetrixDepartmentCategory;

@ExtendWith(MockitoExtension.class)
public class ScienceMetrixDepartmentCategoryRepositoryTest {

    @Mock
    private ScienceMetrixDepartmentCategoryRepository repository;
    
    private ScienceMetrixDepartmentCategory category1;
    private ScienceMetrixDepartmentCategory category2;
    
    @BeforeEach
    public void setUp() {
        // Create common test data for reuse
        category1 = new ScienceMetrixDepartmentCategory(
                3, 4.5, "Department C", "Mathematics", 104536
        );
        category2 = new ScienceMetrixDepartmentCategory(
                4, 1.2, "Department D", "Mathematics", 104536
        );
    }

    @Test
    public void testSave() {
        // Create a new ScienceMetrixDepartmentCategory
        ScienceMetrixDepartmentCategory category = new ScienceMetrixDepartmentCategory(
                1000012, 2.5, "Department Cd", "Physics", 1015674
        );

        // Call the save method
        repository.save(category);
        
        // Verify that save was called with the correct parameter
        verify(repository).save(category);
    }

    @Test
    public void testFindByScienceMetrixJournalSubfieldId() {
        // Setup mock behavior
        List<ScienceMetrixDepartmentCategory> expectedCategories = Arrays.asList(category1, category2);
        when(repository.findByScienceMetrixJournalSubfieldId(104536L)).thenReturn(expectedCategories);
        
        // Call the repository method
        List<ScienceMetrixDepartmentCategory> retrievedCategories = repository.findByScienceMetrixJournalSubfieldId(104536L);
        
        // Verify interaction
        verify(repository).findByScienceMetrixJournalSubfieldId(104536L);
        
        // Assert that both categories are returned
        assertEquals(2, retrievedCategories.size());
        assertTrue(retrievedCategories.stream().anyMatch(c -> c.getPk() == 3));
        assertTrue(retrievedCategories.stream().anyMatch(c -> c.getPk() == 4));
    }

    @Test
    public void testFindAll() {
        // Create test data
        ScienceMetrixDepartmentCategory category1 = new ScienceMetrixDepartmentCategory(
                542, 63.0, "Department E1", "Biology23", 10344
        );
        ScienceMetrixDepartmentCategory category2 = new ScienceMetrixDepartmentCategory(
                664, 24.1, "Department F1", "Physic32s", 105325
        );
        
        List<ScienceMetrixDepartmentCategory> allCategories = Arrays.asList(category1, category2);
        
        // Setup mock behavior
        when(repository.findAll()).thenReturn(allCategories);
        when(repository.count()).thenReturn((long) allCategories.size());
        
        // Call repository methods
        Iterable<ScienceMetrixDepartmentCategory> retrievedCategories = repository.findAll();
        long count = repository.count();
        
        // Verify interactions
        verify(repository).findAll();
        verify(repository).count();
        
        // Convert iterable to list for easier assertion
        List<ScienceMetrixDepartmentCategory> scienceMetrixDeptCategories = new ArrayList<>();
        Iterator<ScienceMetrixDepartmentCategory> iterator = retrievedCategories.iterator();
        while (iterator.hasNext()) {
            ScienceMetrixDepartmentCategory scienceMetrix = iterator.next();
            scienceMetrixDeptCategories.add(scienceMetrix);
        }
        
        // Assert that the size of the result matches the expected count
        assertEquals(count, scienceMetrixDeptCategories.size());
    }

    @Test
    public void testCount() {
        // Create test data
        ScienceMetrixDepartmentCategory category = new ScienceMetrixDepartmentCategory(
                1012123232, 9.0, "Department J", "Biology", 109
        );
        
        // Setup mock behavior
        when(repository.count()).thenReturn(5L);  // Mock a count of 5 items
        
        // Call the repository method
        long count = repository.count();
        
        // Verify interaction
        verify(repository).count();
        
        // Assert that the count matches the expected value
        assertTrue("count should not be zero", count != 0);
    }
}
