package net.jkcode.jkmvc.db

import net.jkcode.jkutil.common.Config
import java.sql.Types

/**
 * 列的逻辑类型
 *    TODO: 目前逻辑类型与sql类型是 1:1 绑定一起的, 以后要分离, 逻辑类型与sql类型是 m:n, 如逻辑类型 currency 对应2位精度的 float sql类型
 *    TODO: 初步预设：　逻辑类型->sql类型,　int->int, short->smallint, byte->tinyint
 *
 * @author shijianhang
 * @date 2020-2-4 下午8:02:47
 */
public enum class DbColumnLogicType(
        public val code: String, // 逻辑类型名, 枚举名的小写, 语法限制不能用 name, 因此只能用 code
        public val sqlType: Int // sql类型, 即jdbc类型
) {
    BIT("bit", Types.BIT),
    BOOLEAN("boolean", Types.BOOLEAN),

    TINYINT("tinyint", Types.TINYINT),
    SMALLINT("smallint", Types.SMALLINT),
    INT("int", Types.INTEGER),
    BIGINT("bigint", Types.BIGINT),

    FLOAT("float", Types.FLOAT),
    DOUBLE("double", Types.DOUBLE),
    REAL("real", Types.REAL),
    DECIMAL("decimal", Types.DECIMAL),
    NUMERIC("numeric", Types.NUMERIC),

    CHAR("char", Types.CHAR),
    VARCHAR("varchar", Types.VARCHAR),
    VARBINARY("varbinary", Types.VARBINARY),

    LONGNVARCHAR("longnvarchar", Types.LONGNVARCHAR),
    LONGVARBINARY("longvarbinary", Types.LONGVARBINARY),
    LONGVARCHAR("longvarchar", Types.LONGVARCHAR),

    BINARY("binary", Types.BINARY),
    BLOB("blob", Types.BLOB),
    CLOB("clob", Types.CLOB),

    NCHAR("nchar", Types.NCHAR),
    NCLOB("nclob", Types.NCLOB),
    NVARCHAR("nvarchar", Types.NVARCHAR),

    DATE("date", Types.DATE),
    TIMESTAMP("timestamp", Types.TIMESTAMP),
    TIME("time", Types.TIME);

    companion object{

        /**
         * 根据code来获得逻辑类型
         * @param code
         * @return
         */
        fun getByCode(code: String): DbColumnLogicType {
            return DbColumnLogicType.valueOf(code.toUpperCase())
        }

        /**
         * 根据code来获得逻辑类型
         * @param code
         * @return
         */
        fun getBySqlType(sqlType: Int): DbColumnLogicType {
            return DbColumnLogicType.values().firstOrNull {
                it.sqlType == sqlType
            } ?: throw IllegalArgumentException("无法识别sql类型: $sqlType")
        }

    }

    /**
     * 是否小数 => 需要精度
     */
    public val isDeciaml: Boolean = "float|double|real|decimal|number|numeric".toRegex().containsMatchIn(code)

    /**
     * 转为物理类型, 带长度与精度
     * @param db
     * @param precision 长度
     * @param scale 精度
     * @return
     */
    public fun toPhysicalType(db: Db,  precision: Int? = null, scale: Int? = null): String {
        // 元数据定义的配置
        val config = Config.instance("meta-define.${db.dbType}", "yaml")
        // 获得对应的物理类型表达式
        val mapping:Map<String, String> = config["logicalType2PhysicalType"]!!
        val physicalType = mapping[code] ?: throw IllegalArgumentException("无法找到逻辑类型[$code]对应的物理类型")
        // 1 如果自带长度精度 或 没指定长度, 则直接输出
        if(physicalType.contains('(') || precision == null)
            return physicalType

        // 2 没指定精度 或 非小数类型(不需要精度)
        if(scale == null || !isDeciaml)
            return "$physicalType($precision)"

        // 3 有指定精度
        return "$physicalType($precision, $scale)"
    }



}