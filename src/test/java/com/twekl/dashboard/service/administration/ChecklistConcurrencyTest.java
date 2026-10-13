package com.twekl.dashboard.service.administration;

import static org.junit.jupiter.api.Assertions.*;

import com.twekl.dashboard.config.administration.*;
import com.twekl.dashboard.model.administration.*;
import com.twekl.dashboard.repository.administration.*;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.Instant;
import java.util.UUID;
import java.util.concurrent.*;

@ExtendWith(SpringExtension.class)
@ContextConfiguration(classes = {JpaConfig.class, SecurityConfig.class})
@org.springframework.test.context.web.WebAppConfiguration
class ChecklistConcurrencyTest {
    @Autowired CustomerRepository customers;
    @Autowired CustomerOrderRepository orders;
    @Autowired OrderFollowupCheckRepository checks;
    @Autowired FollowupService followups;
    @Autowired PlatformTransactionManager transactions;

    @Test
    void simultaneousReconciliationCreatesOneCheckPerPreset() throws Exception {
        TransactionTemplate tx = new TransactionTemplate(transactions);
        long[] ids =
                tx.execute(
                        status -> {
                            Customer customer =
                                    customers.save(
                                            new Customer(
                                                    null,
                                                    "Concurrency " + UUID.randomUUID(),
                                                    "123",
                                                    null,
                                                    null));
                            CustomerOrder order =
                                    orders.save(
                                            new CustomerOrder(
                                                    "C-"
                                                            + UUID.randomUUID()
                                                                    .toString()
                                                                    .substring(0, 20),
                                                    customer,
                                                    "Test item",
                                                    1,
                                                    1,
                                                    Instant.now()));
                            return new long[] {customer.getId(), order.getId()};
                        });
        ExecutorService workers = Executors.newFixedThreadPool(2);
        CountDownLatch start = new CountDownLatch(1);
        try {
            Callable<Void> reconcile =
                    () -> {
                        start.await();
                        followups.synchronizeChecks(ids[1]);
                        return null;
                    };
            Future<Void> first = workers.submit(reconcile);
            Future<Void> second = workers.submit(reconcile);
            start.countDown();
            first.get(15, TimeUnit.SECONDS);
            second.get(15, TimeUnit.SECONDS);
            tx.executeWithoutResult(
                    status -> {
                        var result = checks.findByOrderId(ids[1]);
                        assertFalse(result.isEmpty());
                        assertEquals(
                                result.size(),
                                result.stream().map(c -> c.getPreset().getId()).distinct().count());
                    });
        } finally {
            workers.shutdownNow();
            workers.awaitTermination(5, TimeUnit.SECONDS);
            // Delete only the exact fixture; never select application records by a name prefix.
            tx.executeWithoutResult(status -> customers.deleteById(ids[0]));
        }
    }
}
