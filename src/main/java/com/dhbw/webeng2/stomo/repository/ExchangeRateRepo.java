package com.dhbw.webeng2.stomo.repository;

import com.dhbw.webeng2.stomo.model.entity.ExchangeRate;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ExchangeRateRepo extends JpaRepository<ExchangeRate, String> {
}
