package net.plavcak.jenkins.plugins.scmskip;

import static org.junit.jupiter.api.Assertions.assertEquals;

import hudson.model.Cause;
import hudson.model.CauseAction;
import hudson.model.FreeStyleBuild;
import hudson.model.FreeStyleProject;
import hudson.model.Label;
import hudson.model.Result;
import hudson.model.queue.QueueTaskFuture;
import java.io.IOException;
import java.net.URL;
import java.util.List;
import org.jenkinsci.plugins.workflow.cps.CpsScmFlowDefinition;
import org.jenkinsci.plugins.workflow.flow.FlowDefinition;
import org.jenkinsci.plugins.workflow.job.WorkflowJob;
import org.jenkinsci.plugins.workflow.job.WorkflowRun;
import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.jvnet.hudson.test.FakeChangeLogSCM;
import org.jvnet.hudson.test.JenkinsRule;

public class SCMSkipBuildStepTest {

    @Rule
    public JenkinsRule jenkins = new JenkinsRule();

    @Test
    public void testConfigRoundtrip() throws Exception {
        FreeStyleProject project = createFreestyleProject(null);

        project = jenkins.configRoundtrip(project);

        jenkins.assertEqualDataBoundBeans(
                new SCMSkipBuildStep(), project.getBuilders().get(0));
    }

    @Test
    public void testBuildWithoutChangelog() throws Exception {
        FreeStyleProject project = createFreestyleProject(null);

        FreeStyleBuild build = jenkins.buildAndAssertSuccess(project);

        jenkins.assertLogContains("SCM Skip: Changelog is empty!", build);
    }

    @Test
    public void testBuildWithChangelog() throws Exception {
        FreeStyleProject project = createFreestyleProject(null);

        FakeChangeLogSCM fakeScm = new FakeChangeLogSCM();
        fakeScm.addChange().withMsg("Some change [ci skip] in code.");
        project.setScm(fakeScm);

        FreeStyleBuild build = jenkins.assertBuildStatus(Result.ABORTED, project.scheduleBuild2(0));

        Assert.assertEquals("SCM Skip - build skipped", build.getDescription());
        jenkins.assertLogContains(
                "SCM Skip: Pattern .*\\[ci skip\\].* matched on message: Some change [ci skip] in code.", build);
    }

    @Test
    public void testBuildWithCustomPattern() throws Exception {
        FreeStyleProject project = createFreestyleProject(".*\\[(ci skip|skip ci)\\].*");

        FakeChangeLogSCM fakeScm = new FakeChangeLogSCM();
        fakeScm.addChange().withMsg("Some change [skip ci] in code.");
        project.setScm(fakeScm);

        FreeStyleBuild build = jenkins.assertBuildStatus(Result.ABORTED, project.scheduleBuild2(0));

        Assert.assertEquals("SCM Skip - build skipped", build.getDescription());
        jenkins.assertLogContains(
                "SCM Skip: Pattern .*\\[(ci skip|skip ci)\\].* matched on message: Some change [skip ci] in code.",
                build);
    }

    @Test
    public void testBuildWithMultiChangelog() throws Exception {
        FreeStyleProject project = createFreestyleProject(null);

        FakeChangeLogSCM fakeScm = new FakeChangeLogSCM();
        fakeScm.addChange().withMsg("Some change [ci skip] in code.\n Another message");
        project.setScm(fakeScm);

        FreeStyleBuild build = jenkins.assertBuildStatus(Result.ABORTED, project.scheduleBuild2(0));
        jenkins.assertLogContains(
                "SCM Skip: Pattern .*\\[ci skip\\].* matched on message: " + "Some change [ci skip] in code.", build);
    }

    @Test
    public void testBuildWithGlobalConfiguration() {

        SCMSkipBuildStep.DescriptorImpl descriptor =
                jenkins.getInstance().getDescriptorByType(SCMSkipBuildStep.DescriptorImpl.class);

        Assert.assertEquals(SCMSkipConstants.DEFAULT_PATTERN, descriptor.getSkipPattern());

        descriptor.setSkipPattern(".*\\[skip-ci\\].*");

        descriptor = jenkins.getInstance().getDescriptorByType(SCMSkipBuildStep.DescriptorImpl.class);

        Assert.assertEquals(".*\\[skip-ci\\].*", descriptor.getSkipPattern());
    }

    @Test
    public void testScriptedPipelineEmptyChangeLog() throws Exception {
        String agentLabel = "test-agent";
        jenkins.createOnlineSlave(Label.get(agentLabel));
        WorkflowJob job = preparePipelineJob();
        WorkflowRun completedBuild = jenkins.assertBuildStatusSuccess(job.scheduleBuild2(0));
        String expectedString = "SCM Skip: Changelog is empty!";
        jenkins.assertLogContains(expectedString, completedBuild);
    }

    @Test
    public void testScriptedPipeline() throws Exception {
        String agentLabel = "test-agent";
        jenkins.createOnlineSlave(Label.get(agentLabel));
        WorkflowJob job = preparePipelineJob("Some change [skip ci] in code.", "Additional change.");

        QueueTaskFuture<WorkflowRun> future = job.scheduleBuild2(0);
        Assert.assertNotNull(future);

        WorkflowRun completedBuild = jenkins.assertBuildStatus(Result.SUCCESS, future);
        jenkins.assertLogContains("after skip", completedBuild);
    }

    @Test
    public void testScriptedPipelineNoAgent() throws Exception {
        WorkflowJob job = preparePipelineJob(
                this.getClass().getClassLoader().getResource("testNoAgent.Jenkinsfile"),
                "Some change [skip ci] in code.",
                "Additional change.");

        QueueTaskFuture<WorkflowRun> future = job.scheduleBuild2(0);
        Assert.assertNotNull(future);

        WorkflowRun completedBuild = jenkins.assertBuildStatus(Result.SUCCESS, future);
        jenkins.assertLogContains("after skip", completedBuild);
    }

    @Test
    public void testScriptedPipelineMultilineCommit() throws Exception {
        String agentLabel = "test-agent";
        jenkins.createOnlineSlave(Label.get(agentLabel));
        WorkflowJob job = preparePipelineJob("Some change [skip ci] in code.\n Additional line.");

        QueueTaskFuture<WorkflowRun> future = job.scheduleBuild2(0);
        Assert.assertNotNull(future);

        WorkflowRun completedBuild = jenkins.assertBuildStatus(Result.ABORTED, future);

        String expectedString =
                "SCM Skip: Pattern .*\\[(ci skip|skip ci)\\].* matched on message: " + "Some change [skip ci] in code.";

        Assert.assertEquals("SCM Skip - build skipped", completedBuild.getDescription());

        jenkins.assertLogContains(expectedString, completedBuild);
        jenkins.assertLogContains("before skip", completedBuild);
        jenkins.assertLogNotContains("after skip", completedBuild);
    }

    @Test
    public void testScriptedPipelineMultilineDelete() throws Exception {
        String agentLabel = "test-agent";
        jenkins.createOnlineSlave(Label.get(agentLabel));
        WorkflowJob job = preparePipelineJob(
                getClass().getClassLoader().getResource("testDelete.Jenkinsfile"),
                "Some change [skip ci] in code.\n Additional line.");

        QueueTaskFuture<WorkflowRun> future = job.scheduleBuild2(0);
        Assert.assertNotNull(future);

        WorkflowRun completedBuild = jenkins.assertBuildStatus(Result.ABORTED, future);
        Assert.assertEquals("SCM Skip - build skipped", completedBuild.getDescription());

        assertEquals("", JenkinsRule.getLog(completedBuild), "Should delete log");
        assertEquals(List.of(), job.getBuilds());
    }

    @Test
    public void testPipelineUserIdCause() throws Exception {
        String agentLabel = "test-agent";
        jenkins.createOnlineSlave(Label.get(agentLabel));
        WorkflowJob job = preparePipelineJob("Some change [skip ci] in code.\n Additional line.");
        QueueTaskFuture<WorkflowRun> future = job.scheduleBuild2(0, new CauseAction(new Cause.UserIdCause()));
        Assert.assertNotNull(future);

        WorkflowRun completedBuild = jenkins.assertBuildStatus(Result.SUCCESS, future);
        jenkins.assertLogContains("after skip", completedBuild);
    }

    private WorkflowJob preparePipelineJob(String... commitMessages) throws IOException {
        return preparePipelineJob(this.getClass().getClassLoader().getResource("test.Jenkinsfile"), commitMessages);
    }

    private WorkflowJob preparePipelineJob(URL pipelineFile, String... commitMessages) throws IOException {
        WorkflowJob job = jenkins.createProject(WorkflowJob.class, "test-scripted-pipeline");
        Assert.assertNotNull(pipelineFile);

        SCMSkipFakeSCM scm = new SCMSkipFakeSCM("Jenkinsfile", pipelineFile);
        for (String message : commitMessages) {
            scm.addChange().withMsg(message);
        }
        FlowDefinition fd = new CpsScmFlowDefinition(scm, "Jenkinsfile");

        job.setDefinition(fd);
        return job;
    }

    private FreeStyleProject createFreestyleProject(String skipPattern) throws IOException {
        FreeStyleProject project = jenkins.createFreeStyleProject();
        SCMSkipBuildStep builder = new SCMSkipBuildStep();

        if (skipPattern != null) {
            builder.setSkipPattern(skipPattern);
        }

        project.getBuildersList().add(builder);

        return project;
    }
}
