package net.plavcak.jenkins.plugins.scmskip;

import hudson.plugins.git.GitChangeSet;
import org.junit.Test;

import static java.util.Arrays.asList;
import static org.junit.Assert.assertEquals;

public class GitMessageExtractorTest {

	@Test
	public void getFullMessage() {
		GitChangeSet changeSet = new GitChangeSet(asList(
				"commit 12345678", "",
				"    title", "    details [ci skip]"), true);
		assertEquals("title\ndetails [ci skip]\n", GitMessageExtractor.getFullMessage(changeSet));
	}
}
