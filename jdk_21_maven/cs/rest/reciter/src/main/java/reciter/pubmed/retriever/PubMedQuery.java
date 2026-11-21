package reciter.pubmed.retriever;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import org.apache.commons.lang3.StringUtils;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

@Builder
@Getter
@Setter
public class PubMedQuery {
    private static SimpleDateFormat dt = new SimpleDateFormat("yyyy/MM/dd");

    @JsonProperty("author")
    private String author;

    @JsonProperty("start")
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy/MM/dd")
    private Date start;

    @JsonProperty("end")
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy/MM/dd")
    private Date end;

    @JsonProperty("strategy-query")
    private String strategyQuery;

    @Override
    public String toString() {
//        return author + "[au]" + " AND " + dt.format(start) + ":" + dt.format(end) + "[DP]";
        List<String> parts = new ArrayList<String>();
        if (author != null) {
            //parts.add(author + " [au]");
        	parts.add(author);
        }
        //Added both [DP] and [EDAT] for better capture of pubs : Date of publication - Date added to Entrez
        if (start != null && end != null) {
            parts.add("((" + dt.format(start) + ":" + dt.format(end) + "[EDAT]" + ") OR (" + dt.format(start) + ":" + dt.format(end) + "[DP]))");
        }
        if (strategyQuery != null && !strategyQuery.isBlank()) {
            parts.add(strategyQuery);
        }

        return StringUtils.join(parts, " AND ");
    }
}
