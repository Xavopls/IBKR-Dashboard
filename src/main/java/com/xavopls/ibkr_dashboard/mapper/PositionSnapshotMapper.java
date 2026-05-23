package com.xavopls.ibkr_dashboard.mapper;

import com.xavopls.ibkr_dashboard.entity.Instrument;
import com.xavopls.ibkr_dashboard.entity.Position;
import com.xavopls.ibkr_dashboard.enums.AssetClass;
import com.xavopls.ibkr_dashboard.enums.Currency;
import com.xavopls.ibkr_dashboard.ibkr.IbkrPositionSnapshot;
import com.xavopls.ibkr_dashboard.repository.InstrumentRepository;
import org.springframework.stereotype.Component;

@Component
public class PositionSnapshotMapper {

    private final InstrumentRepository instrumentRepository;

    public PositionSnapshotMapper(InstrumentRepository instrumentRepository) {
        this.instrumentRepository = instrumentRepository;
    }

    public void apply(IbkrPositionSnapshot snapshot, Position position) {
        position.setInstrument(resolveInstrument(snapshot));
        position.setQuantity(snapshot.quantity());
        position.setAvgCost(snapshot.averageCost());
        position.setCurrentPrice(snapshot.marketPrice());
        position.setUnrealizedPnl(snapshot.unrealizedPnl());
    }

    private Instrument resolveInstrument(IbkrPositionSnapshot snapshot) {
        return instrumentRepository
                .findBySymbolAndExchange(snapshot.symbol(), snapshot.exchange())
                .orElseGet(() -> instrumentRepository.save(new Instrument(
                        snapshot.symbol(),
                        snapshot.name(),
                        parseAssetClass(snapshot.assetClass()),
                        snapshot.exchange(),
                        parseCurrency(snapshot.currency())
                )));
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
