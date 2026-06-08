package io.aileron.metacollector.aspect;

import io.aileron.metacollector.annotation.DatahubJob;
import io.aileron.metacollector.context.JobContext;
import io.aileron.metacollector.context.JobContextHolder;
import io.aileron.metacollector.emitter.DatahubEmitter;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.List;

@Aspect
@Component
public class DatahubJobAspect {

    private static final Logger log = LoggerFactory.getLogger(DatahubJobAspect.class);

    private final DatahubEmitter emitter;

    public DatahubJobAspect(DatahubEmitter emitter) {
        this.emitter = emitter;
    }

    @Around("@annotation(datahubJob)")
    public Object around(ProceedingJoinPoint pjp, DatahubJob datahubJob) throws Throwable {
        List<String> upstreamJobs = Arrays.asList(datahubJob.upstreamJobs());
        JobContext job = new JobContext(
                datahubJob.jobId(),
                datahubJob.flow(),
                datahubJob.platform(),
                upstreamJobs,
                datahubJob.description(),
                datahubJob.flowDescription()
        );

        JobContextHolder.set(job);
        log.info("[aileron] job started  | flow={}  job={}  run={}",
                job.getFlow(), job.getJobId(), job.getRunId().substring(0, 8));

        emitter.emitDataflowAsync(job);
        emitter.emitDatajobAsync(job);
        emitter.emitRunStartAsync(job);

        try {
            Object result = pjp.proceed();
            long elapsed = System.currentTimeMillis() - job.getStartTimeMs();
            log.info("[aileron] job finished | flow={}  job={}  run={}  elapsed={}ms  inputs={}  outputs={}",
                    job.getFlow(), job.getJobId(), job.getRunId().substring(0, 8),
                    elapsed, job.getInputs().size(), job.getOutputs().size());
            emitter.emitRunEndAsync(job, true, null, datahubJob.patch());
            return result;
        } catch (Throwable t) {
            long elapsed = System.currentTimeMillis() - job.getStartTimeMs();
            log.warn("[aileron] job failed   | flow={}  job={}  run={}  elapsed={}ms  error={}",
                    job.getFlow(), job.getJobId(), job.getRunId().substring(0, 8), elapsed, t.getMessage());
            emitter.emitRunEndAsync(job, false, t.getMessage(), datahubJob.patch());
            throw t;
        } finally {
            JobContextHolder.clear();
        }
    }
}
