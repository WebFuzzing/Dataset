package jasper.component;

import com.fasterxml.jackson.databind.ObjectMapper;
import jasper.IntegrationTest;
import jasper.domain.Ref;
import jasper.repository.RefRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.test.context.support.WithMockUser;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@WithMockUser("+user/tester")
@IntegrationTest
public class TaggerIT {

	@Autowired
    Tagger tagger;

	@Autowired
	RefRepository refRepository;

	@Autowired
	ObjectMapper objectMapper;

	static final String URL = "https://www.example.com/";

	Ref refWithTags(String url, String... tags) {
		var ref = new Ref();
		ref.setUrl(url);
		ref.setTags(new ArrayList<>(List.of(tags)));
		refRepository.save(ref);
		return ref;
	}

	Ref remoteRefWithTags(String url, String origin, String... tags) {
		var ref = new Ref();
		ref.setUrl(url);
		ref.setOrigin(origin);
		ref.setTags(new ArrayList<>(List.of(tags)));
		refRepository.save(ref);
		return ref;
	}

	@BeforeEach
	void init() {
		refRepository.deleteAll();
	}

	@Test
	void testTagRef() {
		tagger.tag(URL, "", "test");

		assertThat(refRepository.existsByUrlAndOrigin(URL, ""))
			.isTrue();
		var fetched = refRepository.findOneByUrlAndOrigin(URL, "").get();
		assertThat(fetched.getTags())
			.contains("test");
	}

	@Test
	void testTagExistingRef() {
		refWithTags(URL);

		tagger.tag(URL, "", "test");

		assertThat(refRepository.existsByUrlAndOrigin(URL, ""))
			.isTrue();
		var fetched = refRepository.findOneByUrlAndOrigin(URL, "").get();
		assertThat(fetched.getTags())
			.contains("test");
	}

	@Test
	void testTagRemoteRef() {
		tagger.tag(URL, "@other", "test");

		assertThat(refRepository.existsByUrlAndOrigin(URL, "@other"))
			.isTrue();
		var fetched = refRepository.findOneByUrlAndOrigin(URL, "@other").get();
		assertThat(fetched.getTags())
			.contains("test");
	}

	@Test
	void testTagExistingRemoteRef() {
		remoteRefWithTags(URL, "@other");

		tagger.tag(URL, "@other", "test");

		assertThat(refRepository.existsByUrlAndOrigin(URL, "@other"))
			.isTrue();
		var fetched = refRepository.findOneByUrlAndOrigin(URL, "@other").get();
		assertThat(fetched.getTags())
			.contains("test");
	}

	@Test
	void testSilentPluginRef() {
		tagger.silentPlugin(URL, "Test", "", "plugin/test", objectMapper.createObjectNode());

		assertThat(refRepository.existsByUrlAndOrigin(URL, ""))
			.isTrue();
		var fetched = refRepository.findOneByUrlAndOrigin(URL, "").get();
		assertThat(fetched.getTags())
			.contains("plugin/test");
	}

	@Test
	void testSilentPluginExistingRef() {
		refWithTags(URL);

		tagger.silentPlugin(URL, "Test", "", "plugin/test", objectMapper.createObjectNode());

		assertThat(refRepository.existsByUrlAndOrigin(URL, ""))
			.isTrue();
		var fetched = refRepository.findOneByUrlAndOrigin(URL, "").get();
		assertThat(fetched.getTags())
			.contains("plugin/test");
	}

	@Test
	void testSilentPluginRemoteRef() {
		tagger.silentPlugin(URL, "Test", "@other", "plugin/test", objectMapper.createObjectNode());

		assertThat(refRepository.existsByUrlAndOrigin(URL, "@other"))
			.isTrue();
		var fetched = refRepository.findOneByUrlAndOrigin(URL, "@other").get();
		assertThat(fetched.getTags())
			.contains("plugin/test");
	}

	@Test
	void testSilentPluginRemoteRefMultiple() {
		tagger.silentPlugin(URL + 1, "Test", "@other", "plugin/test", objectMapper.createObjectNode());
		tagger.silentPlugin(URL + 2, "Test", "@other", "plugin/test", objectMapper.createObjectNode());
		tagger.silentPlugin(URL + 3, "Test", "@other", "plugin/test", objectMapper.createObjectNode());
		tagger.silentPlugin(URL + 4, "Test", "@other", "plugin/test", objectMapper.createObjectNode());

		assertThat(refRepository.existsByUrlAndOrigin(URL + 1, "@other"))
			.isTrue();
		assertThat(refRepository.existsByUrlAndOrigin(URL + 2, "@other"))
			.isTrue();
		assertThat(refRepository.existsByUrlAndOrigin(URL + 3, "@other"))
			.isTrue();
		assertThat(refRepository.existsByUrlAndOrigin(URL + 4, "@other"))
			.isTrue();
		var fetched1 = refRepository.findOneByUrlAndOrigin(URL + 1, "@other").get();
		var fetched2 = refRepository.findOneByUrlAndOrigin(URL + 2, "@other").get();
		var fetched3 = refRepository.findOneByUrlAndOrigin(URL + 3, "@other").get();
		var fetched4 = refRepository.findOneByUrlAndOrigin(URL + 4, "@other").get();
		assertThat(fetched1.getTags())
			.contains("plugin/test");
		assertThat(fetched2.getTags())
			.contains("plugin/test");
		assertThat(fetched3.getTags())
			.contains("plugin/test");
		assertThat(fetched4.getTags())
			.contains("plugin/test");
	}

	@Test
	void testSilentPluginExistingRemoteRef() {
		remoteRefWithTags(URL, "@other");

		tagger.silentPlugin(URL, "Test", "@other", "plugin/test", objectMapper.createObjectNode());

		assertThat(refRepository.existsByUrlAndOrigin(URL, "@other"))
			.isTrue();
		var fetched = refRepository.findOneByUrlAndOrigin(URL, "@other").get();
		assertThat(fetched.getTags())
			.contains("plugin/test");
	}

}
