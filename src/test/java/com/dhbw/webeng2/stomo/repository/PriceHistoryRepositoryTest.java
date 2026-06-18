package com.dhbw.webeng2.stomo.repository;

import com.dhbw.webeng2.stomo.model.entity.PriceHistory;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Confirms the price-history cache uses optimistic locking — the {@code @Version} column is
 * managed by the DB and bumped on update, which is what coordinates concurrent refreshes
 * across backend instances.
 */
@SpringBootTest
@ActiveProfiles("test")
@Transactional
class PriceHistoryRepositoryTest {

    @Autowired
    private PriceHistoryRepo repo;
    @PersistenceContext
    private EntityManager em;

    @Test
    void versionIsAssignedOnInsertAndBumpedOnUpdate() {
        PriceHistory entity = new PriceHistory();
        entity.setSymbol("AAPL");
        entity.setBarsJson("[]");
        entity.setFineBarsJson("[]");
        entity.setFetchedAt(Instant.now());
        repo.saveAndFlush(entity);
        Long afterInsert = entity.getVersion();

        entity.setBarsJson("[{\"close\":1}]");
        repo.saveAndFlush(entity);
        em.flush();

        assertThat(afterInsert).isNotNull();
        assertThat(entity.getVersion()).isGreaterThan(afterInsert);
    }
}
