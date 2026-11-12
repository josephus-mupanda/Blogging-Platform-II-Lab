# Review notes

Notes for presenting and defending this project. The [README](README.md) is the setup
guide; this file is the walkthrough, the demo script, and the answers to the questions a
reviewer is likely to ask.

---

## 1. What the project is, in one minute

A Spring Boot 3.5 backend on Java 21 that puts the Platform I blogging database behind two
APIs at the same time: a REST API and a GraphQL API. Both APIs call the same services, so
there is one copy of the business rules.

Two databases are used, exactly as designed in Platform I:

| Store | Holds | Why |
|---|---|---|
| PostgreSQL | users, posts, tags, post_tags | Structured, related data with foreign keys |
| MongoDB | comments, reviews | Free-form, high-volume discussion data |

The layering is `Controller -> Service -> Repository`. No controller ever touches a
repository, and every class gets its dependencies through its constructor.

---

## 2. Running it

### Prerequisites

- JDK 21
- PostgreSQL on port 5433 with the `pg_trgm` extension available
- MongoDB on port 27017

`psql` and `createdb` are not on PATH by default. They live in the PostgreSQL `bin`
folder, for example `C:\Program Files\PostgreSQL\18\bin`. Either call them with the full
path or add that folder to PATH.

### First time only

```powershell
createdb -h localhost -p 5433 -U postgres blogging_platform_ii
psql -h localhost -p 5433 -U postgres -d blogging_platform_ii -f db/schema.sql
```

`db/schema.sql` creates the four tables and the indexes. It uses `IF NOT EXISTS`
everywhere, so running it twice is safe and prints only "already exists, skipping"
notices. MongoDB needs no setup script: it creates the `comments` and `reviews`
collections the first time something is written, and Spring creates their indexes at
startup because of `spring.data.mongodb.auto-index-creation=true`.

### Start the app

```powershell
.\mvnw.cmd spring-boot:run
```

Use `./mvnw` on macOS or Linux. Startup takes about 15 seconds. Stop it with `Ctrl+C`.

### Open these

| URL | What it is |
|---|---|
| http://localhost:8080/swagger-ui/index.html | Swagger UI, the whole REST API |
| http://localhost:8080/v3/api-docs | The raw OpenAPI JSON, importable into Postman |
| http://localhost:8080/graphiql | GraphiQL, the browser console for the GraphQL API |

**Do not open `/graphql` in a browser.** It answers `405 Method Not Allowed`, and that is
correct behaviour, not a bug. `/graphql` only accepts `POST` with a JSON body, and a
browser address bar always sends `GET`. The page you want is `/graphiql` with the extra
**i**. Use `/graphql` from Postman, curl, or GraphiQL instead.

### Testing with Postman

[docs/postman_collection.json](postman_collection.json) is a ready collection: 40
requests in 9 folders covering REST CRUD, search, both MongoDB collections, GraphQL, the
validation failures, and cleanup. In Postman choose **Import**, pick that file, and run the
folders top to bottom. The create requests save their new IDs into collection variables, so
every later request finds its post, user, tag, and comment without any copying by hand.

### Start with the AOP timing logs visible

The aspect logs at DEBUG, which is off by default. Turn it on when you want to show Epic 5:

```powershell
.\mvnw.cmd spring-boot:run '-Dspring-boot.run.arguments=--logging.level.com.example.Blogging_Platform_II.config.ServiceMonitoring=DEBUG'
```

Then call any endpoint and watch the console:

```
DEBUG ServiceMonitoring : Entering TagService.list()
DEBUG ServiceMonitoring : Leaving TagService.list()
DEBUG ServiceMonitoring : TagService.list() success in 44.2331 ms
```

Those three lines are `@Before`, `@After`, and `@Around` in that order.

### Reset the demo data

Experimenting leaves rows behind, including comments whose posts were later deleted. Wipe
both stores before a demo so the dashboard numbers are clean and every comment points at a
post you can actually show. Stop the app first.

```powershell
psql -h localhost -p 5433 -U postgres -d blogging_platform_ii -c "TRUNCATE post_tags, posts, tags, users RESTART IDENTITY CASCADE;"
mongosh "mongodb://localhost:27017/blogging_platform_ii" --quiet --eval "db.comments.deleteMany({}); db.reviews.deleteMany({})"
```

`RESTART IDENTITY` resets the ID counters, so the first user and post you create are both
`1`. Tables and indexes survive, so there is no need to rerun `db/schema.sql`.

### Other useful commands

```powershell
.\mvnw.cmd clean compile        # rebuild from scratch
.\mvnw.cmd clean package        # build the jar into target/
```

If port 8080 is already busy, an old run is still alive. Find and stop it:

```powershell
Get-NetTCPConnection -State Listen -LocalPort 8080 | ForEach-Object { Stop-Process -Id $_.OwningProcess -Force }
```

---

## 3. Switching environments

Three profiles exist, which is Epic 1's requirement. `dev` is the default, set by
`spring.profiles.default=dev` in `application.properties`.

| Profile | PostgreSQL database | MongoDB database | GraphiQL | Settings come from |
|---|---|---|---|---|
| `dev` | `blogging_platform_ii` | `blogging_platform_ii` | On | Defaults, overridable |
| `test` | `blogging_platform_ii_test` | `blogging_platform_ii_test` | On | Defaults, overridable |
| `prod` | none, must be supplied | none, must be supplied | Off | Environment only |

### How to switch

```powershell
$env:SPRING_PROFILES_ACTIVE = 'test'
.\mvnw.cmd spring-boot:run
```

Check the console on startup. Spring prints which profile it picked:

```
The following 1 profile is active: "test"
```

With nothing set you see the default instead, which is how you prove `dev` is the fallback:

```
No active profile set, falling back to 1 default profile: "dev"
```

Clear the variable again with `Remove-Item Env:SPRING_PROFILES_ACTIVE`.

Remember the `test` profile points at `blogging_platform_ii_test`, so that database needs
`db/schema.sql` applied to it too before the app will work there.

### How the settings are written

Each profile file uses `${VARIABLE:default}`, which means "use the environment variable if
it is set, otherwise use this default". From `application-dev.properties`:

```properties
spring.datasource.url=${DB_POSTGRES_URL:jdbc:postgresql://localhost:5433/blogging_platform_ii}
```

So `dev` and `test` just work on a normal machine, but can still be pointed somewhere else
without editing any file.

| Profile | Variables it reads |
|---|---|
| `dev`, `prod` | `DB_POSTGRES_URL`, `DB_POSTGRES_USER`, `DB_POSTGRES_PASSWORD`, `DB_MONGO_URI` |
| `test` | `TEST_POSTGRES_URL`, `TEST_POSTGRES_USER`, `TEST_POSTGRES_PASSWORD`, `TEST_MONGO_URI` |

`prod` has no defaults on purpose. If a variable is missing the app refuses to start,
which is better than silently connecting to a developer's local database. It also turns
GraphiQL off, because a browser query console should not be exposed in production.

Running against a different database entirely:

```powershell
$env:SPRING_PROFILES_ACTIVE = 'prod'
$env:DB_POSTGRES_URL = 'jdbc:postgresql://localhost:5433/blogging_platform_ii'
$env:DB_POSTGRES_USER = 'postgres'
$env:DB_POSTGRES_PASSWORD = 'postgres'
$env:DB_MONGO_URI = 'mongodb://localhost:27017/blogging_platform_ii'
.\mvnw.cmd spring-boot:run
```

Remember the MongoDB URI must include the database name at the end, otherwise Spring does
not know which database to use.

---

## 4. Relationships between the entities

Yes, the relationships are all there, and they are enforced by the database itself, not by
Java code. Every one of them is declared in [db/schema.sql](db/schema.sql).

```
                users
                  |
                  | 1 user writes many posts
                  | posts.author_id -> users.user_id   ON DELETE CASCADE
                  v
                posts <------------- post_tags -------------> tags
                  |     many posts        ^        many tags
                  |     have many tags    |
                  |                 PRIMARY KEY (post_id, tag_id)
                  |
                  | 1 post has many comments and reviews  (MongoDB, by postId)
                  v
           comments / reviews
```

| Relationship | Type | How it is enforced |
|---|---|---|
| User writes Posts | one-to-many | `posts.author_id REFERENCES users (user_id) ON DELETE CASCADE` |
| Post has Tags | many-to-many | `post_tags` junction table, two foreign keys, composite primary key |
| Post has Comments | one-to-many | `comments.postId` in MongoDB, checked by `DiscussionService` |
| Post has Reviews | one-to-many | `reviews.postId` in MongoDB, plus a unique index on `(postId, reviewerId)` |
| User has a Role | attribute, not a table | `CHECK (role IN ('ADMIN', 'AUTHOR', 'READER'))` on `users.role` |

### Things to be able to say about each

**The junction table is the interesting one.** A post can have many tags and a tag can
belong to many posts, and SQL cannot express that with a single column. `post_tags` holds
only the two IDs, and its primary key is the pair, which means the same tag cannot be
attached to the same post twice. `PostRepository.replaceTags` deletes the existing rows for
a post and inserts the new ones, so editing a post's tags is one simple operation.

**`ON DELETE CASCADE` is declared but almost never fires**, because posts and users are
soft-deleted rather than removed. It is the safety net for a real hard delete.

**Roles are a check constraint, not a roles table.** With exactly three fixed values that
never change and no extra attributes, a separate `roles` table would add a join to every
query and buy nothing. If roles later needed a description or permissions, that is the
point at which to split it into its own table. This is a normalisation judgement worth
stating as a choice rather than an oversight.

### The honest gap, worth volunteering

`users.role` and `users.email` are stored, constrained, and validated, but **no API
endpoint ever returns them**. The `User` model exposes only `id`, `username`, and
`displayName`. Every user created through the API therefore gets the default role
`READER`, and nothing can change it.

That is a deliberate consequence of the Platform II brief, which asks for CRUD on users,
posts, comments and tags and never mentions roles or authorisation. Spring Security was
removed from this phase for the same reason. Say it that way: the column is the Platform I
design being preserved, waiting for the phase that adds authentication.

If a reviewer pushes on it, the honest answer is that exposing `role` would be a small
change — add the field to `User`, the row mapper, and the create DTO — but it would be
unused without authorisation to enforce it.

---

## 5. How PostgreSQL and MongoDB share data

This is the most likely question in the review, so it is worth being able to draw it.

### The split

```
PostgreSQL                                 MongoDB
----------                                 -------
users (user_id, username, email, ...)      comments  { postId, authorId, authorName, body, ... }
posts (post_id, author_id, title, ...)     reviews   { postId, reviewerId, reviewerName, rating, ... }
tags  (tag_id, name)
post_tags (post_id, tag_id)
```

### The join key

A MongoDB document is linked to PostgreSQL by plain number fields:

- `comments.postId` and `reviews.postId` hold the value of `posts.post_id`
- `comments.authorId` and `reviews.reviewerId` hold the value of `users.user_id`

There is **no database-level foreign key** between the two, because they are two separate
database engines. Nothing in PostgreSQL knows MongoDB exists.

### So what keeps them consistent? The service layer.

Two services touch both stores: `DiscussionService` for writing and reading discussion,
and `AnalyticsService` for the dashboard. Everywhere a comment or review is written,
PostgreSQL is checked first. From `DiscussionService.addComment`:

```java
User author = users.find(authorId).orElseThrow(() -> BlogException.notFound("Author"));  // PostgreSQL
requireVisible(postId);                                                                  // PostgreSQL
return discussion.insert(new Comment(null, postId, author.getId(), author.getDisplayName(),
        body, Instant.now(), null));                                                     // MongoDB
```

So a comment can never be written for a post or an author that does not exist in
PostgreSQL. Try it in the demo: post a comment on post `999999` and you get a 404, with
nothing written to MongoDB.

### Names are copied on purpose

Notice `author.getDisplayName()` is copied into the comment document as `authorName`. That
is deliberate denormalisation. Reading a page of comments needs no lookup back into
PostgreSQL, which is the whole point of putting them in MongoDB. The trade-off is that if
a user later renames themselves, old comments keep the old name. Say this out loud before
the reviewer finds it, and say why: comments are a historical record of who said what at
the time.

### The dashboard reads from both

`/api/dashboard` is the clearest example of the two stores cooperating. `AnalyticsService`:

```java
List<Long> archived = posts.archivedIds();                      // PostgreSQL: deleted post IDs
return new Dashboard(users.count(), posts.countVisible(), tags.countVisible(),
        discussion.countComments(archived),                     // MongoDB, excluding those IDs
        discussion.countReviews(archived));
```

PostgreSQL produces the list of deleted post IDs, and that list is handed to MongoDB as a
`nin` ("not in") filter so comments on deleted posts are not counted. That is how a
"join" across two databases is done when the databases cannot join: do it in the
application.

### Deleting is soft, in both stores

| Thing deleted | What actually happens |
|---|---|
| Post | `posts.deleted_at` is set. Its comments and reviews stay in MongoDB but are filtered out. |
| Comment | `comments.deletedAt` is set in the document. |
| Tag | `tags.deleted_at` is set. |
| User | `users.active` becomes false. Their posts and comments remain. |

Nothing is physically removed, so content written by a deleted user is still readable and
the history stays intact.

### Honest limitation to mention first

`@Transactional` on the service methods covers the PostgreSQL work only. MongoDB is not
part of that transaction, so there is no single rollback across both databases. In theory
a MongoDB write could fail after the PostgreSQL checks passed. For a lab of this size that
is acceptable; the real fix is a distributed transaction or an outbox pattern. Saying this
before being asked is much stronger than being caught by it.

### Seeing it live, side by side

With the app running and at least one comment created, show both stores:

```powershell
# PostgreSQL: the post
psql -h localhost -p 5433 -U postgres -d blogging_platform_ii -c "SELECT post_id, title FROM posts WHERE deleted_at IS NULL ORDER BY post_id;"

# MongoDB: its comments, linked by postId
mongosh "mongodb://localhost:27017/blogging_platform_ii" --quiet --eval "db.comments.find({}, {postId:1, authorName:1, body:1}).pretty()"
```

Point at the `post_id` in one output and the matching `postId` in the other. That is the
whole relationship.

Useful MongoDB commands:

```powershell
mongosh "mongodb://localhost:27017/blogging_platform_ii" --quiet --eval "db.getCollectionNames()"
mongosh "mongodb://localhost:27017/blogging_platform_ii" --quiet --eval "db.reviews.getIndexes()"
```

`db.reviews.getIndexes()` shows `postId_1_reviewerId_1` marked `unique`. That unique index
is what makes a second review by the same user return HTTP 409, and it comes from the
`@CompoundIndex` annotation on the `Review` class.

---

## 6. Searching, sorting, pagination and indexing

This is the DSA part of the brief, and all of it is already implemented. It lives almost
entirely in two methods: `PostService.search` validates the request, and
`PostRepository.search` builds and runs the query.

### Searching

The filter is built up a piece at a time, and only for the parameters that were actually
supplied:

```java
where.append(" AND (p.title ILIKE :keyword OR p.body ILIKE :keyword)");
```

Three things to point out here:

- **`ILIKE` is PostgreSQL's case-insensitive `LIKE`**, so searching `spring`, `Spring` or
  `SPRING` gives the same result. That is the "case-insensitive search" requirement.
- **The value is a named parameter, never string concatenation.** `JdbcClient` sends
  `:keyword` separately from the SQL, so a keyword like `'; DROP TABLE posts; --` is
  searched for as text. That is the parameterised-query requirement, and it is worth
  demonstrating by actually typing it into the search box.
- **The LIKE wildcards are escaped** before the value is sent, so a user searching for
  `100%` gets posts containing "100%" and not every post in the database.

Without an index, `ILIKE '%spring%'` forces PostgreSQL to read every row. The two trigram
GIN indexes on `title` and `body` are what make it fast, and section 10 has the command
that proves it: 6.4 ms with the indexes, 1067 ms without.

**Proving both of those live.** These three searches are a strong thirty seconds of demo:

```powershell
# 1. An injection attempt is just text. Returns 0 matches, and the table is still there.
Invoke-RestMethod "http://localhost:8080/api/posts?keyword=';%20DROP%20TABLE%20posts;%20--"

# 2. A percent sign matches only posts that literally contain "%", not everything.
Invoke-RestMethod "http://localhost:8080/api/posts?keyword=%25"

# 3. Case does not matter: these two return the same posts.
Invoke-RestMethod "http://localhost:8080/api/posts?keyword=spring"
Invoke-RestMethod "http://localhost:8080/api/posts?keyword=SPRING"
```

Verified: the injection attempt returns `status: success` with `totalElements: 0`, and a
follow-up listing shows every post still present. The bare `%` returns 0 rather than the
whole table, which is the escaping doing its job.

### Sorting

The sort option is an **enum**, not a string from the user:

```java
public enum Sort { NEWEST, OLDEST, TITLE_ASC, TITLE_DESC }
```

which maps to a fixed `ORDER BY` clause:

```java
case NEWEST    -> "p.created_at DESC, p.post_id DESC";
case OLDEST    -> "p.created_at, p.post_id";
case TITLE_ASC -> "lower(btrim(p.title)), p.post_id";
case TITLE_DESC-> "lower(btrim(p.title)) DESC, p.post_id DESC";
```

Two deliberate details:

- **The sort column can never come from the request.** Spring converts the query parameter
  into one of four enum values or rejects it, so no user input ever reaches the `ORDER BY`
  clause. That is the one place in SQL where parameters cannot help you, so a whitelist is
  the correct defence.
- **Every sort ends with `post_id`.** If two posts share a timestamp or a title, the ID
  breaks the tie, so the order is total and stable. Without it, page 2 could repeat or skip
  a row that page 1 already showed.

`TITLE_ASC` sorts on `lower(btrim(title))` so that `" apple"` and `"Apple"` sort together
rather than by their raw bytes.

### Pagination

```java
"... ORDER BY " + order + " LIMIT :limit OFFSET :offset"
```

with `offset = page * size`. A separate `count(*)` over the same filter gives
`totalElements`, and `totalPages` is `(total + size - 1) / size`, which is integer ceiling
division. `PostService.search` rejects a negative page or a size outside 1 to 100, so one
request can never ask for the whole table.

The known limit, worth volunteering: `OFFSET` makes PostgreSQL walk past the skipped rows,
so page 1000 is slower than page 1. Keyset pagination solves it at the cost of losing
random page access, which is not a trade worth making for a blog.

### Avoiding the N+1 problem

A page of 10 posts needs each post's tags. The naive version runs 1 query for the posts and
10 more for the tags. `PostRepository.withTags` instead runs **one** query for all the tags
on the page and groups them in a `HashMap` keyed by post ID:

```java
tagsByPost.computeIfAbsent(row.getLong("post_id"), key -> new ArrayList<>())
```

So a page always costs exactly 3 queries, whatever the page size: the count, the posts, and
the tags. That hash-map grouping is the "hashing for fast lookup" item from the brief.

### Indexing on both databases

PostgreSQL, from `db/schema.sql`:

| Index | What it speeds up |
|---|---|
| `idx_posts_title_trgm`, `idx_posts_body_trgm` | the `ILIKE` keyword search |
| `idx_posts_visible_created` | the newest-first page, with `WHERE deleted_at IS NULL` |
| `idx_posts_author_id` | filtering by author |
| `idx_post_tags_tag_id` | filtering by tag |
| `uq_posts_normalized_title` | stops two posts having the same title ignoring case and spaces |

MongoDB does the same job through the `@CompoundIndex` annotations you spotted on the
`Comment` and `Review` classes:

```java
@CompoundIndex(name = "postId_1_createdAt_1", def = "{'postId': 1, 'createdAt': 1}")
@CompoundIndex(name = "postId_1_reviewerId_1", def = "{'postId': 1, 'reviewerId': 1}", unique = true)
```

A **compound index** is simply an index on more than one field, in a defined order. The `1`
means ascending, `-1` would mean descending.

- On `comments`, `(postId, createdAt)` matches exactly how comments are read: find every
  comment for one post, oldest first. One index serves both the filtering and the sorting,
  so MongoDB never has to sort in memory.
- On `reviews`, `(postId, reviewerId)` is marked `unique`, which is what physically stops
  the same user reviewing the same post twice. The duplicate insert fails in the database,
  and `DiscussionService` turns that failure into HTTP 409.

Spring creates both at startup because of `spring.data.mongodb.auto-index-creation=true`.
Prove they exist with `db.reviews.getIndexes()` — the `unique: true` is visible there.

Field order matters: an index on `(postId, createdAt)` helps a query that filters on
`postId` alone, but an index on `(createdAt, postId)` would not. That is the one thing to
be ready to explain if asked why the fields are in that order.

---

## 7. Where each requirement lives

Use this to jump straight to a file when asked.

| Epic | Requirement | Where |
|---|---|---|
| 1 | Profiles for dev, test, prod | `src/main/resources/application-{dev,test,prod}.properties` |
| 1 | Constructor injection everywhere | Any class, for example `PostController`. There is no `@Autowired` in the project. |
| 2.1 | CRUD over REST | `controller/rest/` — user, post, tag, discussion controllers |
| 2.1 | `status` / `message` / `data` responses | `controller/rest/ApiResponse.java` |
| 2.1 | Layered architecture | `controller/` -> `service/` -> `repository/` |
| 2.2 | Pagination, sorting, filtering | `PostService.search` validates, `PostRepository.search` queries |
| 2.2 | Efficient searching | Trigram GIN indexes in `db/schema.sql`, used by `ILIKE` |
| 2.2 | Performance documented | `docs/performance-report.md` |
| 3 | Bean Validation | `dto/` — `@NotBlank`, `@Size`, `@Email`, `@Min`, `@Max`, `@Positive` |
| 3 | Custom validator | `validation/UniqueAccount.java` and `UniqueAccountValidator.java` |
| 3 | Central exception handling | `controller/rest/ApiErrors.java`, `controller/graphql/GraphQlErrors.java` |
| 3 | OpenAPI / Swagger | `config/OpenApiConfig.java`, served at `/swagger-ui/index.html` |
| 4 | GraphQL schema | `src/main/resources/graphql/blog.graphqls` — all five entities |
| 4 | Queries and mutations | `controller/graphql/` — 10 queries, 13 mutations |
| 4 | REST and GraphQL coexist | Both call the same services |
| 5 | AOP with all three advice types | `config/ServiceMonitoring.java` |
| 5 | Performance measured in the aspect | The `@Around` method times every service call |
| DSA | Efficient searching | `ILIKE` plus the two trigram GIN indexes — section 6 |
| DSA | Sorting | The `Sort` enum mapped to a whitelisted `ORDER BY`, with `post_id` as the tie-break |
| DSA | Pagination | `LIMIT`/`OFFSET` plus a `count(*)`, bounded to 1-100 by `PostService.search` |
| DSA | Hashing for fast lookup | `HashMap` grouping of tags per page in `PostRepository.withTags` |
| DSA | Indexing | 14 PostgreSQL indexes in `db/schema.sql`, two `@CompoundIndex` in MongoDB |

Relationships between the entities are in section 4, and the searching, sorting and
pagination details are in section 6.

---

## 8. "Where is it called from?" — how each feature is wired

This is the question that catches people out. The honest answer for most of these is
**nothing in this project calls them**. They are not utilities you invoke; they are
declarations that Spring finds at startup and applies for you. That is the whole point of
the framework, and it is the right answer to give.

The useful follow-up is "then how do you know it works?" — so each row below has a way to
prove it live.

| Feature | Who calls it | What makes Spring find it | How to prove it |
|---|---|---|---|
| REST exception handling | Nobody. Spring calls it. | `@RestControllerAdvice` on `ApiErrors` | `GET /api/posts/999999` returns a clean 404 envelope instead of a stack trace |
| GraphQL exception handling | Nobody. Spring calls it. | `@ControllerAdvice(annotations = Controller.class)` + `@GraphQlExceptionHandler` on `GraphQlErrors` | Query `post(id: 999999)`, look at `errors[0].extensions.code` |
| AOP logging and timing | Nobody. Spring proxies the services. | `@Aspect` + `@Component` on `ServiceMonitoring`, matched by its pointcut | Start with the DEBUG argument, call any endpoint, watch the three log lines |
| Profiles | Nobody. Spring loads them. | `spring.profiles.default=dev`, plus `SPRING_PROFILES_ACTIVE` | The profile line in the startup log |
| Bean Validation | **You do** — `@Valid` is the trigger | `@Valid` on controller parameters, 18 of them | `POST /api/users` with a blank username |
| Custom validator | Bean Validation calls it | `@UniqueAccount` on the `UserCreate` class, pointing at `UniqueAccountValidator` | Create the same username twice |
| Swagger / OpenAPI | Nobody. Springdoc scans at runtime. | The springdoc dependency, plus `OpenApiConfig` for the title | Open `/swagger-ui/index.html` |
| GraphQL schema | Nobody. Spring loads the file. | Any `.graphqls` under `src/main/resources/graphql/` | Open `/graphiql` and look at the Docs panel |
| GraphQL resolvers | Nobody. Spring maps them by name. | `@QueryMapping` / `@MutationMapping`, method name matches the schema field | Run any query |
| GraphQL `Long` scalar | Nobody. Spring wires it in. | The `RuntimeWiringConfigurer` bean in `GraphQlConfig` | `totalElements` in a posts query returns a number, not an error |
| Transactions | Nobody. Spring proxies the services. | `@Transactional` on `PostService` and `DiscussionService` methods | Create a post with a tag id that does not exist: no half-written post is left behind |
| Dependency injection | Nobody. Spring injects. | A single constructor on each `@Service`, `@Repository`, `@RestController` | There is no `@Autowired` anywhere in the project |

### The three that are worth explaining properly

**Centralized exception handling.** `ApiErrors` is never imported or called by the
controllers. It is annotated `@RestControllerAdvice`, so at startup Spring registers it
globally. When any controller throws, Spring looks through the registered advice classes
for an `@ExceptionHandler` whose parameter type matches the thrown exception, and calls
that method instead of letting the exception escape. So the chain is:

```
PostService.get(999999)
  -> throws BlogException (NOT_FOUND)
  -> PostController does not catch it
  -> Spring catches it and looks for a handler
  -> ApiErrors.businessError(BlogException) matches
  -> returns 404 with the status / message / data envelope
```

The benefit to state out loud: no controller contains a single `try/catch`, and the error
format is defined in exactly one place. `GraphQlErrors` does the same job for GraphQL,
which needs a different shape because GraphQL always replies 200 and puts problems in an
`errors` array.

**AOP.** `ServiceMonitoring` is also never called. It is a `@Component`, so it becomes a
bean, and `@Aspect` tells Spring it contains advice. The pointcut is one string:

```java
"execution(public * com.example.Blogging_Platform_II.service.*Service.*(..))"
```

Read it left to right: any **public** method, returning **any** type, on any class in the
`service` package whose name ends in **Service**, with **any** arguments. At startup
Spring wraps every matching bean in a proxy. When a controller calls
`postService.search(...)` it is really calling the proxy, which runs `@Before`, then the
real method, then `@After`, with `@Around` wrapped around the whole thing to time it. Add
a new service tomorrow and it gets logging and timing for free, with no code change.

**Profiles.** `application.properties` is always loaded. On top of it Spring loads
`application-{profile}.properties` for whichever profile is active, and those values win.
Nothing in the Java code reads a profile or branches on one, which is the point: the same
code runs in every environment and only the configuration differs.

---

## 9. Demo script

A run-through that touches every epic in order. Create things as you go so the numbers are
real. Swagger UI is the easiest place to do this; the `curl` equivalents are below each
step.

**1. Show the layering.** Open `PostController`, then `PostService`, then `PostRepository`.
Point at the constructors: no `@Autowired` anywhere.

**2. Create a user.** In Swagger, `POST /api/users`:

```json
{ "username": "demo_author", "email": "demo@example.test", "displayName": "Demo Author" }
```

Note the returned `id` and the `status` / `message` / `data` envelope.

**3. Show the custom validator.** Send the same request again. HTTP 400, and the message
is "This username is already taken.", produced by `UniqueAccountValidator` which queries
the database. Also try `"email": "not-an-email"` to show plain Bean Validation.

**4. Create a tag.** `POST /api/tags` with `{ "name": "  SPRING  " }`. It comes back as
`spring` — the setter trims and lowercases it.

**5. Create a post** with `authorId` from step 2 and `tagIds` from step 4.

**6. Search.** This covers Epic 2.2 in one request:

```
GET /api/posts?keyword=spring&page=0&size=5&sort=TITLE_ASC
```

Change `sort` to `NEWEST`, `OLDEST`, `TITLE_DESC`. Add `authorId` or `tagId`. Then show
`size=500` returning 400 with a clear message.

**7. Add a comment**, which crosses into MongoDB:

```
POST /api/posts/{postId}/comments?authorId={userId}
{ "body": "Great article." }
```

Then immediately run the two side-by-side queries from section 5 to show the same post
living in PostgreSQL while its comment lives in MongoDB.

**8. Add a review**, then add it again with the same `reviewerId`. The second one returns
409, enforced by the unique MongoDB index.

**9. Show the dashboard.** `GET /api/dashboard` counts across both databases in one call.

**10. Switch to GraphQL.** Open GraphiQL and run a query from
[docs/examples.graphql](docs/examples.graphql):

```graphql
query {
  posts(filter: { keyword: "spring", size: 5 }) {
    totalElements
    items { id title tags { name } }
  }
}
```

Then delete a field from the selection and re-run to show GraphQL returning only what was
asked for. That is the point of having it alongside REST.

**11. Show a GraphQL error.** Query `post(id: 999999)`. The response has `errors` with
`extensions.code` of `NOT_FOUND` — the same `BlogException` that REST turns into a 404,
formatted for GraphQL by `GraphQlErrors`.

**12. Show the AOP logs.** Switch to the console and point at the three lines per service
call. Explain the pointcut in `ServiceMonitoring`: it matches every public method of every
`*Service` class, so new services get logging for free.

**13. Finish on performance.** Run the measurements live — see the next section.

---

## 10. Measuring performance live

The dev database has a handful of rows, which is far too few to measure anything. The
benchmark database holds 20,000 posts, 200 of which contain the word `springneedle`. Use
that one.

### Set it up (once)

```powershell
createdb -h localhost -p 5433 -U postgres blogging_platform_ii_bench
psql -h localhost -p 5433 -U postgres -d blogging_platform_ii_bench -f db/schema.sql
psql -h localhost -p 5433 -U postgres -d blogging_platform_ii_bench -f db/benchmark.sql
```

`db/benchmark.sql` seeds the 20,000 posts and then prints the query plans. It is safe to
rerun; the seed skips rows that already exist.

### Part 1 — the database: indexed versus not indexed

This is the strongest number in the whole project, and it is one command:

```powershell
psql -h localhost -p 5433 -U postgres -d blogging_platform_ii_bench -f db/benchmark.sql
```

Scroll to the three `Execution Time` lines at the bottom. A real run on this machine:

| What the plan did | Execution time |
|---|---:|
| Newest 10 matches, using `idx_posts_visible_created` | 14.2 ms |
| Count using both trigram indexes (`Bitmap Index Scan`) | **6.4 ms** |
| The same count forced to `Seq Scan` | **1067.6 ms** |

The last two are the same query over the same rows. The only difference is whether
PostgreSQL is allowed to use the indexes. That is roughly **160 times faster**, and it is
the clearest possible answer to "what did indexing buy you?".

Point at the plan text itself while explaining: `Bitmap Index Scan on idx_posts_title_trgm`
and `idx_posts_body_trgm` combined by a `BitmapOr`, versus `Seq Scan on posts`.

### Part 2 — the APIs: REST versus GraphQL

Start the app against the benchmark database in one terminal:

```powershell
$env:DB_POSTGRES_URL = 'jdbc:postgresql://localhost:5433/blogging_platform_ii_bench'
$env:DB_MONGO_URI = 'mongodb://localhost:27017/blogging_platform_ii_bench'
.\mvnw.cmd spring-boot:run
```

**The quick version**, if you only have a moment. Average over 50 calls, plus the response
size:

```powershell
$rest = 'http://localhost:8080/api/posts?keyword=springneedle&page=0&size=10'
$gql = @{ query = '{ posts(filter: {keyword: "springneedle", page: 0, size: 10}) { page size totalElements totalPages items { id title } } }' } | ConvertTo-Json -Compress
1..10 | ForEach-Object { Invoke-RestMethod $rest | Out-Null }
"REST    avg {0:N1} ms" -f ((Measure-Command { 1..50 | ForEach-Object { Invoke-RestMethod $rest | Out-Null } }).TotalMilliseconds / 50)
"GraphQL avg {0:N1} ms" -f ((Measure-Command { 1..50 | ForEach-Object { Invoke-RestMethod 'http://localhost:8080/graphql' -Method Post -ContentType 'application/json' -Body $gql | Out-Null } }).TotalMilliseconds / 50)
"REST    payload {0} bytes" -f (Invoke-WebRequest $rest -UseBasicParsing).RawContentLength
"GraphQL payload {0} bytes" -f (Invoke-WebRequest 'http://localhost:8080/graphql' -Method Post -ContentType 'application/json' -Body $gql -UseBasicParsing).RawContentLength
```

**The full version**, which produces the same kind of table as the performance report:
median, 95th percentile, and payload size for all three variants. Paste the whole block.

```powershell
$rest = 'http://localhost:8080/api/posts?keyword=springneedle&page=0&size=10'
$gqlUrl = 'http://localhost:8080/graphql'
$compact = @{ query = '{ posts(filter: {keyword: "springneedle", page: 0, size: 10}) { page size totalElements totalPages items { id title } } }' } | ConvertTo-Json -Compress
$full = @{ query = '{ posts(filter: {keyword: "springneedle", page: 0, size: 10}) { page size totalElements totalPages items { id title authorId body createdAt updatedAt tags { id name } } } }' } | ConvertTo-Json -Compress
$calls = [ordered]@{
  'REST'            = { Invoke-WebRequest $rest -UseBasicParsing }
  'GraphQL compact' = { Invoke-WebRequest $gqlUrl -Method Post -ContentType 'application/json' -Body $compact -UseBasicParsing }
  'GraphQL full'    = { Invoke-WebRequest $gqlUrl -Method Post -ContentType 'application/json' -Body $full -UseBasicParsing }
}
$calls.GetEnumerator() | ForEach-Object {
  $b = $_.Value
  1..20 | ForEach-Object { & $b | Out-Null }
  $t = (1..100 | ForEach-Object { (Measure-Command { & $b | Out-Null }).TotalMilliseconds }) | Sort-Object
  [pscustomobject]@{ API = $_.Key; 'p50 ms' = [math]::Round($t[49],1); 'p95 ms' = [math]::Round($t[94],1); 'bytes' = (& $b).RawContentLength }
} | Format-Table -AutoSize
```

The first 20 calls are warm-up and are thrown away, because the JVM and the PostgreSQL
cache are both cold at first and would skew the result. The next 100 are measured, sorted,
and the middle one is the median.

A real run on this machine:

```
API             p50 ms p95 ms bytes
---             ------ ------ -----
REST              76.4  686.8  9781
GraphQL compact   44.9   74.6   545
GraphQL full      66.8  121.5  9787
```

**The byte column is the reliable part.** It was identical in every run, and it matches the
performance report exactly, because it depends only on which fields each variant selects.

**The millisecond columns move a lot.** Across three runs of this same block, the REST
median ranged from 51 to 83 ms and its p95 from 106 to 687 ms, on a machine that was also
running Maven and two database servers. That p95 of 687 is one slow call out of a hundred,
not a real property of the API. If a reviewer asks about a spike, that is the answer: a
single sample on a busy laptop, which is exactly why the median is the number to quote and
why a serious benchmark needs a quiet machine.

### What to say about the result

The honest reading, and the one to give:

- **Payload size is where GraphQL wins, clearly.** 545 bytes against 9,781 for the same ten
  posts — **94.4% smaller** — because the query asked only for `id` and `title`. On a
  mobile connection that is the difference that matters.
- **Latency is roughly a wash.** Both APIs call the same `PostService` and run the same SQL,
  so the database work is identical. GraphQL full is slower because it asks for the tags of
  every post as well.
- **The exact milliseconds move between runs**, depending on what else the machine is doing.
  Run it twice and the numbers shift. What is stable is the shape: the payload ratio and
  the index-versus-sequential-scan gap. Lead with those, not with a single millisecond
  figure.

The numbers recorded in [docs/performance-report.md](docs/performance-report.md) came from
an earlier run on the same machine. If a live run gives different milliseconds, that is
expected and worth saying before being asked.

---

## 11. Questions to expect

**Why two databases?** Platform I designed it that way. Users, posts and tags are
structured and related, so they belong in PostgreSQL with foreign keys. Comments and
reviews are free-form, write-heavy, and never joined against anything except their post,
so a document store fits them better. Section 4 explains how they are linked.

**How do you join across them?** You cannot, so the application does it. The clearest
example is `/api/dashboard`: PostgreSQL returns the deleted post IDs and that list becomes
a `nin` filter on the MongoDB count.

**What makes the search efficient?** Three things. The `pg_trgm` GIN indexes on
`title` and `body` let PostgreSQL use an index for a case-insensitive "contains" search
instead of scanning every row. A partial index on `(created_at DESC, post_id DESC) WHERE
deleted_at IS NULL` lets it stop early when returning the newest page. And tags for a whole
page load in one extra query, grouped by post ID with a `HashMap`, instead of one query per
post.

**Why does sorting always include the ID?** `ORDER BY created_at DESC, post_id DESC`. If
two posts share a timestamp, the ID breaks the tie, so the order is stable and page 2 never
repeats or skips a row from page 1.

**Where is the DSA content?** Indexed search (trigram and B-tree), `LIMIT`/`OFFSET`
pagination with a stable sort key, and hash-map grouping of tags per page in
`PostRepository.withTags`.

**Why does `@Around` exist when `@Before` and `@After` already log?** `@Before` and
`@After` can only observe. `@Around` wraps the call, so it can take a timestamp on both
sides of `proceed()` and report the elapsed time. That is the monitoring part of Epic 5.

**Why is nothing really deleted?** Soft deletes. See the table in section 5. Content
written by a deleted user stays readable, and deleted titles stay reserved so a new post
cannot silently take an old one's place.

**Why are there no unit tests?** The brief lists testing via Postman, Swagger UI, or the
GraphQL playground, and does not ask for an automated suite. The Postman collection and the
demo in section 9 are the testing evidence.

---

## 12. If something goes wrong

| Symptom | Cause and fix |
|---|---|
| `FATAL: database "blogging_platform_ii" does not exist` | The first-time setup in section 2 was not run. |
| `relation "posts" does not exist` | The database exists but `db/schema.sql` was not applied to it. |
| `Connection refused` on port 5433 | PostgreSQL is not running, or is on 5432. Check with `Get-NetTCPConnection -State Listen -LocalPort 5433`. |
| Startup hangs then fails on MongoDB | MongoDB is not running on 27017. The URI has a 3 second timeout, so it fails rather than hanging forever. |
| `Port 8080 was already in use` | An earlier run is still alive. Use the stop command in section 2. |
| GraphiQL returns 404 | The `prod` profile is active, which disables it. Clear `SPRING_PROFILES_ACTIVE`. |
| `psql` or `createdb` not recognised | Not on PATH. Use the full path from section 2. |
| No `ServiceMonitoring` lines in the console | The app was started without the DEBUG argument from section 2. |
