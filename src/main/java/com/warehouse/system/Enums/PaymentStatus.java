package com.warehouse.system.Enums;

public enum PaymentStatus {
        PENDING,         // Invoice created; awaiting tenant action or processing date
        PROCESSING,   // Payment submitted but not yet cleared
        PAID,               // Payment successfully settled and funds received
        PARTIALLY_PAID, // Tenant paid a portion of the total balance due
        OVERDUE,         // Due date has passed without full payment
        FAILED,           // Transaction was rejected (e.g., insufficient funds, expired card)
        BOUNCED,         // Payment cleared initially but reversed later (e.g., bounced check)
        REFUNDED,       // Payment returned to the tenant by the landlord/system
        WAIVED            // forgave or canceled the payment balance
}
