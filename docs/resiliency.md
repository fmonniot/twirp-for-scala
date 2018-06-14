---
id: "resiliency"
title: "Resiliency Protocol"
sidebar_label: "Version 1 (Proposed Draft)"
position: 1
layout: docs
---

In this document we will provides some methods to address resiliency issues.

# Load Balancing and Retry

- https://medium.com/netflix-techblog/open-sourcing-zuul-2-82ea476cb2b3
    - Cold instances: reduced traffic on cold instance (how to know ?), throttle cold instance (how to know ?).
        If hit a cold instance which have throttled, prefer to retry on a hot one.
    - Count error rate per target instance. If error rate is too high,
        blacklist instance for some time (to allow it to recover, or to be removed from the pool).
        It means the client needs to be able to track the error rate. Even better, we should be able to
        track it per service/method, that way we can blacklist only the minimum.
    - Throttle retries and requests on instance with high error rate.
    - Add usage measure to be proactive in how we send the traffic.
        That means adding a new headers to the spec (something like `X-Current-Usage: 0.4` combined
        with a `X-Target-Usage: 0.9`) and use them to make better load balancing decision.
- Awesome talk about retry and load balancing
    - Need to find it…

# Service Discovery

- for all implementation, it needs to answer the question: for a given [package]/service, what are the available backends ?
- We can consider using rpc4s itself as a possible implementation (`rpc serverStreaming (Endpoint) returns (stream ListOfBackends);`)
    It's an interesting take, and does offer the possibility for the discovery to stream back changes as they appear.
    But it also means we need to support long connection, not necessarily a big deal but something we need to be aware of.
- We should have a DNS implementation
    - Do we follow consul model: `A`/`AAAA` and `SRV` requests, with discovery for port in the latter and meta in the former (using `TXT`)
    - or the gRPC one: `A` or `AAAA` records for addresses with client config in `TXT`, plus a discovery service if needed via `SRV`
        The discovery service should be in the first `A` query with a `TXT` meta to indicates it's an external lb service
    - It's finally relatively similar

 