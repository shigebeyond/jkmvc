package com.jkmvc.util

import com.jkmvc.common.Config
import com.jkmvc.db.Db
import com.jkmvc.db.recordTranformer
import java.io.File


/**
 * 模型代码生成器
 *
 * @author shijianhang
 * @date 2017-10-10
 */
class ModelGenerator(val srcDir:String){

    /**
     * 数据库
     */
    private val db = Db.getDb()

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
     * @param pck 包
     * @param model 模型名
     * @param label 标题
     * @param table 表名
     */
    public fun genenateModelFile(pck:String, model:String, label:String, table: String): Unit {
        val file = "$srcDir/$pck/$model".replace('.', '/') + ".kt"
        val code = genenateModelClass(pck, model, label, table)
        File(file).writeText(code)
        println("生成${model}模型文件: $file")
    }

    /**
     * 生成类
     *
     * @param pck 包
     * @param model 模型名
     * @param label 标题
     * @param table 表名
     * @return
     */
    public fun genenateModelClass(pck:String, model:String, label:String, table: String): String {
        // 查询字段的sql
        val sql = config.getString("columns")!!
        val fields = db.queryRows(sql, listOf(table), Map::class.recordTranformer)
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
        code.append("package $pck \n\n")
        code.append("import com.jkmvc.orm.OrmMeta \nimport com.jkmvc.orm.Orm \n\n")
        code.append("/**\n * $label\n *\n * @ClassName: $model\n * @Description:\n * @author shijianhang<772910474@qq.com>\n * @date 2017-09-29 6:56 PM\n */\n")
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