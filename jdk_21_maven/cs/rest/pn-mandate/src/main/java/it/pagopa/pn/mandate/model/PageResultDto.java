package it.pagopa.pn.mandate.model;

import lombok.Builder;
import lombok.Data;

import java.util.List;

@Builder
@Data
public class PageResultDto<V, K> {
    private List<V> page;
    private boolean more;
    private List<K> nextPagesKey;
}
