package reciter.feedback;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

/**
 * Coverage for {@link EntryPath#fromString(String)}, the PM-UI -> Java decoder.
 * The PM_AUTHOR value (Phase 34) must resolve case-insensitively, and unknown/null
 * inputs must fall back to the safe default CANDIDATE_LIST.
 */
public class EntryPathTest {

    @Test
    public void resolvesPmAuthorExact() {
        assertEquals(EntryPath.PM_AUTHOR, EntryPath.fromString("PM_AUTHOR"));
    }

    @Test
    public void resolvesPmAuthorCaseInsensitive() {
        assertEquals(EntryPath.PM_AUTHOR, EntryPath.fromString("pm_author"));
        assertEquals(EntryPath.PM_AUTHOR, EntryPath.fromString("Pm_Author"));
    }

    @Test
    public void resolvesPmAuthorWithSurroundingWhitespace() {
        assertEquals(EntryPath.PM_AUTHOR, EntryPath.fromString("  PM_AUTHOR  "));
    }

    @Test
    public void resolvesOtherKnownValues() {
        assertEquals(EntryPath.PUBMED_SEARCH, EntryPath.fromString("pubmed_search"));
        assertEquals(EntryPath.CANDIDATE_LIST, EntryPath.fromString("candidate_list"));
    }

    @Test
    public void nullEmptyAndUnknownFallBackToCandidateList() {
        assertEquals(EntryPath.CANDIDATE_LIST, EntryPath.fromString(null));
        assertEquals(EntryPath.CANDIDATE_LIST, EntryPath.fromString(""));
        assertEquals(EntryPath.CANDIDATE_LIST, EntryPath.fromString("not_a_real_path"));
    }
}
