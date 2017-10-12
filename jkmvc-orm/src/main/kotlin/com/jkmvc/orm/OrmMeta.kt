package com.jkmvc.orm

import com.jkmvc.common.findFunction
import com.jkmvc.common.findProperty
import com.jkmvc.common.to
import com.jkmvc.db.Db
import com.jkmvc.db.IDb
import java.util.*
import kotlin.reflect.KClass
import kotlin.reflect.KFunction

/**
 * orm的元数据
 * 　两重职责
 *  1 模型映射表的映射元数据，如模型类/数据库/表名
 *  2 代理模型的属性读写
 */
open class OrmMeta(public override val model: KClass<out IOrm> /* 模型类 */,
                   public override val label: String = model.modelName /* 模型中文名 */,
                   public override var table: String = model.modelName /* 表名，假定model类名, 都是以"Model"作为后缀 */,
                   public override var primaryKey: String = "id" /* 主键 */,
                   public override val dbName: String = "default" /* 数据库名 */
) : IOrmMeta {

    companion object{
        /**
         * 事件
         */
        val events:Array<String> = arrayOf("beforeCreate", "afterCreate", "beforeUpdate", "afterUpdate", "beforeSave", "afterSave", "beforeDelete", "afterDelete");
    }

    /**
     * 模型名
     */
    public override val name:String = model.modelName

    /**
     * 主键属性
     */
    public override val primaryProp:String = column2Prop(primaryKey)

    /**
     * 关联关系
     */
    public override val relations: MutableMap<String, IRelationMeta> by lazy {
        HashMap<String, IRelationMeta>()
    }

    /**
     * 每个字段的校验规则
     */
    public override val rules: MutableMap<String, IRuleMeta> by lazy {
        HashMap<String, IRuleMeta>()
    };

    /**
     * 事件处理器
     */
    public override val eventHandlers:Map<String, KFunction<Unit>?> by lazy {
        val handlers = HashMap<String, KFunction<Unit>?>()
        // 遍历每个事件填充
        for (event in events){
            val handler = model.findFunction(event)  as KFunction<Unit>?
            if(handler != null)
                handlers[event] = handler
        }
        handlers
    }

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
    public override fun getRelation(name: String): IRelationMeta? {
        return relations.get(name);
    }

    /**
     * 获得orm查询构建器
     *
     * @param convertValue 查询时是否智能转换字段值
     * @param convertColumn 查询时是否智能转换字段名
     * @return
     */
    public override fun queryBuilder(convertValue: Boolean, convertColumn: Boolean): OrmQueryBuilder {
        return OrmQueryBuilder(this, convertValue, convertColumn);
    }

    /**
     * 添加规则
     * @param name
     * @param label
     * @param rule
     * @return
     */
    public override fun addRule(name: String, label:String, rule: String?): OrmMeta
    {
        rules[name] = RuleMeta(label, rule);
        return this;
    }

    /**
     * 添加规则
     * @param name
     * @param rule
     * @return
     */
    public override fun addRule(name: String, rule: IRuleMeta): OrmMeta
    {
        rules[name] = rule;
        return this;
    }

    /**
     * 获得事件处理器
     * @param event 事件名
     * @return
     */
    public override fun getEventHandler(event:String): KFunction<Unit>? {
        return eventHandlers.getOrDefault(event, null)
    }

    /**
     * 生成属性代理 + 设置关联关系(belongs to)
     * @param name 字段名
     * @param relatedModel 关联模型
     * @param foreignKey 外键
     * @param primaryKey 主键
     * @param conditions 关联查询条件
     * @return
     */
    public override fun belongsTo(name: String, relatedModel: KClass<out IOrm>, foreignKey: String, primaryKey:String, conditions: Map<String, Any?>): IOrmMeta {
        // 设置关联关系
        relations.getOrPut(name) {
            RelationMeta(this, RelationType.BELONGS_TO, relatedModel, foreignKey, primaryKey, conditions)
        }

        return this;
    }

    /**
     * 设置关联关系(has one)
     * @param name 字段名
     * @param relatedModel 关联模型
     * @param foreignKey 外键
     * @param primaryKey 主键
     * @param conditions 关联查询条件
     * @return
     */
    public override fun hasOne(name: String, relatedModel: KClass<out IOrm>, foreignKey: String, primaryKey:String, conditions: Map<String, Any?>): IOrmMeta {
        // 设置关联关系
        relations.getOrPut(name) {
            RelationMeta(this, RelationType.HAS_ONE, relatedModel, foreignKey, primaryKey, conditions)
        }

        return this;
    }

    /**
     * 设置关联关系(has many)
     * @param name 字段名
     * @param relatedModel 关联模型
     * @param foreignKey 外键
     * @param primaryKey 主键
     * @param conditions 关联查询条件
     * @return
     */
    public override fun hasMany(name: String, relatedModel: KClass<out IOrm>, foreignKey: String, primaryKey:String, conditions:Map<String, Any?>): IOrmMeta {
        // 设置关联关系
        relations.getOrPut(name) {
            RelationMeta(this, RelationType.HAS_MANY, relatedModel, foreignKey, primaryKey, conditions)
        }

        return this;
    }

    /**
     * 设置关联关系(has one)
     * @param name 字段名
     * @param relatedModel 关联模型
     * @param foreignKey 外键
     * @param primaryKey 主键
     * @param middleTable 中间表
     * @param farForeignKey 远端外键
     * @param farPrimaryKey 远端主键
     * @return
     */
    public override fun hasOneThrough(name: String, relatedModel: KClass<out IOrm>, foreignKey: String,  primaryKey: String,  middleTable:String,  farForeignKey:String,  farPrimaryKey:String): IOrmMeta {
        // 设置关联关系
        relations.getOrPut(name) {
            MiddleRelationMeta(this, RelationType.HAS_ONE, relatedModel, foreignKey, primaryKey, middleTable, farForeignKey, farPrimaryKey)
        }

        return this;
    }

    /**
     * 设置关联关系(has many)
     * @param name 字段名
     * @param relatedModel 关联模型
     * @param foreignKey 外键
     * @param primaryKey 主键
     * @param middleTable 中间表
     * @param farForeignKey 远端外键
     * @param farPrimaryKey 远端主键
     * @return
     */
    public override fun hasManyThrough(name: String, relatedModel: KClass<out IOrm>, foreignKey: String,  primaryKey: String,  middleTable:String,  farForeignKey:String,  farPrimaryKey:String): IOrmMeta {
        // 设置关联关系
        relations.getOrPut(name) {
            MiddleRelationMeta(this, RelationType.HAS_MANY, relatedModel, foreignKey, primaryKey, middleTable, farForeignKey, farPrimaryKey)
        }

        return this;
    }

    /**
     * 智能转换字段值
     *    在不知字段类型的情况下，将string赋值给属性
     *    => 需要将string转换为属性类型
     *    => 需要显式声明属性
     *
     * @param column
     * @param value 字符串
     * @return
     */
    public override fun convertIntelligent(column:String, value:String):Any?
    {
        // 1 获得属性
        val prop = model.findProperty(column)
        if(prop == null)
            throw OrmException("类 ${model} 没有属性: $column");

        // 2 转换类型
        return value.to(prop.getter.returnType)
    }

    /**
     * 联查关联表
     *
     * @param query 查询构建器
     * @param name 关联关系名
     * @param columns 关联字段列表
     * @param lastName 上一级关系名
     * @param path 列名父路径
     * @return 关联关系
     */
    public override fun joinRelated(query: OrmQueryBuilder, name: String, columns: SelectColumnList?, lastName:String, path:String): IRelationMeta {
        // 获得当前关联关系
        val relation = getRelation(name)!!;
        // 1 非hasMany关系：只处理一层
        if(relation.type == RelationType.HAS_MANY){
            // 单独处理hasMany关系，不在一个sql中联查，而是单独查询
            query.withMany(name, columns)
            return relation;
        }

        // 2 非hasMany关系
        // join关联表
        when (relation.type) {
            // belongsto: join 主表
            RelationType.BELONGS_TO -> query.joinMaster(this, lastName, relation, name);
            // hasxxx: join 从表
            else -> query.joinSlave(this, lastName, relation, name);
        }

        //列名父路径
        val path2 = if(path == "")
                        name
                      else
                        path + ":" + name

        // 递归联查子关系
        columns?.forEachRelatedColumns { subname: String, subcolumns: SelectColumnList? ->
            relation.ormMeta.joinRelated(query, subname, subcolumns, name, path2)
        }

        // 查询当前关系字段
        query.selectRelated(relation, name, columns?.myColumns /* 若字段为空，则查全部字段 */, path2);

        return relation;
    }

}

