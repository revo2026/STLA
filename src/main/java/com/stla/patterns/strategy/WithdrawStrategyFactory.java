package com.stla.patterns.strategy;

import java.util.Map;

/**
 * Factory for withdrawal strategy selection.
 */
public class WithdrawStrategyFactory {
    private static final Map<String, WithdrawStrategy> STRATEGIES = Map.of(
        "visa", new VisaWithdrawStrategy(),
        "digital_wallet", new DigitalWalletWithdrawStrategy()
    );

    public static WithdrawStrategy getStrategy(String method) {
        WithdrawStrategy s = STRATEGIES.get(method);
        if (s == null) throw new IllegalArgumentException("Unknown withdrawal method: " + method);
        return s;
    }

    public static Map<String, WithdrawStrategy> getAll() { return STRATEGIES; }
}
