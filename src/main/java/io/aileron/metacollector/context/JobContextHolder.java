package io.aileron.metacollector.context;

/**
 * 현재 스레드의 JobContext를 보관합니다. Python의 threading.local() 과 동일한 역할.
 */
public class JobContextHolder {

    private static final ThreadLocal<JobContext> CONTEXT = new ThreadLocal<>();

    public static void set(JobContext ctx) { CONTEXT.set(ctx); }
    public static JobContext get() { return CONTEXT.get(); }
    public static void clear() { CONTEXT.remove(); }

    /** 현재 스레드에 활성 job이 있으면 input URN을 추가합니다. */
    public static void addInput(String urn) {
        JobContext ctx = CONTEXT.get();
        if (ctx != null) ctx.addInput(urn);
    }

    /** 현재 스레드에 활성 job이 있으면 output URN을 추가합니다. */
    public static void addOutput(String urn) {
        JobContext ctx = CONTEXT.get();
        if (ctx != null) ctx.addOutput(urn);
    }
}
