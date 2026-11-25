
DELETE FROM enrollments;
DELETE FROM quiz_retake_grants;
DELETE FROM quiz_attempt_answers;
DELETE FROM quiz_attempts;
DELETE FROM quiz_questions;
DELETE FROM quizzes;
DELETE FROM resource_tags;
DELETE FROM resources;
DELETE FROM submissions;
DELETE FROM assignments;
DELETE FROM notifications;
DELETE FROM batches;
DELETE FROM courses;

DELETE FROM users
WHERE username IN ('admin', 'teacher1', 'teacher2', 'student1', 'student2', 'inactive');

INSERT INTO users (username, email, password_hash, role, first_name, last_name, is_active)
VALUES
    ('admin', 'admin@portal.local', '$2a$10$Vl6EL4/HYInYjh49iZFaE.zeLf1kjcpjtnL6L7vcmA/FU15S0MQ7.', 'ADMIN', 'System', 'Admin', 1),
    ('teacher1', 'teacher1@portal.local', '$2a$10$Vl6EL4/HYInYjh49iZFaE.zeLf1kjcpjtnL6L7vcmA/FU15S0MQ7.', 'TEACHER', 'Asha', 'Rao', 1),
    ('teacher2', 'teacher2@portal.local', '$2a$10$Vl6EL4/HYInYjh49iZFaE.zeLf1kjcpjtnL6L7vcmA/FU15S0MQ7.', 'TEACHER', 'Ravi', 'Menon', 1),
    ('student1', 'student1@portal.local', '$2a$10$Vl6EL4/HYInYjh49iZFaE.zeLf1kjcpjtnL6L7vcmA/FU15S0MQ7.', 'STUDENT', 'Priya', 'Sharma', 1),
    ('student2', 'student2@portal.local', '$2a$10$Vl6EL4/HYInYjh49iZFaE.zeLf1kjcpjtnL6L7vcmA/FU15S0MQ7.', 'STUDENT', 'Arjun', 'Patel', 1),
    ('inactive', 'inactive@portal.local', '$2a$10$Vl6EL4/HYInYjh49iZFaE.zeLf1kjcpjtnL6L7vcmA/FU15S0MQ7.', 'STUDENT', 'Inactive', 'User', 0);