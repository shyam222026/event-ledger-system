package com.example.accountservice.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

@Entity
@Table(name = "account_transactions", uniqueConstraints = {
        @UniqueConstraint(name = "uk_account_event_id", columnNames = "eventId")
})
public class AccountTransaction {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, updatable = false)
    private String eventId;

    @Column(nullable = false)
    private String type;

    @Column(nullable = false, precision = 19, scale = 4)
    private BigDecimal amount;

    @Column(nullable = false, length = 3)
    private String currency;

    @Column(nullable = false)
    private Instant eventTimestamp;

    @Convert(converter = JsonStringMapConverter.class)
    @Column(columnDefinition = "TEXT")
    private Map<String, Object> metadata = new HashMap<>();

    @Column(nullable = false)
    private String status = "APPLIED";

    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "account_id")
    private Account account;

    protected AccountTransaction() {
    }

    public AccountTransaction(String eventId, String type, BigDecimal amount, String currency,
                              Instant eventTimestamp, Map<String, Object> metadata) {
        this.eventId = eventId;
        this.type = type;
        this.amount = amount;
        this.currency = currency;
        this.eventTimestamp = eventTimestamp;
        this.metadata = metadata == null ? new HashMap<>() : new HashMap<>(metadata);
    }

    @PrePersist
    public void prePersist() {
        this.createdAt = Instant.now();
    }

    public void setAccount(Account account) {
        this.account = account;
    }

    public Long getId() {
        return id;
    }

    public String getEventId() {
        return eventId;
    }

    public String getType() {
        return type;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public String getCurrency() {
        return currency;
    }

    public Instant getEventTimestamp() {
        return eventTimestamp;
    }

    public Map<String, Object> getMetadata() {
        return metadata;
    }

    public String getStatus() {
        return status;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Account getAccount() {
        return account;
    }
}
