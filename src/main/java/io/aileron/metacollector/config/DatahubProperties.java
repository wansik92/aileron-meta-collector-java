package io.aileron.metacollector.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "aileron.datahub")
public class DatahubProperties {

    /** DataHub 연동 활성화 여부 (기본: true) — false 시 모든 emit skip, 비즈니스 로직에 영향 없음 */
    private boolean enabled = true;

    /** DataHub GMS URL */
    private String gmsUrl = "http://localhost:8080";

    /** 환경 (PROD, DEV, ...) */
    private String env = "PROD";

    /** emit 실패 시 예외를 무시할지 여부 (기본: true — 서비스 장애 전파 방지) */
    private boolean silentFail = true;

    /**
     * 연결 타임아웃 초 (기본: 3초)
     * DNS 실패 / 연결 불가 시 빠르게 포기하여 스레드 블로킹 방지
     */
    private int connectTimeoutSec = 3;

    /**
     * 최대 재시도 횟수 (기본: 0 — 재시도 없이 즉시 실패)
     * DataHub down 시 retry 프로세스 누적 방지
     */
    private int maxRetries = 0;

    /**
     * DataHub 연결 실패 후 재시도 대기 시간 초 (기본: 60초)
     */
    private int cooldownSec = 60;

    public boolean isEnabled() { return enabled; }
    public void setEnabled(boolean enabled) { this.enabled = enabled; }

    public String getGmsUrl() { return gmsUrl; }
    public void setGmsUrl(String gmsUrl) { this.gmsUrl = gmsUrl; }

    public String getEnv() { return env; }
    public void setEnv(String env) { this.env = env; }

    public boolean isSilentFail() { return silentFail; }
    public void setSilentFail(boolean silentFail) { this.silentFail = silentFail; }

    public int getConnectTimeoutSec() { return connectTimeoutSec; }
    public void setConnectTimeoutSec(int connectTimeoutSec) { this.connectTimeoutSec = connectTimeoutSec; }

    public int getMaxRetries() { return maxRetries; }
    public void setMaxRetries(int maxRetries) { this.maxRetries = maxRetries; }

    public int getCooldownSec() { return cooldownSec; }
    public void setCooldownSec(int cooldownSec) { this.cooldownSec = cooldownSec; }
}
