package jasper.component;

import io.micrometer.core.annotation.Timed;
import jasper.component.dto.Bundle;
import jasper.errors.ModifiedException;
import jasper.errors.NotFoundException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.ArrayList;

@Component
public class IngestBundle {
	private static final Logger logger = LoggerFactory.getLogger(IngestBundle.class);

	@Autowired
	Ingest ingestRef;

	@Autowired
	IngestExt ingestExt;

	@Autowired
	IngestUser ingestUser;

	@Autowired
	IngestPlugin ingestPlugin;

	@Autowired
	IngestTemplate ingestTemplate;

	@Autowired
	Tagger tagger;

	private record Log(String title, String message) {}

	@Timed(value = "jasper.bundle", histogram = true)
	public void createOrUpdate(Bundle bundle, String origin, String parentUrl) {
		var logs = new ArrayList<Log>();
		if (bundle.getRef() != null) for (var ref : bundle.getRef()) {
			ref.setOrigin(origin);
			try {
				try {
					ingestRef.update(origin, ref);
				} catch (ModifiedException e) {
					logger.warn("Duplicate ingesting Ref {}", ref.getUrl());
				} catch (NotFoundException e) {
					ingestRef.create(origin, ref);
				}
			} catch (Exception e) {
				logger.error("Error ingesting Ref {}", ref.getUrl(), e);
				logs.add(new Log("Error ingesting Ref " + ref.getUrl(), e.getMessage()));
			}
		}
		if (bundle.getExt() != null) for (var ext : bundle.getExt()) {
			ext.setOrigin(origin);
			try {
				try {
					ingestExt.update(ext);
				} catch (ModifiedException e) {
					logger.error("Duplicate ingesting Ext {}", ext.getTag());
				} catch (NotFoundException e) {
					ingestExt.create(ext);
				}
			} catch (Exception e) {
				logger.error("Error ingesting Ext {}", ext.getTag(), e);
				logs.add(new Log("Error ingesting Ext " + ext.getTag(), e.getMessage()));
			}
		}
		if (bundle.getUser() != null) for (var user : bundle.getUser()) {
			user.setOrigin(origin);
			try {
				try {
					ingestUser.update(user);
				} catch (ModifiedException e) {
					logger.error("Duplicate ingesting User {}", user.getTag());
				} catch (NotFoundException e) {
					ingestUser.create(user);
				}
			} catch (Exception e) {
				logger.error("Error ingesting User {}", user.getTag(), e);
				logs.add(new Log("Error ingesting User " + user.getTag(), e.getMessage()));
			}
		}
		if (bundle.getPlugin() != null) for (var plugin : bundle.getPlugin()) {
			plugin.setOrigin(origin);
			try {
				try {
					ingestPlugin.update(plugin);
				} catch (ModifiedException e) {
					logger.error("Duplicate ingesting Plugin {}", plugin.getTag());
				} catch (NotFoundException e) {
					ingestPlugin.create(plugin);
				}
			} catch (Exception e) {
				logger.error("Error ingesting Plugin {}", plugin.getTag(), e);
				logs.add(new Log("Error ingesting Plugin " + plugin.getTag(), e.getMessage()));
			}
		}
		if (bundle.getTemplate() != null) for (var template : bundle.getTemplate()) {
			template.setOrigin(origin);
			try {
				try {
					ingestTemplate.update(template);
				} catch (ModifiedException e) {
					logger.error("Duplicate ingesting Template {}", template.getTag());
				} catch (NotFoundException e) {
					ingestTemplate.create(template);
				}
			} catch (Exception e) {
				logger.error("Error ingesting Template {}", template.getTag(), e);
				logs.add(new Log("Error ingesting Template " + template.getTag(), e.getMessage()));
			}
		}
		for (var log : logs) tagger.attachError(parentUrl, origin, log.title, log.message);
	}
}
