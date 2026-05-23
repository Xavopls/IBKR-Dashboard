package com.xavopls.ibkr_dashboard.entity;

import com.xavopls.ibkr_dashboard.enums.AssetClass;
import com.xavopls.ibkr_dashboard.enums.Currency;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/**
 * Represents a tradeable financial instrument (stock, option, future, etc.).
 * Instruments are shared across accounts and act as a reference/dimension table.
 */
@Entity
@Table(name = "instruments", uniqueConstraints = {
        @UniqueConstraint(name = "uq_instruments_symbol_exchange", columnNames = {"symbol", "exchange"})
})
public class Instrument {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank
    @Column(name = "symbol", nullable = false, length = 50)
    private String symbol;

    @Column(name = "name", length = 200)
    private String name;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(name = "asset_class", nullable = false, length = 20)
    private AssetClass assetClass;

    @Column(name = "sector", length = 100)
    private String sector;

    @Column(name = "exchange", length = 50)
    private String exchange;

    @Enumerated(EnumType.STRING)
    @Column(name = "currency", length = 10)
    private Currency currency;

    // ── Constructors ──────────────────────────────────────────────────────────

    protected Instrument() {
    }

    public Instrument(String symbol, String name, AssetClass assetClass, String exchange, Currency currency) {
        this.symbol = symbol;
        this.name = name;
        this.assetClass = assetClass;
        this.exchange = exchange;
        this.currency = currency;
    }

    // ── Getters & Setters ─────────────────────────────────────────────────────

    public Long getId() { return id; }

    public String getSymbol() { return symbol; }
    public void setSymbol(String symbol) { this.symbol = symbol; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public AssetClass getAssetClass() { return assetClass; }
    public void setAssetClass(AssetClass assetClass) { this.assetClass = assetClass; }

    public String getSector() { return sector; }
    public void setSector(String sector) { this.sector = sector; }

    public String getExchange() { return exchange; }
    public void setExchange(String exchange) { this.exchange = exchange; }

    public Currency getCurrency() { return currency; }
    public void setCurrency(Currency currency) { this.currency = currency; }
}
