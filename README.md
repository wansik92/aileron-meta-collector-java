# aileron-meta-collector (Java/Spring)

Spring AOP 기반 DataHub lineage 자동 수집 라이브러리.  
Python 버전([aileron-meta-collector](https://pypi.org/project/aileron-meta-collector/))의 Java 대응 버전입니다.

## 설치

```xml
<dependency>
    <groupId>io.aileron</groupId>
    <artifactId>aileron-meta-collector</artifactId>
    <version>0.1.0</version>
</dependency>
```

## 설정

```yaml
aileron:
  datahub:
    gms-url: http://datahub-gms:8080
    env: PROD
    silent-fail: true  # emit 실패 시 예외 무시 (기본값: true)
```

## 사용법

### 기본 — `@DatahubJob`

```java
@DatahubJob(jobId = "external-trigger", flow = "ingest-pipeline")
@PostMapping("/trigger")
public ResponseEntity<?> trigger(@RequestBody TriggerRequest req) {
    // DataFlow, DataJob, DataProcessInstance 자동 emit
    return ResponseEntity.ok().build();
}
```

### 수동 lineage 주입

```java
@DatahubJob(jobId = "ingest", flow = "ingest-pipeline")
public void run() {
    DatahubLineage.addInput("s3", "raw/events", "PROD");
    DatahubLineage.addOutput("glue", "events_cleaned", "PROD");
    // ... 비즈니스 로직
}
```

full URN으로도 가능:

```java
DatahubLineage.addInput("urn:li:dataset:(urn:li:dataPlatform:s3,raw/events,PROD)");
```

### upstream job 연결

```java
// MWAA CLI 호출 후 Airflow DAG와 연결
@DatahubJob(
    jobId = "mwaa-trigger",
    flow = "ingest-pipeline",
    upstreamJobs = {"external-trigger"}
)
public void triggerMwaa() {
    mwaaClient.triggerDag("ingest_dag");
}
```

cross-platform upstream (full URN):

```java
@DatahubJob(
    jobId = "transform",
    flow = "ingest-pipeline",
    upstreamJobs = {"urn:li:dataJob:(urn:li:dataFlow:(airflow,ingest_dag,PROD),extract_task)"}
)
public void transform() { ... }
```

### patch 모드 (기존 lineage에 추가)

```java
@DatahubJob(jobId = "incremental-load", flow = "pipeline", patch = true)
public void load() { ... }
```

## 전체 흐름 예시

```
외부 트리거 API (@DatahubJob "external-trigger")
    → MWAA CLI 호출 (@DatahubJob "mwaa-trigger", upstreamJobs=["external-trigger"])
        → Airflow DAG (DataHub Airflow 플러그인이 자동 수집)
            → 실제 데이터셋
```
