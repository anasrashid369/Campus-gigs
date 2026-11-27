package com.campusgigs.presentation;

import java.util.concurrent.Callable;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import javafx.application.Platform;
import org.junit.jupiter.api.Assumptions;

/**
 * Starts the JavaFX runtime once for all screen tests. On a machine without a display the
 * runtime cannot start, and the tests that need it are skipped instead of failing.
 */
public final class FxTestSupport {

    private static Boolean started;

    private FxTestSupport() {
    }

    /** Starts JavaFX if needed; skips the calling test when it cannot run here. */
    public static synchronized void startToolkit() {
        if (started == null) {
            started = tryStart();
        }
        Assumptions.assumeTrue(started, "JavaFX cannot start here (no display)");
    }

    /** Runs the work on the JavaFX Application Thread and returns its result. */
    public static <T> T onFxThread(Callable<T> work) throws Exception {
        startToolkit();
        CompletableFuture<T> result = new CompletableFuture<>();
        Platform.runLater(() -> {
            try {
                result.complete(work.call());
            } catch (Exception | Error e) {
                result.completeExceptionally(e);
            }
        });
        try {
            return result.get(10, TimeUnit.SECONDS);
        } catch (ExecutionException e) {
            throw e.getCause() instanceof Exception cause ? cause : e;
        }
    }

    private static boolean tryStart() {
        try {
            CountDownLatch ready = new CountDownLatch(1);
            Platform.startup(ready::countDown);
            Platform.setImplicitExit(false);
            return ready.await(10, TimeUnit.SECONDS);
        } catch (IllegalStateException alreadyRunning) {
            return true;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return false;
        } catch (RuntimeException | Error noDisplay) {
            return false;
        }
    }
}
