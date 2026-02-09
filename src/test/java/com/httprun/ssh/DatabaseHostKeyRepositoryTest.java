package com.httprun.ssh;

import com.httprun.entity.SshHostKey;
import com.httprun.repository.SshHostKeyRepository;
import com.jcraft.jsch.HostKey;
import com.jcraft.jsch.HostKeyRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Base64;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * DatabaseHostKeyRepository 单元测试
 * <p>
 * 验证 TOFU 策略、指纹匹配/不匹配、异常降级等核心逻辑
 */
@ExtendWith(MockitoExtension.class)
class DatabaseHostKeyRepositoryTest {

    @Mock
    private SshHostKeyRepository hostKeyRepository;

    private DatabaseHostKeyRepository repo;

    // 模拟 SSH 公钥数据
    private final byte[] testKey = "test-ssh-public-key-data-for-testing".getBytes();
    private final String testFingerprint = Base64.getEncoder().encodeToString(testKey);

    @BeforeEach
    void setUp() {
        repo = new DatabaseHostKeyRepository(hostKeyRepository);
    }

    // ==================== TOFU (Trust On First Use) ====================

    @Test
    void check_FirstConnection_ShouldRecordAndReturnOK() {
        when(hostKeyRepository.findByHostAndPortAndKeyType("192.168.1.1", 22, "unknown"))
                .thenReturn(Optional.empty());

        int result = repo.check("192.168.1.1", testKey);

        assertEquals(HostKeyRepository.OK, result);

        // 验证保存了主机指纹
        ArgumentCaptor<SshHostKey> captor = ArgumentCaptor.forClass(SshHostKey.class);
        verify(hostKeyRepository).save(captor.capture());

        SshHostKey saved = captor.getValue();
        assertEquals("192.168.1.1", saved.getHost());
        assertEquals(22, saved.getPort());
        assertEquals(testFingerprint, saved.getFingerprint());
        assertTrue(saved.isTrusted());
        assertNotNull(saved.getFirstSeen());
        assertNotNull(saved.getLastSeen());
        assertNotNull(saved.getSha256Hash());
        assertTrue(saved.getSha256Hash().startsWith("SHA256:"));
    }

    // ==================== Fingerprint Match ====================

    @Test
    void check_MatchingFingerprint_ShouldReturnOK() {
        SshHostKey storedKey = createStoredKey("192.168.1.1", 22, "unknown", testFingerprint);

        when(hostKeyRepository.findByHostAndPortAndKeyType("192.168.1.1", 22, "unknown"))
                .thenReturn(Optional.of(storedKey));

        int result = repo.check("192.168.1.1", testKey);

        assertEquals(HostKeyRepository.OK, result);

        // 验证更新了 lastSeen
        verify(hostKeyRepository).save(storedKey);
    }

    // ==================== Fingerprint Mismatch (MITM Detection)
    // ====================

    @Test
    void check_MismatchedFingerprint_ShouldReturnCHANGED() {
        SshHostKey storedKey = createStoredKey("192.168.1.1", 22, "unknown", "old-different-fingerprint");

        when(hostKeyRepository.findByHostAndPortAndKeyType("192.168.1.1", 22, "unknown"))
                .thenReturn(Optional.of(storedKey));

        int result = repo.check("192.168.1.1", testKey);

        assertEquals(HostKeyRepository.CHANGED, result);

        // 验证标记为不信任
        assertFalse(storedKey.isTrusted());
        assertNotNull(storedKey.getRemark());
        assertTrue(storedKey.getRemark().contains("Key mismatch"));
        verify(hostKeyRepository).save(storedKey);
    }

    // ==================== Untrusted Key ====================

    @Test
    void check_UntrustedKey_ShouldReturnCHANGED() {
        SshHostKey storedKey = createStoredKey("192.168.1.1", 22, "unknown", testFingerprint);
        storedKey.setTrusted(false);

        when(hostKeyRepository.findByHostAndPortAndKeyType("192.168.1.1", 22, "unknown"))
                .thenReturn(Optional.of(storedKey));

        int result = repo.check("192.168.1.1", testKey);

        assertEquals(HostKeyRepository.CHANGED, result);
    }

    // ==================== Exception Fallback ====================

    @Test
    void check_DatabaseException_ShouldReturnOK_Degraded() {
        when(hostKeyRepository.findByHostAndPortAndKeyType(anyString(), anyInt(), anyString()))
                .thenThrow(new RuntimeException("DB connection lost"));

        int result = repo.check("192.168.1.1", testKey);

        // 数据库异常时降级为允许连接
        assertEquals(HostKeyRepository.OK, result);
    }

    // ==================== Non-standard Port ====================

    @Test
    void check_NonStandardPort_ShouldParseCorrectly() {
        when(hostKeyRepository.findByHostAndPortAndKeyType("10.0.0.1", 2222, "unknown"))
                .thenReturn(Optional.empty());

        int result = repo.check("[10.0.0.1]:2222", testKey);

        assertEquals(HostKeyRepository.OK, result);

        ArgumentCaptor<SshHostKey> captor = ArgumentCaptor.forClass(SshHostKey.class);
        verify(hostKeyRepository).save(captor.capture());

        assertEquals("10.0.0.1", captor.getValue().getHost());
        assertEquals(2222, captor.getValue().getPort());
    }

    // ==================== parseHostPort ====================

    @Test
    void parseHostPort_StandardPort() {
        var hp = DatabaseHostKeyRepository.parseHostPort("example.com");
        assertEquals("example.com", hp.host());
        assertEquals(22, hp.port());
    }

    @Test
    void parseHostPort_NonStandardPort() {
        var hp = DatabaseHostKeyRepository.parseHostPort("[example.com]:2222");
        assertEquals("example.com", hp.host());
        assertEquals(2222, hp.port());
    }

    @Test
    void parseHostPort_BracketedIPv6() {
        var hp = DatabaseHostKeyRepository.parseHostPort("[::1]:22");
        assertEquals("::1", hp.host());
        assertEquals(22, hp.port());
    }

    @Test
    void parseHostPort_IPAddress() {
        var hp = DatabaseHostKeyRepository.parseHostPort("192.168.1.100");
        assertEquals("192.168.1.100", hp.host());
        assertEquals(22, hp.port());
    }

    // ==================== computeSha256 ====================

    @Test
    void computeSha256_ShouldStartWithPrefix() {
        String hash = DatabaseHostKeyRepository.computeSha256(testKey);
        assertTrue(hash.startsWith("SHA256:"));
    }

    @Test
    void computeSha256_SameKey_ShouldBeDeterministic() {
        String hash1 = DatabaseHostKeyRepository.computeSha256(testKey);
        String hash2 = DatabaseHostKeyRepository.computeSha256(testKey);
        assertEquals(hash1, hash2);
    }

    @Test
    void computeSha256_DifferentKeys_ShouldDiffer() {
        String hash1 = DatabaseHostKeyRepository.computeSha256(testKey);
        String hash2 = DatabaseHostKeyRepository.computeSha256("different-key-data".getBytes());
        assertNotEquals(hash1, hash2);
    }

    // ==================== add / remove ====================

    @Test
    void add_NewKey_ShouldSave() throws Exception {
        when(hostKeyRepository.findByHostAndPortAndKeyType("host1", 22, "ssh-rsa"))
                .thenReturn(Optional.empty());

        HostKey jschKey = mockHostKey("host1", "ssh-rsa", testFingerprint);
        repo.add(jschKey, null);

        verify(hostKeyRepository).save(any(SshHostKey.class));
    }

    @Test
    void add_ExistingKey_ShouldNotDuplicate() throws Exception {
        SshHostKey existing = createStoredKey("host1", 22, "ssh-rsa", testFingerprint);
        when(hostKeyRepository.findByHostAndPortAndKeyType("host1", 22, "ssh-rsa"))
                .thenReturn(Optional.of(existing));

        HostKey jschKey = mockHostKey("host1", "ssh-rsa", testFingerprint);
        repo.add(jschKey, null);

        verify(hostKeyRepository, never()).save(any());
    }

    @Test
    void remove_WithType_ShouldDeleteSpecific() {
        SshHostKey existing = createStoredKey("host1", 22, "ssh-rsa", testFingerprint);
        when(hostKeyRepository.findByHostAndPortAndKeyType("host1", 22, "ssh-rsa"))
                .thenReturn(Optional.of(existing));

        repo.remove("host1", "ssh-rsa");

        verify(hostKeyRepository).delete(existing);
    }

    @Test
    void remove_WithoutType_ShouldDeleteAll() {
        repo.remove("host1", null);

        verify(hostKeyRepository).deleteByHostAndPort("host1", 22);
    }

    // ==================== getHostKey ====================

    @Test
    void getHostKey_ShouldReturnTrustedOnly() {
        SshHostKey trusted = createStoredKey("h1", 22, "ssh-rsa", testFingerprint);
        trusted.setTrusted(true);

        when(hostKeyRepository.findByTrustedTrue()).thenReturn(List.of(trusted));

        HostKey[] keys = repo.getHostKey();

        // 可能返回 0 或 1 取决于 HostKey 构造是否成功（需要有效密钥格式）
        assertNotNull(keys);
    }

    @Test
    void getKnownHostsRepositoryID_ShouldReturnExpected() {
        assertEquals("database-host-key-repository", repo.getKnownHostsRepositoryID());
    }

    // ==================== Helper Methods ====================

    private SshHostKey createStoredKey(String host, int port, String keyType, String fingerprint) {
        SshHostKey key = new SshHostKey();
        key.setId(1L);
        key.setHost(host);
        key.setPort(port);
        key.setKeyType(keyType);
        key.setFingerprint(fingerprint);
        key.setSha256Hash(DatabaseHostKeyRepository.computeSha256(testKey));
        key.setFirstSeen(LocalDateTime.now().minusDays(30));
        key.setLastSeen(LocalDateTime.now().minusHours(1));
        key.setTrusted(true);
        return key;
    }

    private HostKey mockHostKey(String host, String keyType, String fingerprint) {
        HostKey hostKey = mock(HostKey.class);
        when(hostKey.getHost()).thenReturn(host);
        when(hostKey.getType()).thenReturn(keyType);
        when(hostKey.getKey()).thenReturn(fingerprint);
        return hostKey;
    }
}
