package com.campusgigs.pattern.observer;

import com.campusgigs.model.DisputeDecision;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class EventBusTest {

    private EventBus bus;

    @BeforeEach
    void setUp() {
        bus = new EventBus();
    }

    @Test
    void listenerReceivesEventOfItsType() {
        List<ApplicationSubmitted> received = new ArrayList<>();
        bus.subscribe(ApplicationSubmitted.class, received::add);
        ApplicationSubmitted event = new ApplicationSubmitted(5, 10, 1, 2);

        bus.publish(event);

        assertEquals(1, received.size());
        assertSame(event, received.get(0));
    }

    @Test
    void listenerReceivesOnlyItsOwnType() {
        List<WorkApproved> received = new ArrayList<>();
        bus.subscribe(WorkApproved.class, received::add);

        bus.publish(new WorkSubmitted(10, 1, 2));
        bus.publish(new RevisionRequested(10, 1, 2));

        assertTrue(received.isEmpty());
    }

    @Test
    void allListenersOfATypeRun() {
        AtomicInteger calls = new AtomicInteger();
        bus.subscribe(FreelancerHired.class, e -> calls.incrementAndGet());
        bus.subscribe(FreelancerHired.class, e -> calls.incrementAndGet());

        bus.publish(new FreelancerHired(10, 1, 2, 500));

        assertEquals(2, calls.get());
    }

    @Test
    void sameListenerSubscribedTwiceRunsOnce() {
        AtomicInteger calls = new AtomicInteger();
        EventListener<ReviewPosted> listener = e -> calls.incrementAndGet();
        bus.subscribe(ReviewPosted.class, listener);
        bus.subscribe(ReviewPosted.class, listener);

        bus.publish(new ReviewPosted(3, 10, 1, 2, 5));

        assertEquals(1, calls.get());
    }

    @Test
    void throwingListenerDoesNotBlockOthersOrThePublisher() {
        List<DisputeResolved> received = new ArrayList<>();
        bus.subscribe(DisputeResolved.class, e -> {
            throw new IllegalStateException("broken listener");
        });
        bus.subscribe(DisputeResolved.class, received::add);

        assertDoesNotThrow(() -> bus.publish(new DisputeResolved(4, 10, 1, 2, DisputeDecision.REFUND)));
        assertEquals(1, received.size());
    }

    @Test
    void publishWithNoSubscribersDoesNothing() {
        assertDoesNotThrow(() -> bus.publish(new WalletToppedUp(1, 100)));
    }

    @Test
    void separateBusesDoNotShareListeners() {
        AtomicInteger calls = new AtomicInteger();
        bus.subscribe(DisputeRaised.class, e -> calls.incrementAndGet());

        new EventBus().publish(new DisputeRaised(4, 10, 1, 2, 1));

        assertEquals(0, calls.get());
    }

    @Test
    void getInstanceAlwaysReturnsTheSameBus() {
        assertSame(EventBus.getInstance(), EventBus.getInstance());
    }

    @Test
    void rejectsNullArguments() {
        assertThrows(NullPointerException.class, () -> bus.publish(null));
        assertThrows(NullPointerException.class, () -> bus.subscribe(null, e -> { }));
        assertThrows(NullPointerException.class, () -> bus.subscribe(WorkApproved.class, null));
    }

    @Test
    void handlesPublishingFromManyThreads() throws InterruptedException {
        AtomicInteger calls = new AtomicInteger();
        bus.subscribe(ApplicationWithdrawn.class, e -> calls.incrementAndGet());
        int publishes = 200;
        CountDownLatch done = new CountDownLatch(publishes);
        ExecutorService pool = Executors.newFixedThreadPool(8);

        for (int i = 0; i < publishes; i++) {
            pool.submit(() -> {
                bus.publish(new ApplicationWithdrawn(5, 10, 1, 2));
                done.countDown();
            });
        }

        assertTrue(done.await(5, TimeUnit.SECONDS));
        pool.shutdown();
        assertEquals(publishes, calls.get());
    }
}
