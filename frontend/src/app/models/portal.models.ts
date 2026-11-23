import { UserRole } from './auth.models';

export type ResourceType = 'NOTE' | 'LECTURE' | 'OTHER';
export type EnrollmentStatus = 'ACTIVE' | 'DROPPED';
export type SubmissionStatus = 'SUBMITTED' | 'LATE' | 'GRADED';

export interface Course {
  id: number;
  code: string;
  title: string;
  description: string | null;
  active: boolean;
  createdAt: string;
  teacherId: number;
  teacherUsername: string;
  teacherName: string;
}

export interface CourseCreate {
  code: string;
  title: string;
  description?: string;
  teacherId: number;
  active?: boolean;
}

export interface UserAccount {
  id: number;
  username: string;
  email: string;
  role: UserRole;
  firstName: string;
  lastName: string;
  active: boolean;
}

export interface UserCreate {
  username: string;
  email: string;
  password: string;
  role: UserRole;
  firstName: string;
  lastName: string;
}

export interface Batch {
  id: number;
  name: string;
  academicYear: string;
  startDate: string | null;
  endDate: string | null;
  courseId: number;
  courseCode: string;
  courseTitle: string;
}

export interface BatchCreate {
  name: string;
  academicYear: string;
  startDate?: string;
  endDate?: string;
}

export interface Enrollment {
  id: number;
  status: EnrollmentStatus;
  enrolledAt: string;
  studentId: number;
  studentUsername: string;
  batchId: number;
  batchName: string;
  courseId: number;
  courseCode: string;
}

export interface ResourceItem {
  id: number;
  title: string;
  description: string | null;
  resourceType: ResourceType;
  secureUrl: string | null;
  published: boolean;
  createdAt: string;
  courseId: number;
  courseCode: string;
  tags: string[];
}

export interface ResourceAccess {
  secureUrl: string;
  resourceType: ResourceType;
  mimeType: string | null;
}

export interface Assignment {
  id: number;
  title: string;
  description: string | null;
  dueAt: string;
  maxMarks: number;
  published: boolean;
  courseId: number;
  courseCode: string;
  attachmentSecureUrl: string | null;
  mySubmission: Submission | null;
}

export interface Submission {
  id: number;
  assignmentId: number;
  studentId: number;
  studentUsername: string;
  submittedAt: string;
  status: SubmissionStatus;
  marks: number | null;
  feedback: string | null;
  secureUrl: string | null;
}

export interface NotificationItem {
  id: number;
  title: string;
  body: string;
  link: string | null;
  read: boolean;
  createdAt: string;
}

export interface SearchHit {
  id: number;
  title: string;
  description: string | null;
  resourceType: ResourceType;
  courseId: number;
  courseCode: string;
  courseTitle: string;
  published: boolean;
  tags: string[];
}

export interface ProfileUpdate {
  firstName: string;
  lastName: string;
  email: string;
}

export interface ChangePassword {
  currentPassword: string;
  newPassword: string;
}