package net.jkcode.jkmvc.db

import net.jkcode.jkutil.common.Config
import java.math.BigDecimal
import java.math.BigInteger
import java.sql.Types
import java.util.*

/**
 * 列的逻辑类型
 *    TODO: 目前逻辑类型与sql类型是 1:1 绑定一起的, 以后要分离, 逻辑类型与sql类型是 m:n, 如逻辑类型 currency 对应2位精度的 float sql类型
 *    TODO: 初步预设：　逻辑类型->sql类型,　int->int, short->smallint, byte->tinyint, bigint->bigint
 *    而sql类型对java类型的映射, 参考 https://docs.oracle.com/javase/6/docs/technotes/guides/jdbc/getstart/mapping.html#996857
 *
 * @author shijianhang
 * @date 2020-2-4 下午8:02:47
 */
public enum class DbColumnLogicType private constructor(
        public val code: String, // 逻辑类型名, 枚举名的小写, 语法限制不能用 name, 因此只能用 code
        public val sqlType: Int, // sql类型, 即jdbc类型
        public val javaType: Class<*> // java类型
) {
    BIT("bit", Types.BIT, Boolean.javaClass),
    BOOLEAN("boolean", Types.BOOLEAN, Boolean.javaClass),

    TINYINT("tinyint", Types.TINYINT, Byte.javaClass),
    SMALLINT("smallint", Types.SMALLINT, Short.javaClass),
    INT("int", Types.INTEGER, Int.javaClass),
    BIGINT("bigint", Types.BIGINT, Long.javaClass), // hibernate是 BigInteger::class.java

    FLOAT("float", Types.FLOAT, Float.javaClass),
    DOUBLE("double", Types.DOUBLE, Double.javaClass),
    REAL("real", Types.REAL, Float.javaClass),

    // 在toJavaType()有特殊处理
    DECIMAL("decimal", Types.DECIMAL, BigDecimal::class.java),
    NUMERIC("numeric", Types.NUMERIC, BigDecimal::class.java),

    CHAR("char", Types.CHAR, String::class.java),
    VARCHAR("varchar", Types.VARCHAR, String::class.java),
    VARBINARY("varbinary", Types.VARBINARY, String::class.java),

    LONGNVARCHAR("longnvarchar", Types.LONGNVARCHAR, String::class.java),
    LONGVARBINARY("longvarbinary", Types.LONGVARBINARY, String::class.java),
    LONGVARCHAR("longvarchar", Types.LONGVARCHAR, String::class.java),

    BINARY("binary", Types.BINARY, ByteArray::class.java),
    BLOB("blob", Types.BLOB, ByteArray::class.java),
    CLOB("clob", Types.CLOB, String::class.java),

    NCHAR("nchar", Types.NCHAR, String::class.java),
    NCLOB("nclob", Types.NCLOB, String::class.java),
    NVARCHAR("nvarchar", Types.NVARCHAR, String::class.java),

    DATE("date", Types.DATE, Date::class.java),
    TIMESTAMP("timestamp", Types.TIMESTAMP, Date::class.java),
    TIME("time", Types.TIME, Date::class.java),

    OBJECT("object", Types.OTHER, Any::class.java);

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
            } ?: throw IllegalArgumentException("Unknow sql type: $sqlType")
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
    public fun toPhysicalType(db: IDb,  precision: Int? = null, scale: Int? = null): String {
        // 元数据定义的配置
        val config = Config.instance("db-meta.${db.dbType}", "yaml")
        // 获得对应的物理类型表达式
        val mapping:Map<String, String> = config["logicalType2PhysicalType"]!!
        val physicalType = mapping[code] ?: throw IllegalArgumentException("No physical type matching logical type [$code]")
        // 1 如果自带长度精度 或 没指定长度, 则直接输出
        if(physicalType.contains('(') || precision == null)
            return physicalType

        // 2 没指定精度 或 非小数类型(不需要精度)
        if(scale == null || !isDeciaml)
            return "$physicalType($precision)"

        // 3 有指定精度
        return "$physicalType($precision, $scale)"
    }

    /**
     * 转为java类型
     *    用于根据表结构来生成model时确定字段的Java类
     * @param tryPrimitiveType 尝试将 DECIMAL/NUMERIC 转为原始类型, 不过不是很准确, 特别是转为float/double会丢失精度, 生成代码后开发人员手动修正吧
     * @param precision 长度
     * @param scale 精度
     * @return
     */
    public open fun toJavaType(tryPrimitiveType: Boolean,  precision: Int? = null, scale: Int? = null): Class<*>{
        // 对 DECIMAL/NUMERIC 尝试转为原始类型
        if(tryPrimitiveType && (this == DECIMAL || this == NUMERIC)){
            if(scale == 0){ // 整型
                if(precision == null)
                    return BigInteger::class.java

                // max int: 2147483647 --10位
                if(precision <= 10)
                    return Int.javaClass

                // max long: 9223372036854775807 -- 19位
                if(precision <= 10)
                    return Long.javaClass
            }else if(precision != null){ // 浮点型
                // max float: 340282346638528860000000000000000000000 --39位
                if(precision <= 39)
                    return Float.javaClass

                // max double: 179769313486231570000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000 -- 309位
                if(precision <= 309)
                    return Double.javaClass
            }
        }
        return javaType
    }

    /**
     * 获得调用 cast() 时要转换的物理类型
     * @param db
     * @return
     */
    public fun getCastPhysicalType(db: IDb): String {
        // mysql单独处理
        if (db.dbType == DbType.Mysql) {
            when (this) {
                INT, BIGINT, SMALLINT -> return "signed"
                FLOAT, NUMERIC, REAL -> return "decimal"
                VARCHAR -> return "char" // cast(xxx as varchar) 报语法错误
                VARBINARY -> return "binary"
            }
        }

        // 其他直接转为对应的物理类型
        return toPhysicalType(db)
    }

}