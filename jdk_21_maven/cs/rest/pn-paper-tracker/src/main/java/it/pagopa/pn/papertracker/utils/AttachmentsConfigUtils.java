package it.pagopa.pn.papertracker.utils;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor(access = AccessLevel.NONE)
public class AttachmentsConfigUtils {

    static String SAFESTORAGE_PREFIX = "safestorage://";

    public static String cleanFileKey(String fileKey) {
        return cleanFileKey(fileKey, true);
    }

    public static String cleanFileKey(String fileKey, boolean cleanSafestoragePrefix) {
        if (fileKey == null) {
            return null;
        }

        StringBuilder fileKeyNew = new StringBuilder();

        if (cleanSafestoragePrefix && fileKey.contains(SAFESTORAGE_PREFIX)){
            //clean safestorage://
            fileKeyNew.append(fileKey.replace(SAFESTORAGE_PREFIX, ""));
        }
        else {
            fileKeyNew.append(fileKey);
        }

        var queryParamStartIndex = fileKeyNew.indexOf("?");
        if(queryParamStartIndex != -1) {
            //clean all query params
            fileKeyNew.delete(queryParamStartIndex, fileKeyNew.length());
        }

        return fileKeyNew.toString();
    }

}
