# AI Workflow

Use AI as a strict tutor, not a code vending machine.

## Feature Work

Before asking AI to write code, use this prompt:

```text
Use the spring-feature skill.

Do not give me the final code immediately.

First explain:
1. What problem this feature solves
2. What files are needed
3. What each file is responsible for
4. What data moves through the system
5. What can go wrong
6. Then give me the smallest working implementation
7. Then quiz me on the code
```

## Code Explanation

After code is written, use this prompt:

```text
Use the explain-like-im-learning skill.

Explain this file line by line like I am new to Java and Spring Boot.
Then explain how an experienced backend engineer would describe it in an interview.
```

## Cloud Review

Before deploying or changing deployment files, use this prompt:

```text
Use the cloud-deploy-check skill.

Review this project for Cloud Run and Cloud SQL readiness.
Explain what is configured correctly, what is missing, and what I should verify before deploying.
```

## Why This Exists

The goal is not to pretend to be a senior engineer. The goal is to understand the system well enough to defend every file, endpoint, transaction, and deployment decision in an interview.
