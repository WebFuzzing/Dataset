package jasper.service;

import io.micrometer.core.annotation.Timed;
import jasper.component.ConfigCache;
import jasper.component.Storage;
import jasper.config.Props;
import jasper.domain.proj.HasOrigin;
import jasper.repository.ExtRepository;
import jasper.repository.PluginRepository;
import jasper.repository.RefRepository;
import jasper.repository.TemplateRepository;
import jasper.repository.UserRepository;
import jasper.security.Auth;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;

import static jasper.security.AuthoritiesConstants.ADMIN;

@Service
public class OriginService {
	private static final Logger logger = LoggerFactory.getLogger(OriginService.class);

	@Autowired
	Props props;

	@Autowired
	ConfigCache configs;

	@Autowired
	RefRepository refRepository;

	@Autowired
	ExtRepository extRepository;

	@Autowired
	UserRepository userRepository;

	@Autowired
	PluginRepository pluginRepository;

	@Autowired
	TemplateRepository templateRepository;

	@Autowired
	Auth auth;

	@Autowired
	Optional<Storage> storage;

	@PreAuthorize("@auth.hasRole('MOD')")
	@Timed(value = "jasper.service", extraTags = {"service", "backup"}, histogram = true)
	public List<String> listOrigins() {
		var set = new HashSet<String>();
		set.addAll(refRepository.origins());
		set.addAll(extRepository.origins());
		set.addAll(pluginRepository.origins());
		set.addAll(templateRepository.origins());
		set.addAll(userRepository.origins());
		set.addAll(userRepository.origins());
		if (storage.isPresent()) {
			set.addAll(storage.get().listTenants().stream()
				.map(HasOrigin::origin).toList());
		}
		return set.stream()
			.filter(auth::subOrigin).toList();
	}

	@Transactional
	@PreAuthorize("@auth.hasRole('MOD') and @auth.subOrigin(#origin)")
	@Timed(value = "jasper.service", extraTags = {"service", "origin"}, histogram = true)
	public void delete(String origin, Instant olderThan) {
		var start = Instant.now();
		logger.info("{} Deleting origin {} older than {}", auth.getOrigin(), origin, olderThan);
		refRepository.deleteByOriginAndModifiedLessThanEqual(origin, olderThan);
		extRepository.deleteByOriginAndModifiedLessThanEqual(origin, olderThan);
		userRepository.deleteByOriginAndModifiedLessThanEqual(origin, olderThan);
		if (!auth.local(origin) || auth.hasRole(ADMIN)) {
			pluginRepository.deleteByOriginAndModifiedLessThanEqual(origin, olderThan);
			templateRepository.deleteByOriginAndModifiedLessThanEqual(origin, olderThan);
		}
		logger.info("{} Finished deleting origin {} older than {} in {}", auth.getOrigin(), origin, olderThan, Duration.between(start, Instant.now()));
		if (props.getOrigin().equals(origin)) {
			configs.clearUserCache();
			configs.clearPluginCache();
			configs.clearTemplateCache();
			configs.clearConfigCache();
		}
	}
}
