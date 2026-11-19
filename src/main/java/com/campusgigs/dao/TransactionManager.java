package com.campusgigs.dao;

import java.util.function.Supplier;

/**
 * Runs business work as one atomic unit. Every DAO call made inside the work
 * joins the same transaction; any exception rolls everything back.
 * Services use this interface so they never touch JDBC types.
 */
public interface TransactionManager {

    /** Runs the work in a transaction and returns its result. */
    <T> T call(Supplier<T> work);

    /** Runs the work in a transaction. */
    void run(Runnable work);
}
