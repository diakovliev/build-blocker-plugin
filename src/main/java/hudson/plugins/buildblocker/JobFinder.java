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

import java.util.List;

import static java.util.Arrays.asList;

public class JobFinder {

    public interface JobAcceptor {
        boolean accept(Job job);
    }

    public Job findFirstPlannedBuild(JobAcceptor acceptor) {
        List<Queue.Item> buildableItems = asList(Jenkins.getInstance().getQueue().getItems());
        for (Queue.Item buildableItem : buildableItems) {
            if (buildableItem.task instanceof Job) {
                Job project = (Job) buildableItem.task;
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

    public Job findFirstRunBuild(JobAcceptor acceptor) {
        Computer[] computers = Jenkins.getInstance().getComputers();
        for (Computer computer : computers) {
            Job task = findComputerRunBuild(computer, acceptor);
            if (task != null) {
                return task;
            }
        }
        return null;
    }

    private Job findComputerRunBuild(Computer computer, JobAcceptor acceptor) {
        List<Executor> executors = computer.getExecutors();
        executors.addAll(computer.getOneOffExecutors());
        for (Executor executor : executors) {
            Job task = checkExecutorForRunBuild(executor, acceptor);
            if (task != null) {
                return task;
            }
        }
        return null;
    }

    private Job checkExecutorForRunBuild(Executor executor, JobAcceptor acceptor) {
        if (executor.isBusy()) {
            Queue.Task task = executor.getCurrentWorkUnit().work.getOwnerTask();
            if (task instanceof MatrixConfiguration) {
                task = ((MatrixConfiguration) task).getParent();
            }
            if (task instanceof Job) {
                Job job = (Job) task;
                if (acceptor != null) {
                    if (acceptor.accept(job)) {
                        return job;
                    }
                } else {
                    return job;
                }
            }
        }
        return null;
    }

}
