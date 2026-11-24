package com.campusgigs.dto;

/**
 * A freelancer's money at a glance, in whole tokens.
 *
 * @param availableBalance tokens that can be withdrawn now
 * @param totalEarned      everything paid out for approved or resolved gigs, after platform fees
 * @param totalFeesPaid    platform fees kept on those payouts
 * @param totalWithdrawn   tokens withdrawn so far
 * @param pendingPayout    budgets of gigs still in progress or in dispute (before the platform fee)
 */
public record EarningsDto(long availableBalance, long totalEarned, long totalFeesPaid,
                          long totalWithdrawn, long pendingPayout) {
}
