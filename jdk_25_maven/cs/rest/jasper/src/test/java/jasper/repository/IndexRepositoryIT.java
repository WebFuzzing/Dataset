package jasper.repository;

import jasper.IntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

@IntegrationTest
public class IndexRepositoryIT {

	@Autowired
	IndexRepository indexRepository;

	@Test
	void testBuildAndDropTags() {
		indexRepository.dropTags();
		indexRepository.buildTags();
		indexRepository.dropTags();
	}

	@Test
	void testBuildAndDropSources() {
		indexRepository.dropSources();
		indexRepository.buildSources();
		indexRepository.dropSources();
	}

	@Test
	void testBuildAndDropPublished() {
		indexRepository.dropPublished();
		indexRepository.buildPublished();
		indexRepository.dropPublished();
	}

	@Test
	void testBuildAndDropModified() {
		indexRepository.dropModified();
		indexRepository.buildModified();
		indexRepository.dropModified();
	}
}
