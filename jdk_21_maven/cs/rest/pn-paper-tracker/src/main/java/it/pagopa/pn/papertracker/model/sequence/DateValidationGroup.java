package it.pagopa.pn.papertracker.model.sequence;

import lombok.Getter;

import java.util.Set;

@Getter
public enum DateValidationGroup {
    RECRN005C(Set.of("RECRN005A", "RECRN005B", "RECRN005C"), Set.of()),
    RECRN004C(Set.of("RECRN004A", "RECRN004B", "RECRN004C"), Set.of()),
    RECRN003C(Set.of("RECRN003A", "RECRN003B", "RECRN003C"), Set.of()),
    RECRN002F(Set.of("RECRN002D", "RECRN002E", "RECRN002F"), Set.of()),
    RECRN002C(Set.of("RECRN002A", "RECRN002B", "RECRN002C"), Set.of()),
    RECRN001C(Set.of("RECRN001A", "RECRN001B", "RECRN001C"), Set.of()),
    RECRI003C(Set.of("RECRI003A", "RECRI003B", "RECRI003C"), Set.of()),
    RECRI004C(Set.of("RECRI004A", "RECRI004B", "RECRI004C"), Set.of()),
    RECAG001C(Set.of("RECAG001A", "RECAG001B", "RECAG001C"), Set.of()),
    RECAG002C(Set.of("RECAG002A", "RECAG002B", "RECAG002C"), Set.of()),
    RECAG003C(Set.of("RECAG003A", "RECAG003B", "RECAG003C"), Set.of()),
    RECAG003F(Set.of("RECAG003D", "RECAG003E", "RECAG003F"), Set.of()),
    RECAG005C(Set.of("RECAG005A", "RECAG005B", "RECAG005C"), Set.of("RECAG011A","RECAG011B")),
    RECAG006C(Set.of("RECAG006A", "RECAG006B", "RECAG006C"), Set.of("RECAG011A","RECAG011B")),
    RECAG007C(Set.of("RECAG007A", "RECAG007B", "RECAG007C"), Set.of("RECAG011A","RECAG011B")),
    RECAG008C(Set.of("RECAG008A", "RECAG008B", "RECAG008C"), Set.of("RECAG011A","RECAG011B")),
    RECRS001C(Set.of("RECRS001C"), Set.of()),
    RECRS002C(Set.of("RECRS002A","RECRS002B","RECRS002C"), Set.of()),
    RECRS002F(Set.of("RECRS002D","RECRS002E","RECRS002F"), Set.of()),
    RECRS003C(Set.of("RECRS003C"), Set.of()),
    RECRS004C(Set.of("RECRS004A","RECRS004B","RECRS004C"), Set.of()),
    RECRS005C(Set.of("RECRS005A","RECRS005B","RECRS005C"), Set.of()),
    RECRSI003C(Set.of("RECRSI003C"), Set.of()),
    RECRSI004C(Set.of("RECRSI004A","RECRSI004B","RECRSI004C"), Set.of());


    private final Set<String> finalEventDateValidationGroup;
    private final Set<String> stockEventsDateValidationGroup;

    DateValidationGroup(Set<String> finalEventDateValidationGroup, Set<String> stockEventsDateValidationGroup) {
        this.finalEventDateValidationGroup = finalEventDateValidationGroup;
        this.stockEventsDateValidationGroup = stockEventsDateValidationGroup;
    }
}
