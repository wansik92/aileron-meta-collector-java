package io.aileron.metacollector.context;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class JobContext {

    private final String jobId;
    private final String flow;
    private final String platform;
    private final List<String> upstreamJobIds;
    private final String description;
    private final String flowDescription;
    private final String runId;
    private final long startTimeMs;

    private final List<String> inputs = new ArrayList<>();
    private final List<String> outputs = new ArrayList<>();

    public JobContext(String jobId, String flow, String platform,
                      List<String> upstreamJobIds, String description, String flowDescription) {
        this.jobId = jobId;
        this.flow = flow;
        this.platform = platform;
        this.upstreamJobIds = upstreamJobIds != null ? upstreamJobIds : List.of();
        this.description = description;
        this.flowDescription = flowDescription;
        this.runId = UUID.randomUUID().toString();
        this.startTimeMs = System.currentTimeMillis();
    }

    public void addInput(String urn) { inputs.add(urn); }
    public void addOutput(String urn) { outputs.add(urn); }

    public String getJobId() { return jobId; }
    public String getFlow() { return flow; }
    public String getPlatform() { return platform; }
    public List<String> getUpstreamJobIds() { return upstreamJobIds; }
    public String getDescription() { return description; }
    public String getFlowDescription() { return flowDescription; }
    public String getRunId() { return runId; }
    public long getStartTimeMs() { return startTimeMs; }
    public List<String> getInputs() { return inputs; }
    public List<String> getOutputs() { return outputs; }
}
