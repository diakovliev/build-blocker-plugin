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

import hudson.matrix.MatrixConfiguration;
import hudson.model.Computer;
import hudson.model.Executor;
import hudson.model.Job;
import hudson.model.Queue;
import jenkins.model.Jenkins;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

import static java.util.Arrays.asList;

public class JobFinder {

    private static final Logger LOG = Logger.getLogger(JobFinder.class.getName());

    public interface JobAcceptor {
        boolean accept(Job job);
    }

    public Job findFirstPlannedBuild(JobAcceptor acceptor) {
        List<? extends Queue.Item> buildableItems = asList(Jenkins.getInstance().getQueue().getItems());
        for (Queue.Item buildableItem : buildableItems) {
            if (buildableItem.task instanceof Job) {
                LOG.fine("Queue item is a job");
                Job job = (Job) buildableItem.task;
                if (acceptor != null) {
                    LOG.fine("Acceptor found");
                    if (acceptor.accept(job)) {
                        LOG.fine(String.format("Accept %s", job.getDisplayName()));
                        return job;
                    } else {
                        LOG.fine(String.format("Not accepted %s", job.getDisplayName()));
                    }
                } else {
                    LOG.fine(String.format("No acceptor, accept %s", job.getDisplayName()));
                    return job;
                }
            } else {
                LOG.fine(String.format("Queue item task is not Job. %s", buildableItem.getDisplayName()));
            }
        }
        return null;
    }

    public List<Job> findAllRunBuilds(JobAcceptor acceptor) {
        List<Job> result = new ArrayList<Job>();
        Computer[] computers = Jenkins.getInstance().getComputers();
        for (Computer computer : computers) {
            List<Job> tasks = findComputerAllRunBuilds(computer, acceptor);
            if (!tasks.isEmpty()) {
                result.addAll(tasks);
            }
        }
        return result;
    }

    public Job findFirstRunBuild(JobAcceptor acceptor) {
        Computer[] computers = Jenkins.getInstance().getComputers();
        for (Computer computer : computers) {
            Job job = findComputerRunBuild(computer, acceptor);
            if (job != null) {
                return job;
            }
        }
        return null;
    }

    private List<Job> findComputerAllRunBuilds(Computer computer, JobAcceptor acceptor) {
        List<Job> result = new ArrayList<Job>();
        List<Executor> executors = computer.getExecutors();
        executors.addAll(computer.getOneOffExecutors());
        for (Executor executor : executors) {
            Job job = checkExecutorForRunBuild(executor, acceptor);
            if (job != null) {
                result.add(job);
            }
        }
        return result;
    }

    private Job findComputerRunBuild(Computer computer, JobAcceptor acceptor) {
        List<Executor> executors = computer.getExecutors();
        executors.addAll(computer.getOneOffExecutors());
        for (Executor executor : executors) {
            Job job = checkExecutorForRunBuild(executor, acceptor);
            if (job != null) {
                return job;
            }
        }
        return null;
    }

    private Job checkExecutorForRunBuild(Executor executor, JobAcceptor acceptor) {
        if (executor.isBusy()) {
            Queue.Task job = executor.getCurrentWorkUnit().work.getOwnerTask();
            if (job instanceof MatrixConfiguration) {
                job = ((MatrixConfiguration) job).getParent();
            }
            if (job instanceof Job) {
                Job project = (Job) job;
                if (acceptor != null) {
                    if (acceptor.accept(project)) {
                        return project;
                    }
                } else {
                    return project;
                }
            }
        }
        return null;
    }

}
