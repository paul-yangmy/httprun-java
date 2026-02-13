package com.httprun.ssh;

import com.httprun.config.SshPoolConfig;
import com.httprun.entity.RemoteConfig;
import com.httprun.repository.SshHostKeyRepository;
import com.httprun.util.CryptoUtils;
import com.jcraft.jsch.JSch;
import com.jcraft.jsch.Session;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.pool2.BaseKeyedPooledObjectFactory;
import org.apache.commons.pool2.PooledObject;
import org.apache.commons.pool2.impl.DefaultPooledObject;

import java.io.File;

/**
 * SSH Session 对象工厂 — Apache Commons Pool2 KeyedPool 工厂实现
 * <p>
 * 负责 SSH Session 的创建、验证、销毁和激活。
 * 认证方式优先级：
 * 1. 用户显式提供的用户名+密码 / 私钥（二选一）
 * 2. 未显式提供时，使用服务器本地的默认 SSH 密钥（如 ~/.ssh/id_rsa）
 * <p>
 * 指纹管理：使用 {@link DatabaseHostKeyRepository} 替代 StrictHostKeyChecking=no，
 * 首次连接自动记录指纹（TOFU），后续连接自动验证（防中间人攻击）。
 */
@Slf4j
public class SshSessionFactory extends BaseKeyedPooledObjectFactory<SshSessionKey, Session> {

    private final CryptoUtils cryptoUtils;
    private final SshPoolConfig poolConfig;
    private final DatabaseHostKeyRepository hostKeyRepo;

    /**
     * 用于连接时提供认证信息的 ThreadLocal，
     * 因为 Pool 工厂 create 方法只接收 key 参数，需要通过 ThreadLocal 传递 RemoteConfig
     */
    private static final ThreadLocal<RemoteConfig> REMOTE_CONFIG_HOLDER = new ThreadLocal<>();

    public SshSessionFactory(CryptoUtils cryptoUtils, SshPoolConfig poolConfig,
            DatabaseHostKeyRepository hostKeyRepo) {
        this.cryptoUtils = cryptoUtils;
        this.poolConfig = poolConfig;
        this.hostKeyRepo = hostKeyRepo;
    }

    /**
     * 设置当前线程的 RemoteConfig（在借用前调用）
     */
    public static void setRemoteConfig(RemoteConfig remoteConfig) {
        REMOTE_CONFIG_HOLDER.set(remoteConfig);
    }

    /**
     * 清除当前线程的 RemoteConfig
     */
    public static void clearRemoteConfig() {
        REMOTE_CONFIG_HOLDER.remove();
    }

    @Override
    public Session create(SshSessionKey key) throws Exception {
        RemoteConfig remoteConfig = REMOTE_CONFIG_HOLDER.get();
        log.info("Creating new SSH session for {}", key.toLabel());

        JSch jsch = new JSch();

        boolean hasPrivateKey = remoteConfig != null
                && remoteConfig.getPrivateKey() != null
                && !remoteConfig.getPrivateKey().isBlank();
        boolean hasPassword = remoteConfig != null
                && remoteConfig.getPassword() != null
                && !remoteConfig.getPassword().isBlank();

        // 优先使用用户显式提供的私钥
        if (hasPrivateKey) {
            String privateKey = cryptoUtils.isEncrypted(remoteConfig.getPrivateKey())
                    ? cryptoUtils.decrypt(remoteConfig.getPrivateKey())
                    : remoteConfig.getPrivateKey();
            jsch.addIdentity("key-" + key.toLabel(), privateKey.getBytes(), null, null);
            log.debug("Using provided private key for SSH authentication to {}", key.toLabel());
        }

        String defaultKeyPath = null;

        // 仅当用户未提供任何认证信息时，才尝试使用本机默认 SSH 密钥
        if (!hasPrivateKey && !hasPassword) {
            defaultKeyPath = getDefaultSshKeyPath();
            if (defaultKeyPath != null) {
                try {
                    jsch.addIdentity(defaultKeyPath);
                    log.debug("Using default SSH key: {} for {}", defaultKeyPath, key.toLabel());
                } catch (Exception e) {
                    log.debug("Failed to load default SSH key for {}: {}", key.toLabel(), e.getMessage());
                }
            }
        }

        // 创建 session
        Session session = jsch.getSession(key.getUsername(), key.getHost(), key.getPort());

        // 用户未提供私钥但提供了密码：仅使用密码认证
        if (!hasPrivateKey && hasPassword) {
            String password = cryptoUtils.isEncrypted(remoteConfig.getPassword())
                    ? cryptoUtils.decrypt(remoteConfig.getPassword())
                    : remoteConfig.getPassword();
            session.setPassword(password);
            session.setConfig("PreferredAuthentications", "password");
            log.debug("Using password authentication for SSH to {}", key.toLabel());
        } else if (hasPrivateKey || defaultKeyPath != null) {
            // 存在私钥（用户提供或默认密钥）：优先尝试公钥认证，允许回退到密码认证以提高兼容性
            session.setConfig("PreferredAuthentications", "publickey,password");
        } else {
            log.warn("No SSH authentication method available for {}, connection may fail", key.toLabel());
        }

        // 连接配置 — 指纹管理
        if (poolConfig.isHostKeyCheckEnabled() && hostKeyRepo != null) {
            // 启用数据库指纹验证：首次连接 TOFU 记录，后续自动验证
            jsch.setHostKeyRepository(hostKeyRepo);
            log.debug("Using database host key verification for {}", key.toLabel());
        } else {
            // 禁用指纹检查（向后兼容）
            session.setConfig("StrictHostKeyChecking", "no");
            log.debug("Host key checking disabled for {}", key.toLabel());
        }
        session.setTimeout(poolConfig.getConnectTimeoutMs());

        // 启用 SSH KeepAlive
        session.setServerAliveInterval((int) (poolConfig.getKeepAliveIntervalMs()));
        session.setServerAliveCountMax(3);

        session.connect(poolConfig.getConnectTimeoutMs());
        log.info("SSH session established for {}", key.toLabel());
        return session;
    }

    @Override
    public PooledObject<Session> wrap(Session session) {
        return new DefaultPooledObject<>(session);
    }

    @Override
    public boolean validateObject(SshSessionKey key, PooledObject<Session> pooledObject) {
        Session session = pooledObject.getObject();
        boolean connected = session != null && session.isConnected();
        if (!connected) {
            log.debug("SSH session validation failed for {}: session disconnected", key.toLabel());
        } else {
            // 尝试发送 keepAlive 探测，验证连接真正可用
            try {
                session.sendKeepAliveMsg();
            } catch (Exception e) {
                log.debug("SSH session keepAlive failed for {}: {}", key.toLabel(), e.getMessage());
                return false;
            }
        }
        return connected;
    }

    @Override
    public void destroyObject(SshSessionKey key, PooledObject<Session> pooledObject) {
        Session session = pooledObject.getObject();
        if (session != null && session.isConnected()) {
            log.info("Destroying SSH session for {}", key.toLabel());
            session.disconnect();
        }
    }

    @Override
    public void activateObject(SshSessionKey key, PooledObject<Session> pooledObject) throws Exception {
        // Session 从空闲变为活跃时无需特殊处理，validateObject 已确保可用性
    }

    @Override
    public void passivateObject(SshSessionKey key, PooledObject<Session> pooledObject) throws Exception {
        // Session 归还回空闲池时无需特殊处理
    }

    /**
     * 获取系统默认 SSH 私钥路径
     */
    private String getDefaultSshKeyPath() {
        String userHome = System.getProperty("user.home");
        String[] keyNames = { "id_rsa", "id_ed25519", "id_ecdsa", "id_dsa" };

        for (String keyName : keyNames) {
            File keyFile = new File(userHome, ".ssh" + File.separator + keyName);
            if (keyFile.exists() && keyFile.isFile() && keyFile.canRead()) {
                return keyFile.getAbsolutePath();
            }
        }
        return null;
    }
}
