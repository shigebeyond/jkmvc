package net.jkcode.jkmvc.orm

import net.jkcode.jkutil.common.generateId
import net.jkcode.jkutil.common.generateUUID

/**
 * 主键生成器
 * @author shijianhang<772910474@qq.com>
 * @date 2019-06-04 9:49 AM
 */
public enum class PkGenerator {

    // 异常数
    SNOWFLAKE {
        override fun generate(ormMeta: OrmMeta): Any = generateId(ormMeta.name)
    },
    // 异常比例
    UUID {
        override fun generate(ormMeta: OrmMeta): Any = generateUUID()
    };

    /**
     * 生成主键值
     * @param ormMeta orm的元数据
     * @return
     */
    public abstract fun generate(ormMeta: OrmMeta): Any

}