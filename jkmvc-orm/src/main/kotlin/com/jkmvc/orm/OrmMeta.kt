package com.jkmvc.orm

import com.jkmvc.common.*
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
                   public override var primaryKey:DbKeyNames = DbKeyNames("id") /* 主键 */,
                   public override val dbName: String = "default" /* 数据库名 */,
                   public override var needInstanceInit: Boolean = true /* 实例化时是否需要初始化, 即调用类自身的默认构造函数 */
) : IOrmMeta {

    companion object{
        /**
         * 事件
         */
        val events:Array<String> = arrayOf("beforeCreate", "afterCreate", "beforeUpdate", "afterUpdate", "beforeSave", "afterSave", "beforeDelete", "afterDelete");
    }

    public constructor(
            model: KClass<out IOrm> /* 模型类 */,
            label: String /* 模型中文名 */,
            table: String /* 表名，假定model类名, 都是以"Model"作为后缀 */,
            primaryKey:String /* 主键 */,
            dbName: String = "default" /* 数据库名 */,
            needInstanceInit: Boolean = true /* 实例化时是否需要初始化, 即调用类自身的默认构造函数 */
    ):this(model, label, table, DbKeyNames(primaryKey), dbName, needInstanceInit)

    init{
        // 检查默认构造函数
        if(needInstanceInit && model.java.getConstructorOrNull() == null)
            throw OrmException("Class [${model}] has no no-arg constructor") // Model类${clazz}无默认构造函数
    }

    /**
     * 模型名
     */
    public override val name:String = model.modelName

    /**
     * 主键属性
     */
    public override val primaryProp:DbKeyNames = columns2Props(primaryKey)

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
            val handler = model.getFunction(event)  as KFunction<Unit>?
            if(handler != null)
                handlers[event] = handler
        }
        handlers
    }

    /**
     * 数据库
     */
    public override val db: IDb
        get() = Db.instance(dbName)

    /**
     * 默认外键
     */
    public override val defaultForeignKey:DbKeyNames
        get() = primaryKey.wrap(table + '_')  // table + '_' + primaryKey;

    /**
     * 表字段
     */
    public override val columns: List<String>
        get() = db.listColumns(table)

    /**
     * 对象属性
     */
    public override val props: List<String> by lazy {
        columns.map {
            column2Prop(it)
        }
    }

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
     * @param convertingValue 查询时是否智能转换字段值
     * @param convertingColumn 查询时是否智能转换字段名
     * @param withSelect with()联查时自动select关联表的字段
     * @return
     */
    public override fun queryBuilder(convertingValue: Boolean, convertingColumn: Boolean, withSelect: Boolean): OrmQueryBuilder {
        return OrmQueryBuilder(this, convertingValue, convertingColumn, withSelect);
    }

    /**
     * 添加规则
     * @param field
     * @param label
     * @param rule
     * @return
     */
    public override fun addRule(field: String, label:String, rule: String?): OrmMeta {
        rules[field] = RuleMeta(label, rule);
        return this;
    }

    /**
     * 添加规则
     * @param field
     * @param rule
     * @return
     */
    public override fun addRule(field: String, rule: IRuleMeta): OrmMeta {
        rules[field] = rule;
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
     * 能否处理任一事件
     * @param events 多个事件名，以|分隔，如 beforeCreate|afterCreate
     * @return
     */
    public override fun canHandleAnyEvent(events:String): Boolean {
        return eventHandlers.any { event, handler ->
            events.contains(event)
        }
    }

    /**
     * 如果有要处理的事件，则开启事务
     *
     * @param events 多个事件名，以|分隔，如 beforeCreate|afterCreate
     * @param statement
     * @return
     */
    public override fun <T> transactionWhenHandlingEvent(events:String, statement: () -> T): T{
        return db.transaction(!canHandleAnyEvent(events), statement)
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
    public override fun belongsTo(name: String, relatedModel: KClass<out IOrm>, foreignKey:DbKeyNames, primaryKey:DbKeyNames, conditions: Map<String, Any?>): IOrmMeta {
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
    public override fun hasOne(name: String, relatedModel: KClass<out IOrm>, foreignKey:DbKeyNames, primaryKey:DbKeyNames, conditions: Map<String, Any?>): IOrmMeta {
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
    public override fun hasMany(name: String, relatedModel: KClass<out IOrm>, foreignKey:DbKeyNames, primaryKey:DbKeyNames, conditions:Map<String, Any?>): IOrmMeta {
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
     * @param conditions 关联查询条件
     * @return
     */
    public override fun hasOneThrough(name: String, relatedModel: KClass<out IOrm>, foreignKey:DbKeyNames, primaryKey:DbKeyNames, middleTable:String, farForeignKey:DbKeyNames, farPrimaryKey:DbKeyNames, conditions:Map<String, Any?>): IOrmMeta {
        // 设置关联关系
        relations.getOrPut(name) {
            MiddleRelationMeta(this, RelationType.HAS_ONE, relatedModel, foreignKey, primaryKey, middleTable, farForeignKey, farPrimaryKey, conditions)
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
     * @param conditions 关联查询条件
     * @return
     */
    public override fun hasManyThrough(name: String, relatedModel: KClass<out IOrm>, foreignKey:DbKeyNames, primaryKey:DbKeyNames, middleTable:String, farForeignKey:DbKeyNames, farPrimaryKey:DbKeyNames, conditions:Map<String, Any?>): IOrmMeta {
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
        val prop = model.getProperty(column)
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
     * @param select 是否select关联字段
     * @param columns 关联字段列表
     * @param lastName 上一级关系名
     * @param path 列名父路径
     * @return 关联关系
     */
    public override fun joinRelated(query: OrmQueryBuilder, name: String, select: Boolean, columns: SelectColumnList?, lastName:String, path:String): IRelationMeta {
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
        if (relation.type == RelationType.BELONGS_TO) { // belongsto: join 主表
            query.joinMaster(this, lastName, relation, name);
        }else{ // hasxxx: join 从表
            if(relation is MiddleRelationMeta) // 有中间表
                query.joinSlaveThrough(this, lastName, relation, name);
            else // 无中间表
                query.joinSlave(this, lastName, relation, name);
        }

        //列名父路径
        val path2 = if(path == "")
                        name
                      else
                        path + ":" + name

        // 递归联查子关系
        columns?.forEachRelatedColumns { subname: String, subcolumns: SelectColumnList? ->
            relation.ormMeta.joinRelated(query, subname, select, subcolumns, name, path2)
        }

        // 查询当前关系字段
        if(select)
            query.selectRelated(relation, name, columns?.myColumns /* 若字段为空，则查全部字段 */, path2);

        return relation;
    }

}

