# CourseVault

Role-based learning portal (Spring Boot API + Angular). Teachers upload materials to Cloudinary; students access content by batch enrollment.

## Repository layout

| Path | Purpose |
|------|---------|
| `src/` | Spring Boot API |
| `frontend/` | Angular SPA |
| `frontend-design/` | Static UI reference (Setu) |

## Prerequisites

- Java 17, Maven 3.9+
- Node.js 18+ and npm
- MySQL (`CREATE DATABASE learning_portal;`)
- Cloudinary account

## Local setup

1. **Secrets (do not commit):** copy `src/main/resources/application-local.properties.example` to `application-local.properties` and fill in values, **or** set environment variables:

   - `DB_PASSWORD`, optional `DB_USERNAME`, `DB_URL`
   - `CLOUDINARY_CLOUD_NAME`, `CLOUDINARY_API_KEY`, `CLOUDINARY_API_SECRET`
   - `JWT_SECRET` (at least 32 characters)
   - `openai.api-key` (optional, for AI quiz — `sk-...` in `application-local.properties` only)

2. **API:** `mvn spring-boot:run` → [http://localhost:8080](http://localhost:8080)

3. **Frontend:** `cd frontend && npm install && npm start` → [http://localhost:4200](http://localhost:4200)

4. **Demo users** (after DB seed / tests data): `admin`, `teacher1`, `student1` — password `Demo@12345` (if seeded in your DB).

## Production deploy (overview)

- **Frontend:** Vercel or Netlify — root `frontend`, build `npm ci && npm run build`, output `dist/frontend/browser`. Set `environment.prod.ts` `apiUrl` to your live API (or use your host’s env + file replacement).
- **API:** Railway, Render, or Fly.io — run the Spring Boot JAR; set the same env vars as above.
- **Database:** hosted MySQL on Railway/RDS/etc.
- **CORS:** set `CORS_ALLOWED_ORIGINS=https://your-frontend.vercel.app` on the API.

Cloudinary must allow delivery for your files (unsigned delivery or signed URLs).

## Tests

```bash
mvn test
```

## OpenAI (local smoke test)

1. Set `openai.api-key=sk-...` in `application-local.properties` (gitignored).
2. Run the API and log in as `teacher1` / `Demo@12345`.
3. **In the app:** log in as `teacher1`, open **AI setup** in the sidebar (or dashboard card) → `/teacher/ai` → **Run connection test**.
4. **Optional Postman:** `GET/POST http://localhost:8080/api/ai/status|test` with `Authorization: Bearer <token>`.

## License

Private / academic use unless you add a license file.