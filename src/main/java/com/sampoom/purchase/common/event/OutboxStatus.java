package com.sampoom.purchase.common.event;

public enum OutboxStatus {
    READY,
    PUBLISHED,
    FAILED,
    DEAD
}
