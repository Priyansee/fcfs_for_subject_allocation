-- 1. Check for overbooking (Booked > Seats)
SELECT tcaid, tcrid, tca_seats, tca_booked 
FROM ec2.termcourseavailablefor 
WHERE tca_booked > tca_seats;

-- 2. Check for duplicate enrollments per student
SELECT srcsrgid, COUNT(*) 
FROM ec2.studentregistrationcourses 
GROUP BY srcsrgid 
HAVING COUNT(*) > 1;

-- 3. Verify total booked count matches entries in studentregistrationcourses
SELECT t.tcaid, t.tca_booked, COUNT(s.srcid) as actual_enrollments
FROM ec2.termcourseavailablefor t
JOIN ec2.termcourse tc ON t.tcrid = tc.tcrid
JOIN ec2.studentregistrationcourses s ON tc.tcrid = s.srctcrid
GROUP BY t.tcaid, t.tca_booked
HAVING t.tca_booked != COUNT(s.srcid);

-- 4. Check if any student is enrolled in multiple slots (violation of business rule)
-- (Assuming each student registration has one srgid, and we check across slots)
-- This logic depends on how srgid is linked to studentId.
