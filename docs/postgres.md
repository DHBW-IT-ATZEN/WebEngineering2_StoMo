# Working with the StoMo PostgreSQL database

A practical cheat sheet for inspecting and fixing data in the StoMo database
(`stomo_db`), which runs inside the `stomo-postgres` Docker container.

Connection details come from your `.env` file (see [`.env.example`](../.env.example));
docker-compose injects them into the container as `POSTGRES_*` variables:

| Setting  | Value                                               |
|----------|-----------------------------------------------------|
| Container| `stomo-postgres`                                    |
| Database | `POSTGRES_DB` (default `stomo_db`)                  |
| User     | `POSTGRES_USER` (default `stomo_user`)             |
| Password | `POSTGRES_PASSWORD` — **required**, set in `.env`  |
| Port     | `5432`                                              |

> `POSTGRES_PASSWORD` has no in-repo default: the stack won't start until it is
> set in `.env` (which is gitignored). The commands below don't pass it
> explicitly — `psql` inside the container authenticates as the configured user
> without prompting. Never hardcode the password in commands or files; reference
> `$POSTGRES_PASSWORD` instead.

---

## 1. Open a SQL shell

First make sure the database container is up:

```bash
docker compose ps
```

Then open `psql` (PostgreSQL's interactive SQL shell) **inside** the container:

```bash
docker exec -it stomo-postgres psql -U stomo_user -d stomo_db
```

Breakdown:

- `docker exec -it stomo-postgres` — run a command interactively (`-it`) inside
  the running container named `stomo-postgres`.
- `psql -U stomo_user -d stomo_db` — start the SQL shell as user `stomo_user`,
  connected to database `stomo_db`.

You'll land at a prompt like `stomo_db=#`. Everything after this is typed there.

> Connecting from your host instead of inside the container? Use
> `psql -h localhost -p 5432 -U stomo_user -d stomo_db` (requires `psql`
> installed locally). Same database, since port 5432 is published.

---

## 2. psql meta-commands (navigation)

Commands starting with a backslash (`\`) are **psql commands**, not SQL. They
need no semicolon.

| Command        | What it does                                            |
|----------------|---------------------------------------------------------|
| `\dt`          | list all tables                                         |
| `\d users`     | describe the `users` table (columns, types, indexes)    |
| `\d`           | list everything (tables, sequences, views)              |
| `\l`           | list all databases                                      |
| `\du`          | list all roles/users                                    |
| `\x`           | toggle expanded output (one field per line — wide rows) |
| `\timing`      | toggle showing how long each query took                 |
| `\e`           | open the last query in an editor                        |
| `\! <cmd>`     | run a shell command                                     |
| `\?`           | help on psql commands                                   |
| `\h SELECT`    | SQL syntax help for a statement                         |
| `\q`           | quit                                                    |

Paging: long results open in a pager. Press **Space** to page down, **q** to
exit the pager back to the prompt.

---

## 3. Case sensitivity — the gotcha

This is the one that bit you. PostgreSQL treats **identifiers** (table and
column names) and **string values** differently:

### Identifiers (table/column names) are folded to lowercase

Unquoted identifiers are automatically lowercased. These are all equivalent:

```sql
SELECT * FROM Users;
SELECT * FROM USERS;
SELECT * FROM users;     -- all three hit the table actually named "users"
```

But **double quotes** make an identifier case-sensitive and exact. This only
matches a table physically named `Users` (capital U) — which we don't have, so
it errors:

```sql
SELECT * FROM "Users";   -- ERROR: relation "Users" does not exist
```

> Rule of thumb: **never quote table/column names** unless you deliberately
> created them with quotes. StoMo's tables are all lowercase
> (`users`, `watchlist`, `price_history`, `exchange_rate`).

### String values ARE case-sensitive

Comparisons of actual data are exact:

```sql
SELECT * FROM users WHERE email = 'Jane@Example.com';   -- exact match only
```

If `jane@example.com` is stored in lowercase, the query above finds nothing.
To match regardless of case, use `ILIKE` (case-insensitive `LIKE`) or
`LOWER(...)`:

```sql
SELECT * FROM users WHERE email ILIKE 'jane@example.com';
SELECT * FROM users WHERE LOWER(email) = LOWER('Jane@Example.com');
```

---

## 4. Common StoMo queries

### Inspect a user account (e.g. a failed login)

The `users` table maps from
[`User.java`](../src/main/java/com/dhbw/webeng2/stomo/model/entity/User.java).
Hibernate converts camelCase fields to snake_case columns
(`failedLoginAttempts` → `failed_login_attempts`).

```sql
SELECT id, email, status, failed_login_attempts, locked_until
FROM users
WHERE email ILIKE 'someone@example.com';
```

What blocks a login:

| Column                  | Login blocked when…                          |
|-------------------------|----------------------------------------------|
| `status`                | value is `LOCKED` (not `ACTIVE`)             |
| `locked_until`          | timestamp is in the **future**               |
| `failed_login_attempts` | too many consecutive bad password attempts   |

### Unlock an account

```sql
UPDATE users
SET status = 'ACTIVE', failed_login_attempts = 0, locked_until = NULL
WHERE email ILIKE 'someone@example.com';
```

### List all users

```sql
SELECT id, email, status, failed_login_attempts, locked_until FROM users;
```

> The `password` column is a **bcrypt hash** (see
> [`SecurityConfig.java`](../src/main/java/com/dhbw/webeng2/stomo/config/SecurityConfig.java)),
> so you cannot read or compare the plaintext password in SQL. You can only
> reset lock state here. If login still fails after unlocking, the cause is a
> wrong password or the email simply not existing.

---

## 5. Quick reference: SQL basics

```sql
-- read
SELECT col1, col2 FROM table WHERE condition ORDER BY col1 LIMIT 10;

-- change existing rows  (ALWAYS include a WHERE, or you update every row!)
UPDATE table SET col = value WHERE condition;

-- remove rows           (ALWAYS include a WHERE, or you delete every row!)
DELETE FROM table WHERE condition;

-- count
SELECT COUNT(*) FROM table WHERE condition;
```

Every SQL statement ends with a **semicolon** `;`. If your prompt changes from
`stomo_db=#` to `stomo_db-#`, psql is waiting for you to finish the statement —
you forgot the semicolon. Just type `;` and Enter.

---

## 6. One-off queries without the interactive shell

Run a single statement and exit (handy for scripts):

```bash
docker exec -it stomo-postgres psql -U stomo_user -d stomo_db -c "SELECT id, email, status FROM users;"
```

### Backup / restore the whole database

```bash
# dump to a file on your host
docker exec stomo-postgres pg_dump -U stomo_user stomo_db > stomo_backup.sql

# restore from that file
docker exec -i stomo-postgres psql -U stomo_user -d stomo_db < stomo_backup.sql
```

---

## 7. Safety notes

- **`UPDATE`/`DELETE` without a `WHERE` affect every row.** Double-check the
  `WHERE` clause before pressing Enter.
- Changes are committed automatically in `psql` (autocommit is on by default).
  To make a reversible change, wrap it in a transaction:

  ```sql
  BEGIN;
  UPDATE users SET status = 'ACTIVE' WHERE email ILIKE 'someone@example.com';
  SELECT id, email, status FROM users WHERE email ILIKE 'someone@example.com';  -- verify
  COMMIT;   -- or ROLLBACK; to undo
  ```
