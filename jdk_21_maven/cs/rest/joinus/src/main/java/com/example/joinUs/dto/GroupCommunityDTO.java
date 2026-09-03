package com.example.joinUs.dto;

import com.example.joinUs.model.neo4j.GroupNeo4J;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;


@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class GroupCommunityDTO {

    private GroupNeo4J group1;
    private GroupNeo4J group2;
    private long sharedMembers;
}

