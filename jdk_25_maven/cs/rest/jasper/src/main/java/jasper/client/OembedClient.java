package jasper.client;

import feign.QueryMap;
import feign.RequestLine;
import org.springframework.cloud.openfeign.FeignClient;

import java.net.URI;
import java.util.Map;

@FeignClient(value = "oembed")
public interface OembedClient {

	@RequestLine("GET")
	String oembed(URI baseUri, @QueryMap Map<String, String> params);
}
