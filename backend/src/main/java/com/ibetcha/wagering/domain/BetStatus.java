package com.ibetcha.wagering.domain;

public enum BetStatus {
    PENDING_ACCEPTANCE,
    ACTIVE,
    PENDING_JURY_VERDICT,
    PENDING_APPROVAL,
    RESOLVED,
    DISPUTED,
    EXPIRED,
    CANCELLED;

    public boolean isTerminal() {
        return this == RESOLVED || this == EXPIRED || this == CANCELLED;
    }
}
