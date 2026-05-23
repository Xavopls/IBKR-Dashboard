package com.xavopls.ibkr_dashboard.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Stores the daily P&amp;L snapshot for an account.
 * Populated either by the CSV import process or computed from trades.
 * The {@code cumulativePnl} column is not stored — it is always derived
 * via a window function query to ensure consistency with the raw data.
 */
@Entity
@Table(name = "daily_pnl", uniqueConstraints = {
        @UniqueConstraint(name = "uq_daily_pnl_account_date", columnNames = {"account_id", "date"})
})
public class DailyPnl {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "account_id", nullable = false)
    private Account account;

    @NotNull
    @Column(name = "date", nullable = false)
    private LocalDate date;

    @Column(name = "realized_pnl", precision = 20, scale = 6)
    private BigDecimal realizedPnl;

    @Column(name = "unrealized_pnl", precision = 20, scale = 6)
    private BigDecimal unrealizedPnl;

    @Column(name = "total_pnl", precision = 20, scale = 6)
    private BigDecimal totalPnl;

    // ── Constructors ──────────────────────────────────────────────────────────

    protected DailyPnl() {
    }

    public DailyPnl(Account account, LocalDate date,
                    BigDecimal realizedPnl, BigDecimal unrealizedPnl, BigDecimal totalPnl) {
        this.account = account;
        this.date = date;
        this.realizedPnl = realizedPnl;
        this.unrealizedPnl = unrealizedPnl;
        this.totalPnl = totalPnl;
    }

    // ── Getters & Setters ─────────────────────────────────────────────────────

    public Long getId() { return id; }

    public Account getAccount() { return account; }
    public void setAccount(Account account) { this.account = account; }

    public LocalDate getDate() { return date; }
    public void setDate(LocalDate date) { this.date = date; }

    public BigDecimal getRealizedPnl() { return realizedPnl; }
    public void setRealizedPnl(BigDecimal realizedPnl) { this.realizedPnl = realizedPnl; }

    public BigDecimal getUnrealizedPnl() { return unrealizedPnl; }
    public void setUnrealizedPnl(BigDecimal unrealizedPnl) { this.unrealizedPnl = unrealizedPnl; }

    public BigDecimal getTotalPnl() { return totalPnl; }
    public void setTotalPnl(BigDecimal totalPnl) { this.totalPnl = totalPnl; }
}
