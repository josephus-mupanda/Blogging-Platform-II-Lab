
# REST and GraphQL post search performance

The benchmark searches 20,000 PostgreSQL posts for the same keyword across both APIs. The keyword matches 200 posts. Each request returns the newest 10 matches, and the runner verifies that all variants return the same post IDs in the same order.

The measurements were taken on 5 November 2026 using Java 21.0.2, Spring Boot 3.5.16, and PostgreSQL 18.3 on Windows. The app, the database, and the HTTP client ran on the same machine. Each variant had 30 warm-up requests and 200 measured requests, sent one at a time over a single keep-alive connection with the request order rotated between variants. Payload sizes are uncompressed response body bytes.

## HTTP results

| Variant | p50 | p95 | Mean payload |
|---|---:|---:|---:|
| REST | 41.53 ms | 70.22 ms | 9,781 bytes |
| GraphQL, full post fields | 43.83 ms | 66.19 ms | 9,787 bytes |
| GraphQL, post IDs and titles | 42.91 ms | 65.43 ms | 545 bytes |

The compact GraphQL response used 94.4% fewer bytes than REST. REST had the lowest median latency, and compact GraphQL had the lowest p95 in this run.

GraphQL's compact selection removes post bodies and other fields from the response. Both GraphQL selections and REST call the same search service, which loads the same page and its tags. The compact response saves transfer size. The timings describe a single local client and do not measure concurrent throughput.

## PostgreSQL search

| Query plan | Execution time | Shared buffers hit |
|---|---:|---:|
| Newest 10 matches via the visible-post timestamp index | 30.99 ms | 108 |
| Count via a bitmap scan combining both trigram indexes | 8.04 ms | 232 |
| Same count via a sequential scan with bitmap scans disabled | 376.76 ms | 2,223 |

The indexed count took about one-forty-seventh of the sequential scan time in this run. The ordered page scan rejected 891 rows before it reached the tenth match.

The count and page queries share a filter. The count can combine the title and body trigram indexes. PostgreSQL can use the visible-post timestamp index to stop after it has found enough rows in order for a small first page. Each result page makes one additional query for all its tags, which the repository groups by post ID with a hash map. Stable sort ties use the post ID. Deep offsets still require the database to skip earlier matches.

## Reproduce

Create a dedicated PostgreSQL database called `blogging_platform_ii_bench` and start the application using that database. Use a dedicated MongoDB database for the benchmark profile overrides as well.

```powershell
createdb -h localhost -p 5433 -U postgres blogging_platform_ii_bench
psql -h localhost -p 5433 -U postgres -d blogging_platform_ii_bench -f db/schema.sql
$env:DB_POSTGRES_URL = 'jdbc:postgresql://localhost:5433/blogging_platform_ii_bench'
$env:DB_MONGO_URI = 'mongodb://localhost:27017/blogging_platform_ii_bench'
.\mvnw.cmd spring-boot:run
```

Seed the posts and print the query plans in a second terminal. The `EXPLAIN (ANALYZE, BUFFERS)` output of this script produces the PostgreSQL table above.

```powershell
psql -h localhost -p 5433 -U postgres -d blogging_platform_ii_bench -v ON_ERROR_STOP=1 -f db/benchmark.sql
```

For the HTTP table, time the same search on each API. Send the request a few times first so the JVM and the database cache warm up, then record `time_total` over a run of requests and take the median and the 95th percentile.

```powershell
curl.exe -s -o NUL -w "%{time_total} %{size_download}`n" "http://localhost:8080/api/posts?keyword=springneedle&page=0&size=10"

curl.exe -s -o NUL -w "%{time_total} %{size_download}`n" -X POST http://localhost:8080/graphql -H "Content-Type: application/json" -d "{\"query\":\"{ posts(filter: {keyword: \\\"springneedle\\\", page: 0, size: 10}) { totalElements items { id title } } }\"}"
```

The first command is the REST variant. The second is the compact GraphQL variant; add `body authorId createdAt updatedAt tags { id name }` to the selection to measure the full GraphQL variant.
