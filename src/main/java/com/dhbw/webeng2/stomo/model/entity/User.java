package com.dhbw.webeng2.stomo.model.entity;

import com.dhbw.webeng2.stomo.model.enums.Status;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
@Entity
@Table(name = "user")
public class User {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = false, nullable = false)
    private String firstname;

    @Column(unique = false, nullable = false)
    private String lastname;

    @Column(unique = true, nullable = false)
    private String email;

    @Column(unique = false, nullable = false)
    private String password;

    @Column(unique = false, nullable = false)
    private Status status;

    @Column(unique = false, nullable = true)
    private List<PortfolioItem> portfolioItems;
}
