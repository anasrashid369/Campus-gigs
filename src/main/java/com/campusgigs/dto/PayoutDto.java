package com.campusgigs.dto;

/**
 * How escrowed tokens were paid out: the gig amount, the platform fee kept, and what
 * the freelancer received ({@code netAmount = grossAmount - feeAmount}).
 */
public record PayoutDto(long grossAmount, long feeAmount, long netAmount) {
}
