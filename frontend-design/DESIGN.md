# Setu UI — design notes for the Angular app

Static HTML prototype of the Online Learning Portal SPA. Reproduce this look in `frontend/` using the tokens and the layout shell below. Sample content is fictional (Nexus Institute of Technology, Nagpur) and is clearly demo data.

Production MVP has **no public sign-up**; the admin issues accounts. The Register tab and role select on `login.html` exist only so reviewers can jump between roles in this prototype.

## Screen map

| Prototype file | Angular route | Component (from PROJECT_PLAN.md) | Notes |
|---|---|---|---|
| `index.html` | — | — | Design index only; not a product route. |
| `login.html` | `/login` | `LoginComponent` | Sign-in form. Register + role select is prototype-only; do not ship public registration. Quick demo buttons are also prototype-only. |
| *(no separate landing file; sign-in panel copy can seed it)* | `/` | `LandingComponent` | Public marketing/intro page. Reuse the left-panel copy and brand from `login.html`. |
| `student-dashboard.html` | `/student/dashboard` | `StudentDashboardComponent` | Enrolled-course cards, recent uploads, deadlines, notifications. |
| `student-dashboard.html#courses` | `/student/courses` | `MyCoursesComponent` | Reuse the enrolled-course cards as a dedicated list if needed. |
| `course-detail.html` | `/student/courses/:id` | `CourseHubComponent` | Tabs: Notes, Lectures (HTML5 / Cloudinary player + playlist), Assignments. Same hub can be reused for teachers with upload/grade actions. |
| `assignment-submit.html` | `/student/assignments/:id` | `AssignmentDetailComponent` | Brief, Cloudinary upload, policy, grade and feedback. |
| `teacher-dashboard.html` | `/teacher/dashboard` | `TeacherDashboardComponent` | My courses, upload stats, pending grades, activity. |
| `teacher-upload.html` | `/teacher/upload` (or from the course hub) | `UploadMaterialComponent` | Title, type, course, tags, drop zone, Cloudinary progress, publish flag. |
| `teacher-submissions.html` | `/teacher/assignments/:id/submissions` | `GradeSubmissionsComponent` | Class table, marks, feedback, save grade. |
| `teacher-dashboard.html` (course cards) | `/teacher/courses/:id/roster` | `TeacherRosterComponent` | Read-only roster lives with the course; not a standalone mock. |
| `admin-dashboard.html` | `/admin` (dashboard home) | `AdminDashboardComponent` | Summary metrics. |
| `admin-dashboard.html#users` | `/admin/users` | `UsersComponent` | User table, approve/decline pending teachers, activate/deactivate. |
| `admin-dashboard.html#courses` | `/admin/courses` | `AdminCoursesComponent` | Course list, assign teacher, active/retired. |
| `admin-dashboard.html#batches` | `/admin/batches` | `BatchesComponent` | Batch / section list. |
| `admin-dashboard.html#enrollments` | `/admin/enrollments` | `EnrollmentsComponent` | Enroll / drop students. |
| `profile.html` | `/profile` (student) | `ProfileComponent` | Shared profile + `PUT /api/auth/password`. |
| `profile-teacher.html` | `/profile` (teacher) | `ProfileComponent` | Same component, role-specific fields. |
| `profile-admin.html` | `/profile` (admin) | `ProfileComponent` | Same component, role-specific fields. |

Shared chrome (every authenticated route):

- Sidebar + top bar → `AppShellComponent` (or `shared/` layout).
- Search field in the top bar → navigates to a search results view (`SearchComponent`, plan route can sit at `/search`).
- Bell → `NotificationsComponent`.
- Auth guard / role guard / JWT interceptor as specified in `PROJECT_PLAN.md`.

## Layout

- App grid: sidebar `248px` + main column. Sidebar is sticky and full viewport height.
- Top bar height: `64px`, sticky, white, 1px bottom border.
- Content padding: `24px`, max width about `1120px`.
- Below `860px`: sidebar becomes an off-canvas drawer; hamburger appears in the top bar.
- Cards, 12px–16px radii, light 1px borders, very soft shadow. Do not add heavy gradients on content cards.
- Sign-in is a two-column split (indigo panel + form). The left panel hides on small screens.

## Colour tokens

Copy these into `:root` (or Angular `styles.scss`) as CSS variables.

| Token | Value | Use |
|---|---|---|
| `--color-bg` | `#F3F5FA` | Page background |
| `--color-surface` | `#FFFFFF` | Cards, sidebar, top bar, inputs |
| `--color-surface-muted` | `#F7F8FC` | Table headers, tabs track, drop zone |
| `--color-border` | `#E4E7F0` | Hairlines |
| `--color-border-strong` | `#CFD5E4` | Drop-zone dash |
| `--color-primary` | `#3B54D1` | Buttons, links, active accents |
| `--color-primary-hover` | `#3248B8` | Primary hover |
| `--color-primary-pressed` | `#2A3C9E` | Primary pressed |
| `--color-primary-soft` | `#EEF1FB` | Active nav, badges, soft buttons |
| `--color-primary-soft-2` | `#DFE5F8` | Progress track, hover soft |
| `--color-primary-ink` | `#2A3A8F` | Text on soft primary |
| `--color-text` | `#1A1F2E` | Headings and body |
| `--color-text-secondary` | `#5C6578` | Supporting copy |
| `--color-text-muted` | `#8B93A7` | Meta, placeholders, kickers (except brand kicker) |
| `--color-text-on-primary` | `#FFFFFF` | Primary button label |
| `--color-success` | `#0F8A62` | Positive status |
| `--color-success-soft` | `#E7F6F0` | Success badge background |
| `--color-warning` | `#B57600` | Deadlines, pending |
| `--color-warning-soft` | `#FFF5E0` | Warning badge background |
| `--color-danger` | `#C53D3D` | Errors, missing work |
| `--color-danger-soft` | `#FDECEC` | Danger badge / PDF icon |
| `--color-login-panel` | `#24305A` | Sign-in left panel start |
| `--color-login-panel-2` | `#2D3C72` | Sign-in left panel mid |

Use primary sparingly: primary buttons, the active nav item, key badges, and the brand mark. Status colour is only for status words and badges.

## Type tokens

Font files are in `assets/fonts/`. In Angular, add Plus Jakarta Sans (variable, weights 400–800) and IBM Plex Mono (400).

| Token | Value |
|---|---|
| `--font-sans` | `"Plus Jakarta Sans", "IBM Plex Sans", "Inter", system-ui, sans-serif` |
| `--font-mono` | `"IBM Plex Mono", ui-monospace, monospace` |
| `--text-xs` | `0.75rem` (12px) — badges, meta, labels |
| `--text-sm` | `0.8125rem` (13px) — nav, table, buttons |
| `--text-base` | `0.9375rem` (15px) — body |
| `--text-md` | `1.0625rem` (17px) — card titles |
| `--text-lg` | `1.25rem` (20px) |
| `--text-xl` | `1.5rem` (24px) |
| `--text-2xl` | `1.875rem` (30px) — page titles |
| `--leading-tight` | `1.25` — headings (`letter-spacing: -0.02em`) |
| `--leading-body` | `1.55` |

Page kicker: 12px, uppercase, 0.06em spacing, weight 600, colour `--color-primary`.

## Spacing, radius, shadow

| Token | Value |
|---|---|
| `--space-1` … `--space-12` | 4, 8, 12, 16, 20, 24, 32, 40, 48 px |
| `--radius-sm` | `8px` — buttons, inputs, nav items |
| `--radius-md` | `12px` — tabs |
| `--radius-lg` | `16px` — cards, player |
| `--radius-full` | `999px` — avatars, chips, search |
| `--shadow-sm` | `0 1px 2px rgba(26, 31, 46, 0.05)` |
| `--shadow-md` | `0 8px 24px rgba(26, 31, 46, 0.06)` |
| `--sidebar-width` | `248px` |
| `--topbar-height` | `64px` |

## Interaction notes for Angular

- Course hub tabs are client-side (`data-tabs` in the prototype). Use Angular routed child routes or a tab component; default the Lectures tab if you want the player visible first.
- The upload drop zone should show Cloudinary progress (bytes uploaded / total), then the returned `public_id` and `resource_type`. The prototype’s “Replay upload” control is for the demo only.
- Saving a grade should set status to `GRADED` and trigger the in-app notification described in the plan.
- Approving a teacher is an admin user-status change (`PATCH /api/users/{id}/status`). The pending-approval rows are a design for that flow.
- Never put `JWT_SECRET` or Cloudinary secrets in the Angular app. The frontend only receives `secure_url` after the API authorizes the user.
