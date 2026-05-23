package com.xavopls.ibkr_dashboard.mapper;

import com.xavopls.ibkr_dashboard.entity.Instrument;
import com.xavopls.ibkr_dashboard.entity.Trade;
import com.xavopls.ibkr_dashboard.enums.AssetClass;
import com.xavopls.ibkr_dashboard.enums.Currency;
import com.xavopls.ibkr_dashboard.enums.TradeDirection;
import com.xavopls.ibkr_dashboard.ibkr.IbkrTradeExecution;
import com.xavopls.ibkr_dashboard.repository.InstrumentRepository;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

@Component
public class TradeExecutionMapper {

    private final InstrumentRepository instrumentRepository;

    public TradeExecutionMapper(InstrumentRepository instrumentRepository) {
        this.instrumentRepository = instrumentRepository;
    }

    public Trade toTrade(IbkrTradeExecution execution) {
        Trade trade = new Trade();
        TradeDirection direction = parseDirection(execution.side());

        trade.setInstrument(resolveInstrument(execution));
        trade.setTradeDate(execution.tradeDate());
        trade.setTradeTime(execution.tradeTime());
        trade.setSettlementDate(execution.settlementDate());
        trade.setIbkrExecutionId(execution.executionId());
        trade.setDirection(direction);
        trade.setQuantity(execution.quantity().abs());
        trade.setPrice(execution.price().abs());
        trade.setCommission(execution.commission());
        trade.setNetAmount(execution.netAmount() != null
                ? execution.netAmount()
                : netAmount(execution.quantity(), execution.price(), direction));
        trade.setRealizedPnl(execution.realizedPnl());
        trade.setNotes(execution.executionId());

        return trade;
    }

    private Instrument resolveInstrument(IbkrTradeExecution execution) {
        return instrumentRepository
                .findBySymbolAndExchange(execution.symbol(), execution.exchange())
                .orElseGet(() -> instrumentRepository.save(new Instrument(
                        execution.symbol(),
                        execution.name(),
                        parseAssetClass(execution.assetClass()),
                        execution.exchange(),
                        parseCurrency(execution.currency())
                )));
    }

    private TradeDirection parseDirection(String side) {
        if (side == null) {
            return TradeDirection.BUY;
        }
        return switch (side.trim().toUpperCase()) {
            case "SLD", "SELL", "S" -> TradeDirection.SELL;
            default -> TradeDirection.BUY;
        };
    }

    private BigDecimal netAmount(BigDecimal quantity, BigDecimal price, TradeDirection direction) {
        BigDecimal amount = quantity.abs().multiply(price.abs());
        return direction == TradeDirection.BUY ? amount.negate() : amount;
    }

    private AssetClass parseAssetClass(String value) {
        try {
            return AssetClass.fromIbkrLabel(value);
        } catch (IllegalArgumentException ignored) {
            return AssetClass.STOCK;
        }
    }

    private Currency parseCurrency(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        try {
            return Currency.valueOf(value.toUpperCase());
        } catch (IllegalArgumentException ignored) {
            return null;
        }
    }
}
