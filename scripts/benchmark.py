
"""Compare post search over REST and GraphQL with a shared keep-alive connection."""

import argparse
import http.client
import json
import math
import statistics
import time
from urllib.parse import urlparse


def request(connection, name):
    start = time.perf_counter_ns()
    if name == "REST":
        connection.request("GET", "/api/posts?keyword=springneedle&page=0&size=10")
    else:
        fields = "id title"
        if name == "GraphQL full":
            fields += " authorId body createdAt updatedAt tags { id name }"
        query = (
            'query { posts(filter: {keyword: "springneedle", page: 0, size: 10})'
            f" {{ page size totalElements totalPages items {{ {fields} }} }} }}"
        )
        connection.request("POST", "/graphql", json.dumps({"query": query}),
                           {"Content-Type": "application/json"})
    response = connection.getresponse()
    body = response.read()
    elapsed = (time.perf_counter_ns() - start) / 1_000_000
    payload = json.loads(body)
    if response.status != 200 or payload.get("errors"):
        raise RuntimeError(f"{name} failed ({response.status}): {payload}")
    page = payload["data"] if name == "REST" else payload["data"]["posts"]
    if page["totalElements"] != 200 or len(page["items"]) != 10:
        raise RuntimeError("Expected 200 matches and 10 items. Load db/benchmark.sql in an isolated database.")
    return elapsed, len(body), tuple(str(item["id"]) for item in page["items"])


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--base-url", default="http://localhost:8080")
    parser.add_argument("--requests", type=int, default=200, help="Timed requests per variant")
    args = parser.parse_args()
    if args.requests < 1:
        parser.error("--requests must be positive")
    url = urlparse(args.base_url)
    connection_type = http.client.HTTPSConnection if url.scheme == "https" else http.client.HTTPConnection
    connection = connection_type(url.hostname, url.port, timeout=10)
    variants = ["REST", "GraphQL full", "GraphQL compact"]
    results = {variant: [] for variant in variants}
    try:
        for iteration in range(30 + args.requests):
            first = iteration % len(variants)
            ordered = variants[first:] + variants[:first]
            round_results = {name: request(connection, name) for name in ordered}
            if len({ids for _, _, ids in round_results.values()}) != 1:
                raise RuntimeError("The variants returned different posts.")
            if iteration >= 30:
                for name, (elapsed, size, _) in round_results.items():
                    results[name].append((elapsed, size))
    finally:
        connection.close()
    print(f"{'Variant':<19} {'p50 ms':>8} {'p95 ms':>8} {'Mean bytes':>12}")
    for name, samples in results.items():
        timings = sorted(elapsed for elapsed, _ in samples)
        p95 = timings[math.ceil(len(timings) * 0.95) - 1]
        size = statistics.mean(size for _, size in samples)
        print(f"{name:<19} {statistics.median(timings):>8.2f} {p95:>8.2f} {size:>12.0f}")


if __name__ == "__main__":
    main()
