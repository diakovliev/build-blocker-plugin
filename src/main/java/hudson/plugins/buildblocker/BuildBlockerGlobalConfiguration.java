package hudson.plugins.buildblocker;

import hudson.Extension;
import jenkins.model.GlobalConfiguration;
import net.sf.json.JSONObject;
import org.kohsuke.stapler.StaplerRequest;

import org.kohsuke.stapler.DataBoundConstructor;

/**
 * Created by diakovliev on 7/12/17.
 */
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
        return GlobalConfiguration.all().get(BuildBlockerGlobalConfiguration.class);
    }

}
