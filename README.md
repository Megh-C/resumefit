# ResumeFit

A resume intelligence platform that analyzes a candidate's resume against 5,000 job postings to produce semantic job matches, skill gap analysis, salary inference, and personalized career insights.

Built backend-first with Spring Boot and Spring AI. The core design principle: **use an LLM only for tasks that genuinely require language understanding.** The pipeline has eight stages but only two LLM calls — everything else is embedding similarity, deterministic logic, or SQL aggregation.

## What it does

Upload a resume PDF and the system:

1. Extracts raw text (PDFBox)
2. Parses structured data — skills, experience, roles, seniority, education (Gemini, LLM call 1)
3. Normalizes extracted skills against a canonical vocabulary via embedding similarity (local ONNX model)
4. Matches the resume against 5,000 job postings using vector cosine similarity (pgvector)
5. Filters out jobs the candidate is underqualified for and ranks the rest with a weighted fit score
6. Computes skill gaps by frequency across the matched jobs
7. Infers current and post-upskilling salary ranges from real job data
8. Generates personalized, seniority-aware career insights (Gemini, LLM call 2)

The full result is cached in Redis per session so repeated requests skip the entire pipeline.

## Architecture

```
PDF ──► PDFBox ──► Gemini extraction ──► ONNX skill normalization ──► pgvector match
                                                                            │
        Redis cache ◄── Gemini insights ◄── salary inference ◄── gap analysis
```

| Stage | Technology | Type |
|-------|-----------|------|
| PDF extraction | Apache PDFBox 3.0.2 | Deterministic |
| Resume parsing | Gemini (Spring AI) | LLM call 1 |
| Skill normalization | all-MiniLM-L6-v2 (ONNX, local) | Embedding similarity |
| Job matching | pgvector cosine search | Embedding similarity |
| Qualification filter + fit scoring | Rule-based | Deterministic |
| Gap analysis | Java Set operations | Deterministic |
| Salary inference | SQL aggregation | Deterministic |
| Insight generation | Gemini (Spring AI) | LLM call 2 |
| Session caching | Redis 7 | Infrastructure |

## Tech stack

- **Java 21 / Spring Boot 4.1.0**
- **Spring AI 2.0.0** — LLM and embedding orchestration
- **PostgreSQL + pgvector** — relational data and 384-dimensional vector storage in one system
- **all-MiniLM-L6-v2 via ONNX Runtime** — local embedding model, no external API, runs offline after first download
- **Google Gemini** — structured extraction and insight generation
- **Redis 7** — session response caching
- **Apache PDFBox, OpenCSV**

## Key design decisions

**Only two LLM calls.** Skill-to-vocabulary mapping uses embedding similarity, not an LLM — embeddings are more reliable and deterministic at fuzzy matching than an LLM scanning a list. Gap analysis and salary inference are pure logic. The LLM is reserved for what only it can do: reading messy resume text and generating personalized prose.

**Local embeddings over an API.** Running all-MiniLM-L6-v2 locally via ONNX avoids rate limits and per-call cost for the 5,000+ embeddings the system generates, and works offline after the model is cached.

**Qualification filtering is directional.** A candidate can apply "down" but not "up" — an M.Tech holder qualifies for B.Tech roles but not vice versa; a 3-year candidate qualifies for 1-year roles but a fresher doesn't qualify for senior roles. Underqualified matches are removed entirely rather than shown with a low score.

**Empirically tuned normalization threshold.** The skill-matching similarity threshold (0.70) was derived from real test data — genuine matches scored 0.73–0.79, confusable-but-different pairs (Microservices vs Microcontrollers) scored 0.43 — not guessed.

## Running locally

**Prerequisites:** Java 21, Maven, Docker.

**1. Start PostgreSQL with pgvector and Redis:**

```bash
docker run -d --name resumematcher-db \
  -e POSTGRES_USER=postgres -e POSTGRES_PASSWORD=postgres \
  -e POSTGRES_DB=resumematcher -p 5433:5432 \
  pgvector/pgvector:pg16

docker run -d --name resumematcher-redis -p 6379:6379 redis:7-alpine
```

**2. Enable the pgvector extension** (connect to the `resumematcher` database):

```sql
CREATE EXTENSION IF NOT EXISTS vector;
```

**3. Configure the application:**

Copy `application.properties.example` to `application.properties` and fill in your Postgres credentials and Gemini API key (get one at [aistudio.google.com](https://aistudio.google.com)).

**4. Add the dataset:**

Place the job postings CSV at `src/main/resources/data/jobs.csv`.

**5. Run:**

```bash
./mvnw spring-boot:run
```

On first startup the app downloads the ONNX embedding model (~90MB, cached afterward), ingests the 5,000 jobs, builds the skill vocabulary, and embeds everything into pgvector. Subsequent startups skip this via idempotency checks.

## API

**`POST /api/v1/resume/analyze`**

Multipart form upload with a `file` field (the resume PDF). Optional `X-Session-Id` header to retrieve a cached result.

Returns a JSON object with `resumeSummary`, `topMatches`, `gapAnalysis`, `salaryAnalysis`, and `insights`. Response headers include `X-Session-Id` (for cache retrieval on subsequent requests) and `X-Cache` (`HIT` or `MISS`).

## Notes

The job postings dataset is synthetic (Kaggle), used deliberately so the engineering focus stays on the intelligence pipeline rather than on job scraping. Salary figures are directional inferences from that dataset, not real market guarantees.