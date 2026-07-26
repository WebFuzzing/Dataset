package jasper.component;

import io.micrometer.core.annotation.Timed;
import jakarta.persistence.EntityExistsException;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceException;
import jakarta.persistence.RollbackException;
import jasper.config.Props;
import jasper.domain.Ext;
import jasper.errors.AlreadyExistsException;
import jasper.errors.DuplicateModifiedDateException;
import jasper.errors.InvalidPushException;
import jasper.errors.ModifiedException;
import jasper.errors.NotFoundException;
import jasper.repository.ExtRepository;
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
public class IngestExt {
	private static final Logger logger = LoggerFactory.getLogger(IngestExt.class);

	@Autowired
	Props props;

	@Autowired
	ExtRepository extRepository;

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

	@Timed(value = "jasper.ext", histogram = true)
	public void create(Ext ext) {
		if (isDeletorTag(ext.getTag())) {
			if (extRepository.existsByQualifiedTag(deletedTag(ext.getQualifiedTag()))) throw new AlreadyExistsException();
		} else {
			delete(deletorTag(ext.getQualifiedTag()));
		}
		validate.ext(ext.getOrigin(), ext);
		ensureCreateUniqueModified(ext);
		messages.updateExt(ext);
	}

	@Timed(value = "jasper.ext", histogram = true)
	public void update(Ext ext) {
		if (!extRepository.existsByQualifiedTag(ext.getQualifiedTag())) throw new NotFoundException("Ext");
		validate.ext(ext.getOrigin(), ext);
		ensureUpdateUniqueModified(ext);
		messages.updateExt(ext);
	}

	@Timed(value = "jasper.ext", histogram = true)
	public void push(String rootOrigin, Ext ext, boolean validation, boolean stripInvalidTemplates) {
		if (validation) validate.ext(rootOrigin, ext, stripInvalidTemplates);
		pushUniqueModified(ext);
		if (isDeletorTag(ext.getTag())) {
			delete(deletedTag(ext.getQualifiedTag()));
		} else {
			delete(deletorTag(ext.getQualifiedTag()));
		}
		messages.updateExt(ext);
	}

	@Timed(value = "jasper.ext", histogram = true)
	public void delete(String qualifiedTag) {
		extRepository.deleteByQualifiedTag(qualifiedTag);
	}

	void ensureCreateUniqueModified(Ext ext) {
		var count = 0;
		while (true) {
			try {
				count++;
				TransactionTemplate transactionTemplate = new TransactionTemplate(transactionManager);
				transactionTemplate.execute(status -> {
					ext.setModified(Instant.now(ensureUniqueModifiedClock));
					em.persist(ext);
					em.flush();
					return null;
				});
				break;
			} catch (DataIntegrityViolationException | PersistenceException | JpaSystemException e) {
				if (e instanceof EntityExistsException) throw new AlreadyExistsException();
				if (isPkViolation(e, "ext")) throw new AlreadyExistsException();
				if (isUniqueModifiedOriginViolation(e, "ext")) {
					if (count > props.getIngestMaxRetry()) throw new DuplicateModifiedDateException();
					continue;
				}
				throw e;
			}
		}
	}

	void ensureUpdateUniqueModified(Ext ext) {
		var cursor = ext.getModified();
		var count = 0;
		while (true) {
			try {
				count++;
				TransactionTemplate transactionTemplate = new TransactionTemplate(transactionManager);
				transactionTemplate.execute(status -> {
					ext.setModified(Instant.now(ensureUniqueModifiedClock));
					var updated = extRepository.optimisticUpdate(
						cursor,
						ext.getTag(),
						ext.getOrigin(),
						ext.getName(),
						ext.getConfig(),
						ext.getModified());
					if (updated == 0) {
						throw new ModifiedException("Ext");
					}
					return null;
				});
				break;
			} catch (DataIntegrityViolationException | PersistenceException | JpaSystemException e) {
				if (isUniqueModifiedOriginViolation(e, "ext")) {
					if (count > props.getIngestMaxRetry()) throw new DuplicateModifiedDateException();
					continue;
				}
				throw e;
			}
		}
	}

	private void pushUniqueModified(Ext ext) {
		try {
			extRepository.save(ext);
		} catch (DataIntegrityViolationException | PersistenceException | JpaSystemException e) {
			if (e instanceof EntityExistsException) throw new AlreadyExistsException();
			if (isPkViolation(e, "ext")) throw new AlreadyExistsException();
			if (isUniqueModifiedOriginViolation(e, "ext")) throw new DuplicateModifiedDateException();
			throw e;
		} catch (TransactionSystemException e) {
			if (e.getCause() instanceof RollbackException r) {
				if (r.getCause() instanceof jakarta.validation.ConstraintViolationException) throw new InvalidPushException();
			}
			throw e;
		}
	}

}
