package com.xavopls.ibkr_dashboard.repository;

import com.xavopls.ibkr_dashboard.entity.Trade;
import com.xavopls.ibkr_dashboard.enums.TradeDirection;
import com.xavopls.ibkr_dashboard.repository.projection.DailyRealizedPnl;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface TradeRepository extends JpaRepository<Trade, Long> {

    @Query("select max(t.tradeDate) from Trade t where t.account.id = :accountId")
    Optional<LocalDate> findLatestTradeDateByAccountId(@Param("accountId") Long accountId);

    @Query("select min(t.tradeDate) from Trade t where t.account.id = :accountId")
    Optional<LocalDate> findEarliestTradeDateByAccountId(@Param("accountId") Long accountId);

    @Query("""
            select t.tradeDate as date,
                   coalesce(sum(coalesce(t.realizedPnlBase, t.realizedPnl, 0)), 0) as realizedPnl
            from Trade t
            where t.account.id = :accountId
              and t.tradeDate between :from and :to
            group by t.tradeDate
            order by t.tradeDate
            """)
    List<DailyRealizedPnl> findDailyRealizedPnl(@Param("accountId") Long accountId,
                                                @Param("from") LocalDate from,
                                                @Param("to") LocalDate to);

    boolean existsByAccountIdAndIbkrExecutionId(Long accountId, String ibkrExecutionId);

    Optional<Trade> findByAccountIdAndIbkrExecutionId(Long accountId, String ibkrExecutionId);

    boolean existsByAccountIdAndInstrumentIdAndTradeDateAndDirectionAndQuantityAndPrice(
            Long accountId,
            Long instrumentId,
            LocalDate tradeDate,
            TradeDirection direction,
            BigDecimal quantity,
            BigDecimal price
    );

}
