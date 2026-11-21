package it.pagopa.pn.mandate.utils;

import it.pagopa.pn.mandate.model.WorkFlowType;
import lombok.Getter;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static it.pagopa.pn.mandate.middleware.db.entities.MandateEntity.COL_S_WORKFLOW_TYPE;

@Getter
public enum TypeSegregatorFilter {
    STANDARD(true, WorkFlowType.STANDARD, WorkFlowType.REVERSE) {
        @Override
        public String buildExpression(Map<String, AttributeValue> expressionValues) {
            return "(attribute_not_exists("+ COL_S_WORKFLOW_TYPE + ") OR " + super.buildExpression(expressionValues) + ")";
        }
    },
    CIE(false, WorkFlowType.CIE),
    ALL(true, WorkFlowType.values()) {
        @Override
        public String buildExpression(Map<String, AttributeValue> expressionValues) {
            return "(attribute_not_exists(" + COL_S_WORKFLOW_TYPE + ") OR " + super.buildExpression(expressionValues) + ")";
        }
    }; // Include tutti i tipi di workflow;

    private final List<WorkFlowType> types;
    private final boolean includesNull;

    TypeSegregatorFilter(boolean includesNull, WorkFlowType... types) {
        this.includesNull = includesNull;
        this.types = List.of(types);
    }

    /**
     * Restituisce il segregatore da utilizzare in base al workflowType.
     * Se il workflowType è null, restituisce il filtro STANDARD.
     *
     * @param workFlowType the WorkFlowType to find the filter for
     * @return the corresponding WorkflowTypeFilter
     * @throws IllegalArgumentException if the WorkFlowType is unknown
     */
    public static TypeSegregatorFilter fromWorkflowType(WorkFlowType workFlowType) {
        if(workFlowType == null) {
            return STANDARD; // Default to STANDARD if workFlowType is null
        }
        for (TypeSegregatorFilter filter : values()) {
            if (filter.getTypes().contains(workFlowType)) {
                return filter;
            }
        }

        throw new IllegalArgumentException("Unknown WorkFlowType: " + workFlowType);
    }

    /**
     * Costruisce l'espressione di filtro per DynamoDB in base ai tipi di workflow associati al filtro.
     * Popola la mappa expressionValues con i valori necessari per l'espressione.
     *
     * @param expressionValues la mappa da popolare con i valori dell'espressione
     * @return l'espressione di filtro come stringa
     */
    public String buildExpression(Map<String, AttributeValue> expressionValues) {
        return IntStream.range(0, this.getTypes().size())
                .mapToObj(i -> {
                    WorkFlowType type = this.getTypes().get(i);
                    String paramName = ":type" + i;
                    expressionValues.put(paramName, AttributeValue.builder().s(type.name()).build());
                    return COL_S_WORKFLOW_TYPE + " = " + paramName;
                })
                .collect(Collectors.joining(" OR "));
    }

    /**
     * Verifica se un dato WorkFlowType è incluso nel filtro.
     *
     * @param workflowType il WorkFlowType da verificare
     * @return true se il WorkFlowType è incluso nel filtro, false altrimenti
     */
    public boolean isIncluded(WorkFlowType workflowType) {
        // Se il workflowType è null, controlla il flag 'includeNull'
        if (workflowType == null) {
            return this.includesNull;
        }

        return this.types.contains(workflowType);
    }
}
