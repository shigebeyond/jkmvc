package com.jkmvc.orm

import com.jkmvc.common.Config
import com.jkmvc.common.format
import com.jkmvc.db.Db
import com.jkmvc.db.IDb
import com.jkmvc.db.IDbQueryBuilder
import java.io.File
import java.util.*
import kotlin.reflect.KClass

/**
 * orm的元数据
 * 　两重职责
 *  1 模型映射表的映射元数据，如模型类/数据库/表名
 *  2 代理模型的属性读写
 */
open class MetaData(public override val model: KClass<out IOrm> /* 模型类 */,
                    public override val dbName: String = "default" /* 数据库名 */,
                    public override var table: String = model.modelName /* 表名，假定model类名, 都是以"Model"作为后缀 */,
                    public override var primaryKey: String = "id" /* 主键 */
) : IMetaData {

    /**
     * 关联关系
     */
    public override val relations: MutableMap<String, IMetaRelation> by lazy {
        HashMap<String, IMetaRelation>()
    }

    /**
     * 每个字段的校验规则
     */
    public override val rules: MutableMap<String, IMetaRule> by lazy {
        HashMap<String, IMetaRule>()
    };

    /**
     * 数据库
     */
    public override val db: IDb
        get() = Db.getDb(dbName)

    /**
     * 默认外键
     */
    public override val defaultForeignKey: String
        get() = table + '_' + primaryKey;

    /**
     * 表字段
     */
    public override val columns: List<String>
        get() = db.listColumns(table)

    /**
     * 是否有某个关联关系
     * @param name
     * @return
     */
    public override fun hasRelation(name: String): Boolean {
        //return name in relations; // 啃爹啊，ConcurrentHashMap下的 in 语义是调用 contains()，但是我想调用 containsKey()
        return relations.containsKey(name)
    }

    /**
     * 获得某个关联关系
     * @param name
     * @return
     */
    public override fun getRelation(name: String): IMetaRelation? {
        return relations.get(name);
    }

    /**
     * 获得orm查询构建器
     * @return
     */
    public override fun queryBuilder(): OrmQueryBuilder {
        return OrmQueryBuilder(this);
    }

    /**
     * 添加规则
     * @param name
     * @param label
     * @param rule
     * @return
     */
    public override fun addRule(name: String, label:String, rule: String?): MetaData
    {
        rules[name] = MetaRule(label, rule);
        return this;
    }

    /**
     * 添加规则
     * @param name
     * @param rule
     * @return
     */
    public override fun addRule(name: String, rule: IMetaRule): MetaData
    {
        rules[name] = rule;
        return this;
    }


    /**
     * 生成属性代理 + 设置关联关系(belongs to)
     * @param name 字段名
     * @param relatedModel 关联模型
     * @param foreignKey 外键
     * @param conditions 关联查询条件
     */
    public override fun belongsTo(name: String, relatedModel: KClass<out IOrm>, foreignKey: String, conditions: ((IDbQueryBuilder) -> Unit)?): IMetaData {
        // 获得外键
        var fk = foreignKey;
        if (fk == "")
            fk = relatedModel.modelMetaData.defaultForeignKey

        // 设置关联关系
        relations.getOrPut(name) {
            MetaRelation(RelationType.BELONGS_TO, relatedModel, fk, conditions)
        }

        return this;
    }

    /**
     * 设置关联关系(has one)
     * @param name 字段名
     * @param relatedModel 关联模型
     * @param foreignKey 外键
     * @param conditions 关联查询条件
     */
    public override fun hasOne(name: String, relatedModel: KClass<out IOrm>, foreignKey: String, conditions: ((IDbQueryBuilder) -> Unit)?): IMetaData {
        // 获得外键
        var fk = foreignKey;
        if (fk == "")
            fk = this.defaultForeignKey


        // 设置关联关系
        relations.getOrPut(name) {
            MetaRelation(RelationType.HAS_ONE, relatedModel, fk, conditions)
        }

        return this;
    }

    /**
     * 设置关联关系(has many)
     * @param name 字段名
     * @param relatedModel 关联模型
     * @param foreignKey 外键
     * @param conditions 关联查询条件
     */
    public override fun hasMany(name: String, relatedModel: KClass<out IOrm>, foreignKey: String, conditions: ((IDbQueryBuilder) -> Unit)?): IMetaData {
        // 获得外键
        var fk = foreignKey;
        if (fk == "")
            fk = this.defaultForeignKey

        // 设置关联关系
        relations.getOrPut(name) {
            MetaRelation(RelationType.HAS_MANY, relatedModel, fk, conditions)
        }

        return this;
    }
}

