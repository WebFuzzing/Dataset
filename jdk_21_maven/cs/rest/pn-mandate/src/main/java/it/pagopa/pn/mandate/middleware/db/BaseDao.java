package it.pagopa.pn.mandate.middleware.db;

import software.amazon.awssdk.enhanced.dynamodb.Key;

public class BaseDao {

    public static final String GSI_INDEX_DELEGATE_STATE = "delegate-state-gsi";

    protected Key getKeyBuild(String pk) {
        return getKeyBuild(pk, null);
    }

    protected Key getKeyBuild(String pk, String sk) {
        if (sk == null)
                return Key.builder().partitionValue(pk).build();
        else
                return Key.builder().partitionValue(pk).sortValue(sk).build();
    }

    protected Key getKeyBuild(String pk, int sk) {
        return Key.builder().partitionValue(pk).sortValue(sk).build();
    }

}
