package com.xavopls.ibkr_dashboard.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * A snapshot of the current holding for a given instrument within an account.
 * Positions are upserted during CSV import and are not computed on-the-fly from trades.
 * {@code @Version} ensures optimistic locking when concurrent imports attempt to
 * update the same position row.
 */
@Entity
@Table(name = "positions", uniqueConstraints = {
        @UniqueConstraint(name = "uq_positions_account_instrument", columnNames = {"account_id", "instrument_id"})
})
public class Position {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Version
    private Long version;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "account_id", nullable = false)
    private Account account;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "instrument_id", nullable = false)
    private Instrument instrument;

    @Column(name = "quantity", precision = 20, scale = 6)
    private BigDecimal quantity;

    @Column(name = "avg_cost", precision = 20, scale = 6)
    private BigDecimal avgCost;

    @Column(name = "current_price", precision = 20, scale = 6)
    private BigDecimal currentPrice;

    @Column(name = "unrealized_pnl", precision = 20, scale = 6)
    private BigDecimal unrealizedPnl;

    @Column(name = "last_updated")
    private LocalDateTime lastUpdated;

    @PrePersist
    @PreUpdate
    private void touch() {
        this.lastUpdated = LocalDateTime.now();
    }

    // ── Constructors ──────────────────────────────────────────────────────────

    public Position() {
    }

    // ── Getters & Setters ─────────────────────────────────────────────────────

    public Long getId() { return id; }

    public Long getVersion() { return version; }

    public Account getAccount() { return account; }
    public void setAccount(Account account) { this.account = account; }

    public Instrument getInstrument() { return instrument; }
    public void setInstrument(Instrument instrument) { this.instrument = instrument; }

    public BigDecimal getQuantity() { return quantity; }
    public void setQuantity(BigDecimal quantity) { this.quantity = quantity; }

    public BigDecimal getAvgCost() { return avgCost; }
    public void setAvgCost(BigDecimal avgCost) { this.avgCost = avgCost; }

    public BigDecimal getCurrentPrice() { return currentPrice; }
    public void setCurrentPrice(BigDecimal currentPrice) { this.currentPrice = currentPrice; }

    public BigDecimal getUnrealizedPnl() { return unrealizedPnl; }
    public void setUnrealizedPnl(BigDecimal unrealizedPnl) { this.unrealizedPnl = unrealizedPnl; }

    public LocalDateTime getLastUpdated() { return lastUpdated; }
}
