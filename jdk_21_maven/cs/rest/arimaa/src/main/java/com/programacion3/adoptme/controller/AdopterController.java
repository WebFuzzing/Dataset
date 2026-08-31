package com.programacion3.adoptme.controller;

import com.programacion3.adoptme.domain.Adopter;
import com.programacion3.adoptme.repo.AdopterRepository; // usa 'repo' si ese es tu package
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/adopters")
@RequiredArgsConstructor
public class AdopterController {

    private final AdopterRepository adopterRepository;

    @GetMapping
    public List<Adopter> getAll() {
        return adopterRepository.findAll();
    }


    @PostMapping
    public Adopter create(@RequestBody Adopter adopter) {
        return adopterRepository.save(adopter);
    }
}
