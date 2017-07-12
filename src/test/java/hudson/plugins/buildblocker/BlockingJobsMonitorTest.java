/*
 * The MIT License
 *
 * Copyright (c) 2004-2011, Sun Microsystems, Inc., Frederik Fromm
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */

package hudson.plugins.buildblocker;

import hudson.model.FreeStyleBuild;
import hudson.model.FreeStyleProject;
import hudson.model.labels.LabelAtom;
import hudson.slaves.DumbSlave;
import hudson.slaves.SlaveComputer;
import hudson.tasks.Shell;
import jenkins.model.Jenkins;
import org.jenkinsci.plugins.workflow.cps.CpsFlowDefinition;
import org.jenkinsci.plugins.workflow.job.WorkflowJob;
import org.jenkinsci.plugins.workflow.job.WorkflowRun;
import org.junit.Rule;
import org.junit.Test;
import org.jvnet.hudson.test.JenkinsRule;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static org.junit.Assert.*;

/**
 * Unit tests
 */
public class BlockingJobsMonitorTest {

    @Rule
    public JenkinsRule j = new JenkinsRule();

    private String blockingJobName;

    private Future<FreeStyleBuild> future;
    private Future<WorkflowRun> futureWorkflow;

    private BuildBlockerGlobalConfiguration globalConfig;

    protected void FreeStyleSetUp() throws Exception {
        blockingJobName = "blockingJob";

        // clear queue from preceding tests
        Jenkins.getInstance().getQueue().clear();

        // init slave
        DumbSlave slave = j.createSlave();
        slave.setLabelString("label");

        SlaveComputer c = slave.getComputer();
        c.connect(false).get(); // wait until it's connected

        FreeStyleProject blockingProject = j.createFreeStyleProject(blockingJobName);
        blockingProject.setAssignedLabel(new LabelAtom("label"));

        Shell shell = new Shell("sleep 1");
        blockingProject.getBuildersList().add(shell);

        future = blockingProject.scheduleBuild2(0);

        // wait until blocking job started
        while (!slave.getComputer().getExecutors().get(0).isBusy()) {
            TimeUnit.SECONDS.sleep(1);
        }
    }

    protected void WorkFlowSetUp() throws Exception {
        blockingJobName = "blockingJob";

        // clear queue from preceding tests
        Jenkins.getInstance().getQueue().clear();

        // init slave
        DumbSlave slave = j.createSlave();
        slave.setLabelString("label");

        SlaveComputer c = slave.getComputer();
        c.connect(false).get(); // wait until it's connected

        WorkflowJob workflowBlockingProject = j.jenkins.createProject(WorkflowJob.class, blockingJobName);
        workflowBlockingProject.setDefinition(new CpsFlowDefinition("node('label') { sleep 10}"));

        futureWorkflow = workflowBlockingProject.scheduleBuild2(0);

        // wait until blocking job started
        while (!slave.getComputer().getExecutors().get(0).isBusy()) {
            TimeUnit.SECONDS.sleep(1);
        }
    }

    @Test
    public void testNullMonitorDoesNotBlockWithFreeSytle() throws Exception {
        FreeStyleSetUp();
        BlockingJobsMonitor blockingJobsMonitorUsingNull = new BlockingJobsMonitor(null);
        assertNull(blockingJobsMonitorUsingNull.checkAllNodesForRunningBuilds());
        assertNull(blockingJobsMonitorUsingNull.checkForBuildableQueueEntries(null));
        // wait until blocking job stopped
        while (!future.isDone()) {
            TimeUnit.SECONDS.sleep(1);
        }
    }

    @Test
    public void testNullMonitorDoesNotBlockWithWorkflow() throws Exception {
        WorkFlowSetUp();
        BlockingJobsMonitor blockingJobsMonitorUsingNull = new BlockingJobsMonitor(null);
        assertNull(blockingJobsMonitorUsingNull.checkAllNodesForRunningBuilds());
        assertNull(blockingJobsMonitorUsingNull.checkForBuildableQueueEntries(null));
        // wait until blocking job stopped
        while (!futureWorkflow.isDone()) {
            TimeUnit.SECONDS.sleep(1);
        }
    }

    @Test
    public void testNonMatchingMonitorDoesNotBlockWithFreeSytle() throws Exception {
        FreeStyleSetUp();
        BlockingJobsMonitor blockingJobsMonitorNotMatching = new BlockingJobsMonitor("xxx");
        assertNull(blockingJobsMonitorNotMatching.checkAllNodesForRunningBuilds());
        assertNull(blockingJobsMonitorNotMatching.checkForBuildableQueueEntries(null));
        // wait until blocking job stopped
        while (!future.isDone()) {
            TimeUnit.SECONDS.sleep(1);
        }
    }

    @Test
    public void testNonMatchingMonitorDoesNotBlockWithWorkflow() throws Exception {
        WorkFlowSetUp();
        BlockingJobsMonitor blockingJobsMonitorNotMatching = new BlockingJobsMonitor("xxx");
        assertNull(blockingJobsMonitorNotMatching.checkAllNodesForRunningBuilds());
        assertNull(blockingJobsMonitorNotMatching.checkForBuildableQueueEntries(null));
        // wait until blocking job stopped
        while (!futureWorkflow.isDone()) {
            TimeUnit.SECONDS.sleep(1);
        }
    }

    @Test
    public void testMatchingMonitorReturnsBlockingJobsDisplayNameWithFreeSytle() throws Exception {
        FreeStyleSetUp();
        BlockingJobsMonitor blockingJobsMonitorUsingFullName = new BlockingJobsMonitor(blockingJobName);

        assertEquals(blockingJobName, blockingJobsMonitorUsingFullName.checkAllNodesForRunningBuilds().getDisplayName
                ());
        assertNull(blockingJobsMonitorUsingFullName.checkForBuildableQueueEntries(null));
        // wait until blocking job stopped
        while (!future.isDone()) {
            TimeUnit.SECONDS.sleep(1);
        }
    }

    @Test
    public void testMatchingMonitorReturnsBlockingJobsDisplayNameWithWorkflow() throws Exception {
        WorkFlowSetUp();
        BlockingJobsMonitor blockingJobsMonitorUsingFullName = new BlockingJobsMonitor(blockingJobName);

        assertEquals(blockingJobName, blockingJobsMonitorUsingFullName.checkAllNodesForRunningBuilds().getDisplayName
                ());
        assertNull(blockingJobsMonitorUsingFullName.checkForBuildableQueueEntries(null));
        // wait until blocking job stopped
        while (!futureWorkflow.isDone()) {
            TimeUnit.SECONDS.sleep(1);
        }
    }

    @Test
    public void testMonitorBlocksBasedOnRegExWithFreeSytle() throws Exception {
        FreeStyleSetUp();
        BlockingJobsMonitor blockingJobsMonitorUsingRegex = new BlockingJobsMonitor("block.*");
        assertEquals(blockingJobName, blockingJobsMonitorUsingRegex.checkAllNodesForRunningBuilds().getDisplayName());
        assertNull(blockingJobsMonitorUsingRegex.checkForBuildableQueueEntries(null));
        // wait until blocking job stopped
        while (!future.isDone()) {
            TimeUnit.SECONDS.sleep(1);
        }
    }

    @Test
    public void testMonitorBlocksBasedOnRegExWitWorkflow() throws Exception {
        WorkFlowSetUp();
        BlockingJobsMonitor blockingJobsMonitorUsingRegex = new BlockingJobsMonitor("block.*");
        assertEquals(blockingJobName, blockingJobsMonitorUsingRegex.checkAllNodesForRunningBuilds().getDisplayName());
        assertNull(blockingJobsMonitorUsingRegex.checkForBuildableQueueEntries(null));
        // wait until blocking job stopped
        while (!futureWorkflow.isDone()) {
            TimeUnit.SECONDS.sleep(1);
        }
    }

    @Test
    public void testMonitorBlocksIfConfiguredWithSeveralProjectnamesWithFreeSytle() throws Exception {
        FreeStyleSetUp();
        BlockingJobsMonitor blockingJobsMonitorUsingMoreLines = new BlockingJobsMonitor("xxx\nblock.*\nyyy");
        assertEquals(blockingJobName, blockingJobsMonitorUsingMoreLines.checkAllNodesForRunningBuilds()
                .getDisplayName());
        assertNull(blockingJobsMonitorUsingMoreLines.checkForBuildableQueueEntries(null));
        // wait until blocking job stopped
        while (!future.isDone()) {
            TimeUnit.SECONDS.sleep(1);
        }
    }

   @Test
    public void testMonitorBlocksIfConfiguredWithSeveralProjectnamesWithWorkflow() throws Exception {
        WorkFlowSetUp();
        BlockingJobsMonitor blockingJobsMonitorUsingMoreLines = new BlockingJobsMonitor("xxx\nblock.*\nyyy");
        assertEquals(blockingJobName, blockingJobsMonitorUsingMoreLines.checkAllNodesForRunningBuilds()
                .getDisplayName());
        assertNull(blockingJobsMonitorUsingMoreLines.checkForBuildableQueueEntries(null));
       // wait until blocking job stopped
       while (!futureWorkflow.isDone()) {
           TimeUnit.SECONDS.sleep(1);
       }
    }

    @Test
    public void testMonitorDoesNotBlockIfRegexDoesNotMatchWithFreeSytle() throws Exception {
        FreeStyleSetUp();
        BlockingJobsMonitor blockingJobsMonitorUsingWrongRegex = new BlockingJobsMonitor("*BW2S.*QRT.");
        assertNull(blockingJobsMonitorUsingWrongRegex.checkAllNodesForRunningBuilds());
        assertNull(blockingJobsMonitorUsingWrongRegex.checkForBuildableQueueEntries(null));
        // wait until blocking job stopped
        while (!future.isDone()) {
            TimeUnit.SECONDS.sleep(1);
        }
    }

    @Test
    public void testMonitorDoesNotBlockIfRegexDoesNotMatchWithWorkflow() throws Exception {
        WorkFlowSetUp();
        BlockingJobsMonitor blockingJobsMonitorUsingWrongRegex = new BlockingJobsMonitor("*BW2S.*QRT.");
        assertNull(blockingJobsMonitorUsingWrongRegex.checkAllNodesForRunningBuilds());
        assertNull(blockingJobsMonitorUsingWrongRegex.checkForBuildableQueueEntries(null));
        // wait until blocking job stopped
        while (!futureWorkflow.isDone()) {
            TimeUnit.SECONDS.sleep(1);
        }
    }

    private void setupMaintenanceConfig() {
        globalConfig = BuildBlockerGlobalConfiguration.get();
        globalConfig.setMaintenanceJobEnabled(true);
        globalConfig.setMaintenanceJobName("JENKINS_MAINTENANCE_JOB");
        globalConfig.save();
    }

    private Future<FreeStyleBuild> MaintenanceSetUp() throws Exception {
        // clear queue from preceding tests
        Jenkins.getInstance().getQueue().clear();

        setupMaintenanceConfig();

        // init slave
        DumbSlave slave = j.createSlave();
        slave.setLabelString("label");

        SlaveComputer c = slave.getComputer();
        c.connect(false).get(); // wait until it's connected

        FreeStyleProject blockingProject = j.createFreeStyleProject(globalConfig.getMaintenanceJobName());
        blockingProject.setAssignedLabel(new LabelAtom("label"));
        Shell shell = new Shell("echo start maintenance; sleep 10");
        blockingProject.getBuildersList().add(shell);

        future = blockingProject.scheduleBuild2(0);

        FreeStyleProject blockedProject = j.createFreeStyleProject("BLOCKED_PROJECT");
        blockedProject.setAssignedLabel(new LabelAtom("label"));
        Shell shell1 = new Shell("echo start blocked; sleep 10");
        blockedProject.getBuildersList().add(shell1);

        // wait until blocking job started
        while (!slave.getComputer().getExecutors().get(0).isBusy()) {
            TimeUnit.SECONDS.sleep(1);
        }

        return blockedProject.scheduleBuild2(0);
    }

    @Test
    public void testMaintenanceBlockAnyOtherJobs() throws Exception {
        Future<FreeStyleBuild> blocked = MaintenanceSetUp();
        BlockingJobsMonitor blockingJobsMonitor = new BlockingJobsMonitor();
        assertNotNull(blockingJobsMonitor.checkForPlannedOrRunnedBuild(globalConfig.getMaintenanceJobName()));
        assertNotNull(blockingJobsMonitor.checkForPlannedBuild("BLOCKED_PROJECT"));
        assertNull(blockingJobsMonitor.checkForRunnedBuild("BLOCKED_PROJECT"));
        assertNotNull(blockingJobsMonitor.checkForAnyRunnedBuild());
        // wait until maintenance job stopped
        while (!future.isDone()) {
            TimeUnit.SECONDS.sleep(1);
        }
        assertNotNull(blockingJobsMonitor.checkForAnyRunnedBuild());
        assertNull(blockingJobsMonitor.checkForPlannedBuild("BLOCKED_PROJECT"));
        assertNotNull(blockingJobsMonitor.checkForRunnedBuild("BLOCKED_PROJECT"));
        // wait until blocked job stopped
        while (!blocked.isDone()) {
            TimeUnit.SECONDS.sleep(1);
        }
        assertNull(blockingJobsMonitor.checkForAnyRunnedBuild());
    }

    private Future<FreeStyleBuild> MaintenanceSetUp2() throws Exception {
        // clear queue from preceding tests
        Jenkins.getInstance().getQueue().clear();

        setupMaintenanceConfig();

        // init slave
        DumbSlave slave = j.createSlave();
        slave.setLabelString("label");

        SlaveComputer c = slave.getComputer();
        c.connect(false).get(); // wait until it's connected

        FreeStyleProject blockingProject = j.createFreeStyleProject("BLOCKING_PROJECT");
        blockingProject.setAssignedLabel(new LabelAtom("label"));
        Shell shell = new Shell("echo start blocking; sleep 10");
        blockingProject.getBuildersList().add(shell);

        FreeStyleProject blockedProject = j.createFreeStyleProject(globalConfig.getMaintenanceJobName());
        blockedProject.setAssignedLabel(new LabelAtom("label"));
        Shell shell1 = new Shell("echo start maintenance; sleep 10");
        blockedProject.getBuildersList().add(shell1);

        future = blockingProject.scheduleBuild2(0);

        // wait until blocking job started
        while (!slave.getComputer().getExecutors().get(0).isBusy()) {
            TimeUnit.SECONDS.sleep(1);
        }

        return blockedProject.scheduleBuild2(0);
    }

    @Test
    public void testMaintenance2BlockAnyOtherJobs() throws Exception {
        Future<FreeStyleBuild> blocked = MaintenanceSetUp2();
        BlockingJobsMonitor blockingJobsMonitor = new BlockingJobsMonitor();
        assertNull(blockingJobsMonitor.checkForPlannedBuild("BLOCKING_PROJECT"));
        assertNotNull(blockingJobsMonitor.checkForRunnedBuild("BLOCKING_PROJECT"));
        assertNotNull(blockingJobsMonitor.checkForPlannedBuild(globalConfig.getMaintenanceJobName()));
        assertNull(blockingJobsMonitor.checkForRunnedBuild(globalConfig.getMaintenanceJobName()));
        assertNotNull(blockingJobsMonitor.checkForPlannedOrRunnedBuild(globalConfig.getMaintenanceJobName()));
        assertNotNull(blockingJobsMonitor.checkForAnyRunnedBuild());
        // wait until blocked job stopped
        while (!future.isDone()) {
            TimeUnit.SECONDS.sleep(1);
        }
        assertNotNull(blockingJobsMonitor.checkForAnyRunnedBuild());
        assertNull(blockingJobsMonitor.checkForPlannedBuild(globalConfig.getMaintenanceJobName()));
        assertNotNull(blockingJobsMonitor.checkForRunnedBuild(globalConfig.getMaintenanceJobName()));
        assertNotNull(blockingJobsMonitor.checkForPlannedOrRunnedBuild(globalConfig.getMaintenanceJobName()));
        // wait until blocked job stopped
        while (!blocked.isDone()) {
            TimeUnit.SECONDS.sleep(1);
        }
        assertNull(blockingJobsMonitor.checkForAnyRunnedBuild());
    }

    private List<DumbSlave> createSlaves(int count, final String label) throws Exception {
        List<DumbSlave> result = new ArrayList<DumbSlave>();
        for (int i = 0; i < count; ++i) {
            DumbSlave slave = j.createSlave();
            slave.setLabelString(label);
            SlaveComputer c = slave.getComputer();
            c.connect(false).get();
            result.add(slave);
        }
        return result;
    }

    private FreeStyleProject createProject(final String buildName, final String assignLabel) throws IOException {
        FreeStyleProject project = j.createFreeStyleProject(buildName);
        project.setAssignedLabel(new LabelAtom(assignLabel));
        Shell shell = new Shell(String.format("echo 'start %s'; sleep 10", buildName));
        project.getBuildersList().add(shell);
        return project;
    }

    private List<FreeStyleProject> createProjects(int count, final String baseBuildName, final String assignLabel) throws IOException {
        List<FreeStyleProject> result = new ArrayList<FreeStyleProject>();
        for (int i = 0; i < count; ++i) {
            result.add(createProject(String.format("%s_%d", baseBuildName, i), assignLabel));
        }
        return result;
    }

    private List<Future<FreeStyleBuild>> scheduleBuilds(List<FreeStyleProject> projects) {
        List<Future<FreeStyleBuild>> futures = new ArrayList<Future<FreeStyleBuild>>();
        for (FreeStyleProject project: projects) {
            futures.add(project.scheduleBuild2(0));
        }
        return futures;
    }

    private void waitAllForDone(List<Future<FreeStyleBuild>> futures) throws InterruptedException {
        for (Future<FreeStyleBuild> future: futures) {
            waitForDone(future);
        }
    }

    private void waitForDone(Future<FreeStyleBuild> future) throws InterruptedException {
        while (!future.isDone()) {
            TimeUnit.SECONDS.sleep(1);
        }
    }

    @Test
    public void testMaintenanceSetUpExt() throws Exception {
        Jenkins.getInstance().getQueue().clear();

        setupMaintenanceConfig();

        List<DumbSlave> slaves = createSlaves(5, "label");

        List<FreeStyleProject> projects = createProjects(5, "REGULAR_BUILD", "label");

        FreeStyleProject maintenance = createProject(globalConfig.getMaintenanceJobName(), "label");

        List<Future<FreeStyleBuild>> regularBuilds = scheduleBuilds(projects);

        TimeUnit.SECONDS.sleep(3);

        Future<FreeStyleBuild> maintenanceBuild = maintenance.scheduleBuild2(0);
        regularBuilds.add(maintenanceBuild);

        waitAllForDone(regularBuilds);
    }

    @Test
    public void testMaintenanceSetUpExt2() throws Exception {
        Jenkins.getInstance().getQueue().clear();

        setupMaintenanceConfig();

        List<DumbSlave> slaves = createSlaves(5, "label");

        List<FreeStyleProject> projects = createProjects(5, "REGULAR_BUILD", "label");

        FreeStyleProject maintenance = createProject(globalConfig.getMaintenanceJobName(), "label");

        Future<FreeStyleBuild> maintenanceBuild = maintenance.scheduleBuild2(0);

        TimeUnit.SECONDS.sleep(3);

        List<Future<FreeStyleBuild>> regularBuilds = scheduleBuilds(projects);

        regularBuilds.add(maintenanceBuild);

        waitAllForDone(regularBuilds);
    }

}
