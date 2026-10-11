package com.leavemanagement.dto;

import com.leavemanagement.entity.TransactionType;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Ledger entry as returned by the API
 */
public record LeaveTransactionView(
        Integer transactionId,
        Integer leaveTypeId,
        String leaveTypeName,
        Integer year,
        BigDecimal days,
        TransactionType type,
        Integer referenceId,
        String reason,
        String createdByName,
        LocalDateTime createdAt) {
}
