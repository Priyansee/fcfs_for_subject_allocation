package com.uni.subjectallocation;

import com.uni.subjectallocation.exception.AlreadyEnrolledException;
import com.uni.subjectallocation.exception.SlotNotFoundException;
import com.uni.subjectallocation.service.AllocationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@Transactional
public class FunctionalTest {

    @Autowired
    private AllocationService allocationService;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    private Long testStudentId = 30000L;
    private String validSlot = "1";

    @BeforeEach
    public void setup() {
        // Ensure we have at least one template student to copy from
        var templates = jdbcTemplate.queryForList("SELECT * FROM ec2.studentregistrations LIMIT 1");
        if (templates.isEmpty()) {
            throw new RuntimeException("No template student found in ec2.studentregistrations to clone from!");
        }
        var template = templates.get(0);

        // Cleanup
        jdbcTemplate.execute("DELETE FROM ec2.studentregistrationcourses WHERE srcsrgid = " + testStudentId);
        jdbcTemplate.execute("DELETE FROM ec2.studentregistrations WHERE srgid = " + testStudentId);

        // Insert one test student
        StringBuilder sql = new StringBuilder("INSERT INTO ec2.studentregistrations (");
        StringBuilder placeholders = new StringBuilder("VALUES (");
        Object[] args = new Object[template.size()];
        int idx = 0;

        for (java.util.Map.Entry<String, Object> entry : template.entrySet()) {
            String key = entry.getKey();
            sql.append(key).append(",");
            placeholders.append("?,");

            if ("srgid".equals(key) || "srgstdid".equals(key)) {
                args[idx] = testStudentId;
            } else {
                args[idx] = entry.getValue();
            }
            idx++;
        }

        sql.setLength(sql.length() - 1);
        placeholders.setLength(placeholders.length() - 1);
        sql.append(") ").append(placeholders).append(")");

        jdbcTemplate.update(sql.toString(), args);

        // Reset seats for validSlot
        jdbcTemplate.execute("UPDATE ec2.termcourseavailablefor SET tca_seats = 20, tca_booked = 0 " +
                             "WHERE tcatcrid IN (SELECT tcrid FROM ec2.termcourses WHERE tcrslot = '" + validSlot + "')");
    }

    @Test
    public void testValidAllocation() {
        String result = allocationService.allocateSlot(testStudentId, validSlot);
        assertEquals("Allocation Successful", result);
    }

    @Test
    public void testSlotNotFound() {
        assertThrows(SlotNotFoundException.class, () -> {
            allocationService.allocateSlot(testStudentId, "999");
        });
    }

    @Test
    public void testStudentNotFound() {
        assertThrows(RuntimeException.class, () -> {
            allocationService.allocateSlot(99999L, validSlot);
        }, "Student registration record not found");
    }

    @Test
    public void testAlreadyEnrolled() {
        // First allocation
        allocationService.allocateSlot(testStudentId, validSlot);

        // Second allocation should fail
        assertThrows(AlreadyEnrolledException.class, () -> {
            allocationService.allocateSlot(testStudentId, validSlot);
        });
    }

    @Test
    public void testSlotFull() {
        // Force slot to be full
        jdbcTemplate.execute("UPDATE ec2.termcourseavailablefor SET tca_seats = 0, tca_booked = 0 " +
                             "WHERE tcatcrid IN (SELECT tcrid FROM ec2.termcourses WHERE tcrslot = '" + validSlot + "')");

        Exception exception = assertThrows(RuntimeException.class, () -> {
            allocationService.allocateSlot(testStudentId, validSlot);
        });
        assertTrue(exception.getMessage().contains("Slot Full"));
    }
}
