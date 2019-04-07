package net.plavcak.jenkins.plugins.scmskip;

import hudson.EnvVars;
import hudson.model.AbstractBuild;
import hudson.model.EnvironmentContributingAction;
import hudson.model.Run;

import javax.annotation.CheckForNull;

public class SCMSkipDeleteEnvironmentAction implements EnvironmentContributingAction {

    private transient boolean deleteBuild;

    public SCMSkipDeleteEnvironmentAction() {
    }

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
    public void buildEnvVars(AbstractBuild<?, ?> build, EnvVars env) {
        env.put(SCMSkipConstants.DELETE_BUILD, String.valueOf(deleteBuild));
    }

    @Override
    public void buildEnvironment(Run<?,?> run, EnvVars env) {
        env.put(SCMSkipConstants.DELETE_BUILD, String.valueOf(deleteBuild));
    }
}
