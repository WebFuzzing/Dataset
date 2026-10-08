package it.pagopa.pn.papertracker.config.springbootcfg;


import it.pagopa.pn.commons.exceptions.ExceptionHelper;
import it.pagopa.pn.commons.exceptions.PnResponseEntityExceptionHandler;
import org.springframework.context.annotation.Import;

@org.springframework.web.bind.annotation.ControllerAdvice
@Import(ExceptionHelper.class)
public class PnResponseEntityExceptionHandlerActivation extends PnResponseEntityExceptionHandler {
    public PnResponseEntityExceptionHandlerActivation(ExceptionHelper exceptionHelper) {
        super(exceptionHelper);
    }
}
