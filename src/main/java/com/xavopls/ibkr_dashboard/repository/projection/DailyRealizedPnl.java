package com.xavopls.ibkr_dashboard.repository.projection;

import java.math.BigDecimal;
import java.time.LocalDate;

public interface DailyRealizedPnl {

    LocalDate getDate();

    BigDecimal getRealizedPnl();
}
