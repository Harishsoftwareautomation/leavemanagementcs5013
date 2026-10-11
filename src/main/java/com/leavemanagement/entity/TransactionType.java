package com.leavemanagement.entity;

/**
 * Kind of balance change recorded in the ledger
 */
public enum TransactionType {
    ALLOCATION,       // balance set up by HR (+ total days)
    LEAVE_DEDUCTED,   // leave approved (- days)
    LEAVE_RESTORED,   // approved leave cancelled (+ days)
    COMP_OFF_CREDIT,  // comp-off request approved (+ days)
    ADJUSTMENT,       // HR correction (+ or - days)
    RESET             // yearly reset of used days (+ days)
}
