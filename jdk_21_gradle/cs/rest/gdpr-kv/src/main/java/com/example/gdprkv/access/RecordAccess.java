package com.example.gdprkv.access;

import com.example.gdprkv.models.Record;
import java.util.List;
import java.util.Optional;

public interface RecordAccess {
    Optional<Record> findBySubjectIdAndRecordKey(String subjectId, String recordKey);

    /**
     * Finds all records for a specific subject, ordered by record key ascending.
     * Used for subject access requests and data export.
     *
     * @param subjectId the subject ID to query
     * @return list of all records for the subject, ordered by record key
     */
    List<Record> findAllBySubjectId(String subjectId);

    /**
     * Finds tombstoned records that are due for purging using the records_by_purge_due GSI.
     * Queries records in a specific purge bucket that have purge_due_at <= cutoffTimestamp.
     *
     * @param purgeBucket the purge bucket to query (e.g., "h#20250827T21")
     * @param cutoffTimestamp records with purge_due_at <= this value will be returned
     * @return list of records due for purging in the specified bucket
     */
    List<Record> findRecordsDueForPurge(String purgeBucket, long cutoffTimestamp);

    Record save(Record record);

    /**
     * Permanently deletes a record from the database.
     * Used by the purge sweeper to physically remove tombstoned records.
     *
     * @param record the record to delete
     */
    void delete(Record record);
}
