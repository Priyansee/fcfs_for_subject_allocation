package com.uni.subjectallocation;

import com.uni.subjectallocation.service.AllocationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

@SpringBootTest
public class ConcurrencyTest {

    @Autowired
    private AllocationService allocationService;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @BeforeEach
    public void setup() {
        System.out.println("Preparing test data with JdbcTemplate...");
        
        // 1. Get a template student to copy mandatory columns like srgstrid, srgcurid, etc.
        var templates = jdbcTemplate.queryForList("SELECT * FROM ec2.studentregistrations LIMIT 1");
        if (templates.isEmpty()) {
            throw new RuntimeException("No template student found in ec2.studentregistrations to clone from!");
        }
        var template = templates.get(0);
        
        // 2. Cleanup Slot 1 and test students
        jdbcTemplate.execute("DELETE FROM ec2.studentregistrationcourses WHERE srcsrgid BETWEEN 20000 AND 21000");
        jdbcTemplate.execute("DELETE FROM ec2.studentregistrations WHERE srgid BETWEEN 20000 AND 21000");
        
        // 3. Insert 1000 students using the template for missing columns
        for (int i = 0; i < 1000; i++) {
            long id = 20000 + i;
            StringBuilder sql = new StringBuilder("INSERT INTO ec2.studentregistrations (");
            StringBuilder placeholders = new StringBuilder("VALUES (");
            Object[] args = new Object[template.size()];
            int idx = 0;

            for (java.util.Map.Entry<String, Object> entry : template.entrySet()) {
                String key = entry.getKey();
                sql.append(key).append(",");
                placeholders.append("?,");

                if ("srgid".equals(key) || "srgstdid".equals(key)) {
                    args[idx] = id;
                } else {
                    args[idx] = entry.getValue();
                }
                idx++;
            }

            sql.setLength(sql.length() - 1); // remove last comma
            placeholders.setLength(placeholders.length() - 1);
            sql.append(") ").append(placeholders).append(")");

            jdbcTemplate.update(sql.toString(), args);
        }
        
        // Reset booked count and ensure seats are available for Slot 1
        jdbcTemplate.execute("UPDATE ec2.termcourseavailablefor SET tca_seats = 20, tca_booked = 0 " +
                             "WHERE tcatcrid IN (SELECT tcrid FROM ec2.termcourses WHERE tcrslot = '1')");

        // DEBUG: Print the seats for slot 1
        jdbcTemplate.query("SELECT tcaid, tca_seats, tca_booked FROM ec2.termcourseavailablefor " +
                           "WHERE tcatcrid IN (SELECT tcrid FROM ec2.termcourses WHERE tcrslot = '1')", 
            rs -> {
                System.out.println("DEBUG SLOT 1 -> tcaid: " + rs.getLong("tcaid") + 
                                   ", seats: " + rs.getInt("tca_seats") + 
                                   ", booked: " + rs.getInt("tca_booked"));
            });

        System.out.println("Data ready.");
    }

    @Test
    public void testConcurrentAllocation() throws InterruptedException {
        int[] threadCounts = {100, 200, 500, 1000};
        
        System.out.println("=== STARTING CONCURRENCY BENCHMARK ===");
        System.out.println("ConcurrencyLevel,Successful,Rejected");
        
        for (int threadCount : threadCounts) {
            // Reset DB state for this iteration
            jdbcTemplate.execute("UPDATE ec2.termcourseavailablefor SET tca_seats = 20, tca_booked = 0 " +
                                 "WHERE tcatcrid IN (SELECT tcrid FROM ec2.termcourses WHERE tcrslot = '1')");
            jdbcTemplate.execute("UPDATE ec2.studentregistrationcourses SET srcstatus = 'REJECTED' WHERE srcsrgid BETWEEN 20000 AND 21000"); // cleanup previous run allocations
            
            ExecutorService executorService = Executors.newFixedThreadPool(threadCount);
            CountDownLatch latch = new CountDownLatch(1);
            AtomicInteger successCount = new AtomicInteger(0);
            AtomicInteger failureCount = new AtomicInteger(0);

            String slot = "1"; 
            Long startStudentId = 20000L;

            for (int i = 0; i < threadCount; i++) {
                Long studentId = startStudentId + i;
                executorService.execute(() -> {
                    try {
                        latch.await(); 
                        String result = allocationService.allocateSlot(studentId, slot);
                        if (result.contains("Successful")) {
                            successCount.incrementAndGet();
                        } else {
                            failureCount.incrementAndGet();
                        }
                    } catch (Exception e) {
                        failureCount.incrementAndGet();
                    }
                });
            }

            latch.countDown(); 
            executorService.shutdown();
            while (!executorService.isTerminated()) {
                Thread.sleep(100);
            }

            System.out.println(threadCount + "," + successCount.get() + "," + failureCount.get());
        }
        System.out.println("=== END CONCURRENCY BENCHMARK ===");
    }
}
