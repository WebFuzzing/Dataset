package jasper.management;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.stereotype.Service;

@Service
public class SecurityMetersService {

    public static final String INVALID_TOKENS_METER_NAME = "security.authentication.invalid-tokens";
    public static final String INVALID_TOKENS_METER_DESCRIPTION =
        "Indicates validation error count of the tokens presented by the clients.";
    public static final String INVALID_TOKENS_METER_BASE_UNIT = "errors";
    public static final String INVALID_TOKENS_METER_CAUSE_DIMENSION = "cause";

    private final Counter tokenInvalidAudienceCounter;
    private final Counter tokenInvalidSignatureCounter;
    private final Counter tokenExpiredCounter;
    private final Counter tokenUnsupportedCounter;
    private final Counter tokenMalformedCounter;
    private final Counter unverifiedEmailCounter;

    public SecurityMetersService(MeterRegistry registry) {
        tokenInvalidAudienceCounter = invalidTokensCounterForCauseBuilder("invalid-audience").register(registry);
        tokenInvalidSignatureCounter = invalidTokensCounterForCauseBuilder("invalid-signature").register(registry);
        tokenExpiredCounter = invalidTokensCounterForCauseBuilder("expired").register(registry);
        tokenUnsupportedCounter = invalidTokensCounterForCauseBuilder("unsupported").register(registry);
        tokenMalformedCounter = invalidTokensCounterForCauseBuilder("malformed").register(registry);
        unverifiedEmailCounter = invalidTokensCounterForCauseBuilder("email-not-verified").register(registry);
    }

    private Counter.Builder invalidTokensCounterForCauseBuilder(String cause) {
        return Counter
            .builder(INVALID_TOKENS_METER_NAME)
            .baseUnit(INVALID_TOKENS_METER_BASE_UNIT)
            .description(INVALID_TOKENS_METER_DESCRIPTION)
            .tag(INVALID_TOKENS_METER_CAUSE_DIMENSION, cause);
    }

    public void trackTokenInvalidAudience() {
        tokenInvalidAudienceCounter.increment();
    }

    public void trackTokenInvalidSignature() {
        tokenInvalidSignatureCounter.increment();
    }

    public void trackTokenExpired() {
        tokenExpiredCounter.increment();
    }

    public void trackTokenUnsupported() {
        tokenUnsupportedCounter.increment();
    }

    public void trackTokenMalformed() {
        tokenMalformedCounter.increment();
    }

    public void trackUnverifiedEmail() {
        unverifiedEmailCounter.increment();
    }
}
