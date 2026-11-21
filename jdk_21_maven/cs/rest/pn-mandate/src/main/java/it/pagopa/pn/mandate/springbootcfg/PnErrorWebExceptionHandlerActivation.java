package it.pagopa.pn.mandate.springbootcfg;

import com.fasterxml.jackson.databind.ObjectMapper;
import it.pagopa.pn.commons.exceptions.ExceptionHelper;
import it.pagopa.pn.commons.exceptions.PnErrorWebExceptionHandler;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.core.annotation.Order;

@Configuration
@Order(-2)
@Import(ExceptionHelper.class)
public class PnErrorWebExceptionHandlerActivation extends PnErrorWebExceptionHandler {
    public PnErrorWebExceptionHandlerActivation(ExceptionHelper exceptionHelper, ObjectMapper objectMapper) {
        super(exceptionHelper,objectMapper);
    }
}