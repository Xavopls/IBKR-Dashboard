package com.xavopls.ibkr_dashboard.repository;

import com.xavopls.ibkr_dashboard.entity.Instrument;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface InstrumentRepository extends JpaRepository<Instrument, Long> {

    Optional<Instrument> findBySymbolAndExchange(String symbol, String exchange);
}
