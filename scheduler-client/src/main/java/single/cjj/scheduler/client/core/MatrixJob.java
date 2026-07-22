package single.cjj.scheduler.client.core;

@FunctionalInterface
public interface MatrixJob {

    JobResult execute(JobContext context) throws Exception;
}
