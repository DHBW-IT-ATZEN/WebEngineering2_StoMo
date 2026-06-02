package com.dhbw.webeng2.stomo.repository;

import com.dhbw.webeng2.stomo.model.entity.YodaTranslation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface YodaTranslationRepo extends JpaRepository<YodaTranslation, Long> {
    Optional<YodaTranslation> findBySource(String source);

    List<YodaTranslation> findBySourceIn(List<String> sources);
}
