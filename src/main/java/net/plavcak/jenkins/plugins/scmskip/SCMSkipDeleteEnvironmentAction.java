package net.plavcak.jenkins.plugins.scmskip;

import edu.umd.cs.findbugs.annotations.CheckForNull;
import edu.umd.cs.findbugs.annotations.NonNull;
import hudson.EnvVars;
import hudson.model.EnvironmentContributingAction;
import hudson.model.Run;

public class SCMSkipDeleteEnvironmentAction implements EnvironmentContributingAction {

    private transient boolean deleteBuild;

    public SCMSkipDeleteEnvironmentAction() {}

    public SCMSkipDeleteEnvironmentAction(boolean deleteBuild) {
        this.deleteBuild = deleteBuild;
    }

    public boolean isDeleteBuild() {
        return deleteBuild;
    }

    public void setDeleteBuild(boolean deleteBuild) {
        this.deleteBuild = deleteBuild;
    }

    @CheckForNull
    @Override
    public String getIconFileName() {
        return null;
    }

    @CheckForNull
    @Override
    public String getDisplayName() {
        return "Delete Build After Completed";
    }

    @CheckForNull
    @Override
    public String getUrlName() {
        return null;
    }

    @Override
    public void buildEnvironment(@NonNull Run<?, ?> run, @NonNull EnvVars env) {
        env.put(SCMSkipConstants.DELETE_BUILD, String.valueOf(deleteBuild));
    }
}
