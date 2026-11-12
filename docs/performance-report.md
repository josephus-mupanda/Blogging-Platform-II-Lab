# Performance report

This report answers two questions:

1. **Did the indexes make the search faster?** Yes, about 50 times faster.
2. **Is GraphQL better than REST?** It is not faster, but its answers are 18 times smaller.

The test database holds **20,000 posts**. The word `springneedle` appears in **200** of
them. Every test searches for that word and asks for the first 10 results.

---

## Setup (do this once)

```powershell
$env:Path += ";C:\Program Files\PostgreSQL\18\bin"
$env:PGPASSWORD = 'postgres'

createdb -h localhost -p 5433 -U postgres blogging_platform_ii_bench
psql -h localhost -p 5433 -U postgres -d blogging_platform_ii_bench -f db/schema.sql
psql -h localhost -p 5433 -U postgres -d blogging_platform_ii_bench -f db/benchmark.sql
```

The last line creates the 20,000 posts. Run these commands from the project folder.

---

## Test 1: the database, before and after indexing

### What we measure

How long PostgreSQL takes to count the 200 posts that contain the word.

We run the **same query twice**. The first time PostgreSQL may use the indexes. The second
time we switch them off with `SET enable_bitmapscan = off`, so it must read all 20,000 rows
one by one. Same query, same data, same machine. Only the indexes change.

### The command

```powershell
psql -h localhost -p 5433 -U postgres -d blogging_platform_ii_bench -f db/benchmark.sql
```

### The result

Look for the lines that say `Execution Time` near the bottom of the output.

| | How PostgreSQL searched | Time |
|---|---|---:|
| **Before** (indexes off) | Read all 20,000 rows: `Seq Scan on posts` | **376.76 ms** |
| **After** (indexes on) | Jumped straight to the matches: `Bitmap Index Scan` | **8.04 ms** |

**The search is about 47 times faster with the indexes.**

### What this means

The two indexes doing the work are `idx_posts_title_trgm` and `idx_posts_body_trgm`. They
are trigram indexes, which means PostgreSQL can use them even when the search is
"contains this word anywhere", not just "starts with this word".

Without them, the database must open every post and look inside it. With them, it goes
straight to the 200 that match.

**Note on the exact number.** Run the command again and you will not get 47 times. We got
167, 95 and 50 on different runs of the same machine. The timings change because the
computer is busier or quieter, and because PostgreSQL remembers recent data in memory. What
never changes is that the indexed version wins by a very large margin. Say "about 50 times
faster" or "two orders of magnitude", not one exact number.

---

## Test 2: REST and GraphQL, side by side

### What we measure

The same search through both APIs, three ways:

- **REST** returns the full posts. You cannot choose the fields.
- **GraphQL, all fields** returns the same data as REST.
- **GraphQL, titles only** asks for the id and title of each post and nothing else.

### The command

Start the app on the test database in one terminal:

```powershell
$env:DB_POSTGRES_URL = 'jdbc:postgresql://localhost:5433/blogging_platform_ii_bench'
$env:DB_MONGO_URI = 'mongodb://localhost:27017/blogging_platform_ii_bench'
.\mvnw.cmd spring-boot:run
```

Then paste this in a second terminal:

```powershell
$rest = 'http://localhost:8080/api/posts?keyword=springneedle&page=0&size=10'
$gqlUrl = 'http://localhost:8080/graphql'
$compact = @{ query = '{ posts(filter: {keyword: "springneedle", page: 0, size: 10}) { page size totalElements totalPages items { id title } } }' } | ConvertTo-Json -Compress
$full = @{ query = '{ posts(filter: {keyword: "springneedle", page: 0, size: 10}) { page size totalElements totalPages items { id title authorId body createdAt updatedAt tags { id name } } } }' } | ConvertTo-Json -Compress
$calls = [ordered]@{
  'REST'                  = { Invoke-WebRequest $rest -UseBasicParsing }
  'GraphQL, all fields'   = { Invoke-WebRequest $gqlUrl -Method Post -ContentType 'application/json' -Body $full -UseBasicParsing }
  'GraphQL, titles only'  = { Invoke-WebRequest $gqlUrl -Method Post -ContentType 'application/json' -Body $compact -UseBasicParsing }
}
$calls.GetEnumerator() | ForEach-Object {
  $b = $_.Value
  1..20 | ForEach-Object { & $b | Out-Null }
  $t = (1..100 | ForEach-Object { (Measure-Command { & $b | Out-Null }).TotalMilliseconds }) | Sort-Object
  [pscustomobject]@{ API = $_.Key; 'typical ms' = [math]::Round($t[49],1); 'slow ms' = [math]::Round($t[94],1); 'bytes' = (& $b).RawContentLength }
} | Format-Table -AutoSize
```

It calls each API 120 times. The first 20 calls are thrown away, because the program is
still warming up and those are always slow. The other 100 are sorted.

- **typical ms** is the middle value of the 100 calls.
- **slow ms** is the value that only 5 calls out of 100 were slower than.

### The result

| API | typical ms | slow ms | Response size |
|---|---:|---:|---:|
| REST | 41.53 ms | 70.22 ms | 9,781 bytes |
| GraphQL, all fields | 43.83 ms | 66.19 ms | 9,787 bytes |
| GraphQL, titles only | 42.91 ms | 65.43 ms | **545 bytes** |

### What this means

**Speed is the same.** All three take about the same time, because all three run the exact
same SQL. REST and GraphQL call the same `PostService`, so the database does identical work.
GraphQL is not a faster way to reach the database.

**Size is where GraphQL wins.** 545 bytes instead of 9,781 for the same 10 posts. That is
**94% smaller**, because the client asked for only the id and the title. REST always sends
the whole post, including the long body text nobody asked for. On a phone or a slow
connection, this is the difference that matters.

**The sizes are exact, the times are not.** The byte counts come out the same on every run,
because they depend only on which fields you ask for. The milliseconds move around
depending on what else the computer is doing. Trust the sizes; treat the times as
approximate.

---

## Other things that keep the API fast

**One query per page, not one per post.** A page of 10 posts needs each post's tags. The
simple way costs 11 queries: 1 for the posts and 10 for the tags. Instead
`PostRepository.withTags` loads all the tags for the page in a single query and sorts them
by post in memory with a `HashMap`. A page always costs 3 queries, whatever the page size.

**The database does the paging.** `LIMIT` and `OFFSET` are in the SQL, so only 10 rows
travel from PostgreSQL to Java. The application never loads 20,000 posts to throw most of
them away.

**Sorting is stable.** Every `ORDER BY` ends with `post_id`. If two posts share the same
date, the id decides. Without that, page 2 could show a post you already saw on page 1.

**One known limit.** `OFFSET` makes PostgreSQL walk past the rows it skips, so page 1000 is
slower than page 1. Fixing it means keyset pagination, which loses the ability to jump to
any page number. For a blog that trade is not worth it.

---

## Test conditions

Measured on 5 November 2026. Java 21.0.2, Spring Boot 3.5.16, PostgreSQL 18.3, Windows.
The application, both databases, and the test client all ran on the same laptop.
