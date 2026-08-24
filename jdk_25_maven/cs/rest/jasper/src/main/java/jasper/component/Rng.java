package jasper.component;

import io.micrometer.core.annotation.Timed;
import jasper.domain.Ref;
import jasper.domain.Ref_;
import jasper.repository.RefRepository;
import jasper.repository.filter.RefFilter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Component;

import java.util.UUID;

import static jasper.repository.spec.OriginSpec.isUnderOrigin;
import static org.springframework.data.domain.Sort.Order.desc;
import static org.springframework.data.domain.Sort.by;

@Component
public class Rng {
	private static final Logger logger = LoggerFactory.getLogger(Rng.class);

	@Autowired
	RefRepository refRepository;

	@Timed(value = "jasper.rng.update", histogram = true)
	public void update(String rootOrigin, Ref ref, Ref existing) {
		if (!ref.hasTag("plugin/rng")) return;
		if (existing != null) {
			var remotes = refRepository.findAll(RefFilter.builder()
				.url(ref.getUrl())
				.modifiedAfter(existing.getModified())
				.build().spec().and(isUnderOrigin(rootOrigin)), PageRequest.of(0, 1, by(desc(Ref_.MODIFIED))));
			if (remotes.isEmpty() || remotes.getContent().get(0).getOrigin().equals(ref.getOrigin())) {
				ref.removeTag("plugin/rng");
				for (var t : existing.getTags()) {
					if (t.startsWith("+plugin/rng/")) {
						ref.addTag(t);
					}
				}
				return;
			}
		} else {
			if (refRepository.count(RefFilter.builder()
				.url(ref.getUrl())
				.build().spec()) == 0) {
				return;
			}
		}
		ref.removeTag("+plugin/rng");
		ref.addTag("+plugin/rng/" + UUID.randomUUID().toString().toLowerCase().replaceAll("\\W", ""));
	}
}
