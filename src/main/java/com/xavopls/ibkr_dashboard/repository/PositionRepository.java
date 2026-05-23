package com.xavopls.ibkr_dashboard.repository;

import com.xavopls.ibkr_dashboard.entity.Position;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface PositionRepository extends JpaRepository<Position, Long> {

    Optional<Position> findByAccountIdAndInstrumentId(Long accountId, Long instrumentId);
}
