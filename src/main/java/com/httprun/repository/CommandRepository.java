package com.httprun.repository;

import com.httprun.entity.Command;
import com.httprun.enums.CommandStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * 命令数据访问层
 */
@Repository
public interface CommandRepository extends JpaRepository<Command, Long> {

    /**
     * 根据名称查找命令
     */
    Optional<Command> findByName(String name);

    /**
     * 根据路径查找命令
     */
    Optional<Command> findByPath(String path);

    /**
     * 检查名称是否存在
     */
    boolean existsByName(String name);

    /**
     * 检查路径是否存在
     */
    boolean existsByPath(String path);

    /**
     * 根据状态查找命令列表
     */
    List<Command> findByStatus(CommandStatus status);

    /**
     * 根据分组查找命令列表
     */
    List<Command> findByGroupName(String groupName);

    /**
     * 分页查询活跃的命令
     */
    Page<Command> findByStatus(CommandStatus status, Pageable pageable);

    /**
     * 根据名称列表查找命令
     */
    List<Command> findByNameIn(List<String> names);

    /**
     * 搜索命令
     */
    @Query("SELECT c FROM Command c WHERE " +
            "(c.name LIKE %:keyword% OR c.description LIKE %:keyword%) " +
            "AND c.status = :status")
    Page<Command> searchCommands(@Param("keyword") String keyword,
            @Param("status") CommandStatus status,
            Pageable pageable);

    /**
     * 获取所有分组
     */
    @Query("SELECT DISTINCT c.groupName FROM Command c WHERE c.groupName IS NOT NULL")
    List<String> findAllGroups();

    /**
     * 批量更新命令状态
     */
    @Modifying
    @Query("UPDATE Command c SET c.status = :status WHERE c.name IN :names")
    void updateStatusByNameIn(@Param("names") List<String> names, @Param("status") CommandStatus status);

    /**
     * 批量删除命令
     */
    @Modifying
    void deleteByNameIn(List<String> names);
}
