package reciter.xml.retriever.pubmed;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.concurrent.atomic.AtomicBoolean;

import org.junit.jupiter.api.Test;

class RetrievalErrorTrackerTest {

    @Test
    void resetClearsFlagAndMarkErrorSetsIt() {
        RetrievalErrorTracker.reset();
        assertFalse(RetrievalErrorTracker.hadError());

        RetrievalErrorTracker.markError();
        assertTrue(RetrievalErrorTracker.hadError());

        RetrievalErrorTracker.reset();
        assertFalse(RetrievalErrorTracker.hadError());
    }

    /** Each uid retrieval runs on its own thread — one run's failure must not leak to another. */
    @Test
    void errorFlagIsThreadScoped() throws InterruptedException {
        RetrievalErrorTracker.reset();
        RetrievalErrorTracker.markError();
        assertTrue(RetrievalErrorTracker.hadError());

        AtomicBoolean otherThreadSawError = new AtomicBoolean(true);
        Thread other = new Thread(() -> otherThreadSawError.set(RetrievalErrorTracker.hadError()));
        other.start();
        other.join();

        assertFalse(otherThreadSawError.get(), "a different thread must not see another run's error flag");
    }
}
