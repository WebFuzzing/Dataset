package jasper.security;

import jasper.component.ConfigCache;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.net.InetAddress;
import java.net.URI;

import static org.apache.commons.collections4.CollectionUtils.isNotEmpty;

@Component
public class HostCheck {
	private static final Logger logger = LoggerFactory.getLogger(HostCheck.class);

	@Autowired
	ConfigCache configs;

	public boolean validHost(URI uri) {
		var root = configs.root();
		try {
			var host = InetAddress.getByName(uri.getHost());
			if (root.getHostWhitelist() != null && !root.getHostWhitelist().isEmpty()) {
				if (!whitelisted(uri.getHost())) return false;
			} else {
				if (host.isLoopbackAddress()) return false;
				if (host.isMulticastAddress()) return false;
				if (host.isAnyLocalAddress()) return false;
				if (host.isSiteLocalAddress()) return false;
			}
			if (isNotEmpty(root.getHostBlacklist())) {
				for (var h : root.getHostBlacklist()) {
					if (uri.getHost().equals(h)) return false;
				}
			}
		} catch (IOException e) {
			return false;
		}
		return true;
	}

	private boolean whitelisted(String host) {
		for (var h : configs.root().getHostWhitelist()) {
			if (host.equals(h)) return true;
		}
		return false;
	}
}
