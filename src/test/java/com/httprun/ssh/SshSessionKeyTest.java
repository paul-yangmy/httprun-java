package com.httprun.ssh;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * SSH Session Key 单元测试
 */
class SshSessionKeyTest {

    @Test
    void testEquals_SameHostPortUsername() {
        SshSessionKey key1 = new SshSessionKey("192.168.1.1", 22, "root");
        SshSessionKey key2 = new SshSessionKey("192.168.1.1", 22, "root");

        assertEquals(key1, key2);
        assertEquals(key1.hashCode(), key2.hashCode());
    }

    @Test
    void testEquals_DifferentHost() {
        SshSessionKey key1 = new SshSessionKey("192.168.1.1", 22, "root");
        SshSessionKey key2 = new SshSessionKey("192.168.1.2", 22, "root");

        assertNotEquals(key1, key2);
    }

    @Test
    void testEquals_DifferentPort() {
        SshSessionKey key1 = new SshSessionKey("192.168.1.1", 22, "root");
        SshSessionKey key2 = new SshSessionKey("192.168.1.1", 2222, "root");

        assertNotEquals(key1, key2);
    }

    @Test
    void testEquals_DifferentUsername() {
        SshSessionKey key1 = new SshSessionKey("192.168.1.1", 22, "root");
        SshSessionKey key2 = new SshSessionKey("192.168.1.1", 22, "admin");

        assertNotEquals(key1, key2);
    }

    @Test
    void testToLabel() {
        SshSessionKey key = new SshSessionKey("192.168.1.1", 22, "root");
        assertEquals("root@192.168.1.1:22", key.toLabel());
    }

    @Test
    void testGetters() {
        SshSessionKey key = new SshSessionKey("10.0.0.1", 2222, "deploy");

        assertEquals("10.0.0.1", key.getHost());
        assertEquals(2222, key.getPort());
        assertEquals("deploy", key.getUsername());
    }
}
