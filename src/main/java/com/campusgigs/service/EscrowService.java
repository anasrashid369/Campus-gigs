package com.campusgigs.service;

import com.campusgigs.dto.PayoutDto;
import com.campusgigs.dto.WalletDto;

/**
 * Moves gig budgets between a client's available balance, escrow and the freelancer.
 * Every method is one transaction (wallet rows locked, ledger rows written together)
 * and joins the caller's transaction when there is one, so the gig lifecycle can
 * change the gig status and move the tokens atomically. Escrow never goes through
 * the payment gateway.
 */
public interface EscrowService {

    /**
     * Moves {@code amount} tokens from the client's available balance into escrow.
     *
     * @throws com.campusgigs.exception.ValidationException if the amount is not positive or the client has too few available tokens
     * @throws com.campusgigs.exception.ResourceNotFoundException if the client has no wallet
     */
    WalletDto holdInEscrow(long clientId, long gigId, long amount);

    /**
     * Pays escrowed tokens to the freelancer minus the platform fee
     * ({@code fee = amount * feePercent / 100}, rounded down).
     * The total of client escrow, freelancer balance and fee stays equal to the amount.
     *
     * @param feePercent platform fee from 0 to 100
     * @throws com.campusgigs.exception.ValidationException if an argument is invalid or the client's escrow holds less than the amount
     * @throws com.campusgigs.exception.ResourceNotFoundException if either wallet is missing
     */
    PayoutDto releaseToFreelancer(long clientId, long freelancerId, long gigId, long amount, int feePercent);

    /**
     * Returns escrowed tokens to the client's available balance.
     *
     * @throws com.campusgigs.exception.ValidationException if the amount is not positive or the client's escrow holds less than the amount
     * @throws com.campusgigs.exception.ResourceNotFoundException if the client has no wallet
     */
    WalletDto refundToClient(long clientId, long gigId, long amount);
}
