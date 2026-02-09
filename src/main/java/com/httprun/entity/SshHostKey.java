package com.httprun.entity;

import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * SSH 主机指纹实体
 * <p>
 * 记录已知主机的 SSH 公钥指纹，用于防止中间人攻击。
 * 首次连接时自动记录指纹，后续连接时自动验证。
 */
@Data
@Entity
@Table(name = "ssh_host_keys", uniqueConstraints = {
        @UniqueConstraint(name = "uk_host_port_type", columnNames = { "host", "port", "keyType" })
}, indexes = {
        @Index(name = "idx_hostkey_host_port", columnList = "host, port")
})
public class SshHostKey {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * 主机地址
     */
    @Column(nullable = false, length = 255)
    private String host;

    /**
     * 端口号
     */
    @Column(nullable = false)
    private int port;

    /**
     * 密钥类型（ssh-rsa, ssh-ed25519, ecdsa-sha2-nistp256 等）
     */
    @Column(nullable = false, length = 50)
    private String keyType;

    /**
     * 公钥指纹（Base64 编码的公钥数据）
     */
    @Column(nullable = false, length = 2000)
    private String fingerprint;

    /**
     * SHA-256 哈希摘要（便于展示和比对）
     */
    @Column(length = 100)
    private String sha256Hash;

    /**
     * 首次记录时间
     */
    @Column(nullable = false, updatable = false)
    private LocalDateTime firstSeen;

    /**
     * 最后验证时间
     */
    @Column(nullable = false)
    private LocalDateTime lastSeen;

    /**
     * 是否被信任
     */
    @Column(nullable = false)
    private boolean trusted = true;

    /**
     * 备注
     */
    @Column(length = 500)
    private String remark;
}
