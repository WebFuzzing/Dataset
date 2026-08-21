package jasper.component;

import io.micrometer.core.annotation.Timed;
import jakarta.persistence.EntityExistsException;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceException;
import jakarta.persistence.RollbackException;
import jasper.config.Props;
import jasper.domain.Template;
import jasper.errors.AlreadyExistsException;
import jasper.errors.DuplicateModifiedDateException;
import jasper.errors.InvalidPushException;
import jasper.errors.ModifiedException;
import jasper.errors.NotFoundException;
import jasper.repository.TemplateRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.orm.jpa.JpaSystemException;
import org.springframework.stereotype.Component;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionSystemException;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.Clock;
import java.time.Instant;

import static jasper.component.Replicator.deletedTag;
import static jasper.component.Replicator.deletorTag;
import static jasper.component.Replicator.isDeletorTag;
import static jasper.util.DbConstraint.isPkViolation;
import static jasper.util.DbConstraint.isUniqueModifiedOriginViolation;

@Component
public class IngestTemplate {
	private static final Logger logger = LoggerFactory.getLogger(IngestTemplate.class);

	@Autowired
	Props props;

	@Autowired
	TemplateRepository templateRepository;

	@Autowired
	EntityManager em;

	@Autowired
	Validate validate;

	@Autowired
	Messages messages;

	@Autowired
	PlatformTransactionManager transactionManager;

	// Exposed for testing
	Clock ensureUniqueModifiedClock = Clock.systemUTC();

	@Timed(value = "jasper.template", histogram = true)
	public void create(Template template) {
		if (isDeletorTag(template.getTag())) {
			if (templateRepository.existsByQualifiedTag(deletedTag(template.getQualifiedTag()))) throw new AlreadyExistsException();
		} else {
			delete(deletorTag(template.getQualifiedTag()));
		}
		validate.template(template.getOrigin(), template);
		ensureCreateUniqueModified(template);
		messages.updateTemplate(template);
	}

	@Timed(value = "jasper.template", histogram = true)
	public void update(Template template) {
		if (!templateRepository.existsByQualifiedTag(template.getQualifiedTag())) throw new NotFoundException("Template");
		validate.template(template.getOrigin(), template);
		ensureUpdateUniqueModified(template);
		messages.updateTemplate(template);
	}

	@Timed(value = "jasper.template", histogram = true)
	public void push(Template template) {
		validate.template(template.getOrigin(), template);
		try {
			templateRepository.save(template);
		} catch (DataIntegrityViolationException | PersistenceException | JpaSystemException e) {
			if (e instanceof EntityExistsException) throw new AlreadyExistsException();
			if (isPkViolation(e, "template")) throw new AlreadyExistsException();
			if (isUniqueModifiedOriginViolation(e, "template")) throw new DuplicateModifiedDateException();
			throw e;
		} catch (TransactionSystemException e) {
			if (e.getCause() instanceof RollbackException r) {
				if (r.getCause() instanceof jakarta.validation.ConstraintViolationException) throw new InvalidPushException();
			}
			throw e;
		}
		if (isDeletorTag(template.getTag())) {
			delete(deletedTag(template.getQualifiedTag()));
		}
		messages.updateTemplate(template);
	}

	@Timed(value = "jasper.template", histogram = true)
	public void delete(String qualifiedTag) {
		templateRepository.deleteByQualifiedTag(qualifiedTag);
		messages.deleteTemplate(qualifiedTag);
	}

	void ensureCreateUniqueModified(Template template) {
		var count = 0;
		while (true) {
			try {
				count++;
				TransactionTemplate transactionTemplate = new TransactionTemplate(transactionManager);
				transactionTemplate.execute(status -> {
					template.setModified(Instant.now(ensureUniqueModifiedClock));
					em.persist(template);
					em.flush();
					return null;
				});
				break;
			} catch (DataIntegrityViolationException | PersistenceException | JpaSystemException e) {
				if (e instanceof EntityExistsException) throw new AlreadyExistsException();
				if (isPkViolation(e, "template")) throw new AlreadyExistsException();
				if (isUniqueModifiedOriginViolation(e, "template")) {
					if (count > props.getIngestMaxRetry()) throw new DuplicateModifiedDateException();
					continue;
				}
				throw e;
			}
		}
	}

	void ensureUpdateUniqueModified(Template template) {
		var cursor = template.getModified();
		var count = 0;
		while (true) {
			try {
				count++;
				TransactionTemplate transactionTemplate = new TransactionTemplate(transactionManager);
				transactionTemplate.execute(status -> {
					template.setModified(Instant.now(ensureUniqueModifiedClock));
					var updated = templateRepository.optimisticUpdate(
						cursor,
						template.getTag(),
						template.getOrigin(),
						template.getName(),
						template.getConfig(),
						template.getSchema(),
						template.getDefaults(),
						template.getModified());
					if (updated == 0) {
						throw new ModifiedException("Template");
					}
					return null;
				});
				break;
			} catch (DataIntegrityViolationException | PersistenceException | JpaSystemException e) {
				if (isUniqueModifiedOriginViolation(e, "template")) {
					if (count > props.getIngestMaxRetry()) throw new DuplicateModifiedDateException();
					continue;
				}
				throw e;
			}
		}
	}

}
