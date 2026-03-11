package com.httprun.dto.response;

import lombok.Builder;
import lombok.Data;

import java.util.List;

/**
 * 命令导入结果
 */
@Data
@Builder
public class CommandImportResult {

    /** 成功导入（新建）数量 */
    private int created;

    /** 覆盖更新数量 */
    private int overwritten;

    /** 跳过数量（同名已存在且 mode=skip） */
    private int skipped;

    /** 自动重命名数量（同名已存在且 mode=rename，添加 -copy 后缀） */
    private int renamed;

    /** 失败数量 */
    private int failed;

    /** 失败详情（命令名 → 错误信息） */
    private List<String> errors;
}
