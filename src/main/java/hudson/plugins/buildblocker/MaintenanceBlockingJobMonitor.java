/*
 * The MIT License
 *
 * Copyright (C) 2017 Zodiac Interactive, LCC, Dmytro Iakovliev
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

import com.tikal.jenkins.plugins.multijob.MultiJobProject;
import hudson.model.AbstractProject;
import hudson.model.Job;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

public class MaintenanceBlockingJobMonitor {

    private JobFinder jobFinder;

    private static final Logger LOG = Logger.getLogger(MaintenanceBlockingJobMonitor.class.getName());

    public MaintenanceBlockingJobMonitor() {
        this.jobFinder = new JobFinder();
    }

    public Job checkForRunnedMaintenanceBuild(final BuildBlockerGlobalConfiguration config) {
        return jobFinder.findFirstRunBuild(new JobFinder.JobAcceptor() {
            @Override
            public boolean accept(Job job) {
                LOG.info(String.format("checkForRunnedMaintenanceBuild: Check %s for matching %s", job.getFullName(), config.getMaintenanceJobName()));
                return config.isMaintenanceJob(job.getFullName());
            }
        });
    }

    public Job checkForPlannedMaintenanceBuild(final BuildBlockerGlobalConfiguration config) {
        return jobFinder.findFirstPlannedBuild(new JobFinder.JobAcceptor() {
            @Override
            public boolean accept(Job job) {
                LOG.info(String.format("checkForPlannedMaintenanceBuild: Check %s for matching %s", job.getFullName(), config.getMaintenanceJobName()));
                return config.isMaintenanceJob(job.getFullName());
            }
        });
    }

    public Job checkForPlannedOrRunnedMaintenanceBuild(final BuildBlockerGlobalConfiguration config) {
        Job result = checkForPlannedMaintenanceBuild(config);
        if (result == null) {
            LOG.info(String.format("No planned build for regexp %s", config.getMaintenanceJobName()));
            result = checkForRunnedMaintenanceBuild(config);
            if (result != null) {
                LOG.info(String.format("Found run build %s", result.getFullName()));
            } else {
                LOG.info(String.format("No run build for regexp %s", config.getMaintenanceJobName()));
            }
        } else {
            LOG.info(String.format("Found planned build %s", result.getFullName()));
        }
        return result;
    }

    public Job checkForAnyRunnedBuild() {
        return jobFinder.findFirstRunBuild(null);
    }

    public boolean isPartOfRunnedMultijob(Job job) {
        boolean result = false;
        if (job instanceof AbstractProject) {
            List<MultiJobProject> runnedMultijobs = getAllRunnedMultijobs();
            for (MultiJobProject project: runnedMultijobs) {
                result = isPartOfMultijob(project, (AbstractProject) job);
                if (result) break;
            }
        }
        return result;
    }

    private boolean isPartOfMultijob(MultiJobProject multiJobProject, AbstractProject project) {
        boolean result;
        List<AbstractProject> subjobs = multiJobProject.getDownstreamProjects();
        result = subjobs.contains(project);
        if (!result) {
            for (Job subjob: subjobs) {
                if (subjob instanceof MultiJobProject) {
                    result = isPartOfMultijob((MultiJobProject)subjob, project);
                    if (result) break;
                }
            }
        }
        return result;
    }

    private List<MultiJobProject> getAllRunnedMultijobs() {
        List<MultiJobProject> result = new ArrayList<MultiJobProject>();
        List<Job> jobs = jobFinder.findAllRunBuilds(new JobFinder.JobAcceptor() {
            @Override
            public boolean accept(Job job) {
                return job instanceof MultiJobProject;
            }
        });
        for (Job job: jobs) {
            result.add((MultiJobProject)job);
        }
        return result;
    }

}
