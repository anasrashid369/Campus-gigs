package com.campusgigs.dto;

/** Wallet balances shown to the user, in whole tokens. */
public record WalletDto(long walletId, long userId, long availableBalance, long escrowBalance) {
}
