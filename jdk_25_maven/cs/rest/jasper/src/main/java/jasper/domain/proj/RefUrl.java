package jasper.domain.proj;

import static org.apache.commons.lang3.StringUtils.isNotBlank;

public interface RefUrl {
	String getUrl();
	String getProxy();

	default String get() {
		if (isNotBlank(getProxy())) return getProxy();
		return getUrl();
	}
}
