package com.portal.learningportal.service;

import com.portal.learningportal.dto.QuizAnswerItem;
import com.portal.learningportal.dto.QuizAttemptResponse;
import com.portal.learningportal.dto.QuizAttemptSummaryResponse;
import com.portal.learningportal.dto.QuizGenerateRequest;
import com.portal.learningportal.dto.QuizQuestionUpdateRequest;
import com.portal.learningportal.dto.QuizResponse;
import com.portal.learningportal.dto.QuizSubmitRequest;
import com.portal.learningportal.entity.QuizAttempt;
import com.portal.learningportal.entity.QuizAttemptAnswer;
import com.portal.learningportal.entity.Course;
import com.portal.learningportal.entity.Quiz;
import com.portal.learningportal.entity.QuizQuestion;
import com.portal.learningportal.entity.Resource;
import com.portal.learningportal.entity.Role;
import com.portal.learningportal.entity.User;
import com.portal.learningportal.exception.BadRequestException;
import com.portal.learningportal.exception.ForbiddenException;
import com.portal.learningportal.exception.ResourceNotFoundException;
import com.portal.learningportal.repository.QuizAttemptRepository;
import com.portal.learningportal.repository.QuizQuestionRepository;
import com.portal.learningportal.repository.QuizRepository;
import com.portal.learningportal.repository.ResourceRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestClient;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class QuizService {

    private final QuizRepository quizRepository;
    private final QuizAttemptRepository quizAttemptRepository;
    private final QuizQuestionRepository quizQuestionRepository;
    private final ResourceRepository resourceRepository;
    private final CurrentUserService currentUserService;
    private final CourseAccessService courseAccessService;
    private final PdfTextExtractorService pdfTextExtractorService;
    private final OpenAiService openAiService;
    private final QuizJsonParser quizJsonParser;
    private final NotificationService notificationService;

    public QuizService(
            QuizRepository quizRepository,
            QuizAttemptRepository quizAttemptRepository,
            QuizQuestionRepository quizQuestionRepository,
            ResourceRepository resourceRepository,
            CurrentUserService currentUserService,
            CourseAccessService courseAccessService,
            PdfTextExtractorService pdfTextExtractorService,
            OpenAiService openAiService,
            QuizJsonParser quizJsonParser,
            NotificationService notificationService
    ) {
        this.quizRepository = quizRepository;
        this.quizAttemptRepository = quizAttemptRepository;
        this.quizQuestionRepository = quizQuestionRepository;
        this.resourceRepository = resourceRepository;
        this.currentUserService = currentUserService;
        this.courseAccessService = courseAccessService;
        this.pdfTextExtractorService = pdfTextExtractorService;
        this.openAiService = openAiService;
        this.quizJsonParser = quizJsonParser;
        this.notificationService = notificationService;
    }

    @Transactional(readOnly = true)
    public List<QuizResponse> listCourseQuizzes(Long courseId) {
        User user = currentUserService.requireAuthenticatedUser();
        courseAccessService.requireReadableCourse(user, courseId);
        return quizRepository.findByCourseIdWithDetails(courseId).stream()
                .filter(q -> user.getRole() != Role.STUDENT || Boolean.TRUE.equals(q.getIsPublished()))
                .map(q -> QuizResponse.summary(q, (int) quizQuestionRepository.countByQuizId(q.getId())))
                .toList();
    }

    @Transactional(readOnly = true)
    public QuizResponse getQuiz(Long quizId) {
        User user = currentUserService.requireAuthenticatedUser();
        Quiz quiz = requireQuiz(quizId);
        courseAccessService.requireReadableCourse(user, quiz.getCourse().getId());
        boolean includeAnswers = user.getRole() == Role.TEACHER || user.getRole() == Role.ADMIN;
        if (user.getRole() == Role.STUDENT && !Boolean.TRUE.equals(quiz.getIsPublished())) {
            throw new ForbiddenException("Quiz is not published yet.");
        }
        return QuizResponse.from(quiz, includeAnswers);
    }

    @Transactional
    public QuizResponse generateFromResource(Long resourceId, QuizGenerateRequest request) {
        User user = currentUserService.requireAnyRole(Role.TEACHER, Role.ADMIN);
        Resource resource = resourceRepository.findByIdWithDetails(resourceId)
                .orElseThrow(() -> new ResourceNotFoundException("Resource not found"));
        Course course = resource.getCourse();
        courseAccessService.requireManageableCourse(user, course.getId());
        if (user.getRole() == Role.TEACHER && !course.getTeacher().getId().equals(user.getId())) {
            throw new ForbiddenException("You can only generate quizzes for your own courses.");
        }
        if (!isPdf(resource)) {
            throw new BadRequestException("Quizzes can only be generated from PDF notes.");
        }

        byte[] pdfBytes = RestClient.create()
                .get()
                .uri(resource.getSecureUrl())
                .retrieve()
                .body(byte[].class);
        if (pdfBytes == null || pdfBytes.length == 0) {
            throw new BadRequestException("Could not download PDF from storage.");
        }
        String pages = request.pagesOrBlank();
        String text = pdfTextExtractorService.extractText(pdfBytes, pages.isEmpty() ? null : pages);
        int count = request.resolvedCount();
        List<QuizJsonParser.ParsedQuestion> parsed = null;
        for (int attempt = 0; attempt < 2; attempt++) {
            String aiJson = openAiService.generateQuizJson(request, resource.getTitle(), text);
            try {
                parsed = quizJsonParser.parseModelJson(aiJson, count);
                break;
            } catch (BadRequestException ex) {
                if (attempt == 0) {
                    continue;
                }
                throw ex;
            }
        }
        if (parsed == null) {
            throw new BadRequestException("Could not generate a valid quiz from AI output.");
        }

        Quiz quiz = Quiz.builder()
                .title("Quiz: " + resource.getTitle())
                .isPublished(false)
                .course(course)
                .sourceResource(resource)
                .createdBy(user)
                .build();

        int order = 0;
        for (QuizJsonParser.ParsedQuestion pq : parsed) {
            QuizQuestion question = QuizQuestion.builder()
                    .questionText(pq.text())
                    .optionA(QuizJsonParser.normalizeOption(pq.options().get(0)))
                    .optionB(QuizJsonParser.normalizeOption(pq.options().get(1)))
                    .optionC(QuizJsonParser.normalizeOption(pq.options().get(2)))
                    .optionD(QuizJsonParser.normalizeOption(pq.options().get(3)))
                    .correctIndex(pq.correctIndex())
                    .explanation(pq.explanation())
                    .sortOrder(order++)
                    .quiz(quiz)
                    .build();
            quiz.getQuestions().add(question);
        }
        quizRepository.save(quiz);
        return QuizResponse.from(requireQuiz(quiz.getId()), true);
    }

    @Transactional
    public QuizResponse publish(Long quizId, boolean published) {
        User user = currentUserService.requireAnyRole(Role.TEACHER, Role.ADMIN);
        Quiz quiz = requireQuiz(quizId);
        courseAccessService.requireManageableCourse(user, quiz.getCourse().getId());
        if (user.getRole() == Role.TEACHER && !quiz.getCourse().getTeacher().getId().equals(user.getId())) {
            throw new ForbiddenException("Access denied");
        }
        boolean wasPublished = Boolean.TRUE.equals(quiz.getIsPublished());
        quiz.setIsPublished(published);
        if (published && !wasPublished) {
            notificationService.notifyQuizPublished(quiz);
        }
        return QuizResponse.from(quiz, true);
    }

    @Transactional
    public void deleteQuiz(Long quizId) {
        User user = currentUserService.requireAnyRole(Role.TEACHER, Role.ADMIN);
        Quiz quiz = requireQuiz(quizId);
        courseAccessService.requireManageableCourse(user, quiz.getCourse().getId());
        assertTeacherOwnsCourse(user, quiz.getCourse());
        if (Boolean.TRUE.equals(quiz.getIsPublished())) {
            throw new BadRequestException("Published quizzes cannot be deleted. Unpublish is not supported yet; create a new draft instead.");
        }
        quizRepository.delete(quiz);
    }

    @Transactional
    public QuizResponse updateQuestion(Long quizId, Long questionId, QuizQuestionUpdateRequest body) {
        User user = currentUserService.requireAnyRole(Role.TEACHER, Role.ADMIN);
        Quiz quiz = requireQuiz(quizId);
        courseAccessService.requireManageableCourse(user, quiz.getCourse().getId());
        assertTeacherOwnsCourse(user, quiz.getCourse());
        requireDraft(quiz);
        QuizQuestion question = quiz.getQuestions().stream()
                .filter(q -> q.getId().equals(questionId))
                .findFirst()
                .orElseThrow(() -> new ResourceNotFoundException("Question not found"));
        question.setQuestionText(body.questionText().trim());
        question.setOptionA(QuizJsonParser.normalizeOption(body.options().get(0)));
        question.setOptionB(QuizJsonParser.normalizeOption(body.options().get(1)));
        question.setOptionC(QuizJsonParser.normalizeOption(body.options().get(2)));
        question.setOptionD(QuizJsonParser.normalizeOption(body.options().get(3)));
        question.setCorrectIndex(body.correctIndex());
        return QuizResponse.from(quiz, true);
    }

    @Transactional
    public QuizResponse deleteQuestion(Long quizId, Long questionId) {
        User user = currentUserService.requireAnyRole(Role.TEACHER, Role.ADMIN);
        Quiz quiz = requireQuiz(quizId);
        courseAccessService.requireManageableCourse(user, quiz.getCourse().getId());
        assertTeacherOwnsCourse(user, quiz.getCourse());
        requireDraft(quiz);
        if (quiz.getQuestions().size() <= 1) {
            throw new BadRequestException("Quiz must have at least one question.");
        }
        QuizQuestion question = quiz.getQuestions().stream()
                .filter(q -> q.getId().equals(questionId))
                .findFirst()
                .orElseThrow(() -> new ResourceNotFoundException("Question not found"));
        quiz.getQuestions().remove(question);
        quizQuestionRepository.delete(question);
        renumberSortOrder(quiz);
        return QuizResponse.from(quiz, true);
    }

    @Transactional(readOnly = true)
    public List<QuizAttemptSummaryResponse> listQuizAttempts(Long quizId) {
        User user = currentUserService.requireAnyRole(Role.TEACHER, Role.ADMIN);
        Quiz quiz = requireQuiz(quizId);
        courseAccessService.requireManageableCourse(user, quiz.getCourse().getId());
        assertTeacherOwnsCourse(user, quiz.getCourse());
        return quizAttemptRepository.findByQuizIdWithStudent(quizId).stream()
                .map(QuizAttemptSummaryResponse::from)
                .toList();
    }

    private static void renumberSortOrder(Quiz quiz) {
        int order = 0;
        for (QuizQuestion q : quiz.getQuestions()) {
            q.setSortOrder(order++);
        }
    }

    private static void requireDraft(Quiz quiz) {
        if (Boolean.TRUE.equals(quiz.getIsPublished())) {
            throw new BadRequestException("Unpublish the quiz before editing questions.");
        }
    }

    private static void assertTeacherOwnsCourse(User user, Course course) {
        if (user.getRole() == Role.TEACHER && !course.getTeacher().getId().equals(user.getId())) {
            throw new ForbiddenException("Access denied");
        }
    }

    @Transactional
    public QuizAttemptResponse submitAttempt(Long quizId, QuizSubmitRequest request) {
        User user = currentUserService.requireRole(Role.STUDENT);
        Quiz quiz = requireQuiz(quizId);
        courseAccessService.requireReadableCourse(user, quiz.getCourse().getId());
        if (!Boolean.TRUE.equals(quiz.getIsPublished())) {
            throw new ForbiddenException("This quiz is not available yet.");
        }
        List<QuizQuestion> questions = quiz.getQuestions();
        if (questions.isEmpty()) {
            throw new BadRequestException("This quiz has no questions.");
        }
        Map<Long, QuizQuestion> byId = new HashMap<>();
        for (QuizQuestion q : questions) {
            byId.put(q.getId(), q);
        }
        if (request.answers().size() != questions.size()) {
            throw new BadRequestException("Answer every question before submitting.");
        }
        QuizAttempt attempt = QuizAttempt.builder()
                .quiz(quiz)
                .student(user)
                .totalQuestions(questions.size())
                .score(0)
                .build();
        int score = 0;
        for (QuizAnswerItem item : request.answers()) {
            QuizQuestion question = byId.get(item.questionId());
            if (question == null) {
                throw new BadRequestException("Invalid question in submission.");
            }
            boolean correct = question.getCorrectIndex() == item.selectedIndex();
            if (correct) {
                score++;
            }
            QuizAttemptAnswer answer = QuizAttemptAnswer.builder()
                    .attempt(attempt)
                    .question(question)
                    .selectedIndex(item.selectedIndex())
                    .correct(correct)
                    .build();
            attempt.getAnswers().add(answer);
        }
        attempt.setScore(score);
        quizAttemptRepository.save(attempt);
        return QuizAttemptResponse.from(
                quizAttemptRepository.findByIdWithAnswers(attempt.getId()).orElseThrow());
    }

    @Transactional(readOnly = true)
    public QuizAttemptResponse getMyLatestAttempt(Long quizId) {
        User user = currentUserService.requireRole(Role.STUDENT);
        Quiz quiz = requireQuiz(quizId);
        courseAccessService.requireReadableCourse(user, quiz.getCourse().getId());
        return quizAttemptRepository.findByQuizIdAndStudentIdOrderBySubmittedAtDesc(quizId, user.getId()).stream()
                .findFirst()
                .flatMap(a -> quizAttemptRepository.findByIdWithAnswers(a.getId()))
                .map(QuizAttemptResponse::from)
                .orElseThrow(() -> new ResourceNotFoundException("No attempt found for this quiz."));
    }

    private Quiz requireQuiz(Long id) {
        return quizRepository.findByIdWithDetails(id)
                .orElseThrow(() -> new ResourceNotFoundException("Quiz not found"));
    }

    private static boolean isPdf(Resource resource) {
        String mime = resource.getMimeType();
        if (mime != null && mime.toLowerCase().contains("pdf")) {
            return true;
        }
        String url = resource.getSecureUrl();
        return url != null && url.toLowerCase().contains(".pdf");
    }
}