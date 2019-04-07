package net.plavcak.jenkins.plugins.scmskip;

import org.junit.Assert;
import org.junit.Test;

import static org.junit.Assert.*;

public class SCMSkipMatcherTest {

    @Test
    public void match() {
        SCMSkipMatcher matcher = new SCMSkipMatcher();
        Assert.assertTrue(matcher.match("Message with skip on the end. [ci skip] "));
        Assert.assertTrue(matcher.match("[ci skip] Message with skip on beginning."));
        Assert.assertTrue(matcher.match("Message with [ci skip] in the middle end."));

        Assert.assertTrue(matcher.match("Message with \n - [ci skip] - \n in multi line."));

        Assert.assertFalse(matcher.match("Message without skip."));
        Assert.assertFalse(matcher.match("Message without invalid  [ci_skip]."));
    }

    @Test
    public void getPattern() {
        SCMSkipMatcher matcher = new SCMSkipMatcher();
        assertEquals(".*\\[ci skip\\].*", matcher.getPattern().pattern());

        matcher = new SCMSkipMatcher(".*\\[skip\\].*");
        assertEquals(".*\\[skip\\].*", matcher.getPattern().pattern());
    }
}