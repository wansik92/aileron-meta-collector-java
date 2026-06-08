package io.aileron.metacollector.emitter;

import com.linkedin.common.AuditStamp;
import com.linkedin.common.DataJobUrnArray;
import com.linkedin.common.DatasetUrnArray;
import com.linkedin.common.UrnArray;
import com.linkedin.common.urn.DataFlowUrn;
import com.linkedin.common.urn.DataJobUrn;
import com.linkedin.common.urn.Urn;
import com.linkedin.data.template.StringMap;
import com.linkedin.datajob.DataFlowInfo;
import com.linkedin.datajob.DataJobInfo;
import com.linkedin.datajob.DataJobInputOutput;
import com.linkedin.dataprocess.DataProcessInstanceInput;
import com.linkedin.dataprocess.DataProcessInstanceOutput;
import com.linkedin.dataprocess.DataProcessInstanceProperties;
import com.linkedin.dataprocess.DataProcessInstanceRelationships;
import com.linkedin.dataprocess.DataProcessInstanceRunEvent;
import com.linkedin.dataprocess.DataProcessInstanceRunResult;
import com.linkedin.dataprocess.DataProcessRunStatus;
import com.linkedin.dataprocess.DataProcessType;
import com.linkedin.dataprocess.RunResultType;
import datahub.client.rest.RestEmitter;
import datahub.event.MetadataChangeProposalWrapper;
import io.aileron.metacollector.config.DatahubProperties;
import io.aileron.metacollector.context.JobContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.net.URISyntaxException;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Component
public class DatahubEmitter {

    private static final Logger log = LoggerFactory.getLogger(DatahubEmitter.class);

    private final DatahubProperties props;
    private final ExecutorService executor = Executors.newFixedThreadPool(2, r -> {
        Thread t = new Thread(r, "aileron-emit");
        t.setDaemon(true);
        return t;
    });

    public DatahubEmitter(DatahubProperties props) {
        this.props = props;
    }

    // ── public async API ──────────────────────────────────────────────────────

    public void emitDataflowAsync(JobContext job) {
        executor.submit(() -> emitDataflow(job));
    }

    public void emitDatajobAsync(JobContext job) {
        executor.submit(() -> emitDatajob(job));
    }

    public void emitRunStartAsync(JobContext job) {
        executor.submit(() -> emitRunStart(job));
    }

    public void emitRunEndAsync(JobContext job, boolean success, String errorMsg, boolean patch) {
        executor.submit(() -> emitRunEnd(job, success, errorMsg, patch));
    }

    // ── private emit methods ──────────────────────────────────────────────────

    private void emitDataflow(JobContext job) {
        try (RestEmitter emitter = buildEmitter()) {
            DataFlowUrn flowUrn = flowUrn(job);
            DataFlowInfo info = new DataFlowInfo().setName(job.getFlow());
            if (!job.getFlowDescription().isEmpty()) {
                info.setDescription(job.getFlowDescription());
            }
            emit(emitter, MetadataChangeProposalWrapper.builder()
                    .entityType("dataFlow")
                    .entityUrn(flowUrn)
                    .upsert()
                    .aspect(info)
                    .build());
            log.info("[aileron] emit ok | dataflow  flow={}", job.getFlow());
        } catch (Exception e) {
            handleError("DataFlow emit failed", e);
        }
    }

    private void emitDatajob(JobContext job) {
        try (RestEmitter emitter = buildEmitter()) {
            DataFlowUrn flowUrn = flowUrn(job);
            DataJobUrn jobUrn = jobUrn(job);
            DataJobInfo info = new DataJobInfo()
                    .setName(job.getJobId())
                    .setFlowUrn(flowUrn)
                    .setType(DataJobInfo.Type.create("PYTHON"));
            if (!job.getDescription().isEmpty()) {
                info.setDescription(job.getDescription());
            }
            emit(emitter, MetadataChangeProposalWrapper.builder()
                    .entityType("dataJob")
                    .entityUrn(jobUrn)
                    .upsert()
                    .aspect(info)
                    .build());
            log.info("[aileron] emit ok | datajob   flow={}  job={}", job.getFlow(), job.getJobId());
        } catch (Exception e) {
            handleError("DataJob emit failed", e);
        }
    }

    private void emitRunStart(JobContext job) {
        try (RestEmitter emitter = buildEmitter()) {
            String instanceUrn = instanceUrn(job);
            AuditStamp audit = new AuditStamp()
                    .setTime(job.getStartTimeMs())
                    .setActor(Urn.createFromString("urn:li:corpuser:datahub"));

            emit(emitter, MetadataChangeProposalWrapper.builder()
                    .entityType("dataProcessInstance")
                    .entityUrn(Urn.createFromString(instanceUrn))
                    .upsert()
                    .aspect(new DataProcessInstanceProperties()
                            .setName(job.getJobId() + "#" + job.getRunId().substring(0, 8))
                            .setCreated(audit)
                            .setType(DataProcessType.BATCH_AD_HOC))
                    .build());

            emit(emitter, MetadataChangeProposalWrapper.builder()
                    .entityType("dataProcessInstance")
                    .entityUrn(Urn.createFromString(instanceUrn))
                    .upsert()
                    .aspect(new DataProcessInstanceRelationships()
                            .setUpstreamInstances(new UrnArray())
                            .setParentTemplate(jobUrn(job)))
                    .build());

            emit(emitter, MetadataChangeProposalWrapper.builder()
                    .entityType("dataProcessInstance")
                    .entityUrn(Urn.createFromString(instanceUrn))
                    .upsert()
                    .aspect(new DataProcessInstanceRunEvent()
                            .setStatus(DataProcessRunStatus.STARTED)
                            .setTimestampMillis(job.getStartTimeMs())
                            .setAttempt(1))
                    .build());

            log.info("[aileron] emit ok | run_start  flow={}  job={}  run={}",
                    job.getFlow(), job.getJobId(), job.getRunId().substring(0, 8));
        } catch (Exception e) {
            handleError("DataProcessInstance start emit failed", e);
        }
    }

    private void emitRunEnd(JobContext job, boolean success, String errorMsg, boolean patch) {
        try (RestEmitter emitter = buildEmitter()) {
            long endTimeMs = System.currentTimeMillis();
            String instanceUrn = instanceUrn(job);
            DataJobUrn jobUrn = jobUrn(job);

            RunResultType resultType = success ? RunResultType.SUCCESS : RunResultType.FAILURE;

            // run event
            emit(emitter, MetadataChangeProposalWrapper.builder()
                    .entityType("dataProcessInstance")
                    .entityUrn(Urn.createFromString(instanceUrn))
                    .upsert()
                    .aspect(new DataProcessInstanceRunEvent()
                            .setStatus(DataProcessRunStatus.COMPLETE)
                            .setTimestampMillis(endTimeMs)
                            .setAttempt(1)
                            .setDurationMillis(endTimeMs - job.getStartTimeMs())
                            .setResult(new DataProcessInstanceRunResult()
                                    .setType(resultType)
                                    .setNativeResultType(resultType.toString())))
                    .build());

            // instance inputs/outputs
            List<String> inputs = job.getInputs();
            List<String> outputs = job.getOutputs();

            if (!inputs.isEmpty()) {
                UrnArray inputUrns = toUrnArray(inputs);
                emit(emitter, MetadataChangeProposalWrapper.builder()
                        .entityType("dataProcessInstance")
                        .entityUrn(Urn.createFromString(instanceUrn))
                        .upsert()
                        .aspect(new DataProcessInstanceInput().setInputs(inputUrns))
                        .build());
            }
            if (!outputs.isEmpty()) {
                UrnArray outputUrns = toUrnArray(outputs);
                emit(emitter, MetadataChangeProposalWrapper.builder()
                        .entityType("dataProcessInstance")
                        .entityUrn(Urn.createFromString(instanceUrn))
                        .upsert()
                        .aspect(new DataProcessInstanceOutput().setOutputs(outputUrns))
                        .build());
            }

            // DataJob input/output (replace 방식만 지원 — patch는 REST API 제약으로 별도 구현 필요)
            List<String> upstreamJobUrns = resolveUpstreamUrns(job);
            if (!inputs.isEmpty() || !outputs.isEmpty() || !upstreamJobUrns.isEmpty()) {
                DataJobInputOutput inputOutput = new DataJobInputOutput()
                        .setInputDatasets(toDatasetUrnArray(inputs))
                        .setOutputDatasets(toDatasetUrnArray(outputs))
                        .setInputDatajobs(toDataJobUrnArray(upstreamJobUrns));

                emit(emitter, MetadataChangeProposalWrapper.builder()
                        .entityType("dataJob")
                        .entityUrn(jobUrn)
                        .upsert()
                        .aspect(inputOutput)
                        .build());
            }

            log.info("[aileron] emit ok | run_end    flow={}  job={}  run={}  result={}",
                    job.getFlow(), job.getJobId(), job.getRunId().substring(0, 8), resultType);
        } catch (Exception e) {
            handleError("DataProcessInstance end emit failed", e);
        }
    }

    // ── helpers ───────────────────────────────────────────────────────────────

    private RestEmitter buildEmitter() {
        return RestEmitter.create(b -> b.server(props.getGmsUrl()));
    }

    private void emit(RestEmitter emitter, MetadataChangeProposalWrapper mcp) throws Exception {
        emitter.emit(mcp, null).get();
    }

    private DataFlowUrn flowUrn(JobContext job) throws URISyntaxException {
        return new DataFlowUrn(job.getPlatform(), job.getFlow(), props.getEnv());
    }

    private DataJobUrn jobUrn(JobContext job) throws URISyntaxException {
        return new DataJobUrn(flowUrn(job), job.getJobId());
    }

    private String instanceUrn(JobContext job) {
        return "urn:li:dataProcessInstance:" + job.getRunId();
    }

    private List<String> resolveUpstreamUrns(JobContext job) throws URISyntaxException {
        List<String> result = new java.util.ArrayList<>();
        for (String id : job.getUpstreamJobIds()) {
            if (id.startsWith("urn:li:")) {
                result.add(id);
            } else {
                DataFlowUrn fUrn = new DataFlowUrn(job.getPlatform(), job.getFlow(), props.getEnv());
                result.add(new DataJobUrn(fUrn, id).toString());
            }
        }
        return result;
    }

    private UrnArray toUrnArray(List<String> urns) {
        UrnArray arr = new UrnArray();
        for (String u : urns) {
            try { arr.add(Urn.createFromString(u)); } catch (URISyntaxException ignored) {}
        }
        return arr;
    }

    private DatasetUrnArray toDatasetUrnArray(List<String> urns) {
        DatasetUrnArray arr = new DatasetUrnArray();
        for (String u : urns) {
            try { arr.add(com.linkedin.common.urn.DatasetUrn.createFromString(u)); } catch (URISyntaxException ignored) {}
        }
        return arr;
    }

    private DataJobUrnArray toDataJobUrnArray(List<String> urns) {
        DataJobUrnArray arr = new DataJobUrnArray();
        for (String u : urns) {
            try { arr.add(DataJobUrn.createFromString(u)); } catch (URISyntaxException ignored) {}
        }
        return arr;
    }

    private void handleError(String msg, Exception e) {
        if (props.isSilentFail()) {
            log.warn("[aileron] {} (silent): {}", msg, e.getMessage());
        } else {
            throw new RuntimeException(msg, e);
        }
    }
}
