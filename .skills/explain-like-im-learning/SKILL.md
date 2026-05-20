---
name: explain-like-im-learning
description: Use when the user wants a high-density, beginner-friendly, interview-ready explanation of Java, Spring Boot, backend architecture, database behavior, cloud deployment, or a specific code change.
---

Provide a deep, fluff-free technical walkthrough.

## 1. Big Picture

Explain the feature or code in plain English.

Cover:
- what problem it solves
- where it fits in the backend system
- why this design was chosen
- what would happen without this layer or file

Avoid filler. Start with the actual explanation.

## 2. System Flow Map

Provide an ASCII flow diagram.

Trace the request from entry point to final state.

Example:

```text
HTTP Request
   |
   v
Controller
   |
   v
Request DTO validation
   |
   v
Service business logic
   |
   v
Repository
   |
   v
Database
   |
   v
Response DTO
   |
   v
HTTP Response
```

Explicitly mark:

- where validation happens
- where business rules happen
- where database state changes happen
- where exceptions can be thrown
- where transactions begin/end

## 3. File-by-File Breakdown

For each relevant file, explain:

### Role

What this file is responsible for.

### Contract

What input it accepts and what output it guarantees.

### Why It Exists

Why this responsibility belongs here instead of another layer.

### Key Code

Explain important methods, fields, constructors, records, annotations, and return types.

### Spring Magic

Explain what Spring does at runtime.

Examples:

- `@RestController`
- `@Service`
- `@Repository`
- `@Entity`
- `@Transactional`
- `@Valid`
- `@RequestBody`
- `@PathVariable`
- `@GeneratedValue`

### Java Moment

Explain any Java-specific concept used.

Examples:

- class
- object
- interface
- enum
- record
- generics
- Optional
- BigDecimal
- List
- Map
- constructor injection
- immutability

For Java 25, mention modern Java only when relevant. Do not force random language trivia.

## 4. Database Explanation

If persistence is involved, explain:

- which table/entity changes
- what rows are inserted, updated, deleted, or queried
- what relationships exist
- why the relationship is one-to-one, one-to-many, or many-to-one
- what could go wrong with duplicate data, missing data, or invalid references

If transactions are involved, explain what must succeed or fail together.

## 5. API Explanation

If an endpoint is involved, explain:

- HTTP method
- URL path
- request body
- response body
- status code
- validation errors
- not-found errors
- business-rule errors

Include one sample request and one sample response.

## 6. Cloud/Production Explanation

If deployment, Docker, database config, or environment variables are involved, explain:

- what runs locally
- what runs in the cloud
- what is containerized
- where secrets/config values come from
- how the app connects to the managed database
- what should never be hardcoded
- what logs/metrics would matter in production

## 7. Jargon Buster

Define 3-5 technical terms used in the explanation.

Definitions must be precise and short.

## 8. Edge Cases

Give realistic edge cases.

Examples:

- product does not exist
- stock is insufficient
- same order request is sent twice
- database write fails halfway
- cloud database is unreachable
- invalid enum value is passed
- payment succeeds but order update fails

For each edge case, explain how the current code handles it or how it should handle it.

## 9. Interview Explanation

End with two versions:

### Beginner Explanation

A simple explanation in plain language.

### Senior Engineer Explanation

A more technical explanation using correct backend terminology.

## 10. Quiz Me

Ask 5 questions to check understanding.

Questions should test:

- Java basics
- Spring Boot layers
- database behavior
- API design
- production/cloud reasoning

Rules:

- No filler.
- No vague explanations.
- Always include a visual flow.
- Do not just say "it saves data." Explain exactly what object becomes what row and through which layer.
- Prioritize understanding over speed.
