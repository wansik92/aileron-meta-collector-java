package io.aileron.metacollector.annotation;

import java.lang.annotation.*;

/**
 * 메서드에 붙이면 DataHub DataJob 리니지가 자동으로 수집됩니다.
 *
 * <pre>{@code
 * @DatahubJob(jobId = "external-trigger", flow = "ingest-pipeline")
 * @PostMapping("/trigger")
 * public ResponseEntity<?> trigger(@RequestBody TriggerRequest req) { ... }
 * }</pre>
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface DatahubJob {

    /** DataHub DataJob 식별자 */
    String jobId();

    /** DataFlow 이름 (파이프라인 단위) */
    String flow() default "default";

    /** 플랫폼 (기본값: restApi) */
    String platform() default "restApi";

    /** 이 job이 의존하는 상위 DataJob ID 목록. 같은 flow 내 job_id 또는 full URN */
    String[] upstreamJobs() default {};

    /** DataJob 설명 */
    String description() default "";

    /** DataFlow 설명 */
    String flowDescription() default "";

    /**
     * true이면 DataJobInputOutput을 patch MCP로 emit — 기존 데이터에 추가 (덮어쓰지 않음).
     * false(기본값)이면 replace 방식 유지.
     */
    boolean patch() default false;
}
