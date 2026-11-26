package pe.upc.peaceapp.trust.messaging;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.TransactionCallback;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@SuppressWarnings("unchecked")
class OutboxRelayTest {
    @Mock
    private JdbcTemplate jdbc;
    @Mock
    private RabbitTemplate rabbit;
    @Mock
    private TransactionTemplate tx;

    private OutboxRelay relay;

    @BeforeEach
    void setUp() {
        relay = new OutboxRelay(jdbc, rabbit, tx, "peaceapp.events");
        when(tx.execute(any())).thenAnswer(inv -> ((TransactionCallback<Object>) inv.getArgument(0))
                .doInTransaction(org.mockito.Mockito.mock(TransactionStatus.class)));
    }

    private void pending(List<OutboxRelay.Pending> batch) {
        when(jdbc.query(anyString(), any(RowMapper.class), anyInt())).thenReturn(batch);
    }

    @Test
    void marksEventsAsPublishedWhenBrokerConfirms() {
        pending(List.of(new OutboxRelay.Pending(UUID.randomUUID(), "trust.case", "{}")));
        when(rabbit.invoke(any())).thenReturn(Boolean.TRUE);
        relay.relay();
        verify(jdbc).update(anyString(), any(java.sql.Timestamp.class), any(UUID.class));
    }

    @Test
    void leavesEventPendingWhenBrokerDoesNotConfirm() {
        pending(List.of(new OutboxRelay.Pending(UUID.randomUUID(), "trust.case", "{}")));
        when(rabbit.invoke(any())).thenReturn(Boolean.FALSE);
        relay.relay();
        verify(jdbc, never()).update(anyString(), any(java.sql.Timestamp.class), any(UUID.class));
    }

    @Test
    void doesNothingWhenThereAreNoPendingEvents() {
        pending(List.of());
        relay.relay();
        verify(rabbit, never()).invoke(any());
    }

    @Test
    void swallowsFailuresSoTheNextPollRetries() {
        when(jdbc.query(anyString(), any(RowMapper.class), eq(50))).thenThrow(new IllegalStateException("db caida"));
        relay.relay();
        verify(tx, times(1)).execute(any());
    }
}
