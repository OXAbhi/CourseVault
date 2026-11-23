-- CS department demo catalog (run after demo-users.sql).
-- USE learning_portal;

DELETE FROM enrollments;
DELETE FROM submissions;
DELETE FROM assignments;
DELETE FROM resource_tags;
DELETE FROM resources;
DELETE FROM batches;
DELETE FROM courses;

INSERT INTO courses (code, title, description, is_active, created_at, teacher_id)
SELECT 'CS-DSA-101', 'Data Structures & Algorithms',
       'Core CS course: arrays, trees, graphs, complexity analysis.', 1, UTC_TIMESTAMP(), u.id
FROM users u WHERE u.username = 'teacher1' LIMIT 1;

INSERT INTO courses (code, title, description, is_active, created_at, teacher_id)
SELECT 'CS-WEB-201', 'Web Technologies',
       'HTTP, REST, Spring Boot, and front-end integration.', 1, UTC_TIMESTAMP(), u.id
FROM users u WHERE u.username = 'teacher2' LIMIT 1;

INSERT INTO batches (name, academic_year, start_date, end_date, course_id)
SELECT 'CS-DSA-A-2025', '2025-26', '2025-07-01', '2026-04-30', c.id
FROM courses c WHERE c.code = 'CS-DSA-101' LIMIT 1;

INSERT INTO batches (name, academic_year, start_date, end_date, course_id)
SELECT 'CS-WEB-B-2025', '2025-26', '2025-07-01', '2026-04-30', c.id
FROM courses c WHERE c.code = 'CS-WEB-201' LIMIT 1;

INSERT INTO enrollments (status, enrolled_at, student_id, batch_id)
SELECT 'ACTIVE', UTC_TIMESTAMP(), s.id, b.id
FROM users s, batches b
WHERE s.username = 'student1' AND b.name = 'CS-DSA-A-2025' LIMIT 1;

INSERT INTO enrollments (status, enrolled_at, student_id, batch_id)
SELECT 'ACTIVE', UTC_TIMESTAMP(), s.id, b.id
FROM users s, batches b
WHERE s.username = 'student2' AND b.name = 'CS-WEB-B-2025' LIMIT 1;