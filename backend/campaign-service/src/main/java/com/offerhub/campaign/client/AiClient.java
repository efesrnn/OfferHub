package com.offerhub.campaign.client;

import com.offerhub.campaign.entity.CampaignType;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.time.Duration;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Campaign's only door to AI Service.
 *
 * Every call returns an Optional and never throws: the case document requires a campaign
 * to be created even when AI is unreachable (5.1), and the closing demo deliberately stops
 * a service to prove it. Turning an AI outage into a failed campaign would break both.
 * An empty Optional therefore means "no advice", not "error" - the caller falls back.
 *
 * The timeouts matter as much as the try/catch. A socket that hangs blocks the request
 * thread holding an open transaction; without them "AI is slow" and "AI is down" would
 * have very different, and much worse, consequences.
 */
@Slf4j
@Component
public class AiClient {

    /**
     * Connecting to a service on the same Docker network is either instant or never, so a
     * second is generous. It is the number that decides how long campaign creation stalls
     * when AI is down: a stopped container does not refuse the connection, it swallows it,
     * and the caller waits out this timeout.
     * The read timeout is looser because a reachable AI is allowed to think.
     */
    private static final Duration CONNECT_TIMEOUT = Duration.ofSeconds(1);
    private static final Duration READ_TIMEOUT = Duration.ofSeconds(3);

    /**
     * How long to stop calling AI after it fails. A stopped container does not refuse
     * connections - its name stops resolving, and each attempt pays a DNS wait that no
     * connect timeout covers. Remembering the failure for a few seconds turns "every
     * campaign creation is slow while AI is down" into "the first one is".
     */
    private static final Duration FAILURE_COOLDOWN = Duration.ofSeconds(15);

    private final RestClient restClient;

    /** Epoch millis of the last failure, 0 when AI last answered. Plain long: a stale read
     * only costs one extra attempt, which is not worth synchronising for. */
    private final AtomicLong failedAt = new AtomicLong();

    public AiClient(@Value("${ai.service.url:http://localhost:8083}") String baseUrl) {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(CONNECT_TIMEOUT);
        factory.setReadTimeout(READ_TIMEOUT);

        this.restClient = RestClient.builder().baseUrl(baseUrl).requestFactory(factory).build();
        log.info("AI Service client pointing at {}", baseUrl);
    }

    /**
     * @param subscriberRef the readable code AI knows subscribers by (SUB-0001), which is
     *                      what the projection keeps in externalRef. AI stores its training
     *                      profiles under that code, so sending our UUID would make it fall
     *                      back to a synthetic profile and score a subscriber it never saw.
     */
    public Optional<AiRecommendation> recommend(String subscriberRef, CampaignType campaignType) {
        if (coolingDown()) {
            return Optional.empty();
        }
        try {
            RecommendEnvelope envelope = restClient.post()
                    .uri("/api/v1/ai/recommend")
                    .body(new RecommendRequest(subscriberRef, campaignType.name()))
                    .retrieve()
                    .body(RecommendEnvelope.class);

            failedAt.set(0);
            return Optional.ofNullable(envelope).map(RecommendEnvelope::data);

        } catch (Exception ex) {
            failedAt.set(System.currentTimeMillis());
            // Deliberately broad: connection refused, timeout, 5xx and malformed JSON all
            // mean the same thing to the caller - no advice available right now.
            log.warn("AI recommend failed for {} ({}), falling back", subscriberRef, ex.getMessage());
            return Optional.empty();
        }
    }

    /**
     * Case document 6.3: AI picks the expert from specialty match, spare capacity and past
     * performance. Empty means no advice - the case then waits in the manual queue, which
     * is also what the contract asks for when every expert is at capacity.
     */
    public Optional<AiAssignment> assignExpert(String caseId, String segment) {
        if (coolingDown()) {
            return Optional.empty();
        }
        try {
            AssignEnvelope envelope = restClient.post()
                    .uri("/api/v1/ai/assign-expert")
                    .body(new AssignRequest(caseId, segment))
                    .retrieve()
                    .body(AssignEnvelope.class);

            failedAt.set(0);
            return Optional.ofNullable(envelope)
                    .map(AssignEnvelope::data)
                    .filter(assignment -> assignment.expertId() != null);

        } catch (Exception ex) {
            failedAt.set(System.currentTimeMillis());
            log.warn("AI assign-expert failed for case {} ({}), leaving it unassigned",
                    caseId, ex.getMessage());
            return Optional.empty();
        }
    }

    /** True while a recent failure says there is no point in trying again yet. */
    private boolean coolingDown() {
        long last = failedAt.get();
        return last != 0 && System.currentTimeMillis() - last < FAILURE_COOLDOWN.toMillis();
    }

    /** AI answers in the shared envelope; only the payload is of interest here. */
    private record RecommendEnvelope(boolean success, AiRecommendation data) {
    }

    private record RecommendRequest(String subscriberId, String campaignType) {
    }

    private record AssignEnvelope(boolean success, AiAssignment data) {
    }

    private record AssignRequest(String caseId, String segment) {
    }
}
