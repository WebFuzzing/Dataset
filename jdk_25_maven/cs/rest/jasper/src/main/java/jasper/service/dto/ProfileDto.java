package jasper.service.dto;

import jakarta.validation.constraints.Pattern;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.validator.constraints.Length;

import java.io.Serializable;

import static jasper.domain.User.QTAG_REGEX;
import static jasper.domain.proj.Tag.QTAG_LEN;

@Getter
@Setter
public class ProfileDto implements Serializable {
	@Pattern(regexp = QTAG_REGEX)
	@Length(max = QTAG_LEN)
	private String tag;
	private boolean active;
	private String password;
	private String role;
}
