package com.campusgigs.service;

import com.campusgigs.dto.EarningsDto;

/** Read-only earnings summary for freelancers (F26). */
public interface EarningsService {

    /**
     * Returns the freelancer's available balance, lifetime earnings after fees, fees paid,
     * withdrawals and the budgets still waiting to be paid.
     *
     * @throws com.campusgigs.exception.ValidationException if the user is not a freelancer
     * @throws com.campusgigs.exception.ResourceNotFoundException if the user or the wallet does not exist
     */
    EarningsDto getEarnings(long userId);
}
