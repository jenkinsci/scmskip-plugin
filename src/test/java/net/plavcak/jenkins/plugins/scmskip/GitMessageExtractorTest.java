package net.plavcak.jenkins.plugins.scmskip;

import static java.util.Arrays.asList;
import static org.junit.jupiter.api.Assertions.assertEquals;

import hudson.plugins.git.GitChangeSet;
import org.junit.jupiter.api.Test;

class GitMessageExtractorTest {

    @Test
    void getFullMessage() {
        GitChangeSet changeSet = new GitChangeSet(
                asList(
                        "commit 12345678", "",
                        "    title", "    details [ci skip]"),
                true);
        assertEquals("title\ndetails [ci skip]\n", GitMessageExtractor.getFullMessage(changeSet));
    }
}
