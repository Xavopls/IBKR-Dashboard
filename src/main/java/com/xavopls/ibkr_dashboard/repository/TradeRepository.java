package com.xavopls.ibkr_dashboard.repository;

import com.xavopls.ibkr_dashboard.entity.Trade;
import com.xavopls.ibkr_dashboard.enums.TradeDirection;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Optional;

public interface TradeRepository extends JpaRepository<Trade, Long> {

    @Query("select max(t.tradeDate) from Trade t where t.account.id = :accountId")
    Optional<LocalDate> findLatestTradeDateByAccountId(@Param("accountId") Long accountId);

    boolean existsByAccountIdAndIbkrExecutionId(Long accountId, String ibkrExecutionId);

    boolean existsByAccountIdAndInstrumentIdAndTradeDateAndDirectionAndQuantityAndPrice(
            Long accountId,
            Long instrumentId,
            LocalDate tradeDate,
            TradeDirection direction,
            BigDecimal quantity,
            BigDecimal price
    );

}
