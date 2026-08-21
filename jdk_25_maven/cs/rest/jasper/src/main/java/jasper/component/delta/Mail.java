package jasper.component.delta;

import jakarta.annotation.PostConstruct;
import jasper.component.ConfigCache;
import jasper.domain.Ref;
import jasper.domain.proj.RefUrl;
import jasper.repository.ExtRepository;
import jasper.repository.RefRepository;
import jasper.repository.filter.TagQuery;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Profile;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Component;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static jasper.domain.proj.Tag.localTag;
import static jasper.domain.proj.Tag.tagOrigin;
import static java.util.Arrays.stream;
import static java.util.stream.Stream.concat;
import static org.apache.commons.lang3.StringUtils.isNotBlank;

@Profile("smtp-relay")
@Component
public class Mail implements Async.AsyncRunner {
	private static final Logger logger = LoggerFactory.getLogger(Mail.class);

	@Autowired
	Async async;

	@Autowired
	RefRepository refRepository;

	@Autowired
	ExtRepository extRepository;

	@Autowired
	JavaMailSender emailSender;

	@Autowired
	ConfigCache configs;

	record QT(String tag, String origin) {}

	@PostConstruct
	void init() {
		async.addAsyncTag("plugin/email", this);
	}

	@Override
	public String signature() {
		return "+plugin/email";
	}

	@Override
	public void run(Ref ref) throws Exception {
		var mb = ref.getTags().stream()
			.filter(t -> t.startsWith("plugin/inbox/") || t.startsWith("plugin/outbox/"))
			.toArray(String[]::new);
		String[] emails = new String[]{};
		String[] outboxUserTags = new String[]{};
		if (mb.length != 0) {
			var inboxUserTags = stream(mb)
				.filter(t -> t.startsWith("plugin/inbox/"))
				.map(t -> "+" + t.substring("plugin/inbox/".length()) + ref.getOrigin())
				.toArray(String[]::new);
			outboxUserTags = stream(mb)
				.filter(t -> t.startsWith("plugin/outbox/"))
				.map(t -> t.substring("plugin/outbox/".length()))
				.map(t -> new QT(t.substring(t.indexOf("/") + 1), t.substring(0, t.indexOf("/"))))
				.map(to -> to.tag + (isNotBlank(to.origin) ? ("@" + to.origin) : ""))
				.toArray(String[]::new);
			var query = String.join("|", concat(stream(inboxUserTags), stream(outboxUserTags)).toArray(String[]::new));
			emails = extRepository.findAll(new TagQuery(query).spec())
				.stream()
				.filter(ext -> ext.getConfig().has("email"))
				.map(ext -> ext.getConfig().get("email").asText())
				.filter(StringUtils::isNotBlank)
				.toArray(String[]::new);
		}
		if (!ref.hasTag("+user") && !ref.hasTag("_user") && emails.length == 0) {
			// Mail from webhook with no recipient
			return;
		}
		var tos = stream(outboxUserTags)
			.map(t -> localTag(t) + refRepository.originUrl(ref.getOrigin(), tagOrigin(t))
				.map(RefUrl::get)
				.map(o -> "@" + o)
				.orElse(tagOrigin(t)))
			.toArray(String[]::new);
		if (tos.length == 0 && emails.length == 0) {
			logger.error("No recipients for email.");
			return;
		}
		SimpleMailMessage message = new SimpleMailMessage();
		message.setTo(concat(stream(tos), stream(emails))
			.distinct()
			.toArray(String[]::new));
		var host = refRepository.originUrl(ref.getOrigin(), ref.getOrigin()).map(RefUrl::get).map(str -> {
			try {
				return new URI(str);
			} catch (URISyntaxException e) {
				return null;
			}
		}).map(URI::getHost).orElse(Stream.of(ref.getOrigin(), configs.root().getEmailHost()).filter(StringUtils::isNotBlank).collect(Collectors.joining(".")));
		message.setFrom(ref.getExpandedTags().stream()
			.filter(t -> t.startsWith("+user/") || t.startsWith("+user") || t.startsWith("_user/") || t.startsWith("_user"))
			.findFirst()
			.map(t -> t + "@" + host)
			.orElse("no-reply@" + host)
		);
		// TODO: Add notifications as reply-to ?
		message.setSubject(ref.getTitle());
		message.setText(ref.getComment());
		emailSender.send(message);
	}
}
