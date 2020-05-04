package net.jkcode.jkmvc.db

import net.jkcode.jkutil.common.Config
import net.jkcode.jkutil.common.generateUUID
import net.jkcode.jkutil.common.replacesFormat
import java.util.HashMap
import java.util.regex.Pattern

/**
 * 函数
 *
 * @author shijianhang
 * @date 2020-2-4 下午8:02:47
 */
object DbFunction {

    /**
     * 单个参数的正则
     *    函数表达式语法: 函数名(?1, ?2, ...), 其中 ?+数字i 表示第i个参数(从1开始), 其中特别的是 ?n 表示不定参数(不确定个数)
     */
    val ARG_REG = "\\?(\\d+)".toRegex()

    /**
     * 不定参数表达式的正则
     *   函数表达式语法: 前缀?n<分隔符>后缀 , 其中 ?n 表示不定参数(不确定个数), ?n紧接着的 <> 包住的是分隔符, 一般 ?n 都用()包住
     *   匹配项: 1 前缀 2 分隔符 3 后缀
     */
    val VAR_ARG_REG = "(.*)\\?n<(.+)>(.*)".toRegex()

    /**
     * 转为物理的语句
     *   解析语句, 并转换其中的函数
     *
     * @param db
     * @param query 语句, 可以是sql, 也可以是sql的部分
     * @return
     */
    public fun toPhysicalQuery(db: IDb, query: String): String {
        var tempQuery = query
        // <uuid, 物理函数>
        val usedFunctions = HashMap<String, String>()
        // 发现函数
        val pattern = Pattern.compile("([a-zA-z0-9]+)\\(([^\\(^\\)]*)\\)") // 函数名(函数参数)
        val matcher = pattern.matcher(query)
        // 循环匹配并收集函数
        while (matcher.find()) {
            val uuid = "func_" + generateUUID()
            val functionString = matcher.group(0) // 函数表达式
            val functionName = matcher.group(1).toLowerCase() // 函数名
            val agrsString = matcher.group(2) // 函数参数
            // 隐藏函数: 逻辑函数 -> uuid
            // 隐藏当前这一层函数, 以便继续处理上一层函数
            tempQuery = tempQuery.replace(functionString, uuid)

            // 解析参数
            var args: List<String> = if (agrsString.contains(" as "))
                agrsString.split(" as ")
            else
                agrsString.split(",")

            // 转换jql的函数为物理函数: 特定db的sql函数
            val resultedQuery = toPhysicalFunction(db, functionName, args)
            usedFunctions.put(uuid, resultedQuery)
        }

        if (!usedFunctions.isEmpty()) {
            // 转为物理函数
            tempQuery = toPhysicalQuery(db, tempQuery)
            // 恢复函数: uuid -> 物理函数
            for ((uuid, func) in usedFunctions)
                tempQuery = tempQuery.replace(uuid, func)
        }

        return tempQuery
    }

    /**
     * 转为物理的函数表达式
     * @param db
     * @param name 函数名
     * @param args 参数
     * @return
     */
    public fun toPhysicalFunction(db: IDb, name: String, args: List<String> = emptyList()): String {
        // 函数映射的配置
        val config = Config.instance("db-function", "yaml")
        // 获得对应的物理的函数表达式
        val customMapping: Map<String, String>? = config[db.dbType.name] // 特定db的函数映射
        val allMapping: Map<String, String>? = config["All"] // 通用的函数映射
        val physicalFunction = customMapping?.get(name) ?: allMapping?.get(name)

        // 1 无映射, 则输出函数名+参数列表(用,分割)
        if (physicalFunction.isNullOrBlank() || physicalFunction.equals(name, true))
            return args.joinToString(", ", "$name(", ")")

        // 2 有映射, 则转为表达式
        // 2.1 无指定参数, 则直接输出
        if (!physicalFunction.contains("?"))
            return physicalFunction

        // 2.2 不定参数, 识别出前缀/后缀/分隔符, 然后拼接
        if (physicalFunction.contains("?n")) {
            val matches: MatchResult? = VAR_ARG_REG.find(physicalFunction)
            if (matches == null)
                return physicalFunction

            val prefix = matches.groupValues[1] // 前缀
            val separator = matches.groupValues[2] // 分隔符
            val postfix = matches.groupValues[3] // 后缀
            return args.joinToString(separator, prefix, postfix)
        }

        // 2.3 确定参数, 直接替换参数
        // 特别处理 cast() 中的类型参数
        if (name.equals("cast", true)) {
            // 只有2个参数, 且第二个参数是类型, 需要转换
            if (args.size != 2)
                throw IllegalArgumentException("cast() requires two arguments; found : " + args.size)

            // 转换cast类型
            val type = DbColumnLogicType.getByCode(args[1])
            (args as MutableList)[1] = type.getCastPhysicalType(db)
        }

        return ARG_REG.replace(physicalFunction) { matches ->
            val i = matches.groupValues[1].toInt() // 形参从1开始
            args[i-1] // 实参从0开始
        }
    }

}