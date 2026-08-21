package jasper.component.dto;

import jasper.domain.Ext;
import jasper.domain.Plugin;
import jasper.domain.Ref;
import jasper.domain.Template;
import jasper.domain.User;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class Bundle {
	private Ref[] ref;
	private Ext[] ext;
	private Plugin[] plugin;
	private Template[] template;
	private User[] user;
}
