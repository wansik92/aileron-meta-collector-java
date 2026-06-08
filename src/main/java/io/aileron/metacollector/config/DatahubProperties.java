package io.aileron.metacollector.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "aileron.datahub")
public class DatahubProperties {

    /** DataHub GMS URL */
    private String gmsUrl = "http://localhost:8080";

    /** 환경 (PROD, DEV, ...) */
    private String env = "PROD";

    /** emit 실패 시 예외를 무시할지 여부 (기본: true — 서비스 장애 전파 방지) */
    private boolean silentFail = true;

    public String getGmsUrl() { return gmsUrl; }
    public void setGmsUrl(String gmsUrl) { this.gmsUrl = gmsUrl; }

    public String getEnv() { return env; }
    public void setEnv(String env) { this.env = env; }

    public boolean isSilentFail() { return silentFail; }
    public void setSilentFail(boolean silentFail) { this.silentFail = silentFail; }
}
