package jasper.service;

import io.micrometer.core.annotation.Timed;
import jasper.domain.Ref_;
import jasper.errors.NotFoundException;
import jasper.repository.RefRepository;
import jasper.repository.filter.RefFilter;
import jasper.service.dto.DtoMapper;
import jasper.service.dto.RefNodeDto;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.access.prepost.PostAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import static org.springframework.data.domain.Sort.Order.desc;
import static org.springframework.data.domain.Sort.by;

@Service
@Transactional(readOnly = true)
public class GraphService {
	private static final Logger logger = LoggerFactory.getLogger(GraphService.class);

	@Autowired
	RefRepository refRepository;

	@Autowired
	DtoMapper mapper;

	@PostAuthorize("@auth.canReadRef(returnObject)")
	@Timed(value = "jasper.service", extraTags = {"service", "graph"}, histogram = true)
	public RefNodeDto get(String url) {
		var page = refRepository.findAll(RefFilter.builder().url(url).build().spec(),
			PageRequest.of(0, 1, by(desc(Ref_.MODIFIED))));
		if (page.isEmpty()) throw new NotFoundException("Ref " + url);
		return mapper.domainToNodeDto(page.getContent().get(0));
	}
}
