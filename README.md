
# Blogging Platform II

A Java 21 and Spring Boot 3.5 backend that exposes the Blogging Platform I data and workflows over REST and GraphQL. PostgreSQL stores accounts, posts, and tags. MongoDB stores comments and reviews.

## Run locally

Install JDK 21, PostgreSQL with `pg_trgm`, and MongoDB. The project includes a Maven wrapper. From the repository root, create the development database and start the app.

```powershell
createdb -h localhost -p 5433 -U postgres blogging_platform
.\mvnw.cmd spring-boot:run
```

Use `./mvnw` on macOS or Linux. For an existing Platform I database, skip `createdb`. Flyway adopts its PostgreSQL schema and adds any missing columns and indexes. Use the same MongoDB database name to read its existing discussions. If a very old PostgreSQL database has duplicate titles that differ only by case or surrounding spaces, resolve those duplicates before starting so the unique index can be created.

For a fresh local demo, run `db/dev-admin.sql` after the app starts.

```powershell
psql -h localhost -p 5433 -U postgres -d blogging_platform -v ON_ERROR_STOP=1 -f db/dev-admin.sql
```

The demo account is `admin` with password `111111`. Existing Platform I accounts can sign in with their current passwords. New accounts use PBKDF2 and sign in through Platform II. For a fresh production database, register an account with a strong password and have a database administrator set its `users.role` to `ADMIN`.

## Environment profiles

| Profile | PostgreSQL | MongoDB | GraphiQL |
|---|---|---|---|
| `dev` (default) | `jdbc:postgresql://localhost:5433/blogging_platform`, user and password `postgres` | `mongodb://localhost:27017/blogging_platform` | Enabled |
| `test` | `jdbc:postgresql://localhost:5433/blogging_platform_ii_test`, user and password `postgres` | `mongodb://localhost:27017/blogging_platform_ii_test` | Enabled |
| `prod` | Explicit connection settings | Explicit connection settings | Disabled |

Select a profile with `SPRING_PROFILES_ACTIVE`. Set `DB_POSTGRES_URL`, `DB_POSTGRES_USER`, `DB_POSTGRES_PASSWORD`, and `DB_MONGO_URI` for dev or prod. Use the `TEST_` prefix instead of `DB_` for the test profile. Include the database name in the MongoDB URI. PostgreSQL requires a role that can apply the migration, and MongoDB requires permission to create indexes at startup. Serve the production API over HTTPS for HTTP Basic authentication.

## REST

Open Swagger UI at [localhost:8080/swagger-ui/index.html](http://localhost:8080/swagger-ui/index.html) or import [the OpenAPI JSON](http://localhost:8080/v3/api-docs) into Postman. Use **Authorize** for operations that require an account. The REST response envelope has `status`, `message`, and `data`. Creation returns HTTP 201. Validation errors include field messages when available.

| Routes | Operations |
|---|---|
| `/api/users`, `/api/users/{id}`, `/api/users/me` | Register, list as admin, read or update an account, deactivate as admin |
| `PATCH /api/users/{id}/status` | Set `{"active": true}` or `{"active": false}` as admin |
| `/api/posts`, `/api/posts/{id}` | Browse, read, create, edit, and archive posts |
| `/api/tags`, `/api/tags/{id}` | Browse and read tags, or create, rename, and archive them as admin |
| `/api/posts/{postId}/comments`, `/api/posts/{postId}/comments/{id}` | List, read, add, edit, and archive comments |
| `/api/posts/{postId}/reviews` | List reviews with the average rating, or submit a review |
| `/api/dashboard` | View counts, recent posts, and tag usage with an active account |
| `/api/trash/{kind}`, `POST /api/trash/{kind}/{id}/restore` | Inspect and restore archived items as admin. Use `POST`, `COMMENT`, or `TAG` for `kind` and send `{}` as JSON when restoring. |

Public clients can browse posts and their discussions. Active readers can comment and leave one review per post. Authors can also create posts. Authors and commenters can edit or archive their own content, and administrators can act on any content. Archiving a post hides its comments and reviews until the post is restored. Comments archived separately stay in Trash until restored separately. Archived titles and tag names remain reserved.

```powershell
curl.exe "http://localhost:8080/api/posts?keyword=spring&page=0&size=10&sort=NEWEST"
```

Post searches also accept `authorId` and `tagId`. Pages start at zero and have at most 100 items. Sort values are `NEWEST`, `OLDEST`, `TITLE_ASC`, and `TITLE_DESC`. Keyword search matches a case-insensitive literal substring in the title or body.

## GraphQL

Open [GraphiQL](http://localhost:8080/graphiql) in dev or test. Send queries and mutations to `POST /graphql`. Paste one of the operations from [examples.graphql](docs/examples.graphql) into GraphiQL. For authenticated operations, set the `Authorization` header to `Basic <base64(username:password)>` in the Headers editor.

```graphql
query {
  posts(filter: { keyword: "spring", size: 5 }) {
    totalElements
    items { id title tags { name } }
  }
}
```

REST and GraphQL call the same services. GraphQL returns standard `data` and `errors`, with an application error code under `extensions.code` for handled service and validation failures.

## Tests and performance

Create the isolated `blogging_platform_ii_test` PostgreSQL database, make MongoDB available, and run the integration suite.

```powershell
createdb -h localhost -p 5433 -U postgres blogging_platform_ii_test
$env:RUN_BLOG_INTEGRATION_TESTS = 'true'
.\mvnw.cmd verify
```

Use `TEST_POSTGRES_URL` and `TEST_MONGO_URI` to select dedicated test stores. The test creates unique fixtures and leaves them in those stores. The suite checks the two HTTP APIs, compatibility with legacy credentials and MongoDB records, authorization, filtering, validation, and Trash.

Controllers delegate to constructor-injected services and repositories. Spring's `JdbcClient` runs parameterized queries and `MongoTemplate` works with the legacy documents. PostgreSQL performs filtering, ordering, and pagination before rows reach the API. Each result page loads tags in one query and groups them by post ID in memory.

`ServiceMonitoring` uses `@Before` and `@After` for service entry and exit logs and `@Around` to time successful and failed calls. Enable `logging.level.com.example.Blogging_Platform_II.config.ServiceMonitoring=DEBUG` to see these logs. An administrator can read the `blog.service.duration` timer at `/actuator/metrics/blog.service.duration`.

See the [performance report](docs/performance-report.md) for measurements and reproduction steps. The [Platform II lab brief](Blogging-Platform-II.md) describes the requirements.
