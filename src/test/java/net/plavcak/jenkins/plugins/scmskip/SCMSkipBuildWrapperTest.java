package net.plavcak.jenkins.plugins.scmskip;

import hudson.model.FreeStyleProject;
import hudson.model.Label;
import hudson.tasks.BuildWrapperDescriptor;
import org.jenkinsci.plugins.workflow.cps.CpsFlowDefinition;
import org.jenkinsci.plugins.workflow.job.WorkflowJob;
import org.jenkinsci.plugins.workflow.job.WorkflowRun;
import org.junit.Rule;
import org.junit.Test;
import org.jvnet.hudson.test.JenkinsRule;

public class SCMSkipBuildWrapperTest {

    @Rule
    public JenkinsRule jenkins = new JenkinsRule();

    @Test
    public void testConfigRoundtrip() throws Exception {
        FreeStyleProject project = jenkins.createFreeStyleProject();

        BuildWrapperDescriptor descriptor = new SCMSkipBuildWrapper.DescriptorImpl();

        project.getBuildWrappersList().add(
                new SCMSkipBuildWrapper(false, null));

        project = jenkins.configRoundtrip(project);

        jenkins.assertEqualDataBoundBeans(
                new SCMSkipBuildWrapper(false, null),
                project.getBuildWrappers().get(descriptor));
    }

    @Test
    public void testScriptedPipeline() throws Exception {
        String agentLabel = "test-agent";
        jenkins.createOnlineSlave(Label.get(agentLabel));
        WorkflowJob job = jenkins.createProject(WorkflowJob.class, "test-scripted-pipeline");
        String pipelineScript
                = "node {\n"
                + "  scmSkip() \n"
                + "}";
        job.setDefinition(new CpsFlowDefinition(pipelineScript, true));
        WorkflowRun completedBuild = jenkins.assertBuildStatusSuccess(job.scheduleBuild2(0));
        String expectedString = "SCM Skip: Changelog is empty!";
        jenkins.assertLogContains(expectedString, completedBuild);
    }

}