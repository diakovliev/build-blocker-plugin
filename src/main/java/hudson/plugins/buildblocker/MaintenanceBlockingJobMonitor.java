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
import hudson.model.Job;

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
                return config.isMaintenanceJob(job.getFullName());
            }
        });
    }

    public Job checkForPlannedMaintenanceBuild(final BuildBlockerGlobalConfiguration config) {
        return jobFinder.findFirstPlannedBuild(new JobFinder.JobAcceptor() {
            @Override
            public boolean accept(Job job) {
                return config.isMaintenanceJob(job.getFullName());
            }
        });
    }

    public Job checkForPlannedOrRunnedMaintenanceBuild(final BuildBlockerGlobalConfiguration config) {
        Job result = checkForPlannedMaintenanceBuild(config);
        if (result == null) {
            result = checkForRunnedMaintenanceBuild(config);
            if (result != null) {
                LOG.info(String.format("Found runned build %s", result.getFullName()));
            }
        } else {
            LOG.info(String.format("Found planned build %s", result.getFullName()));
        }
        return result;
    }

    public Job checkForAnyRunnedBuild() {
        return jobFinder.findFirstRunBuild(null);
    }

    public Job checkForAnyRunnedMultijob() {
        return jobFinder.findFirstRunBuild(new JobFinder.JobAcceptor() {
            @Override
            public boolean accept(Job job) {
                return job instanceof MultiJobProject;
            }
        });
    }

}
