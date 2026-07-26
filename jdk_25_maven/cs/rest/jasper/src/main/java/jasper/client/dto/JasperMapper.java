package jasper.client.dto;

import jasper.domain.Ref;
import jasper.domain.User;
import jasper.service.dto.RefReplDto;
import jasper.service.dto.UserDto;
import org.mapstruct.Mapper;

/**
 * Non-filtering mapper.
 */
@Mapper(componentModel = "spring")
public abstract class JasperMapper {
	public abstract RefReplDto domainToDto(Ref ref);
	public abstract UserDto domainToDto(User user);
}
