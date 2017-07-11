package hudson.plugins.buildblocker;

public class DefaultMonitorFactory implements MonitorFactory {
    @Override
    public BlockingJobsMonitor build(String blockingJobs) {
        return new BlockingJobsMonitor(blockingJobs);
    }
    @Override
    public BlockingJobsMonitor build() {
        return new BlockingJobsMonitor();
    }
}
