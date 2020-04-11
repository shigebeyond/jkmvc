package net.jkcode.jkmvc.util

import net.jkcode.jkutil.common.Config
import net.jkcode.jkutil.common.format
import net.jkcode.jkutil.common.prepareDirectory
import net.jkcode.jkmvc.db.Db
import net.jkcode.jkmvc.db.DbColumn
import java.io.File
import java.util.*


/**
 * 模型代码生成器
 *
 * @author shijianhang
 * @date 2017-10-10
 */
class ModelGenerator(val srcDir:String /* 源码目录 */,
                     val pck: String /* 包路径 */,
                     val dbName: String = "default" /* 数据库名 */,
                     val author: String = "" /* 作者 */){

    /**
     * 数据库
     */
    private val db = Db.instance(dbName)

    /**
     * 元数据定义的配置
     */
    private val config = Config.instance("db-meta.${db.dbType}", "yaml")

    /**
     * 获得字段的对应的属性名
     * @return
     */
    private fun getProp(column:String):String{
        return db.column2Prop(column)
    }

    /**
     * 获得字段的java类型
     * @param column 字段
     * @return
     */
    private fun getType(column: DbColumn):String{
        val clazz = column.logicType.toJavaType(true, column.precision, column.scale)
        return clazz.name.replace("java.lang.", "")
    }

    /**
     * 生成类文件
     *
     * @param model 模型名
     * @param label 标题
     * @param table 表名
     * @return
     */
    public fun genenateModelFile(model:String, label:String, table: String): Boolean {
        val dir = "$srcDir/$pck".replace('.', '/')
        dir.prepareDirectory()
        val path = "$dir/$model.kt"
        val f = File(path)
        if(f.exists()){
            println("生成${model}模型文件失败: $path 文件已存在")
            return false
        }

        val code = genenateModelClass(model, label, table)
        f.createNewFile()
        f.writeText(code)
        println("生成${model}模型文件: $path")
        return true
    }

    /**
     * 生成类
     *
     * @param model 模型名
     * @param label 标题
     * @param table 表名
     * @return
     */
    public fun genenateModelClass(model:String, label:String, table: String): String {
        // 查询字段的sql
        val fields = db.getColumnsByTable(table)
        // 找到主键
        val pks = ArrayList<Pair<String, String>>() // 主键的字段名+类型
        for (field in fields){
            if(field.name == "PRI"){
                val name = field.name
                val type = getType(field)
                pks.add(name to type)
            }
        }

        // 0 注释与包
        val code = StringBuilder()
        val date = Date().format()
        code.append("package $pck \n\n")
        code.append("import net.jkcode.jkmvc.orm.* \nimport java.util.*\n\n")
        code.append("/**\n * $label\n *\n * @author shijianhang<772910474@qq.com>\n * @date $date\n */\n")
        // 1 类
        code.append("class $model")
        // 2 主键参数
        if(pks.size == 1) { // 单主键
            val (name, type) = pks.first()
            code.append("(${getProp(name)}:$type? = null): Orm(${getProp(name)}) {\n")
        }else { // 多主键
            // 默认构造函数
            code.append("(pk: Array<Any> = emptyArray()): Orm(pk) {\n\n")

            // 多参数构造函数
            // public constructor((name1:type1, name2:type2)
            pks.joinTo(code, ", ", "\tpublic constructor(", ")") { (name, type) ->
                "${getProp(name)}: $type"
            }
            // : Orm(name1, name2) {\n
            pks.joinTo(code, ", ", " : this(arrayOf(", "))\n\n") { (name, type) ->
                getProp(name)
            }
        }
        // 3 元数据
        val pkMeta = if(pks.size == 1) "\"${pks.first().first}\"" else pks.joinToString(", ", "DbKeyNames(", ")") {(name, type) ->
            "\"$name\""
        }
        code.append("\t// 伴随对象就是元数据\n \tcompanion object m: OrmMeta($model::class, \"$label\", \"$table\", $pkMeta){}\n\n")
        // 4 属性
        code.append("\t// 代理属性读写")
        // 遍历字段来生成属性
        for (field in fields){
            val name = getProp(field.name)
            val type = getType(field)
            val comment = field.comment
            code.append("\n\tpublic var $name:$type by property() // $comment \n")
        }
        code.append("\n}")
        return code.toString()
    }
}