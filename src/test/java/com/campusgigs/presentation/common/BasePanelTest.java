package com.campusgigs.presentation.common;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.campusgigs.exception.ValidationException;
import com.campusgigs.presentation.FxTestSupport;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import javafx.application.Platform;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class BasePanelTest {

    private static final class TestPanel extends BasePanel {
    }

    @BeforeEach
    void startFx() {
        FxTestSupport.startToolkit();
    }

    @Test
    void runsTaskOffTheUiThreadAndDeliversResultOnTheUiThread() throws InterruptedException {
        TestPanel panel = new TestPanel();
        AtomicBoolean taskOnUiThread = new AtomicBoolean(true);
        AtomicBoolean callbackOnUiThread = new AtomicBoolean(false);
        AtomicReference<String> result = new AtomicReference<>();
        CountDownLatch done = new CountDownLatch(1);

        panel.runInBackground(() -> {
            taskOnUiThread.set(Platform.isFxApplicationThread());
            return "loaded";
        }, value -> {
            callbackOnUiThread.set(Platform.isFxApplicationThread());
            result.set(value);
            done.countDown();
        }, error -> done.countDown());

        assertTrue(done.await(5, TimeUnit.SECONDS));
        assertFalse(taskOnUiThread.get());
        assertTrue(callbackOnUiThread.get());
        assertEquals("loaded", result.get());
    }

    @Test
    void passesTheOriginalExceptionToFailureHandler() throws InterruptedException {
        TestPanel panel = new TestPanel();
        AtomicReference<Exception> failure = new AtomicReference<>();
        AtomicBoolean callbackOnUiThread = new AtomicBoolean(false);
        CountDownLatch done = new CountDownLatch(1);

        panel.<String>runInBackground(() -> {
            throw new ValidationException("Budget must be positive");
        }, value -> done.countDown(), error -> {
            callbackOnUiThread.set(Platform.isFxApplicationThread());
            failure.set(error);
            done.countDown();
        });

        assertTrue(done.await(5, TimeUnit.SECONDS));
        assertTrue(callbackOnUiThread.get());
        assertInstanceOf(ValidationException.class, failure.get());
        assertEquals("Budget must be positive", failure.get().getMessage());
    }
}
