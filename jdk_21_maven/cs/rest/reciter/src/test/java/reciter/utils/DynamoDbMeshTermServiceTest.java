package reciter.utils;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import reciter.database.dynamodb.model.MeshTerm;
import reciter.database.dynamodb.repository.DynamoMeshTermRepository;
import reciter.service.dynamo.DynamoDbMeshTermService;

import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;

class DynamoDbMeshTermServiceTest {

    @InjectMocks
    private DynamoDbMeshTermService dynamoDbMeshTermService;

    @Mock
    private DynamoMeshTermRepository dynamoMeshTermRepository;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    void testSave() {
        List<MeshTerm> meshTerms = Arrays.asList(
                new MeshTerm("Term1",40L),
                new MeshTerm("Term2",30L)
        );

        dynamoDbMeshTermService.save(meshTerms);

        verify(dynamoMeshTermRepository, times(1)).saveAll(meshTerms);
    }

    @Test
    void testFindAll() {
        MeshTerm term1 = new MeshTerm("Term1",22L);
        MeshTerm term2 = new MeshTerm("Term2",31L);
        List<MeshTerm> mockMeshTerms = Arrays.asList(term1, term2);

        when(dynamoMeshTermRepository.findAll()).thenReturn(mockMeshTerms);

        List<MeshTerm> result = dynamoDbMeshTermService.findAll();

        assertEquals(mockMeshTerms, result);
        verify(dynamoMeshTermRepository, times(1)).findAll();
    }

    @Test
    void testGetItemCount() {
        long mockItemCount = 42;
        when(dynamoMeshTermRepository.getItemCount()).thenReturn(mockItemCount);

        long itemCount = dynamoDbMeshTermService.getItemCount();

        assertEquals(mockItemCount, itemCount);
        verify(dynamoMeshTermRepository, times(1)).getItemCount();
    }
}
