package net.plavcak.jenkins.plugins.scmskip;

import hudson.model.FreeStyleProject;
import hudson.model.Label;
import hudson.model.Result;
import hudson.model.queue.QueueTaskFuture;
import hudson.tasks.BuildWrapperDescriptor;
import org.apache.commons.io.IOUtils;
import org.jenkinsci.plugins.workflow.cps.CpsFlowDefinition;
import org.jenkinsci.plugins.workflow.cps.CpsScmFlowDefinition;
import org.jenkinsci.plugins.workflow.flow.FlowDefinition;
import org.jenkinsci.plugins.workflow.job.WorkflowJob;
import org.jenkinsci.plugins.workflow.job.WorkflowRun;
import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.jvnet.hudson.test.JenkinsRule;

import java.io.InputStream;
import java.net.URL;

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
                project.getBuildWrappersList().get(0));
    }

    @Test
    public void testScriptedPipelineEmptyChangeLog() throws Exception {
        String agentLabel = "test-agent";
        jenkins.createOnlineSlave(Label.get(agentLabel));
        WorkflowJob job = jenkins.createProject(WorkflowJob.class, "test-scripted-pipeline");

        InputStream pipelineStream = this.getClass().getClassLoader().getResourceAsStream("test.Jenkinsfile");

        Assert.assertNotNull(pipelineStream);

        String pipelineScript = IOUtils.toString(pipelineStream);
        job.setDefinition(new CpsFlowDefinition(pipelineScript, true));
        WorkflowRun completedBuild = jenkins.assertBuildStatusSuccess(job.scheduleBuild2(0));
        String expectedString = "SCM Skip: Changelog is empty!";
        jenkins.assertLogContains(expectedString, completedBuild);
    }

    @Test
    public void testScriptedPipeline() throws Exception {
        String agentLabel = "test-agent";
        jenkins.createOnlineSlave(Label.get(agentLabel));
        WorkflowJob job = jenkins.createProject(WorkflowJob.class, "test-scripted-pipeline");

        URL pipelineFile = this.getClass().getClassLoader().getResource("test.Jenkinsfile");

        Assert.assertNotNull(pipelineFile);

        SCMSkipFakeSCM scm = new SCMSkipFakeSCM("Jenkinsfile", pipelineFile);
        scm.addChange().withMsg("Some change [skip ci] in code.");

        FlowDefinition fd = new CpsScmFlowDefinition(scm, "Jenkinsfile");

        job.setDefinition(fd);

        QueueTaskFuture<WorkflowRun> future =job.scheduleBuild2(0);
        Assert.assertNotNull(future);

        WorkflowRun completedBuild = jenkins.assertBuildStatus(Result.ABORTED, future);

        String expectedString = "SCM Skip: Pattern .*\\[(ci skip|skip ci)\\].* matched on message: Some change [skip ci] in code.";

        Assert.assertEquals("SCM Skip - build skipped", completedBuild.getDescription());

        jenkins.assertLogContains(expectedString, completedBuild);
    }
}