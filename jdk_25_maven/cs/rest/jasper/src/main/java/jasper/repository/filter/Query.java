package jasper.repository.filter;

public interface Query {
//	String ATOM = "!?" + QualifiedTag.SELECTOR;
//	String ATOMS = ATOM + "(?:[|:]" + ATOM + ")*";
//	String GROUP = "(?:" + ATOMS + "|\\(" + ATOMS + "\\))"; // Allow  parentheses
//	String GROUPS = GROUP + "(?:[|:]" + GROUP + ")*";
//	String GROUP2 = "(?:" + GROUPS + "|\\(" + GROUPS + "\\))";  // Allow nested parentheses
//	String GROUP2S = GROUP2 + "(?:[|:]" + GROUP2 + ")*";
//	String GROUP3 = "(?:" + GROUP2S + "|\\(" + GROUP2S + "\\))";  // Allow third parentheses
//	String GROUP3S = GROUP3 + "(?:[|:]" + GROUP3 + ")*";
//	String REGEX = GROUP3S;
	// TODO: Query with || causes infinite loop in validator
	String REGEX = "[!_+a-z0-9/.@|:()*]+";

	int QUERY_LEN = 4096;
	int SEARCH_LEN = 512;

	String getQuery();
}
