package com.campusgigs.presentation.common;

import com.campusgigs.exception.ValidationException;
import org.junit.jupiter.api.Test;

import javax.swing.SwingUtilities;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BasePanelTest {

    private static final class TestPanel extends BasePanel {
        private static final long serialVersionUID = 1L;
    }

    @Test
    void runsTaskOffTheEdtAndDeliversResultOnTheEdt() throws InterruptedException {
        TestPanel panel = new TestPanel();
        AtomicBoolean taskOnEdt = new AtomicBoolean(true);
        AtomicBoolean callbackOnEdt = new AtomicBoolean(false);
        AtomicReference<String> result = new AtomicReference<>();
        CountDownLatch done = new CountDownLatch(1);

        panel.runInBackground(() -> {
            taskOnEdt.set(SwingUtilities.isEventDispatchThread());
            return "loaded";
        }, value -> {
            callbackOnEdt.set(SwingUtilities.isEventDispatchThread());
            result.set(value);
            done.countDown();
        }, error -> done.countDown());

        assertTrue(done.await(5, TimeUnit.SECONDS));
        assertFalse(taskOnEdt.get());
        assertTrue(callbackOnEdt.get());
        assertEquals("loaded", result.get());
    }

    @Test
    void passesTheOriginalExceptionToFailureHandler() throws InterruptedException {
        TestPanel panel = new TestPanel();
        AtomicReference<Exception> failure = new AtomicReference<>();
        AtomicBoolean callbackOnEdt = new AtomicBoolean(false);
        CountDownLatch done = new CountDownLatch(1);

        panel.<String>runInBackground(() -> {
            throw new ValidationException("Budget must be positive");
        }, value -> done.countDown(), error -> {
            callbackOnEdt.set(SwingUtilities.isEventDispatchThread());
            failure.set(error);
            done.countDown();
        });

        assertTrue(done.await(5, TimeUnit.SECONDS));
        assertTrue(callbackOnEdt.get());
        assertInstanceOf(ValidationException.class, failure.get());
        assertEquals("Budget must be positive", failure.get().getMessage());
    }
}
