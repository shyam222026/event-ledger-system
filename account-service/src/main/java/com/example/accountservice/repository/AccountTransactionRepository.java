package com.example.accountservice.repository;

import com.example.accountservice.entity.AccountTransaction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface AccountTransactionRepository extends JpaRepository<AccountTransaction, Long> {
    Optional<AccountTransaction> findByEventId(String eventId);
}
