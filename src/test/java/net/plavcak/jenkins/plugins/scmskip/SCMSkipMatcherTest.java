package net.plavcak.jenkins.plugins.scmskip;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class SCMSkipMatcherTest {

    @Test
    void match() {
        SCMSkipMatcher matcher = new SCMSkipMatcher();
        assertTrue(matcher.match("Message with skip on the end. [ci skip] "));
        assertTrue(matcher.match("[ci skip] Message with skip on beginning."));
        assertTrue(matcher.match("Message with [ci skip] in the middle end."));

        assertTrue(matcher.match("Message with \n - [ci skip] - \n in multi line."));

        assertFalse(matcher.match("Message without skip."));
        assertFalse(matcher.match("Message without invalid  [ci_skip]."));
    }

    @Test
    void getPattern() {
        SCMSkipMatcher matcher = new SCMSkipMatcher();
        assertEquals(".*\\[ci skip\\].*", matcher.getPattern().pattern());

        matcher = new SCMSkipMatcher(".*\\[skip\\].*");
        assertEquals(".*\\[skip\\].*", matcher.getPattern().pattern());
    }
}
