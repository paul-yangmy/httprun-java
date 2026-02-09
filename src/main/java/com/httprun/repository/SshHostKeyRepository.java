package com.httprun.repository;

import com.httprun.entity.SshHostKey;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * SSH 主机指纹数据访问层
 */
@Repository
public interface SshHostKeyRepository extends JpaRepository<SshHostKey, Long> {

    /**
     * 根据主机和端口查找所有已记录的指纹
     */
    List<SshHostKey> findByHostAndPort(String host, int port);

    /**
     * 根据主机、端口和密钥类型精确查找
     */
    Optional<SshHostKey> findByHostAndPortAndKeyType(String host, int port, String keyType);

    /**
     * 根据主机查找所有指纹
     */
    List<SshHostKey> findByHost(String host);

    /**
     * 删除指定主机和端口的所有指纹
     */
    void deleteByHostAndPort(String host, int port);

    /**
     * 查找所有受信任的指纹
     */
    List<SshHostKey> findByTrustedTrue();
}
