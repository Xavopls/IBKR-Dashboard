package com.xavopls.ibkr_dashboard.entity;

import com.xavopls.ibkr_dashboard.enums.TradeDirection;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * A single executed trade associated with an account and instrument.
 * {@code netAmount} is the signed cash-flow impact of the trade (negative for buys).
 * {@code realizedPnl} is populated for closing trades and is used for all P&L aggregations.
 */
@Entity
@Table(name = "trades")
public class Trade {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "account_id", nullable = false)
    private Account account;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "instrument_id", nullable = false)
    private Instrument instrument;

    @NotNull
    @Column(name = "trade_date", nullable = false)
    private LocalDate tradeDate;

    @Column(name = "trade_time")
    private LocalDateTime tradeTime;

    @Column(name = "settlement_date")
    private LocalDate settlementDate;

    @Column(name = "ibkr_execution_id", length = 100)
    private String ibkrExecutionId;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(name = "direction", nullable = false, length = 10)
    private TradeDirection direction;

    @NotNull
    @Positive
    @Column(name = "quantity", nullable = false, precision = 20, scale = 6)
    private BigDecimal quantity;

    @NotNull
    @Positive
    @Column(name = "price", nullable = false, precision = 20, scale = 6)
    private BigDecimal price;

    @Column(name = "commission", precision = 20, scale = 6)
    private BigDecimal commission;

    @Column(name = "trade_currency", length = 10)
    private String tradeCurrency;

    @Column(name = "fx_rate_to_base", precision = 20, scale = 10)
    private BigDecimal fxRateToBase;

    /** Signed net cash impact: negative for buys, positive for sells. */
    @Column(name = "net_amount", precision = 20, scale = 6)
    private BigDecimal netAmount;

    @Column(name = "net_amount_base", precision = 20, scale = 6)
    private BigDecimal netAmountBase;

    /** Populated only for closing trades (SELL that closes a position). Null for opening trades. */
    @Column(name = "realized_pnl", precision = 20, scale = 6)
    private BigDecimal realizedPnl;

    @Column(name = "realized_pnl_base", precision = 20, scale = 6)
    private BigDecimal realizedPnlBase;

    @Column(name = "commission_base", precision = 20, scale = 6)
    private BigDecimal commissionBase;

    @Column(name = "strategy_tag", length = 100)
    private String strategyTag;

    @Column(name = "notes", length = 1000)
    private String notes;

    // ── Constructors ──────────────────────────────────────────────────────────

    public Trade() {
    }

    // ── Getters & Setters ─────────────────────────────────────────────────────

    public Long getId() { return id; }

    public Account getAccount() { return account; }
    public void setAccount(Account account) { this.account = account; }

    public Instrument getInstrument() { return instrument; }
    public void setInstrument(Instrument instrument) { this.instrument = instrument; }

    public LocalDate getTradeDate() { return tradeDate; }
    public void setTradeDate(LocalDate tradeDate) { this.tradeDate = tradeDate; }

    public LocalDateTime getTradeTime() { return tradeTime; }
    public void setTradeTime(LocalDateTime tradeTime) { this.tradeTime = tradeTime; }

    public LocalDate getSettlementDate() { return settlementDate; }
    public void setSettlementDate(LocalDate settlementDate) { this.settlementDate = settlementDate; }

    public String getIbkrExecutionId() { return ibkrExecutionId; }
    public void setIbkrExecutionId(String ibkrExecutionId) { this.ibkrExecutionId = ibkrExecutionId; }

    public TradeDirection getDirection() { return direction; }
    public void setDirection(TradeDirection direction) { this.direction = direction; }

    public BigDecimal getQuantity() { return quantity; }
    public void setQuantity(BigDecimal quantity) { this.quantity = quantity; }

    public BigDecimal getPrice() { return price; }
    public void setPrice(BigDecimal price) { this.price = price; }

    public BigDecimal getCommission() { return commission; }
    public void setCommission(BigDecimal commission) { this.commission = commission; }

    public String getTradeCurrency() { return tradeCurrency; }
    public void setTradeCurrency(String tradeCurrency) { this.tradeCurrency = tradeCurrency; }

    public BigDecimal getFxRateToBase() { return fxRateToBase; }
    public void setFxRateToBase(BigDecimal fxRateToBase) { this.fxRateToBase = fxRateToBase; }

    public BigDecimal getNetAmount() { return netAmount; }
    public void setNetAmount(BigDecimal netAmount) { this.netAmount = netAmount; }

    public BigDecimal getNetAmountBase() { return netAmountBase; }
    public void setNetAmountBase(BigDecimal netAmountBase) { this.netAmountBase = netAmountBase; }

    public BigDecimal getRealizedPnl() { return realizedPnl; }
    public void setRealizedPnl(BigDecimal realizedPnl) { this.realizedPnl = realizedPnl; }

    public BigDecimal getRealizedPnlBase() { return realizedPnlBase; }
    public void setRealizedPnlBase(BigDecimal realizedPnlBase) { this.realizedPnlBase = realizedPnlBase; }

    public BigDecimal getCommissionBase() { return commissionBase; }
    public void setCommissionBase(BigDecimal commissionBase) { this.commissionBase = commissionBase; }

    public String getStrategyTag() { return strategyTag; }
    public void setStrategyTag(String strategyTag) { this.strategyTag = strategyTag; }

    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }
}
