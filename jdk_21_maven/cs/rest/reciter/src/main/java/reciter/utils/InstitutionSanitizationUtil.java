package reciter.utils;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import reciter.model.identity.Identity;
import reciter.model.identity.OrganizationalUnit;
import reciter.model.identity.OrganizationalUnit.OrganizationalUnitType;
import reciter.model.identity.OrganizationalUnitSynonym;

public class InstitutionSanitizationUtil {

	private static final Logger log = LoggerFactory.getLogger(InstitutionSanitizationUtil.class);

	private static final List<List<String>> synonymGroups;

	static {
		List<List<String>> loaded = new ArrayList<>();
		try (InputStream is = InstitutionSanitizationUtil.class.getResourceAsStream("/files/OrganizationalUnitSynonyms.json")) {
			if (is != null) {
				ObjectMapper mapper = new ObjectMapper();
				List<OrganizationalUnitSynonym> synonyms = mapper.readValue(is, new TypeReference<List<OrganizationalUnitSynonym>>() {});
				for (OrganizationalUnitSynonym syn : synonyms) {
					loaded.add(syn.getMembers());
				}
				log.info("Loaded {} organizational unit synonym groups", loaded.size());
			} else {
				log.warn("OrganizationalUnitSynonyms.json not found on classpath");
			}
		} catch (Exception e) {
			log.error("Failed to load OrganizationalUnitSynonyms.json", e);
		}
		synonymGroups = loaded;
	}

	/**
	 * This function gets orgUnits from Identity and organizationalUnitSynonyms from JSON and returns a unique set of Departments.
	 * It also substitutes any "and" for "&" and vice versa in identity departments, removes commas or dashes,
	 * and substitutes "Tri-I" for "Tri-Institutional" and vice versa.
	 * @see <a href="https://github.com/wcmc-its/ReCiter/issues/264">Issue Details</a>
	 * @param identity
	 */
	public void populateSanitizedIdentityInstitutions(Identity identity) {
		Set<OrganizationalUnit> sanitizedIdentityInstitutions = new HashSet<>();
		Map<String, List<String>> identityOrgUnitToSynonymMap = new HashMap<>();

		if (identity.getOrganizationalUnits() != null && identity.getOrganizationalUnits().size() > 0) {
			for (OrganizationalUnit orgUnit : identity.getOrganizationalUnits()) {
				List<List<String>> matchedGroups = synonymGroups.stream()
						.filter(group -> group.contains(orgUnit.getOrganizationalUnitLabel()))
						.collect(Collectors.toList());

				if (!matchedGroups.isEmpty()) {
					for (List<String> group : matchedGroups) {
						identityOrgUnitToSynonymMap.put(orgUnit.getOrganizationalUnitLabel(), group);
						for (String synonym : group) {
							OrganizationalUnit expanded = new OrganizationalUnit();
							expanded.setOrganizationalUnitLabel(synonym.trim());
							expanded.setOrganizationalUnitType(OrganizationalUnitType.DEPARTMENT);
							sanitizedIdentityInstitutions.add(expanded);
						}
					}
				} else {
					sanitizedIdentityInstitutions.add(orgUnit);
				}
			}
		}
		identity.setSanitizedIdentityInstitutions(sanitizedIdentityInstitutions);
		identity.setIdentityOrgUnitToSynonymMap(identityOrgUnitToSynonymMap);
	}
}
