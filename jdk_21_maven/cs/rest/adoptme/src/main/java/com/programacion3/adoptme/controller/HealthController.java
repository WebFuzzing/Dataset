package com.programacion3.adoptme.controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class HealthController {

    @GetMapping("/ping")
    public String ping() {
        return "OK 🐶 AdoptMe funcionando!";
    }
}
