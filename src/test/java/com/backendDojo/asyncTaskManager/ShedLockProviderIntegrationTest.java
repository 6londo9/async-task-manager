package com.backendDojo.asyncTaskManager;

import net.javacrumbs.shedlock.core.LockConfiguration;
import net.javacrumbs.shedlock.core.LockProvider;
import net.javacrumbs.shedlock.core.SimpleLock;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.Duration;
import java.time.Instant;
import java.util.Optional;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ShedLockProviderIntegrationTest extends AbstractIntegrationTest {

    @Autowired
    private LockProvider lockProvider;

    @Test
    void sameLockNameCanBeAcquiredByOnlyOneConcurrentThread() throws Exception {
        CountDownLatch start = new CountDownLatch(1);
        LockConfiguration lockConfiguration = new LockConfiguration(
                Instant.now(),
                "test_lock_provider_concurrent_owner",
                Duration.ofSeconds(10),
                Duration.ZERO
        );

        try (ExecutorService executor = Executors.newFixedThreadPool(2)) {
            Future<Optional<SimpleLock>> first = executor.submit(() -> acquireAfter(start, lockConfiguration));
            Future<Optional<SimpleLock>> second = executor.submit(() -> acquireAfter(start, lockConfiguration));

            start.countDown();

            Optional<SimpleLock> firstLock = first.get(5, TimeUnit.SECONDS);
            Optional<SimpleLock> secondLock = second.get(5, TimeUnit.SECONDS);

            try {
                long acquiredLocks = Stream.of(firstLock, secondLock)
                        .filter(Optional::isPresent)
                        .count();

                assertEquals(1, acquiredLocks);
            } finally {
                firstLock.ifPresent(SimpleLock::unlock);
                secondLock.ifPresent(SimpleLock::unlock);
            }
        }
    }

    private Optional<SimpleLock> acquireAfter(CountDownLatch start, LockConfiguration lockConfiguration) throws InterruptedException {
        assertTrue(start.await(5, TimeUnit.SECONDS));
        return lockProvider.lock(lockConfiguration);
    }
}
