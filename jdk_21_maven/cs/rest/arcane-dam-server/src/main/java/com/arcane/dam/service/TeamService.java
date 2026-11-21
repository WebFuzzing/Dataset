package com.arcane.dam.service;

import com.arcane.dam.dto.TeamDTO;

import java.util.List;

public interface TeamService {
    List<TeamDTO> getAllTeams();
    TeamDTO createTeam(TeamDTO teamDTO);
    boolean updateTeam(TeamDTO teamDTO);
    boolean deleteTeam(String id);
    TeamDTO getTeam(String id);
}
