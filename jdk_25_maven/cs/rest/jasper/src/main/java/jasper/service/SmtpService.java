package jasper.service;

import io.micrometer.core.annotation.Timed;
import jasper.component.ConfigCache;
import jasper.domain.Ref;
import jasper.domain.Ref_;
import jasper.repository.RefRepository;
import jasper.repository.filter.RefFilter;
import jasper.security.Auth;
import jasper.service.dto.SmtpWebhookDto;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;

import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.UUID;

import static jasper.repository.spec.QualifiedTag.concat;
import static jasper.repository.spec.QualifiedTag.selector;
import static org.springframework.data.domain.Sort.Order.desc;
import static org.springframework.data.domain.Sort.by;

@Service
public class SmtpService {
	private static final Logger logger = LoggerFactory.getLogger(SmtpService.class);

	@Autowired
	Auth auth;

	DateTimeFormatter smtp1 = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss Z Z", Locale.US);
	DateTimeFormatter smtp2 = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss Z z", Locale.US);

	@Autowired
	RefService refService;

	@Autowired
	RefRepository refRepository;

	@Autowired
	ConfigCache configs;

	@PreAuthorize("@auth.hasRole('USER') and @auth.local(#origin)")
	@Timed(value = "jasper.service", extraTags = {"service", "smtp"}, histogram = true)
	public void create(SmtpWebhookDto email, String origin) {
		var ref = smtpToDomain(email);
		ref.setOrigin(origin);
		Page<Ref> source = refRepository.findAll(
				RefFilter.builder()
						.query("plugin/email:!internal")
						.endsTitle(ref.getTitle())
						.publishedBefore(ref.getPublished()).build().spec(),
				PageRequest.of(0, 1, by(desc(Ref_.PUBLISHED))));
		if (!source.isEmpty()) {
			ref.setSources(List.of(source.getContent().get(0).getUrl()));
			ref.addTag("internal");
		}
		refService.create(ref);
	}

	private Ref smtpToDomain(SmtpWebhookDto msg) {
		var result = new Ref();
		result.setUrl("comment:" + UUID.randomUUID());
		if (!msg.getDate().startsWith("0001-01-01")) {
			try {
				result.setPublished(ZonedDateTime.parse(msg.getDate(), smtp1).toInstant());
			} catch (RuntimeException e) {
				result.setPublished(ZonedDateTime.parse(msg.getDate(), smtp2).toInstant());
			}
		}
		result.setTitle(msg.getSubject());
		result.setComment(msg.getBody().getHtml() == null ?
			msg.getBody().getText() :
			msg.getBody().getHtml().replaceAll("(?m)^\\s+", ""));
		result.setTags(emailToTags(msg));
		return result;
	}

	private List<String> emailToTags(SmtpWebhookDto msg) {
		var tags = new ArrayList<>(List.of("plugin/email", "plugin/thread"));
		if (msg.getAddresses() != null) {
			if (Optional.ofNullable(msg.getAddresses().getTo())
				.map(SmtpWebhookDto.EmailAddress::getAddress).isPresent()) {
				tags.add(emailAddressToNotification(msg.getAddresses().getTo().getAddress()));
			}
			if (Optional.ofNullable(msg.getAddresses().getFrom())
				.map(SmtpWebhookDto.EmailAddress::getAddress).isPresent()) {
				tags.add(emailAddressFrom(msg.getAddresses().getFrom().getAddress()));
			}
			if (msg.getAddresses().getCc() != null) {
				for (var e : msg.getAddresses().getCc()) {
					if (e.getAddress() != null) {
						tags.add(emailAddressToNotification(e.getAddress()));
					}
				}
			}
			if (msg.getAddresses().getBcc() != null) {
				for (var e : msg.getAddresses().getBcc()) {
					if (e.getAddress() != null) {
						tags.add(emailAddressToNotification(e.getAddress()));
					}
				}
			}
			if (msg.getAddresses().getReplyTo() != null) {
				for (var e : msg.getAddresses().getReplyTo()) {
					if (e.getAddress() != null) {
						tags.add(emailAddressFrom(e.getAddress()));
					}
				}
			}
		}
		return tags;
	}

	private String emailAddressToNotification(String email) {
		var root = configs.root();
		var qt = selector(email);
		var remote = qt.origin;
		if (remote.endsWith("." + root.getEmailHost())) {
			remote = remote.substring(0, remote.length() - root.getEmailHost().length() - 1);
		}
		if (auth.local(remote)) {
			return concat("plugin/inbox", qt.tag);
		} else {
			return concat("plugin/outbox", remote, qt.tag);
		}
	}

	private String emailAddressFrom(String email) {
		var qt = selector(email);
		return concat("plugin/from", qt.origin, qt.tag);
	}

}
