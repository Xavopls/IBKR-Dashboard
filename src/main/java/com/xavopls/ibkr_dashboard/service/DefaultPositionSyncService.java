package com.xavopls.ibkr_dashboard.service;

import com.xavopls.ibkr_dashboard.config.IbkrProperties;
import com.xavopls.ibkr_dashboard.dto.PositionSyncResponse;
import com.xavopls.ibkr_dashboard.entity.Account;
import com.xavopls.ibkr_dashboard.entity.Position;
import com.xavopls.ibkr_dashboard.enums.Currency;
import com.xavopls.ibkr_dashboard.ibkr.IbkrClient;
import com.xavopls.ibkr_dashboard.ibkr.IbkrPositionSnapshot;
import com.xavopls.ibkr_dashboard.mapper.PositionSnapshotMapper;
import com.xavopls.ibkr_dashboard.repository.AccountRepository;
import com.xavopls.ibkr_dashboard.repository.PositionRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;

@Service
public class DefaultPositionSyncService implements PositionSyncService {

    private final IbkrProperties ibkrProperties;
    private final IbkrClient ibkrClient;
    private final AccountRepository accountRepository;
    private final PositionRepository positionRepository;
    private final PositionSnapshotMapper positionSnapshotMapper;

    public DefaultPositionSyncService(IbkrProperties ibkrProperties,
                                      IbkrClient ibkrClient,
                                      AccountRepository accountRepository,
                                      PositionRepository positionRepository,
                                      PositionSnapshotMapper positionSnapshotMapper) {
        this.ibkrProperties = ibkrProperties;
        this.ibkrClient = ibkrClient;
        this.accountRepository = accountRepository;
        this.positionRepository = positionRepository;
        this.positionSnapshotMapper = positionSnapshotMapper;
    }

    @Override
    @Transactional
    public PositionSyncResponse syncPositions() {
        Account account = resolveAccount();
        List<IbkrPositionSnapshot> snapshots = ibkrClient.getPositions(account.getAccountNumber());
        int updated = 0;

        for (IbkrPositionSnapshot snapshot : snapshots) {
            upsertPosition(account, snapshot);
            updated++;
        }

        return new PositionSyncResponse(account.getAccountNumber(), snapshots.size(), updated, Instant.now());
    }

    private Account resolveAccount() {
        String accountId = ibkrProperties.getAccountId();
        if (accountId == null || accountId.isBlank()) {
            throw new IllegalStateException("IBKR account id is required. Set IBKR_ACCOUNT_ID.");
        }
        return accountRepository.findByAccountNumber(accountId)
                .orElseGet(() -> accountRepository.save(new Account(accountId, accountId, Currency.USD)));
    }

    private void upsertPosition(Account account, IbkrPositionSnapshot snapshot) {
        Position incoming = new Position();
        incoming.setAccount(account);
        positionSnapshotMapper.apply(snapshot, incoming);

        Position position = positionRepository
                .findByAccountIdAndInstrumentId(account.getId(), incoming.getInstrument().getId())
                .orElseGet(Position::new);

        position.setAccount(account);
        position.setInstrument(incoming.getInstrument());
        position.setQuantity(incoming.getQuantity());
        position.setAvgCost(incoming.getAvgCost());
        position.setCurrentPrice(incoming.getCurrentPrice());
        position.setUnrealizedPnl(incoming.getUnrealizedPnl());

        positionRepository.save(position);
    }
}
