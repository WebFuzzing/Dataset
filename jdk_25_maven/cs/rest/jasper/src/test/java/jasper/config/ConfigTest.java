package jasper.config;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class ConfigTest {

	record TestHasTags(List<String> tags, String origin) implements jasper.domain.proj.HasTags {
		@Override
		public String getTitle() { return "test"; }
		@Override
		public String getUrl() { return "http://test"; }
		@Override
		public List<String> getTags() { return tags; }
		@Override
		public void setTags(List<String> tags) {}
		@Override
		public com.fasterxml.jackson.databind.node.ObjectNode getPlugins() { return null; }
		@Override
		public void setPlugins(com.fasterxml.jackson.databind.node.ObjectNode plugins) {}
		@Override
		public String getOrigin() { return origin; }
		@Override
		public void setOrigin(String origin) {}
		@Override
		public java.time.Instant getModified() { return null; }
	}

	@Test
	void testScriptWithPluginAndOrigin_MatchesExactOrigin() {
		var config = Config.ServerConfig.builder()
			.scriptSelectors(List.of("_plugin/delta/scrape@alice"))
			.build();

		assertThat(config.script("_plugin/delta/scrape", "@alice")).isTrue();
		assertThat(config.script("_plugin/delta/scrape", "@bob")).isFalse();
		assertThat(config.script("_plugin/delta/cache", "@alice")).isFalse();
	}

	@Test
	void testScriptWithPluginAndOrigin_MatchesDefaultOrigin() {
		var config = Config.ServerConfig.builder()
			.scriptSelectors(List.of("_plugin/delta/scrape"))
			.build();

		assertThat(config.script("_plugin/delta/scrape", "")).isTrue();
		assertThat(config.script("_plugin/delta/scrape", "@alice")).isFalse();
	}

	@Test
	void testScriptWithPluginAndOrigin_MatchesMultipleOrigins() {
		var config = Config.ServerConfig.builder()
			.scriptSelectors(List.of(
				"_plugin/delta/scrape@alice",
				"_plugin/delta/cache@alice",
				"_plugin/delta/scrape@bob",
				"_plugin/delta/cache@bob"
			))
			.build();

		assertThat(config.script("_plugin/delta/scrape", "@alice")).isTrue();
		assertThat(config.script("_plugin/delta/scrape/ref", "@alice")).isTrue();
		assertThat(config.script("_plugin/delta/cache", "@alice")).isTrue();
		assertThat(config.script("_plugin/delta/scrape", "@bob")).isTrue();
		assertThat(config.script("_plugin/delta/scrape", "@charlie")).isFalse();
	}

	@Test
	void testScriptWithPluginAndOrigin_IgnoresWildcards() {
		var config = Config.ServerConfig.builder()
			.scriptSelectors(List.of(
				"_plugin/delta/scrape@alice",
				"_plugin/delta/cache@alice",
				"_plugin/delta/scrape@bob",
				"_plugin/delta/cache@bob",
				"_plugin/delta/scrape@charlie",
				"_plugin/delta/cache@charlie",
				"_plugin/delta/scrape@navi.*",
				"_plugin/delta/cache@navi.*"
			))
			.build();

		assertThat(config.script("_plugin/delta/scrape/ref", "@alice")).isTrue();
		assertThat(config.script("_plugin/delta/scrape/ref", "@navi.garden")).isTrue();
	}

	@Test
	void testScriptWithPluginAndOrigin_MatchesWildcardOrigin() {
		var config = Config.ServerConfig.builder()
			.scriptSelectors(List.of("_plugin/delta/scrape@navi.*"))
			.build();

		assertThat(config.script("_plugin/delta/scrape", "@navi")).isFalse();
		assertThat(config.script("_plugin/delta/scrape", "@navi.sub")).isTrue();
		assertThat(config.script("_plugin/delta/scrape", "@alice")).isFalse();
	}

	@Test
	void testScriptWithPluginAndOrigin_MatchesNestedOrigin() {
		var config = Config.ServerConfig.builder()
			.scriptSelectors(List.of("_plugin/delta/scrape@alice"))
			.build();

		// Nesting check: @alice has nesting 1, @alice.sub has nesting 2
		assertThat(config.script("_plugin/delta/scrape", "@alice")).isTrue();
		assertThat(config.script("_plugin/delta/scrape", "@alice.sub")).isFalse();
	}

	@Test
	void testScriptWithPluginAndOrigin_MatchesSubPlugin() {
		var config = Config.ServerConfig.builder()
			.scriptSelectors(List.of("_plugin/delta@alice"))
			.build();

		// Should match sub-plugins
		assertThat(config.script("_plugin/delta", "@alice")).isTrue();
		assertThat(config.script("_plugin/delta/scrape", "@alice")).isTrue();
		assertThat(config.script("_plugin/delta/cache", "@alice")).isTrue();
		assertThat(config.script("_plugin/other", "@alice")).isFalse();
	}

	@Test
	void testScriptWithPluginAndOrigin_AllOriginsSelector() {
		var config = Config.ServerConfig.builder()
			.scriptSelectors(List.of("_plugin/delta/scrape@*"))
			.build();

		assertThat(config.script("_plugin/delta/scrape", "")).isFalse();
		assertThat(config.script("_plugin/delta/scrape", "@alice")).isTrue();
		assertThat(config.script("_plugin/delta/scrape", "@navi")).isTrue();
	}

	@Test
	void testScriptWithPluginAndOrigin_EmptySelectors() {
		var config = Config.ServerConfig.builder()
			.scriptSelectors(null)
			.build();

		assertThat(config.script("_plugin/delta/scrape", "@alice")).isFalse();
	}

	@Test
	void testScriptWithPluginAndOrigin_AllPluginsForOrigin() {
		var config = Config.ServerConfig.builder()
			.scriptSelectors(List.of("@alice"))
			.build();

		assertThat(config.script("_plugin/delta/scrape", "@alice")).isTrue();
		assertThat(config.script("+plugin/delta/scrape", "@alice")).isTrue();
		assertThat(config.script("plugin/anything", "@alice")).isTrue();
		assertThat(config.script("_plugin/delta/scrape", "@bob")).isFalse();
	}

	@Test
	void testScriptWithHasTags_MatchesExactOrigin() {
		var config = Config.ServerConfig.builder()
			.scriptSelectors(List.of("_plugin/delta/scrape@alice"))
			.build();

		assertThat(config.script("_plugin/delta", new TestHasTags(List.of("_plugin/delta/scrape"), "@alice"))).isTrue();
		assertThat(config.script("_plugin/delta", new TestHasTags(List.of("_plugin/delta/scrape"), "@bob"))).isFalse();
		assertThat(config.script("_plugin/delta", new TestHasTags(List.of("_plugin/delta/cache"), "@alice"))).isFalse();
	}

	@Test
	void testScriptWithHasTags_MatchesDefaultOrigin() {
		var config = Config.ServerConfig.builder()
			.scriptSelectors(List.of("_plugin/delta/scrape"))
			.build();

		assertThat(config.script("_plugin/delta", new TestHasTags(List.of("_plugin/delta/scrape"), ""))).isTrue();
		assertThat(config.script("_plugin/delta", new TestHasTags(List.of("_plugin/delta/scrape"), "@alice"))).isFalse();
	}

	@Test
	void testScriptWithHasTags_MatchesMultipleTags() {
		var config = Config.ServerConfig.builder()
			.scriptSelectors(List.of("_plugin/delta/scrape@alice"))
			.build();

		// Ref with multiple tags - should match if any tag matches
		assertThat(config.script("_plugin/delta", new TestHasTags(List.of("other", "_plugin/delta/scrape", "another"), "@alice"))).isTrue();
		assertThat(config.script("_plugin/delta", new TestHasTags(List.of("other", "another"), "@alice"))).isFalse();
	}

	@Test
	void testScriptWithHasTags_MatchesNestedOrigin() {
		var config = Config.ServerConfig.builder()
			.scriptSelectors(List.of("_plugin/delta/scrape@alice"))
			.build();

		// Nesting check: @alice has nesting 1, @alice.sub has nesting 2
		assertThat(config.script("_plugin/delta", new TestHasTags(List.of("_plugin/delta/scrape"), "@alice"))).isTrue();
		assertThat(config.script("_plugin/delta", new TestHasTags(List.of("_plugin/delta/scrape"), "@alice.sub"))).isFalse();
	}

	@Test
	void testScriptWithHasTags_MatchesWildcardOrigin() {
		var config = Config.ServerConfig.builder()
			.scriptSelectors(List.of("_plugin/delta/scrape@navi.*"))
			.build();

		assertThat(config.script("_plugin/delta", new TestHasTags(List.of("_plugin/delta/scrape"), "@navi"))).isFalse();
		assertThat(config.script("_plugin/delta", new TestHasTags(List.of("_plugin/delta/scrape"), "@navi.sub"))).isTrue();
		assertThat(config.script("_plugin/delta", new TestHasTags(List.of("_plugin/delta/scrape"), "@alice"))).isFalse();
	}

	@Test
	void testScriptWithHasTags_MatchesSubPlugin() {
		var config = Config.ServerConfig.builder()
			.scriptSelectors(List.of("_plugin/delta@alice"))
			.build();

		// Should match sub-plugins
		assertThat(config.script("_plugin/delta", new TestHasTags(List.of("_plugin/delta"), "@alice"))).isTrue();
		assertThat(config.script("_plugin/delta", new TestHasTags(List.of("_plugin/delta/scrape"), "@alice"))).isTrue();
		assertThat(config.script("_plugin/delta", new TestHasTags(List.of("_plugin/delta/cache"), "@alice"))).isTrue();
		assertThat(config.script("_plugin/delta", new TestHasTags(List.of("_plugin/other"), "@alice"))).isFalse();
	}

	@Test
	void testScriptWithHasTags_AllOriginsSelector() {
		var config = Config.ServerConfig.builder()
			.scriptSelectors(List.of("_plugin/delta/scrape@*"))
			.build();

		assertThat(config.script("_plugin/delta", new TestHasTags(List.of("_plugin/delta/scrape"), ""))).isFalse();
		assertThat(config.script("_plugin/delta", new TestHasTags(List.of("_plugin/delta/scrape"), "@alice"))).isTrue();
		assertThat(config.script("_plugin/delta", new TestHasTags(List.of("_plugin/delta/scrape"), "@navi"))).isTrue();
	}

	@Test
	void testScriptWithHasTags_EmptySelectors() {
		var config = Config.ServerConfig.builder()
			.scriptSelectors(null)
			.build();

		assertThat(config.script("_plugin/delta", new TestHasTags(List.of("_plugin/delta/scrape"), "@alice"))).isFalse();
	}

	@Test
	void testScriptWithHasTags_NullRef() {
		var config = Config.ServerConfig.builder()
			.scriptSelectors(List.of("_plugin/delta/scrape@alice"))
			.build();

		assertThat(config.script("_plugin/delta", (jasper.domain.proj.HasTags) null)).isFalse();
	}

	@Test
	void testScriptWithHasTags_NullTags() {
		var config = Config.ServerConfig.builder()
			.scriptSelectors(List.of("_plugin/delta/scrape@alice"))
			.build();

		assertThat(config.script("_plugin/delta", new TestHasTags(null, "@alice"))).isFalse();
	}

	@Test
	void testScriptWithHasTags_EmptyTags() {
		var config = Config.ServerConfig.builder()
			.scriptSelectors(List.of("_plugin/delta/scrape@alice"))
			.build();

		assertThat(config.script("_plugin/delta", new TestHasTags(List.of(), "@alice"))).isFalse();
	}

	@Test
	void testScriptWithHasTags_AllPluginsForOrigin() {
		var config = Config.ServerConfig.builder()
			.scriptSelectors(List.of("@alice"))
			.build();

		assertThat(config.script("_plugin/delta", new TestHasTags(List.of("_plugin/delta/scrape"), "@alice"))).isTrue();
		assertThat(config.script("+plugin/delta", new TestHasTags(List.of("+plugin/delta/scrape"), "@alice"))).isTrue();
		assertThat(config.script("plugin/anything", new TestHasTags(List.of("plugin/anything"), "@alice"))).isTrue();
		assertThat(config.script("_plugin/delta", new TestHasTags(List.of("_plugin/delta/scrape"), "@bob"))).isFalse();
	}
}
