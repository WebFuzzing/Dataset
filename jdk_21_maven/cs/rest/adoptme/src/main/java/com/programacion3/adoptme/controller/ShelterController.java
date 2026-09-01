package com.programacion3.adoptme.controller;

import com.programacion3.adoptme.domain.Shelter;
import com.programacion3.adoptme.repo.ShelterRepository;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/shelters")
public class ShelterController {
    private final ShelterRepository repo;
    public ShelterController(ShelterRepository repo) { this.repo = repo; }

    @GetMapping
    public List<Shelter> all() { return repo.findAll(); }
}
