package net.jkcode.jkmvc.util

import net.jkcode.jkmvc.common.Config
import net.jkcode.jkmvc.common.format
import net.jkcode.jkmvc.common.prepareDirectory
import net.jkcode.jkmvc.db.Db
import org.apache.commons.collections.map.HashedMap
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
     * 元数据查询的配置
     */
    private val config = Config.instance("meta-query.${db.dbType}", "yaml")

    /**
     * 获得查询字段的sql
     * @return
     */
    private fun getColumnsSql():String{
        return config.getString("columns")!!
    }

    /**
     * 获得字段的对应的属性名
     * @return
     */
    private fun getProp(column:String):String{
        return db.column2Prop(column)
    }

    /**
     * 获得字段的类型
     * @return
     */
    private fun getType(columnType:String):String{
        val mapping:Map<String, String> = config["types"]!!
        for((typeRegex, propType) in mapping){
            if(typeRegex.toRegex().containsMatchIn(columnType))
                return propType
        }
        return "*"
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
        val sql = config.getString("columns")!!
        val fields = db.queryMaps(sql, listOf(table, db.schema)) // org.apache.commons.collections.map.HashedMap.HashedMap(java.util.Map)
        // 找到主键
        var pk:String = ""
        var pkType: String = "Any"
        for (field in fields){
            if(field["COLUMN_KEY"] == "PRI"){
                pk = field["COLUMN_NAME"] as String
                pkType = getType(field["COLUMN_TYPE"] as String)
            }
        }

        // 1 注释与包
        val code = StringBuilder()
        val date = Date().format()
        code.append("package $pck \n\n")
        code.append("import net.jkcode.jkmvc.orm.OrmMeta \nimport net.jkcode.jkmvc.orm.Orm \n\n")
        code.append("/**\n * $label\n *\n * @author shijianhang<772910474@qq.com>\n * @date $date\n */\n")
        // 2 类
        code.append("class $model(id:$pkType? = null): Orm(id) {\n")
        // 3 元数据
        code.append("\t// 伴随对象就是元数据\n \tcompanion object m: OrmMeta($model::class, \"$label\", \"$table\", \"$pk\"){}\n\n")
        // 4 属性
        code.append("\t// 代理属性读写")
        // 遍历字段来生成属性
        for (field in fields){
            val name = getProp(field["COLUMN_NAME"] as String)
            val type = getType(field["COLUMN_TYPE"] as String)
            val comment = field["COLUMN_COMMENT"]
            code.append("\n\tpublic var $name:$type by property() // $comment \n")
        }
        code.append("\n}")
        return code.toString()
    }
}