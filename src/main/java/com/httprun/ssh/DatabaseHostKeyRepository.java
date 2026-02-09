package com.httprun.ssh;

import com.httprun.entity.SshHostKey;
import com.httprun.repository.SshHostKeyRepository;
import com.jcraft.jsch.HostKey;
import com.jcraft.jsch.HostKeyRepository;
import com.jcraft.jsch.UserInfo;
import lombok.extern.slf4j.Slf4j;

import java.security.MessageDigest;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.List;

/**
 * 基于数据库的 JSch HostKeyRepository 实现
 * <p>
 * 替代 JSch 内置的 KnownHosts 文件方式，将主机指纹持久化到数据库。
 * <ul>
 * <li>首次连接：TOFU（Trust On First Use）策略，自动记录并信任指纹</li>
 * <li>后续连接：自动验证指纹是否匹配，不匹配时拒绝连接（防中间人攻击）</li>
 * <li>指纹变更：标记为不信任并记录告警日志，管理员可通过 API 重新确认</li>
 * </ul>
 */
@Slf4j
public class DatabaseHostKeyRepository implements HostKeyRepository {

    private final SshHostKeyRepository hostKeyRepository;

    public DatabaseHostKeyRepository(SshHostKeyRepository hostKeyRepository) {
        this.hostKeyRepository = hostKeyRepository;
    }

    /**
     * JSch 核心验证方法：检查主机公钥是否可信
     *
     * @return {@link HostKeyRepository#OK} 如果指纹已知且匹配；
     *         {@link HostKeyRepository#NOT_INCLUDED} 如果该主机从未记录过；
     *         {@link HostKeyRepository#CHANGED} 如果指纹不匹配（可能遭遇中间人攻击）
     */
    @Override
    public int check(String host, byte[] key) {
        // 通过构造 HostKey 来确定密钥类型
        String keyType;
        try {
            HostKey hk = new HostKey(host, key);
            keyType = hk.getType();
        } catch (Exception e) {
            keyType = "unknown";
        }
        String fingerprint = Base64.getEncoder().encodeToString(key);

        // 解析 host:port（JSch 使用 [host]:port 格式表示非标端口）
        HostPort hp = parseHostPort(host);

        try {
            var existingKey = hostKeyRepository.findByHostAndPortAndKeyType(hp.host, hp.port, keyType);

            if (existingKey.isEmpty()) {
                // 首次连接，使用 TOFU 策略：自动记录并信任
                log.info("First connection to {}:{}, recording host key (type={})", hp.host, hp.port, keyType);
                saveHostKey(hp.host, hp.port, keyType, fingerprint, key);
                return OK;
            }

            SshHostKey stored = existingKey.get();

            if (!stored.isTrusted()) {
                log.warn("Host key for {}:{} (type={}) is marked as untrusted!", hp.host, hp.port, keyType);
                return CHANGED;
            }

            if (stored.getFingerprint().equals(fingerprint)) {
                // 指纹匹配，更新最后验证时间
                stored.setLastSeen(LocalDateTime.now());
                hostKeyRepository.save(stored);
                log.debug("Host key verified for {}:{} (type={})", hp.host, hp.port, keyType);
                return OK;
            } else {
                // 指纹不匹配！可能遭遇中间人攻击
                log.error("HOST KEY MISMATCH for {}:{} (type={})! Possible MITM attack. " +
                        "Stored SHA-256: {}, Received SHA-256: {}",
                        hp.host, hp.port, keyType, stored.getSha256Hash(), computeSha256(key));

                // 标记为不信任
                stored.setTrusted(false);
                stored.setRemark("Key mismatch detected at " + LocalDateTime.now() +
                        ". New key SHA-256: " + computeSha256(key));
                hostKeyRepository.save(stored);

                return CHANGED;
            }
        } catch (Exception e) {
            log.error("Failed to check host key for {}:{}: {}", hp.host, hp.port, e.getMessage());
            // 数据库异常时降级为允许连接（避免阻塞业务）
            return OK;
        }
    }

    @Override
    public void add(HostKey hostkey, UserInfo ui) {
        String host = hostkey.getHost();
        HostPort hp = parseHostPort(host);
        String keyType = hostkey.getType();
        // JSch mwiede 0.2.x: getKey() returns Base64 encoded String
        String fingerprint = hostkey.getKey();
        byte[] keyBytes = Base64.getDecoder().decode(fingerprint);

        try {
            var existing = hostKeyRepository.findByHostAndPortAndKeyType(hp.host, hp.port, keyType);
            if (existing.isEmpty()) {
                saveHostKey(hp.host, hp.port, keyType, fingerprint, keyBytes);
                log.info("Added host key for {}:{} (type={})", hp.host, hp.port, keyType);
            }
        } catch (Exception e) {
            log.error("Failed to add host key for {}:{}: {}", hp.host, hp.port, e.getMessage());
        }
    }

    @Override
    public void remove(String host, String type) {
        HostPort hp = parseHostPort(host);
        try {
            if (type == null) {
                hostKeyRepository.deleteByHostAndPort(hp.host, hp.port);
                log.info("Removed all host keys for {}:{}", hp.host, hp.port);
            } else {
                hostKeyRepository.findByHostAndPortAndKeyType(hp.host, hp.port, type)
                        .ifPresent(key -> {
                            hostKeyRepository.delete(key);
                            log.info("Removed host key for {}:{} (type={})", hp.host, hp.port, type);
                        });
            }
        } catch (Exception e) {
            log.error("Failed to remove host key for {}:{}: {}", hp.host, hp.port, e.getMessage());
        }
    }

    @Override
    public void remove(String host, String type, byte[] key) {
        remove(host, type);
    }

    @Override
    public String getKnownHostsRepositoryID() {
        return "database-host-key-repository";
    }

    @Override
    public HostKey[] getHostKey() {
        try {
            List<SshHostKey> allKeys = hostKeyRepository.findByTrustedTrue();
            return allKeys.stream()
                    .map(this::toJSchHostKey)
                    .filter(java.util.Objects::nonNull)
                    .toArray(HostKey[]::new);
        } catch (Exception e) {
            log.error("Failed to get all host keys: {}", e.getMessage());
            return new HostKey[0];
        }
    }

    @Override
    public HostKey[] getHostKey(String host, String type) {
        HostPort hp = parseHostPort(host);
        try {
            List<SshHostKey> keys;
            if (type == null) {
                keys = hostKeyRepository.findByHostAndPort(hp.host, hp.port);
            } else {
                keys = hostKeyRepository.findByHostAndPortAndKeyType(hp.host, hp.port, type)
                        .map(List::of)
                        .orElse(List.of());
            }
            return keys.stream()
                    .filter(SshHostKey::isTrusted)
                    .map(this::toJSchHostKey)
                    .filter(java.util.Objects::nonNull)
                    .toArray(HostKey[]::new);
        } catch (Exception e) {
            log.error("Failed to get host keys for {}:{}: {}", hp.host, hp.port, e.getMessage());
            return new HostKey[0];
        }
    }

    /**
     * 保存主机指纹到数据库
     */
    private void saveHostKey(String host, int port, String keyType, String fingerprint, byte[] key) {
        SshHostKey hostKey = new SshHostKey();
        hostKey.setHost(host);
        hostKey.setPort(port);
        hostKey.setKeyType(keyType);
        hostKey.setFingerprint(fingerprint);
        hostKey.setSha256Hash(computeSha256(key));
        hostKey.setFirstSeen(LocalDateTime.now());
        hostKey.setLastSeen(LocalDateTime.now());
        hostKey.setTrusted(true);
        hostKeyRepository.save(hostKey);
    }

    /**
     * 将数据库实体转为 JSch HostKey
     */
    private HostKey toJSchHostKey(SshHostKey entity) {
        try {
            byte[] keyBytes = Base64.getDecoder().decode(entity.getFingerprint());
            String hostEntry = entity.getPort() == 22
                    ? entity.getHost()
                    : "[" + entity.getHost() + "]:" + entity.getPort();
            return new HostKey(hostEntry, keyBytes);
        } catch (Exception e) {
            log.warn("Failed to convert host key entity {} to JSch HostKey: {}",
                    entity.getId(), e.getMessage());
            return null;
        }
    }

    /**
     * 计算公钥的 SHA-256 摘要
     */
    static String computeSha256(byte[] key) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(key);
            return "SHA256:" + Base64.getEncoder().withoutPadding().encodeToString(hash);
        } catch (Exception e) {
            return "unknown";
        }
    }

    /**
     * 解析 JSch 格式的 host 字符串
     * <p>
     * JSch 对非标端口使用 [host]:port 格式，标准端口直接使用 host
     */
    static HostPort parseHostPort(String host) {
        if (host.startsWith("[")) {
            int closeBracket = host.indexOf(']');
            if (closeBracket > 0 && closeBracket + 1 < host.length() && host.charAt(closeBracket + 1) == ':') {
                String h = host.substring(1, closeBracket);
                int p = Integer.parseInt(host.substring(closeBracket + 2));
                return new HostPort(h, p);
            }
        }
        return new HostPort(host, 22);
    }

    record HostPort(String host, int port) {
    }
}
