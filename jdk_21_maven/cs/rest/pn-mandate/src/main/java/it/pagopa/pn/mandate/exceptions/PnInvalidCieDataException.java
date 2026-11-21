package it.pagopa.pn.mandate.exceptions;

import it.pagopa.pn.ciechecker.model.ResultCieChecker;
import it.pagopa.pn.commons.exceptions.PnRuntimeException;
import it.pagopa.pn.mandate.utils.CieErrorCategory;
import lombok.Getter;
import org.springframework.http.HttpStatus;

import java.util.List;

@Getter
public class PnInvalidCieDataException extends PnRuntimeException {
    private final static String MESSAGE_FORMAT = "CIE Data validation error: %s";
    private final static String DESCRIPTION = "CIE Data Validation Failed: Client-side issue encountered";
    private final ResultCieChecker resultCieChecker;
    private final List<CieErrorCategory> errorCategories;

    public PnInvalidCieDataException(ResultCieChecker resultCieChecker, List<CieErrorCategory> errorCategories) {
        this(getErrorCode(errorCategories), getErrorMessage(errorCategories), resultCieChecker, errorCategories);
    }

    public PnInvalidCieDataException(String code, String detail, ResultCieChecker resultCieChecker, List<CieErrorCategory> errorCategories) {
        super(
                String.format(MESSAGE_FORMAT, resultCieChecker.name()),
                DESCRIPTION,
                HttpStatus.UNPROCESSABLE_ENTITY.value(),
                code,
                null,
                detail
        );
        this.resultCieChecker = resultCieChecker;
        this.errorCategories = errorCategories;
    }


    private static String getErrorCode(List<CieErrorCategory> categories) {
        return categories.stream()
                .findFirst()
                .map(CieErrorCategory::getCode)
                .orElse("UNKNOWN_CIE_ERROR");
    }

    private static String getErrorMessage(List<CieErrorCategory> categories) {
        return categories.stream()
                .findFirst()
                .map(CieErrorCategory::getMessage)
                .orElse("Unknown CIE validation error");
    }
}
