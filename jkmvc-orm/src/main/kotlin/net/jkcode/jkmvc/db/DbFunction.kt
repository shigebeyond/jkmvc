package net.jkcode.jkmvc.db

import net.jkcode.jkutil.common.Config
import net.jkcode.jkutil.common.replacesFormat

/**
 * 函数
 *
 * @author shijianhang
 * @date 2020-2-4 下午8:02:47
 */
class DbFunction(public val name: String /* 函数名 */ ) {

    companion object{

        /**
         * 单个参数的正则
         */
        val ARG_REG = "\\?(\\d+)".toRegex()

        /**
         * 不定参数表达式的正则
         *   匹配项: 1 前缀 2 分隔符 3 后缀
         */
        val VAR_ARG_REG = "(.*)\\?n<(.+)>(.*)".toRegex()
    }

    /**
     * 转为物理的函数表达式
     * @param db
     * @param args 参数
     * @return
     */
    public fun toPhysicalFunction(db: IDb, args: List<String>): String {
        // 函数映射的配置
        val config = Config.instance("db-function", "yaml")
        // 获得对应的物理的函数表达式
        val customMapping:Map<String, String>? = config[db.dbType.name] // 特定db的函数映射
        val allMapping:Map<String, String>? = config["All"] // 通用的函数映射
        val physicalFunction = customMapping?.get(name) ?: allMapping?.get(name)

        // 1 无映射, 则输出函数名+参数列表(用,分割)
        if(physicalFunction.isNullOrBlank() || physicalFunction.equals(name, true))
            return args.joinToString(", ", "$name(", ")")

        // 2 有映射, 则转为表达式
        // 2.1 无指定参数, 则直接输出
        if(!physicalFunction.contains("?"))
            return physicalFunction

        // 2.2 不定参数, 识别出前缀/后缀/分隔符, 然后拼接
        if(physicalFunction.contains("?n")){
            val matches:MatchResult? = VAR_ARG_REG.find(physicalFunction)
            if(matches == null)
                return physicalFunction

            val prefix = matches.groupValues[1] // 前缀
            val separator = matches.groupValues[2] // 分隔符
            val postfix = matches.groupValues[3] // 后缀
            return args.joinToString(separator, prefix, postfix)
        }

        // 2.3 确定参数, 直接替换参数
        // 特别处理 cast() 中的类型参数
        if(name.equals("cast", true)){
            // 只有2个参数, 且第二个参数是类型, 需要转换
            if (args.size != 2)
                throw IllegalArgumentException("cast() requires two arguments; found : " + args.size)

            // 转换cast类型
            val type = DbColumnLogicType.getByCode(args[1])
            (args as MutableList)[1] = type.getCastPhysicalType(db)
        }

        return VAR_ARG_REG.replace(physicalFunction){ matches ->
            val i = matches.groupValues[1].toInt()
            args[i]
        }
    }

}