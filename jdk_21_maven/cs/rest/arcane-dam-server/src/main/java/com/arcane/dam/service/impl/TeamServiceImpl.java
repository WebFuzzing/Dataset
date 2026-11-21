package com.arcane.dam.service.impl;

import com.arcane.dam.dto.TeamDTO;
import com.arcane.dam.entity.Team;
import com.arcane.dam.repository.TeamRepository;
import com.arcane.dam.service.TeamService;
import lombok.RequiredArgsConstructor;
import org.modelmapper.ModelMapper;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class TeamServiceImpl implements TeamService {

    private final TeamRepository teamRepository;
    private final ModelMapper modelMapper;

    @Override
    public List<TeamDTO> getAllTeams() {
        List<Team> teams = teamRepository.findAll();

        List<TeamDTO> teamDTOs = new ArrayList<>();

        for (Team team : teams) {
            TeamDTO teamDTO = modelMapper.map(team, TeamDTO.class);
            teamDTOs.add(teamDTO);
        }
        return teamDTOs;
    }

    @Override
    public TeamDTO createTeam(TeamDTO teamDTO) {
        teamDTO.setCreatedAt(Instant.now());
        teamDTO.setUpdatedAt(Instant.now());
        teamDTO.setIsEnabled(true);
        return modelMapper.map(
                teamRepository.save(modelMapper.map(
                        teamDTO, Team.class)
                ),TeamDTO.class
        );
    }

    @Override
    public boolean updateTeam(TeamDTO teamDTO) {
        teamDTO.setUpdatedAt(Instant.now());
        TeamDTO updatedTeam = modelMapper.map(
                teamRepository.update(modelMapper.map(
                        teamDTO, Team.class)
                ),TeamDTO.class
        );
        return updatedTeam != null;
    }

    @Override
    public boolean deleteTeam(String id) {
        return teamRepository.delete(id);
    }

    @Override
    public TeamDTO getTeam(String id) {
        return null;
    }
}
