package org.streamhub.api.base.scheduling;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Duration;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.data.redis.RedisConnectionFailureException;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

/**
 * Behaviour of the scheduled-job lease. The jobs it guards move money and write ledger rows, so the
 * three cases that matter are: the winner runs, a loser does not, and an unreachable lock store
 * does not silently degrade into "everyone runs".
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class SchedulerLockTest {

    @Mock
    private StringRedisTemplate redisTemplate;
    @Mock
    private ValueOperations<String, String> valueOps;

    private SchedulerLock lock;
    private AtomicInteger runs;

    @BeforeEach
    void setUp() {
        when(redisTemplate.opsForValue()).thenReturn(valueOps);
        lock = new SchedulerLock(redisTemplate);
        runs = new AtomicInteger();
    }

    @Test
    void leaseWinner_runsTheJob_andReleasesAfterwards() {
        when(valueOps.setIfAbsent(anyString(), anyString(), any(Duration.class))).thenReturn(true);
        // Release only deletes when the stored owner is still this instance.
        when(valueOps.get("schedulerLock:billing")).thenAnswer(inv -> ownerId());

        lock.runIfLeader("billing", Duration.ofMinutes(4), runs::incrementAndGet);

        assertThat(runs.get()).isEqualTo(1);
        verify(redisTemplate).delete("schedulerLock:billing");
    }

    @Test
    void whenAnotherInstanceHoldsTheLease_theJobDoesNotRun() {
        when(valueOps.setIfAbsent(anyString(), anyString(), any(Duration.class))).thenReturn(false);

        lock.runIfLeader("pointExpiry", Duration.ofMinutes(30), runs::incrementAndGet);

        assertThat(runs.get()).isZero();
        // Critically, a loser must not delete the winner's lease on its way out.
        verify(redisTemplate, never()).delete(anyString());
    }

    @Test
    void whenTheLockStoreIsDown_theJobIsSkipped_notRunUnguarded() {
        when(valueOps.setIfAbsent(anyString(), anyString(), any(Duration.class)))
                .thenThrow(new RedisConnectionFailureException("redis down"));

        lock.runIfLeader("billing", Duration.ofMinutes(4), runs::incrementAndGet);

        // Fail closed: a periodic job loses nothing by waiting for the next tick, whereas running it
        // unguarded during an outage is exactly the double-charge the lease exists to prevent.
        assertThat(runs.get()).isZero();
    }

    @Test
    void aLeaseOwnedByAnotherInstance_isNotDeletedOnRelease() {
        when(valueOps.setIfAbsent(anyString(), anyString(), any(Duration.class))).thenReturn(true);
        // Simulates this run overrunning its TTL: the lease has since been taken by someone else.
        when(valueOps.get("schedulerLock:logArchive")).thenReturn("some-other-instance");

        lock.runIfLeader("logArchive", Duration.ofHours(1), runs::incrementAndGet);

        assertThat(runs.get()).isEqualTo(1);
        verify(redisTemplate, never()).delete(eq("schedulerLock:logArchive"));
    }

    /** The value the lock wrote for itself, captured from the setIfAbsent call. */
    private String ownerId() {
        org.mockito.ArgumentCaptor<String> captor = org.mockito.ArgumentCaptor.forClass(String.class);
        verify(valueOps).setIfAbsent(anyString(), captor.capture(), any(Duration.class));
        return captor.getValue();
    }
}
