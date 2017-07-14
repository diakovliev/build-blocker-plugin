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

import hudson.Extension;
import jenkins.model.GlobalConfiguration;
import net.sf.json.JSONObject;
import org.kohsuke.stapler.StaplerRequest;

import org.kohsuke.stapler.DataBoundConstructor;

@Extension
public class BuildBlockerGlobalConfiguration extends GlobalConfiguration {

    private boolean maintenanceJobEnabled = false;
    private String maintenanceJobName = "";

    public BuildBlockerGlobalConfiguration() {
        load();
    }

    @DataBoundConstructor
    public BuildBlockerGlobalConfiguration(boolean maintenanceJobEnabled, String maintenanceJobName) {
        this.maintenanceJobEnabled = maintenanceJobEnabled;
        this.maintenanceJobName = maintenanceJobName;
    }

    public boolean isMaintenanceJobEnabled() {
        return maintenanceJobEnabled && !getMaintenanceJobName().isEmpty();
    }

    public void setMaintenanceJobEnabled(boolean maintenanceJobEnabled) {
        this.maintenanceJobEnabled = maintenanceJobEnabled;
    }

    public void setMaintenanceJobName(final String maintenanceJobName) {
        this.maintenanceJobName = maintenanceJobName;
    }

    public String getMaintenanceJobName() {
        return maintenanceJobName.trim();
    }

    @Override
    public boolean configure(StaplerRequest req, JSONObject json) throws FormException {
        maintenanceJobEnabled = json.getBoolean("maintenanceJobEnabled");
        maintenanceJobName = json.getString("maintenanceJobName");
        save();
        return true;
    }

    public static BuildBlockerGlobalConfiguration get() {
        BuildBlockerGlobalConfiguration globalConfig = null;
        try {
            globalConfig = GlobalConfiguration.all().get(BuildBlockerGlobalConfiguration.class);
        } catch (Exception e) {}
        return globalConfig;
    }

    public boolean isMaintenanceJob(final String jobNameToTest) {
        if (jobNameToTest == null) return false;
        return jobNameToTest.matches(maintenanceJobName);
    }
}
