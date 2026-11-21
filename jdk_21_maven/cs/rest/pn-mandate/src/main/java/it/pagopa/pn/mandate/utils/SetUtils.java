package it.pagopa.pn.mandate.utils;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

import java.util.HashSet;
import java.util.Set;

@NoArgsConstructor(access = AccessLevel.NONE)
public class SetUtils {

    public static Set<String> getDiffFromSet1ToSet2(Set<String> set1, Set<String> set2) {
        Set<String> diff = new HashSet<>();
        if (set1 != null) {
            diff.addAll(set1);
        }
        if (set2 != null) {
            diff.removeAll(set2);
        }
        return diff;
    }
}
