package reciter.service.dynamo;

import java.util.List;

import reciter.api.parameters.GoldStandardUpdateFlag;
import reciter.database.dynamodb.model.GoldStandard;

public interface IDynamoDbGoldStandardService {
    void save(GoldStandard goldStandard, GoldStandardUpdateFlag goldStandardUpdateFlag, String provenanceSource);
    void save(List<GoldStandard> goldStandard, GoldStandardUpdateFlag goldStandardUpdateFlag, String provenanceSource);

    /**
     * Phase 33-02 overload propagating the PM UI entry path so the service can
     * apply Phase 33 D-13 (PM_UI_SEARCH retrieval write) before the D-11 transition.
     * Pre-existing 3-arg signatures default {@code entryPath} to
     * {@link reciter.feedback.EntryPath#CANDIDATE_LIST}.
     */
    void save(GoldStandard goldStandard, GoldStandardUpdateFlag goldStandardUpdateFlag,
              String provenanceSource, reciter.feedback.EntryPath entryPath);
    void save(List<GoldStandard> goldStandard, GoldStandardUpdateFlag goldStandardUpdateFlag,
              String provenanceSource, reciter.feedback.EntryPath entryPath);

    /**
     * Overload carrying the curating user's id (admin_users.userID from Publication
     * Manager) so FeedbackLog rows record who performed the action. Other overloads
     * default {@code curatedBy} to 0 (unknown — bulk/list updates or non-PM callers).
     */
    void save(GoldStandard goldStandard, GoldStandardUpdateFlag goldStandardUpdateFlag,
              String provenanceSource, reciter.feedback.EntryPath entryPath, int curatedBy);
    
    GoldStandard findByUid(String uid);
    List<GoldStandard> findByUids(List<String> uid);
    void delete(String uid);
}
