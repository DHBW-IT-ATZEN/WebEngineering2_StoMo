package com.dhbw.webeng2.stomo.model.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

/**
 * Persistent cache for Yoda translations. A translation is deterministic, so each
 * distinct source string is fetched from the paid API at most once — ever.
 */
@Getter
@Setter
@Entity
@Table(name = "yoda_translations")
public class YodaTranslation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 512)
    private String source;

    @Column(nullable = false, length = 2048)
    private String translated;
}
