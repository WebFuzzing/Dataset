package jasper.component.channel;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import jasper.component.dto.RefUpdateDto;
import jasper.component.dto.ComponentDtoMapper;
import jasper.service.dto.ExtDto;
import jasper.service.dto.RefDto;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.messaging.support.MessageBuilder;

import java.io.UncheckedIOException;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class StompTest {

	@InjectMocks
	Stomp stompChannel;

	@Mock
	SimpMessagingTemplate stomp;

	@Mock
	ComponentDtoMapper mapper;

	@Mock
	ObjectMapper objectMapper;

	@Test
	void handleRefUpdateSerializesPayloadBeforeSending() throws Exception {
		var ref = new RefDto();
		var updateDto = new RefUpdateDto();
		updateDto.setUrl("https://example.com");
		var message = MessageBuilder.withPayload(ref)
			.setHeader("origin", "")
			.setHeader("url", "https://example.com")
			.build();

		when(mapper.dtoToUpdateDto(ref)).thenReturn(updateDto);
		when(objectMapper.writeValueAsString(updateDto)).thenReturn("{\"url\":\"https://example.com\"}");

		stompChannel.handleRefUpdate(message);

		verify(stomp).convertAndSend(
			"/topic/ref/default/https%3A%2F%2Fexample.com",
			"{\"url\":\"https://example.com\"}"
		);
	}

	@Test
	void handleRefUpdatePropagatesSerializationFailure() throws Exception {
		var ref = new RefDto();
		var updateDto = new RefUpdateDto();
		var message = MessageBuilder.withPayload(ref)
			.setHeader("origin", "")
			.setHeader("url", "https://example.com")
			.build();

		when(mapper.dtoToUpdateDto(ref)).thenReturn(updateDto);
		when(objectMapper.writeValueAsString(updateDto)).thenThrow(new JsonProcessingException("boom") { });

		assertThatThrownBy(() -> stompChannel.handleRefUpdate(message))
			.isInstanceOf(UncheckedIOException.class)
			.hasMessage("Failed to serialize ref websocket payload")
			.hasCauseInstanceOf(JsonProcessingException.class);

		verifyNoInteractions(stomp);
	}

	@Test
	void handleExtUpdatePropagatesSerializationFailure() throws Exception {
		var ext = new ExtDto();
		var message = MessageBuilder.withPayload(ext)
			.setHeader("origin", "")
			.setHeader("tag", "plugin/test")
			.build();

		when(objectMapper.writeValueAsString(ext)).thenThrow(new JsonProcessingException("boom") { });

		assertThatThrownBy(() -> stompChannel.handleExtUpdate(message))
			.isInstanceOf(UncheckedIOException.class)
			.hasMessage("Failed to serialize ext websocket payload")
			.hasCauseInstanceOf(JsonProcessingException.class);

		verifyNoInteractions(stomp);
	}
}
