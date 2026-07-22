package single.cjj.scheduler.client.core;

@FunctionalInterface
public interface JobProgressReporter {

    JobProgressReporter NOOP = (progress, stage, message) -> { };

    void report(int progress, String stage, String message);
}
