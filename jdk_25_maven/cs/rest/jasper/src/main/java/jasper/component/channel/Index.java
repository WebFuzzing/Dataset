package jasper.component.channel;

import jasper.component.ConfigCache;
import jasper.repository.IndexRepository;
import jasper.service.dto.TemplateDto;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.integration.annotation.ServiceActivator;
import org.springframework.messaging.Message;
import org.springframework.stereotype.Component;

import static jasper.domain.proj.HasOrigin.origin;
import static org.apache.commons.lang3.StringUtils.isBlank;

@Component
public class Index {
	private static final Logger logger = LoggerFactory.getLogger(Index.class);

	@Autowired
	IndexRepository indexRepository;

	@Autowired
	ConfigCache configs;

	@ServiceActivator(inputChannel = "templateRxChannel")
	public void handleTemplateUpdate(Message<TemplateDto> message) {
		if (!configs.root().script("_config/index", "")) return;
		if (isBlank(origin(message.getHeaders().get("origin").toString())) && "_config/index".equals(message.getPayload().getTag())) {
			updateIndex();
		}
	}

	@EventListener(ApplicationReadyEvent.class)
	public void init() {
		if (!configs.root().script("_config/index", "")) return;
		updateIndex();
	}

	public void updateIndex() {
		logger.info("Updating DB indices");
		var index = configs.index();
		if (index.isTags()) {
			try {
				indexRepository.buildTags();
			} catch (Exception ignored) {}
			try {
				indexRepository.buildExpandedTags();
			} catch (Exception ignored) {}
		} else {
			indexRepository.dropTags();
			indexRepository.dropExpandedTags();
		}
		if (index.isSources()) {
			try {
				indexRepository.buildSources();
			} catch (Exception ignored) {}
		} else {
			indexRepository.dropSources();
		}
		if (index.isAlts()) {
			try {
				indexRepository.buildAlts();
			} catch (Exception ignored) {}
		} else {
			indexRepository.dropAlts();
		}
		if (index.isFulltext()) {
			try {
				indexRepository.buildFulltext();
			} catch (Exception ignored) {}
		} else {
			indexRepository.dropFulltext();
		}
		if (index.isPublished()) {
			try {
				indexRepository.buildPublished();
			} catch (Exception ignored) {}
		} else {
			indexRepository.dropPublished();
		}
		if (index.isModified()) {
			try {
				indexRepository.buildModified();
			} catch (Exception ignored) {}
		} else {
			indexRepository.dropModified();
		}
	}

}
