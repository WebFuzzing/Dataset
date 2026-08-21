package jasper.config;

import io.micrometer.core.aop.CountedAspect;
import io.micrometer.core.aop.TimedAspect;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Tag;
import io.micrometer.core.instrument.Tags;
import jasper.repository.spec.QualifiedTag;
import jasper.security.Auth;
import org.apache.logging.log4j.util.Strings;
import org.aspectj.lang.ProceedingJoinPoint;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.support.ScopeNotActiveException;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.core.authority.AuthorityUtils;

import java.util.List;
import java.util.Optional;

@Configuration
public class MetricsConfig {

	@Autowired
	Props props;

	@Autowired
	Auth auth;

	@Bean
	CountedAspect countedAspect(MeterRegistry registry) {
		return new CountedAspect(registry, this::tagFactory);
	}

	@Bean
	TimedAspect timedAspect(MeterRegistry registry) {
		return new TimedAspect(registry, this::tagFactory);
	}

	private Iterable<Tag> tagFactory(ProceedingJoinPoint pjp) {
		return Tags.of(
			"class", pjp.getStaticPart().getSignature().getDeclaringTypeName(),
				"method", pjp.getStaticPart().getSignature().getName(),
				"debug", ""+props.isDebug())
			.and(getUserTags());
	}

	private Iterable<Tag> getUserTags() {
		try {
			var userTag = Optional.ofNullable(auth.getUserTag()).map(QualifiedTag::toString);
			var roles = auth.getAuthentication() != null ? AuthorityUtils.authorityListToSet(auth.getAuthentication().getAuthorities()) : List.of();
			return Tags.of(
				"scope", "request",
				"userTag", userTag.orElse(""),
				"roles", Strings.join(roles, ','),
				"origin", auth.getOrigin()
			);
		} catch (ScopeNotActiveException e) {
			return Tags.of(
				"scope", "system",
				"userTag", "",
				"roles", "",
				"origin", ""
			);
		}
	}
}
