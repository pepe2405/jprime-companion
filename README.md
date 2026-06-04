# jPrime Connect

jPrime Connect is a full-stack companion networking app for the jPrime event. Attendees register from `/join/jprime`, complete a quick profile, select interests and talks, discover relevant people, create mutual matches, connect through personal QR codes, plan meetings, and ask a rule-based assistant for lecture recommendations.

## Stack

- Backend: Java, Spring Boot, Spring Security, JWT, JDBC/JPA dependencies, Flyway, PostgreSQL
- Frontend: Vite, React, TypeScript, React Router, Tailwind CSS, Axios, QR code rendering
- Local database: PostgreSQL 16 through Docker Compose

## Run locally

1. Start PostgreSQL:

   ```bash
   docker compose up -d
   ```

   The local Docker database uses ephemeral demo storage. Restarting the Postgres container resets users, swipes, matches, and meetings back to the seeded demo state.

2. Start the backend:

   ```bash
   cd backend
   ./mvnw spring-boot:run
   ```

3. Start the frontend:

   ```bash
   cd frontend
   npm install
   npm run dev
   ```

4. Open:

   ```txt
   http://localhost:5173/join/jprime
   ```

## Demo flow

1. Log in with the pre-populated demo account:

   ```txt
   maria@example.com
   password123
   ```

2. Open Matches to see several existing mutual matches, unlocked contact details, icebreakers, and planned meetings.
3. Open Discussions to see talk rooms with attendees who want to discuss specific sessions.
4. Open Forum to create a theme and reply to event discussion threads.
5. Open Meetings to accept or decline scheduled meeting proposals.
6. Open Discover to connect with additional attendees.
7. Ask the assistant: `Recommend talks for a backend Java developer interested in AI.`
8. Try an event-logistics prompt like `Where is the coffee area?` to see a non-lecture answer.
9. Plan a meeting in front of Hall B.
10. Open Profile and show the personal QR code.

Seed data includes interests, looking-for goals, agenda talks, demo attendee profiles, pre-created mutual matches for Maria, and a few planned meetings.

To reset the demo data manually:

```bash
docker compose restart postgres
cd backend
./mvnw spring-boot:run
```

After reset, log in again as `maria@example.com` / `password123`. If the browser still has an old token from a previous database, click Logout first or clear local storage.

## API

The backend serves APIs under `/api`, including:

- `/api/auth/register`, `/api/auth/login`, `/api/auth/me`
- `/api/users/me`, `/api/users/public/{publicId}`
- `/api/interests`, `/api/goals`, `/api/talks`, `/api/talk-discussions`
- `/api/users/me/talk-selections`
- `/api/discover`, `/api/swipes`
- `/api/matches`, `/api/matches/{matchId}`
- `/api/matches/{matchId}/meetings`, `/api/meetings`, `/api/meetings/{meetingId}/status`, `/api/meetings/{meetingId}/ics`
- `/api/forum/topics`, `/api/forum/topics/{topicId}/comments`
- `/api/assistant/recommend-talks`, `/api/assistant/icebreaker`

## Configuration

Backend defaults:

```properties
spring.datasource.url=jdbc:postgresql://localhost:5432/jprime_connect
spring.datasource.username=jprime
spring.datasource.password=jprime
jprime.cors.allowed-origins=http://localhost:5173
```

Frontend default:

```txt
VITE_API_BASE_URL=http://localhost:8080/api
```
