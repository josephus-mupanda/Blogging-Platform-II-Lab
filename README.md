
# Blogging Platform II

A Java 21 and Spring Boot 3.5 backend that exposes the Blogging Platform I data and workflows over REST and GraphQL. PostgreSQL stores accounts, posts, and tags. MongoDB stores comments and reviews.

## Lab requirements

| Epic | What to show |
|---|---|
| 1. Setup | The `dev`, `test`, and `prod` configuration files and the constructor in `PostController`. |
| 2. REST | CRUD for users, posts, comments, and tags in Swagger. Search posts with `keyword`, `sort`, `page`, and `size` and show the `status`, `message`, and `data` response. |
| 3. Validation | Try creating a user with an existing username. `UniqueAccountValidator` checks the database, and `ApiErrors` formats the error response. |
| 4. GraphQL | Run the post search and a comment mutation from [examples.graphql](docs/examples.graphql) in GraphiQL. |
| 5. AOP and performance | Call post search and the dashboard with timing logs enabled. Use the [performance report](docs/performance-report.md) to explain the indexes and compare the two APIs. |

## Code structure

REST and GraphQL follow the same flow. For example, `PostController` and `PostGraphQlController` call `PostService`, which uses `PostRepository` to query PostgreSQL.

| Package | Responsibility |
|---|---|
| `controller.rest` and `controller.graphql` | Bind requests, validate DTOs, and format responses and errors. |
| `service` | Apply business rules, manage transactions, and call repositories. |
| `repository` | Read and write PostgreSQL and MongoDB records. |

The `dto` package contains the input classes that requests are bound to, and `model` contains the classes that services return. Both are plain Java classes with a constructor, getters, and setters. The uniqueness validator calls `UserService` for its checks. Services raise `BlogException` with a shared error code, which each controller's exception handler converts to its own response format.

## Run locally

Install JDK 21, PostgreSQL with `pg_trgm`, and MongoDB. The project includes a Maven wrapper. From the repository root, create the development database, run the setup script once, and start the app.

```powershell
createdb -h localhost -p 5433 -U postgres blogging_platform_ii
psql -h localhost -p 5433 -U postgres -d blogging_platform_ii -f db/schema.sql
.\mvnw.cmd spring-boot:run
```

`createdb` and `psql` live in the PostgreSQL `bin` folder, for example `C:\Program Files\PostgreSQL\18\bin`. Call them with the full path or add that folder to `PATH`. Use `./mvnw` on macOS or Linux.

[db/schema.sql](db/schema.sql) creates the tables and indexes designed in Platform I. It only creates what is missing, so it is safe to run again, and it can also be run against an existing Platform I database to add the columns this phase uses. If an older database has duplicate titles that differ only by case or surrounding spaces, resolve those duplicates first so the unique title index can be created. MongoDB needs no setup script because it creates its collections and indexes on first use.

## Environment profiles

| Profile | PostgreSQL | MongoDB | GraphiQL |
|---|---|---|---|
| `dev` (default) | `jdbc:postgresql://localhost:5433/blogging_platform_ii`, user and password `postgres` | `mongodb://localhost:27017/blogging_platform_ii` | Enabled |
| `test` | `jdbc:postgresql://localhost:5433/blogging_platform_ii_test`, user and password `postgres` | `mongodb://localhost:27017/blogging_platform_ii_test` | Enabled |
| `prod` | Explicit connection settings | Explicit connection settings | Disabled |

Select a profile with `SPRING_PROFILES_ACTIVE`. Set `DB_POSTGRES_URL`, `DB_POSTGRES_USER`, `DB_POSTGRES_PASSWORD`, and `DB_MONGO_URI` for dev or prod. Use the `TEST_` prefix instead of `DB_` for the test profile. Include the database name in the MongoDB URI. PostgreSQL requires a role that can apply the migration, and MongoDB requires permission to create indexes at startup. The lab API listens on localhost by default and accepts requests without login.

## REST

Open Swagger UI at [localhost:8080/swagger-ui/index.html](http://localhost:8080/swagger-ui/index.html), or import [docs/postman_collection.json](docs/postman_collection.json) into Postman for a ready set of requests covering REST, GraphQL, and the validation failures. [The OpenAPI JSON](http://localhost:8080/v3/api-docs) can also be imported if you prefer to start from the generated spec. The REST response envelope has `status`, `message`, and `data`, and is produced by `ApiResponse`. Creation returns HTTP 201. Validation errors include field messages when available.

| Routes | Operations |
|---|---|
| `/api/users`, `/api/users/{id}` | Create, list, read, update, or delete user profiles |
| `/api/posts`, `/api/posts/{id}` | Browse, read, create, edit, and archive posts |
| `/api/tags`, `/api/tags/{id}` | Browse, read, create, rename, or delete tags |
| `/api/posts/{postId}/comments`, `/api/posts/{postId}/comments/{id}` | List, read, add, edit, and archive comments |
| `/api/posts/{postId}/reviews` | List reviews with the average rating, or submit a review |
| `/api/dashboard` | Count visible users, posts, tags, comments, and reviews |

Create a user first and copy its ID from the response. Supply `authorId` as a query parameter when creating a post or comment and `reviewerId` when adding a review. Each user can review a post once. Deleting a user hides the profile and preserves its existing content. Deleting a post hides it and its discussion. Deleted titles and tag names remain reserved.

```powershell
curl.exe "http://localhost:8080/api/posts?keyword=spring&page=0&size=10&sort=NEWEST"
```

Post searches also accept `authorId` and `tagId`. Pages start at zero and have at most 100 items. Sort values are `NEWEST`, `OLDEST`, `TITLE_ASC`, and `TITLE_DESC`. Keyword search matches a case-insensitive literal substring in the title or body.

## GraphQL

Open [GraphiQL](http://localhost:8080/graphiql) in dev or test. Send queries and mutations to `POST /graphql`. Paste an operation from [examples.graphql](docs/examples.graphql) into GraphiQL and supply any required IDs in the Variables editor.

```graphql
query {
  posts(filter: { keyword: "spring", size: 5 }) {
    totalElements
    items { id title tags { name } }
  }
}
```

REST and GraphQL call the same services. GraphQL returns standard `data` and `errors`, with an application error code under `extensions.code` for handled service and validation failures.

## Performance

Repositories use Spring's `JdbcClient` for parameterized SQL and `MongoTemplate` for MongoDB documents. PostgreSQL performs filtering, ordering, and pagination before rows reach the API. Each result page loads tags in one query and groups them by post ID in memory.

`ServiceMonitoring` uses `@Before` and `@After` for service entry and exit logs and `@Around` to log elapsed time for CRUD and analytics calls. Start the app with the command below, then call the post search or dashboard endpoint to see the timings.

```powershell
.\mvnw.cmd spring-boot:run '-Dspring-boot.run.arguments=--logging.level.com.example.Blogging_Platform_II.config.ServiceMonitoring=DEBUG'
```

See the [performance report](docs/performance-report.md) for measurements and reproduction steps. The [Platform II lab brief](Blogging-Platform-II.md) describes the requirements.
