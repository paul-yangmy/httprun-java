package com.httprun.repository;

import com.httprun.entity.CommandVersion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * 命令版本历史数据访问层
 */
@Repository
public interface CommandVersionRepository extends JpaRepository<CommandVersion, Long> {

    /**
     * 查询指定命令的所有版本，按版本号倒序排列
     */
    List<CommandVersion> findByCommandNameOrderByVersionDesc(String commandName);

    /**
     * 查询指定命令的最新版本号
     */
    @Query("SELECT MAX(v.version) FROM CommandVersion v WHERE v.commandName = :name")
    Optional<Integer> findMaxVersionByCommandName(@Param("name") String name);

    /**
     * 删除指定命令的所有版本（命令删除时联动清理）
     */
    void deleteByCommandName(String commandName);
}
