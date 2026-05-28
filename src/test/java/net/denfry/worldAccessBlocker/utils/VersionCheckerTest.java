package net.denfry.worldAccessBlocker.utils;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class VersionCheckerTest {

    // --- parseVersionFromResponse ---

    @Test
    void parsesVersionFromTypicalModrinthResponse() {
        String json = "[{\"id\":\"abc\",\"project_id\":\"worldaccessblocker\",\"version_number\":\"0.8.0\",\"name\":\"WorldAccessBlocker 0.8.0\"}]";
        assertEquals("0.8.0", VersionChecker.parseVersionFromResponse(json));
    }

    @Test
    void returnsNullForEmptyArray() {
        assertNull(VersionChecker.parseVersionFromResponse("[]"));
    }

    @Test
    void returnsNullForNullInput() {
        assertNull(VersionChecker.parseVersionFromResponse(null));
    }

    @Test
    void returnsNullForBlankInput() {
        assertNull(VersionChecker.parseVersionFromResponse("   "));
    }

    @Test
    void returnsNullWhenFieldMissing() {
        assertNull(VersionChecker.parseVersionFromResponse("[{\"id\":\"abc\"}]"));
    }

    // --- isNewer ---

    @Test
    void detectsNewerMinorVersion() {
        assertTrue(VersionChecker.isNewer("0.8.0", "0.7.1"));
    }

    @Test
    void detectsNewerPatchVersion() {
        assertTrue(VersionChecker.isNewer("0.7.2", "0.7.1"));
    }

    @Test
    void detectsNewerMajorVersion() {
        assertTrue(VersionChecker.isNewer("1.0.0", "0.7.1"));
    }

    @Test
    void returnsFalseForSameVersion() {
        assertFalse(VersionChecker.isNewer("0.7.1", "0.7.1"));
    }

    @Test
    void returnsFalseWhenCandidateIsOlder() {
        assertFalse(VersionChecker.isNewer("0.7.0", "0.7.1"));
    }

    @Test
    void returnsFalseForMalformedVersion() {
        assertFalse(VersionChecker.isNewer("nightly", "0.7.1"));
        assertFalse(VersionChecker.isNewer("0.8.0", "invalid"));
    }
}
