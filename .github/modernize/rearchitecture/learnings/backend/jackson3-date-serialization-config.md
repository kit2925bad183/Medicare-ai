# Jackson 3 Date Serialization Configuration

Spring Boot 4's Jackson 3 configuration must not bind Jackson 2's `WRITE_DATES_AS_TIMESTAMPS` feature.

## What Happened

In MediCare AI task t1.1.1.1, the existing `spring.jackson.serialization.write-dates-as-timestamps: false` setting prevented Spring Boot 4.1.2 startup because Jackson 3.1.7's `tools.jackson.databind.SerializationFeature` enum no longer declares `WRITE_DATES_AS_TIMESTAMPS`. Removing that obsolete feature setting allowed the full application context and packaged app to start; a regression test confirmed `Instant` values still serialize as ISO-8601 JSON strings.

## Takeaway

For Boot 4 / Jackson 3 configuration binding failures, verify the requested feature against the resolved `tools.jackson` enum before choosing a replacement. Do not retain Jackson 2 feature names or add dependencies speculatively; test the actual required wire behavior using Boot's configured `JsonMapper`.

## History

- 2026-10-27 (MediCare AI/t1.1.1.1): initial
