package it.pagopa.pn.mandate.utils;

import it.pagopa.pn.mandate.exceptions.PnForbiddenException;
import it.pagopa.pn.mandate.middleware.db.entities.MandateEntity;
import it.pagopa.pn.mandate.generated.openapi.server.v1.dto.CxTypeAuthFleet;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.springframework.util.CollectionUtils;
import reactor.core.publisher.Mono;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;

import java.util.*;

@lombok.CustomLog
@NoArgsConstructor(access = AccessLevel.NONE)
public class PgUtils {

    public static final Set<String> ALLOWED_ROLES = Set.of("ADMIN");

    private static final String OR = "OR";

    /**
     * Effettua la validazione dell'accesso per le Persone Giuridiche su risorse accessibili solo dagli amministratori.
     *
     * @param pnCxType   tipo utente (PF, PG, PA)
     * @param pnCxRole   ruolo (admin, operator)
     * @param pnCxGroups gruppi
     */
    public static Mono<Object> validaAccessoOnlyAdmin(CxTypeAuthFleet pnCxType, String pnCxRole, List<String> pnCxGroups) {
        String process = "validating access admin only";
        log.logChecking(process);
        if (CxTypeAuthFleet.PG == pnCxType
                && (pnCxRole == null || !ALLOWED_ROLES.contains(pnCxRole.toUpperCase()) || !CollectionUtils.isEmpty(pnCxGroups))) {

            log.logCheckingOutcome(process, false, "only a PG admin can access this resource");
            return Mono.error(new PnForbiddenException());
        }
        log.debug("access granted for {}, role: {}, groups: {}", pnCxType, pnCxRole, pnCxGroups);
        log.logCheckingOutcome(process, true);
        return Mono.just(new Object());
    }

    /**
     * Effettua la validazione dell'accesso per le Persone Giuridiche su risorse accessibili solo dagli amministratori
     * o dagli amministratori di gruppo.
     *
     * @param pnCxType   tipo utente (PF, PG, PA)
     * @param pnCxRole   ruolo (admin, operator)
     * @param pnCxGroups gruppi
     */
    public static Mono<Object> validaAccessoOnlyGroupAdmin(CxTypeAuthFleet pnCxType, String pnCxRole, List<String> pnCxGroups) {
        String process = "validating access admin only";
        log.logChecking(process);
        if (CxTypeAuthFleet.PG == pnCxType && (pnCxRole == null || !ALLOWED_ROLES.contains(pnCxRole.toUpperCase()))) {
            log.logCheckingOutcome(process, false, "only a PG admin / group admin can access this resource");
            return Mono.error(new PnForbiddenException());
        }
        log.debug("access granted for {}, role: {}, groups: {}", pnCxType, pnCxRole, pnCxGroups);
        log.logCheckingOutcome(process, true);
        return Mono.just(new Object());
    }

    /**
     * Costruisce la filter expression per la verifica dei gruppi in caso di PG
     *
     * @param xPagopaPnCxGroups groupsList
     * @param expressionValues  map of AttributeValue
     */
    public static String buildExpressionGroupFilter(List<String> xPagopaPnCxGroups, Map<String, AttributeValue> expressionValues) {
        StringBuilder expressionGroup = new StringBuilder();
        expressionGroup.append("(");
        for (int i = 0; i < xPagopaPnCxGroups.size(); i++) {
            AttributeValue pnCxGroup = AttributeValue.builder().s(xPagopaPnCxGroups.get(i)).build();
            expressionValues.put(":group" + i, pnCxGroup);
            expressionGroup.append(" contains(").append(MandateEntity.COL_A_GROUPS).append(",:group").append(i).append(") ").append(OR);
        }
        expressionGroup.replace(expressionGroup.length() - OR.length(), expressionGroup.length(), ")");
        return expressionGroup.toString();
    }

    /**
     * Costruisce una lista di gruppi da usare in modo sicuro nei filtri.
     * Se valorizzati entrambi, i groups devono essere un sottoinsieme dei cx-groups.
     * Se i groups non sono valorizzati, ma i cx-groups lo sono, allora il metodo ritorna la lista dei cx-groups.
     *
     * @param groups     gruppi (in filtro)
     * @param pnCxGroups gruppi (header cx-groups)
     * @return lista di gruppi
     * @throws PnForbiddenException in caso i groups non siano un sottoinsieme dei cx-groups
     */
    public static List<String> getGroupsForSecureFilter(List<String> groups, List<String> pnCxGroups) {
        List<String> secureGroups;
        if (CollectionUtils.isEmpty(pnCxGroups)) {
            secureGroups = groups;
        } else if (CollectionUtils.isEmpty(groups)) {
            secureGroups = pnCxGroups;
        } else {
            Set<String> setOfPnCxGroups = new HashSet<>(pnCxGroups);
            if (!setOfPnCxGroups.containsAll(groups)) {
                log.warn("groups {} must be a subset of pnCxGroups {}", groups, pnCxGroups);
                throw new PnForbiddenException();
            }
            secureGroups = groups;
        }
        secureGroups = secureGroups != null ? List.copyOf(secureGroups) : Collections.emptyList();
        log.debug("groups: {}, pnCxGroups: {} - secure filter: {}", groups, pnCxGroups, secureGroups);
        return secureGroups;
    }
}
