# Online Learning Portal — Coding Agent Brief

Use this document as the source of truth while building the project. Follow it in milestone order. Ship the MVP completely before any stretch work. Do not introduce Docker, Flyway, S3, or local file storage. Do not change the Maven coordinates, Java version, Spring Boot version, or base package listed below.

## 1. Project overview

Build a role-based online learning portal that replaces sharing notes, assignments, and recorded lectures over WhatsApp or email at Indian colleges and coaching centers.

- Teachers upload notes, lectures, and assignments for the courses they teach.
- The API sends every file to Cloudinary and stores `public_id` and `secure_url` in MySQL.
- Students see only batches they are enrolled in: open notes, watch lectures, submit work, and read grades.
- An admin issues accounts (no public sign-up), creates courses and batches, assigns teachers, and enrolls students.
- After login, a JWT is returned and every later API call sends `Authorization: Bearer <token>`. Angular routes the user to a role dashboard. File bytes never live on the API host.

## 2. Exact stack (do not change)

Two applications, two folders.

### Backend — already generated from start.spring.io

| Item | Value |
|------|--------|
| Java | 17 |
| Framework | Spring Boot **4.2.0** (Maven, `spring-boot-starter-parent`) |
| groupId | `com.portal` |
| artifactId | `learning-portal` |
| Version | `0.0.1-SNAPSHOT` |
| Base package | `com.portal.learningportal` |
| Main class | `com.portal.learningportal.LearningPortalApplication` |

Dependencies already in `pom.xml` (keep these; add nothing equivalent):

- `spring-boot-starter-webmvc` (Boot 4 name for the web starter — do not replace with `spring-boot-starter-web`)
- `spring-boot-starter-data-jpa`
- `spring-boot-starter-security`
- `spring-boot-starter-validation`
- `mysql-connector-j` (runtime)
- `lombok` (optional, already configured as an annotation processor)
- `jjwt-api` / `jjwt-impl` / `jjwt-jackson` **0.12.6**
- `cloudinary-http5` **2.6.0**
- Test starters already present: `data-jpa-test`, `security-test`, `validation-test`, `webmvc-test`

Database: local MySQL in development (`CREATE DATABASE learning_portal;`). Hibernate owns the schema: `spring.jpa.hibernate.ddl-auto=update`. In production you may switch this to `validate` or add an optional `schema.sql`. **No Flyway. No Docker. No Docker Compose.**

Files: Cloudinary only, in development and production. Upload with the Cloudinary Java SDK from the backend. Persist `public_id` and `secure_url` (and `cloudinary_resource_type`) in MySQL. Lectures use Cloudinary `resource_type=video`. PDFs and other documents use `resource_type=raw`.

Run:

```bash
mvn spring-boot:run
# or
mvn clean package
java -jar target/learning-portal-0.0.1-SNAPSHOT.jar
```

### Frontend — create in a separate folder

Create an Angular SPA next to the API, for example `frontend/`. Standalone components preferred (NgModules are acceptable if grouped the same way). Use either Angular Material or Bootstrap and stay with one. Put the API base URL in `environment.ts` / `environment.prod.ts` (default `http://localhost:8080`).

### Repo layout

```
learning-portal/          # Maven API (existing start.spring.io project)
  pom.xml
  src/main/java/com/portal/learningportal/
  src/main/resources/application.properties
frontend/                 # Angular SPA (create this)
```

### Existing `application.properties` keys

These are already present. Wire your code to them. Do not hard-code secrets. Override with environment variables.

```properties
spring.application.name=learning-portal

spring.datasource.url=jdbc:mysql://localhost:3306/learning_portal?useSSL=false&serverTimezone=Asia/Kolkata&allowPublicKeyRetrieval=true
spring.datasource.username=${DB_USERNAME:root}
spring.datasource.password=${DB_PASSWORD:root}

spring.jpa.hibernate.ddl-auto=update
spring.jpa.show-sql=true

spring.servlet.multipart.max-file-size=100MB
spring.servlet.multipart.max-request-size=100MB

cloudinary.cloud-name=${CLOUDINARY_CLOUD_NAME:your_cloud_name}
cloudinary.api-key=${CLOUDINARY_API_KEY:your_api_key}
cloudinary.api-secret=${CLOUDINARY_API_SECRET:your_api_secret}

jwt.secret=${JWT_SECRET:change-this-to-a-long-random-secret-key-123456}
jwt.expiration-ms=86400000
```

Environment variables: `DB_USERNAME`, `DB_PASSWORD`, `CLOUDINARY_CLOUD_NAME`, `CLOUDINARY_API_KEY`, `CLOUDINARY_API_SECRET`, `JWT_SECRET`. JWT expiry is already `jwt.expiration-ms` (24 hours for the MVP). Set app timezone to `Asia/Kolkata`. Store instants in UTC.

## 3. Roles and permissions

`User.role` is an enum: `ADMIN`, `TEACHER`, `STUDENT`. In Spring Security, expose them as `ROLE_ADMIN`, `ROLE_TEACHER`, `ROLE_STUDENT` (add the `ROLE_` prefix only in `UserDetailsService`). Protect endpoints with `@PreAuthorize` (for example `@PreAuthorize("hasRole('TEACHER')")`). That is the first check only. Services must also enforce object-level rules: a teacher manages only courses they teach; a student sees only enrolled batches. Never trust Angular route guards alone.

| Action | Admin | Teacher | Student |
|--------|-------|---------|---------|
| Create and deactivate users | Yes | No | No |
| Create courses, batches, assign teachers | Yes | No | No |
| Enroll students in a batch | Yes | No | No |
| Upload or unpublish notes and lectures | Yes | Own courses | No |
| View or access material (get `secure_url`) | Yes | Own courses | Enrolled only |
| Create assignments and set deadlines | Yes | Own courses | No |
| Submit an assignment | No | No | Enrolled, by policy |
| Grade submissions and leave feedback | Yes | Own courses | No |
| View teacher analytics | Yes | Own courses | No (stretch) |

Assignment policy (MVP): one submission per student, replaceable until the deadline. Optional late flag only if you later allow late uploads. Teachers cannot enroll students in the MVP. No public registration.

## 4. Navigation per role

Shared chrome after login: logo, role-specific links, search, notification bell, profile, logout. Shared routes: `/login`, search, notifications, profile, password change. Public: landing and login.

**Student** (`/student/...`): Dashboard (recent files, upcoming deadlines, unread notifications) → My courses (enrolled batches only) → Course hub → Notes (open Cloudinary file) / Lectures (HTML5 player on Cloudinary video URL) / Assignments (submit or replace, then view grade) → Search, Notifications, Profile.

**Teacher** (`/teacher/...`): Dashboard (my courses, pending grades) → My courses (assigned only) → Course hub → Upload material, Assignments, Grade submissions, Enrolled students (read-only roster) → Notifications, Profile. Analytics is stretch.

**Admin** (`/admin/...`): Thin dashboard → Users, Courses, Batches, Enrollments. Keep these as simple list-and-form screens.

## 5. Entities, fields, and relationships

Use explicit `@Table` and `@Column` names. Use `@Enumerated(EnumType.STRING)` for role, types, and status. Index foreign keys and `resources.title`.

| Entity | Table | Key fields | Relations |
|--------|-------|------------|-----------|
| `User` | `users` | username (unique), email (unique), password_hash, role, first_name, last_name, is_active | teaches Course; has Enrollment, Submission, Notification |
| `Course` | `courses` | code (unique), title, description, is_active, created_at | `@ManyToOne` teacher; `@OneToMany` Batch, Resource, Assignment |
| `Batch` | `batches` | name, academic_year, start_date, end_date | `@ManyToOne` Course; `@OneToMany` Enrollment |
| `Enrollment` | `enrollments` | status (`ACTIVE`/`DROPPED`), enrolled_at; **unique (student_id, batch_id)** | `@ManyToOne` student, `@ManyToOne` batch |
| `Resource` | `resources` | title, description, resource_type (`NOTE`/`LECTURE`/`OTHER`), public_id, secure_url, cloudinary_resource_type (`video`/`raw`), mime_type, file_size, is_published, created_at | `@ManyToOne` course, `@ManyToOne` uploadedBy; `@ManyToMany` Tag via `resource_tags` |
| `Tag` | `tags` | name (unique), slug | `@ManyToMany` Resource |
| `Assignment` | `assignments` | title, description, due_at, max_marks, attachment public_id + secure_url (optional), is_published, created_at | `@ManyToOne` course, `@ManyToOne` createdBy; `@OneToMany` Submission |
| `Submission` | `submissions` | public_id, secure_url, submitted_at, marks, feedback, status (`SUBMITTED`/`LATE`/`GRADED`); **unique (assignment_id, student_id)** | `@ManyToOne` assignment, `@ManyToOne` student |
| `Notification` | `notifications` | title, body, link, is_read, created_at | `@ManyToOne` user |
| `ResourceAccess` | `resource_access` | action (`VIEW`/`DOWNLOAD`), accessed_at | `@ManyToOne` resource, `@ManyToOne` student. **Stretch only — do not build in MVP.** |

No session table for MVP JWT. A refresh-token table is stretch.

## 6. Backend package structure

Place everything under `com.portal.learningportal`:

```
com.portal.learningportal
  LearningPortalApplication.java
  config/          # SecurityConfig, CorsConfig, CloudinaryConfig
  security/        # JwtService, JwtAuthFilter, UserDetailsServiceImpl
  entity/          # User, Role, Course, Batch, Enrollment, Resource, Tag, Assignment, Submission, Notification, enums
  repository/      # Spring Data JPA repositories
  dto/             # request and response DTOs only — never return entities
  service/         # Auth, User, Course, Resource, Assignment, Cloudinary, Notification, Search
  controller/      # REST controllers under /api
  exception/       # GlobalExceptionHandler and typed exceptions
```

Controllers stay thin. Business rules and object-level access live in services. Repositories are Spring Data JPA interfaces. Create notifications in the service layer when a resource is published, an assignment is created, or a submission is graded.

## 7. REST API

All routes except `POST /api/auth/login` require a valid JWT. Then apply `@PreAuthorize` plus service-level enrollment/teaching checks. Resource GET endpoints include `secure_url` only after that check.

| Method | Path | Access | Notes |
|--------|------|--------|-------|
| POST | `/api/auth/login` | Public | Returns JWT + role |
| GET | `/api/auth/me` | Authenticated | Current user DTO |
| PUT | `/api/auth/password` | Authenticated | Change password |
| GET, PUT | `/api/profile` | Authenticated | Profile fields, not password hash |
| GET, POST | `/api/users` | ADMIN | Create teacher/student accounts |
| GET, PUT | `/api/users/{id}` | ADMIN | |
| PATCH | `/api/users/{id}/status` | ADMIN | Activate / deactivate |
| GET, POST | `/api/courses` | Auth; POST is ADMIN | GET is role-filtered |
| GET, PUT, PATCH | `/api/courses/{id}` | Auth + object checks | |
| GET, POST | `/api/courses/{id}/batches` | Auth; POST is ADMIN | |
| GET, PUT | `/api/batches/{id}` | Auth; write is ADMIN | |
| GET, POST | `/api/enrollments` | ADMIN | |
| PATCH | `/api/enrollments/{id}` | ADMIN | e.g. drop |
| GET, POST | `/api/courses/{id}/resources` | Auth; POST is TEACHER, multipart | |
| GET | `/api/resources/{id}` | Auth + object checks | Includes `secure_url` if allowed |
| GET | `/api/resources/{id}/access` | Auth + object checks | Returns URL after check |
| DELETE | `/api/resources/{id}` | TEACHER (own course) or ADMIN | |
| GET, POST | `/api/courses/{id}/assignments` | Auth; POST is TEACHER | |
| GET, PUT | `/api/assignments/{id}` | Auth + object checks | |
| POST | `/api/assignments/{id}/submissions` | STUDENT, multipart | Replace until deadline |
| GET | `/api/assignments/{id}/submissions` | TEACHER (own course) | |
| PUT | `/api/submissions/{id}/grade` | TEACHER (own course) | marks + feedback |
| GET | `/api/notifications` | Authenticated | Own notifications |
| PATCH | `/api/notifications/{id}/read` | Authenticated | Own only |
| GET | `/api/search?q=&type=&tag=` | Authenticated | Scoped to allowed courses |
| GET | `/api/analytics/courses/{id}` | TEACHER/ADMIN | **Stretch** |

Enable CORS for `http://localhost:4200` (and later the Vercel/Netlify origin). CSRF is disabled on the API because the JWT is sent in the `Authorization` header, not a cookie.

## 8. Security and JWT design

- Stateless Spring Security. `SecurityFilterChain` permits `/api/auth/login`, requires authentication for everything else under `/api/**`.
- Passwords stored only as BCrypt hashes (`password_hash`). Never return the hash in a DTO.
- `JwtService` signs and parses tokens with `jwt.secret` (`JWT_SECRET`) and expiry `jwt.expiration-ms`. Secret must be at least 32 characters.
- `JwtAuthFilter` reads `Authorization: Bearer`, validates the token, and sets the `SecurityContext`.
- `UserDetailsService` loads the user and maps `Role` → `ROLE_*` authorities. Inactive users cannot authenticate.
- Login body: username (or email) + password. Response: access token, role, and basic profile — no password.
- MVP uses a single access token (24 hours). Refresh tokens are stretch.
- File access: after role + enrollment/teaching checks, return the stored Cloudinary `secure_url`. A public `secure_url` can still be forwarded; signed/authenticated Cloudinary URLs are stretch.
- Validate file type and size (PDF, PPTX, MP4, and similar; respect the 100MB multipart limit) **before** calling Cloudinary.
- Never put `JWT_SECRET` or Cloudinary secrets in the Angular app or in git.

## 9. Cloudinary upload design

`CloudinaryConfig` builds a `com.cloudinary.Cloudinary` client from `cloudinary.cloud-name`, `cloudinary.api-key`, and `cloudinary.api-secret`.

`CloudinaryService.upload(MultipartFile file, String resourceType)`:

1. Reject disallowed MIME types or oversized files.
2. Upload via the Cloudinary SDK. Use `resource_type=video` for lectures; `resource_type=raw` for PDFs and other documents.
3. Return `public_id`, `secure_url`, and the Cloudinary resource type.
4. The calling service persists those fields on `Resource`, `Assignment` (optional attachment), or `Submission`.

Do not store file bytes on disk or in MySQL. Do not use S3. On delete, remove the Cloudinary asset by `public_id` when practical. Angular never uploads directly to Cloudinary; it posts `multipart/form-data` to the API and later uses `secure_url` only after a successful authorized GET.

Lectures: play `secure_url` in an HTML5 video player. Notes/docs: open the raw `secure_url`.

## 10. Angular structure

```
frontend/src/app/
  core/
    guards/          # authGuard, roleGuard
    interceptors/    # jwtInterceptor → Authorization: Bearer <token>
    services/        # auth.service.ts
  shared/            # shell layout, nav, notification bell
  features/
    auth/            # landing, login
    student/         # dashboard, courses, notes, lectures, assignments
    teacher/         # dashboard, upload, grade, roster
    admin/           # users, courses, batches, enrollments
  models/
  services/          # course, resource, assignment, notification, search
```

Suggested routes:

```ts
{ path: '', component: LandingComponent },
{ path: 'login', component: LoginComponent },
{
  path: 'student',
  canActivate: [authGuard, roleGuard],
  data: { roles: ['STUDENT'] },
  children: [
    { path: 'dashboard', component: StudentDashboardComponent },
    { path: 'courses', component: MyCoursesComponent },
    { path: 'courses/:id', component: CourseHubComponent },
    { path: 'assignments/:id', component: AssignmentDetailComponent },
  ],
},
{
  path: 'teacher',
  canActivate: [authGuard, roleGuard],
  data: { roles: ['TEACHER'] },
  loadChildren: () => teacherRoutes,
},
{
  path: 'admin',
  canActivate: [authGuard, roleGuard],
  data: { roles: ['ADMIN'] },
  children: [
    { path: 'users', component: UsersComponent },
    { path: 'courses', component: AdminCoursesComponent },
    { path: 'batches', component: BatchesComponent },
    { path: 'enrollments', component: EnrollmentsComponent },
  ],
},
{ path: '**', redirectTo: '' },
```

- Interceptor attaches the JWT and redirects to `/login` on 401.
- Guards hide the wrong routes; the API still enforces every rule.
- Store the access token in memory or `sessionStorage`, not in environment files.
- Frontend receives `secure_url` only after the API authorizes the user.

## 11. MVP vs stretch

**MVP (build all of this):**

- JWT login, `/me`, password change, role dashboards, auth + role guards
- Courses, batches, enrollment; students see only their batches
- Thin admin list-and-form screens (users, courses, batches, enrollments)
- Multipart upload to Cloudinary; store `public_id` + `secure_url`
- Authorized access to `secure_url` (video for lectures, raw for docs)
- Assignment create / submit-or-replace until deadline / grade + feedback
- In-app notifications (new material, new assignment, posted grade)
- Search and tags, scoped to allowed courses
- HTML5 lecture playback from the Cloudinary video URL
- Cloudinary in both development and production

**Stretch (do not start until the MVP is demoable):**

- Signed or authenticated Cloudinary URLs
- PDF watermark with student ID
- Teacher analytics (`ResourceAccess`, `/api/analytics/courses/{id}`)
- Refresh tokens and email notifications
- CSV bulk enrollment

## 12. Coding conventions

- Request and response bodies are DTOs. **Never expose JPA entities** (no lazy-load leaks, no password hashes).
- Validate inputs with `@Valid` and Bean Validation (`@NotBlank`, `@Email`, `@Size`, `@Future`, etc.).
- Centralize errors in `GlobalExceptionHandler` (`@RestControllerAdvice`): 400 validation, 401/403 authz, 404 not found, 409 uniqueness, 413/415 file issues. Return a consistent error JSON.
- Secrets only from environment variables / `application.properties` placeholders already defined. Never commit real keys.
- Thin controllers; all business rules and object-level checks in services.
- Use JPA / parameterized queries only. Do not concatenate SQL.
- Enums as `STRING`. Explicit table and column names.
- Seed an admin, two teachers, and a few students with a `CommandLineRunner` or optional `data.sql` (BCrypt hashes, known demo passwords documented in README only).
- Tests: JUnit 5, Mockito, MockMvc on the API; Jasmine/Karma for auth service, interceptor, and guards. Permission tests matter most.

## 13. Build order

Work these milestones in order. Check a box only when its acceptance criteria pass.

### Milestone 1 — Foundation

- [ ] Package layout under `com.portal.learningportal` as specified
- [ ] Enums and JPA entities with relationships and unique constraints
- [ ] Repositories
- [ ] DTO package + `GlobalExceptionHandler` skeleton
- [ ] App starts with local MySQL; Hibernate creates tables (`ddl-auto=update`)

**Acceptance:** `mvn spring-boot:run` starts, connects with `DB_USERNAME` / `DB_PASSWORD`, and creates `users`, `courses`, `batches`, `enrollments`, `resources`, `tags`, `resource_tags`, `assignments`, `submissions`, `notifications`. No Flyway, no Docker.

### Milestone 2 — JWT auth

- [ ] `SecurityConfig`, `JwtService` (`jwt.secret`, `jwt.expiration-ms`), `JwtAuthFilter`, `UserDetailsService`
- [ ] `POST /api/auth/login`, `GET /api/auth/me`, `PUT /api/auth/password`, profile GET/PUT
- [ ] CORS for `http://localhost:4200`
- [ ] Seed admin, two teachers, two students (inactive user cannot log in)
- [ ] BCrypt; password hash never appears in responses

**Acceptance:** Valid login returns a JWT whose role matches the user. `/api/auth/me` works with the token and returns 401 without one. Wrong password is 401. Tests cover login and role in the token.

### Milestone 3 — Courses, batches, enrollment

- [ ] Admin user / course / batch / enrollment APIs
- [ ] Role-filtered `GET /api/courses`
- [ ] Object-level checks (teacher sees only assigned courses; student only enrolled batches)
- [ ] MockMvc isolation tests

**Acceptance:** Admin can create two teachers, two batches, and two students, and enroll each student in only one batch. Student A cannot read Student B's course. A teacher cannot write to a course they do not teach. Unpublished material stays hidden from students.

### Milestone 4 — Cloudinary resources

- [ ] `CloudinaryConfig` + `CloudinaryService` using existing Cloudinary properties
- [ ] Multipart upload on `POST /api/courses/{id}/resources`
- [ ] Persist `public_id`, `secure_url`, `cloudinary_resource_type` (`video` or `raw`)
- [ ] Tags, publish flag, authorized GET / access / delete
- [ ] Type and size validation before the SDK call

**Acceptance:** A teacher can upload a PDF note (`raw`) and an MP4 lecture (`video`). MySQL stores `public_id` and `secure_url`. An enrolled student can GET the resource and receive `secure_url`; a non-enrolled student cannot. Invalid or oversized files never reach Cloudinary.

### Milestone 5 — Assignments

- [ ] Teacher creates an assignment (deadline, max marks, optional Cloudinary attachment)
- [ ] Student submits or replaces one file until `due_at`; unique (assignment, student)
- [ ] Teacher lists submissions and posts marks + feedback (`GRADED`)
- [ ] Deadline policy enforced

**Acceptance:** Submit before the deadline succeeds and a second submit updates the same row. Submit after `due_at` is rejected (or marked late only if that policy is explicitly implemented). Only the course teacher can grade. Student sees marks and feedback after grading.

### Milestone 6 — Notifications and search

- [ ] Notifications created when a resource is published, an assignment is created, or a submission is graded
- [ ] `GET /api/notifications` and mark-as-read
- [ ] `GET /api/search` by title, type, tag, scoped to allowed courses

**Acceptance:** The enrolled student receives the three notification types and does not receive events for other batches. Search never returns another batch's material.

### Milestone 7 — Angular SPA shell

- [ ] New `frontend/` app with environments pointing at the API
- [ ] Login, `AuthService`, JWT interceptor, `authGuard`, `roleGuard`
- [ ] Shared shell (nav, bell placeholder, profile/logout)
- [ ] Role route trees and empty dashboards
- [ ] Token in memory or `sessionStorage` only

**Acceptance:** Login stores the token and routes to the correct dashboard. A missing token redirects to `/login`. A student cannot open `/admin`. 401 from the API sends the user back to login. Jasmine/Karma tests cover the interceptor and guards.

### Milestone 8 — Angular MVP screens

- [ ] Admin: users, courses, batches, enrollments (simple tables and forms)
- [ ] Teacher: my courses, upload, create assignment, grade submissions, read-only roster
- [ ] Student: my courses, course hub, notes, HTML5 lecture player, assignment submit and grade view
- [ ] Search, notifications, profile, password change

**Acceptance (end-to-end UAT):**

1. Admin creates two teachers, two batches, two students; enrolls each student in one batch.
2. Teacher 1 uploads a note and a lecture, then creates an assignment with a deadline.
3. Student 1 sees the notification, opens the note, watches the lecture, and submits the assignment.
4. Student 2 cannot see Teacher 1's course or files.
5. Teacher 1 grades the submission; Student 1 sees marks, feedback, and a notification.

### After MVP (optional stretch, then harden)

- [ ] Signed Cloudinary URLs, watermarking, analytics, refresh tokens, email, or CSV enrollment — only if Milestone 8 already passes
- [ ] Permission and business-rule tests completed; `ddl-auto=validate` documented for production
- [ ] README with how to run (`mvn spring-boot:run`, `ng serve`), env vars, and demo accounts
- [ ] Deploy notes: Maven jar on Render/Railway (native Java) or AWS EC2/Elastic Beanstalk; Angular on Vercel/Netlify; hosted MySQL (Railway, Aiven, or RDS); Cloudinary for files

## 14. Out of scope unless asked

Docker, Docker Compose, Flyway, S3, local disk uploads, public user registration, teacher-managed enrollment, and all stretch features before the MVP UAT passes.
