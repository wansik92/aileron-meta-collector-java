package io.aileron.metacollector.context;

/**
 * 현재 job context에 input/output 데이터셋 URN을 수동으로 추가합니다.
 *
 * <pre>{@code
 * @DatahubJob(jobId = "ingest", flow = "pipeline")
 * public void run() {
 *     DatahubLineage.addInput("urn:li:dataset:(urn:li:dataPlatform:s3,raw/events,PROD)");
 *     DatahubLineage.addOutput("urn:li:dataset:(urn:li:dataPlatform:glue,events,PROD)");
 *     // ... 비즈니스 로직
 * }
 * }</pre>
 */
public class DatahubLineage {

    public static void addInput(String datasetUrn) {
        JobContextHolder.addInput(datasetUrn);
    }

    public static void addOutput(String datasetUrn) {
        JobContextHolder.addOutput(datasetUrn);
    }

    /**
     * dataset URN을 직접 조립할 필요 없이 플랫폼 + 경로로 추가합니다.
     *
     * @param platform  dataPlatform 이름 (예: s3, glue, snowflake)
     * @param name      데이터셋 경로 또는 테이블명
     * @param env       환경 (예: PROD, DEV)
     */
    public static void addInput(String platform, String name, String env) {
        addInput(buildUrn(platform, name, env));
    }

    public static void addOutput(String platform, String name, String env) {
        addOutput(buildUrn(platform, name, env));
    }

    public static String buildUrn(String platform, String name, String env) {
        return String.format("urn:li:dataset:(urn:li:dataPlatform:%s,%s,%s)", platform, name, env);
    }
}
