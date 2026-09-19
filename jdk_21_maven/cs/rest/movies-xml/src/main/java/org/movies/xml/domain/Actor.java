package org.movies.xml.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;

@Embeddable
public class Actor {

    @Column(name = "actor_name", nullable = false, length = 120)
    private String name;

    @Column(name = "billing")
    private Integer billing;

    protected Actor() {
    }

    public Actor(String name, Integer billing) {
        this.name = name;
        this.billing = billing;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Integer getBilling() {
        return billing;
    }

    public void setBilling(Integer billing) {
        this.billing = billing;
    }
}
