package com.xavopls.ibkr_dashboard.repository;

import com.xavopls.ibkr_dashboard.entity.DailyPnl;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.Optional;

public interface DailyPnlRepository extends JpaRepository<DailyPnl, Long> {

    Optional<DailyPnl> findByAccountIdAndDate(Long accountId, LocalDate date);
}
