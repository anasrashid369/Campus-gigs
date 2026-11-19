package com.campusgigs.service;

import com.campusgigs.dto.TransactionDto;

import java.util.List;

/** Read access to a user's token ledger. */
public interface TransactionService {

    /**
     * Returns the user's transaction history, newest first.
     *
     * @throws com.campusgigs.exception.ResourceNotFoundException if the user has no wallet
     */
    List<TransactionDto> getHistory(long userId);
}
