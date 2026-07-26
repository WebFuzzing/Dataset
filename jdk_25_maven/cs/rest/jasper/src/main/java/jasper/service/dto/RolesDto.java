package jasper.service.dto;

import lombok.Builder;
import lombok.Getter;

import java.io.Serializable;

@Getter
@Builder
public class RolesDto implements Serializable {
	boolean debug;
	String tag;
	boolean admin;
	boolean mod;
	boolean editor;
	boolean user;
	boolean viewer;
	boolean banned;
}
