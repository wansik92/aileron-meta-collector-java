package io.aileron.metacollector;

import io.aileron.metacollector.annotation.DatahubJob;
import io.aileron.metacollector.config.DatahubAutoConfiguration;
import io.aileron.metacollector.context.DatahubLineage;
import io.aileron.metacollector.context.JobContext;
import io.aileron.metacollector.context.JobContextHolder;
import io.aileron.metacollector.emitter.DatahubEmitter;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.stereotype.Service;

import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.verify;

@SpringBootTest(classes = DatahubJobAspectTest.TestService.class)
@Import(DatahubAutoConfiguration.class)
class DatahubJobAspectTest {

    @MockBean
    DatahubEmitter emitter;

    @Autowired
    TestService svc;

    @BeforeEach
    void setUp() {
        JobContextHolder.clear();
    }

    @Test
    void jobContext_isSetDuringExecution() {
        AtomicReference<JobContext> captured = new AtomicReference<>();
        svc.captureContext(captured);
        assertThat(captured.get()).isNotNull();
        assertThat(captured.get().getJobId()).isEqualTo("test-job");
        assertThat(captured.get().getFlow()).isEqualTo("test-flow");
    }

    @Test
    void jobContext_isClearedAfterExecution() {
        svc.simple();
        assertThat(JobContextHolder.get()).isNull();
    }

    @Test
    void jobContext_isClearedOnException() {
        assertThatThrownBy(() -> svc.failing())
                .isInstanceOf(RuntimeException.class);
        assertThat(JobContextHolder.get()).isNull();
    }

    @Test
    void emitRunEnd_calledWithSuccess() {
        svc.simple();
        verify(emitter).emitRunEndAsync(any(), eq(true), isNull(), eq(false));
    }

    @Test
    void emitRunEnd_calledWithFailureOnException() {
        assertThatThrownBy(() -> svc.failing());
        verify(emitter).emitRunEndAsync(any(), eq(false), anyString(), eq(false));
    }

    @Test
    void lineage_inputOutputCaptured() {
        AtomicReference<JobContext> captured = new AtomicReference<>();
        svc.withLineage(captured);
        assertThat(captured.get().getInputs()).containsExactly("urn:li:dataset:(urn:li:dataPlatform:s3,raw,PROD)");
        assertThat(captured.get().getOutputs()).containsExactly("urn:li:dataset:(urn:li:dataPlatform:glue,out,PROD)");
    }

    @Service
    static class TestService {

        @DatahubJob(jobId = "test-job", flow = "test-flow")
        public void simple() {}

        @DatahubJob(jobId = "test-job", flow = "test-flow")
        public void captureContext(AtomicReference<JobContext> ref) {
            ref.set(JobContextHolder.get());
        }

        @DatahubJob(jobId = "test-job", flow = "test-flow")
        public void failing() {
            throw new RuntimeException("oops");
        }

        @DatahubJob(jobId = "test-job", flow = "test-flow")
        public void withLineage(AtomicReference<JobContext> ref) {
            DatahubLineage.addInput("s3", "raw", "PROD");
            DatahubLineage.addOutput("glue", "out", "PROD");
            ref.set(JobContextHolder.get());
        }
    }
}
