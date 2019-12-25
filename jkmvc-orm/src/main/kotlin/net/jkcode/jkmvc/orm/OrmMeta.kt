package net.jkcode.jkmvc.orm

import net.jkcode.jkutil.common.*
import net.jkcode.jkmvc.db.Db
import net.jkcode.jkmvc.db.IDb
import net.jkcode.jkmvc.model.GeneralModel
import net.jkcode.jkmvc.query.DbExpr
import net.jkcode.jkutil.validator.IValidator
import net.jkcode.jkutil.validator.ValidateException
import java.util.*
import kotlin.collections.set
import kotlin.reflect.KClass
import kotlin.reflect.full.isSubclassOf

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
                   public override val dbName: String = "default" /* 数据库名 */
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
            dbName: String = "default" /* 数据库名 */
    ):this(model, label, table, DbKeyNames(primaryKey), dbName)

    init{
        // 检查 model 类的默认构造函数
        if(model != GeneralModel::class && model.java.getConstructorOrNull() == null)
            throw OrmException("Model Class [$model] has no no-arg constructor") // Model类${clazz}无默认构造函数
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
    public override val rules: MutableMap<String, IValidator> by lazy {
        HashMap<String, IValidator>()
    };

    /**
     * 能处理的序列化事件
     *   就是在子类中重写了事件处理函数
     */
    public override val processableEvents: List<String> by lazy{
        "beforeCreate|afterCreate|beforeUpdate|afterUpdate|beforeSave|afterSave|beforeDelete|afterDelete".split('|').filter { event ->
            val method = model.java.getMethod(event)
            method.declaringClass != OrmEntity::class.java // 实际上事件处理方法是定义在 IOrmPersistent 接口, 但反射中获得声明类却是 OrmEntity 抽象类
        }
    }

    /**
     * 获得实体类: 模型类实现 IEntitiableOrm 接口时, 指定的泛型类型
     */
    public override val entityClass: Class<*>? by lazy {
        if(model.isSubclassOf(IEntitiableOrm::class))
            model.java.getInterfaceGenricType(IEntitiableOrm::class.java)
        else
            null
    }

    /**
     * 数据库
     */
    public override val db: IDb
        get() = Db.instance(dbName)

    /**
     * 默认外键
     */
    public override val defaultForeignKey:DbKeyNames by lazy{
        primaryKey.wrap(table + '_')  // table + '_' + primaryKey;
    }

    /**
     * 表字段
     */
    public override val columns: List<String> by lazy{
        db.listColumns(table)
    }

    /**
     * 对象属性
     */
    public override val props: List<String> by lazy {
        columns.map {
            column2Prop(it)
        }
    }

    /**
     * 要序列化的对象属性
     *   写时序列化, 读时反序列化
     */
    public override val serializingProps: List<String> = emptyList()

    /**
     * 默认要设置的字段名
     */
    public override val defaultExpectedProps: List<String> by lazy{
        val columns = ArrayList<String>()
        // 本模型的字段
        columns.addAll(props)
        // 关联对象
        columns.addAll(relations.keys)
        columns
    }

    /**
     * 数据的工厂
     */
    public override val dataFactory: FixedKeyMapFactory by lazy{
        val keys = ArrayList<String>(props.size + relations.size)
        keys.addAll(props) // 1 对象属性
        keys.addAll(relations.keys) // 2 关联属性
        for((_, relation) in relations){ // 3 有中间表关联关系: 中间表的外键属性
            if(relation is MiddleRelationMeta)
                keys.addAll(relation.middleForeignProp.columns)
        }
        FixedKeyMapFactory(*keys.toTypedArray())
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
     * 如果有要处理的事件，则开启事务
     *
     * @param events 多个事件名，以|分隔，如 beforeCreate|afterCreate
     * @param statement
     * @return
     */
    public override fun <T> transactionWhenHandlingEvent(events:String, statement: () -> T): T{
        return db.transaction(!canHandleAnyEvent(events), statement)
    }

    /********************************* 校验 **************************************/
    /**
     * 添加规则
     * @param field
     * @param rule
     * @return
     */
    public override fun addRule(field: String, rule: IValidator): OrmMeta {
        rules[field] = rule;
        return this;
    }

    /**
     * 校验orm对象数据
     * @param item
     */
    public override fun validate(item: IOrmEntity) {
        // 逐个属性校验
        val errors = HashMap<String, String>()
        for ((field, rule) in rules) {
            // 1 获得属性值
            val value: Any = item[field];

            // 2 校验单个属性: 属性值可能被修改
            val result = rule.validate(value, (item as OrmEntity).getData())

            // 3 校验失败, 记录错误
            if(result.error != null) {
                errors[field] = result.error!!
                continue
            }

            // 4 校验成功, 更新被修改的属性值
            if (value !== result.value)
                item[field] = result.value;
        }

        if(errors.isNotEmpty())
            throw ValidateException("Fail to validate $name model", name, errors)
    }

    /********************************* query builder **************************************/
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
     * 批量插入
     *   一般用于批量插入 OrmEntity 对象, 而不是 Orm 对象, 因此也不会触发 Orm 中的前置后置事件
     *
     * @param items
     * @return
     */
    public override fun batchInsert(items: List<IOrmEntity>): IntArray {
        if(items.isEmpty())
            return emptyIntArray()

        // 校验
        for (item in items)
            validate(item)

        // 构建insert语句
        // insert字段 -- 取全部字段, 不能取第一个元素的字段, 因为每个元素可能修改的字段都不一样, 这样会导致其他元素漏掉更新某些字段
        /*val props = (items.first() as OrmEntity).getData().keys
        val columns = props.mapToArray { prop ->
            prop2Column(prop)
        }*/
        // value字段值
        val values = DbExpr.question.repeateToArray(columns.size)
        val query = queryBuilder().insertColumns(*columns.toTypedArray()).value(*values)

        // 构建参数
        val params:ArrayList<Any?> = ArrayList()
        for (item in items){
            for(prop in props) {
                params.add(item[prop])
            }
        }

        // 批量插入
        return query.batchInsert(params)
    }

    /**
     * 批量更新
     *   一般用于批量更新 OrmEntity 对象, 而不是 Orm 对象, 因此也不会触发 Orm 中的前置后置事件
     *
     * @param items
     * @return
     */
    public override fun batchUpdate(items: List<IOrmEntity>): IntArray {
        if(items.isEmpty())
            return emptyIntArray()

        // 校验
        for (item in items)
            validate(item)

        // 构建update语句
        val query = queryBuilder()
        // set字段: 取全部字段, 不能取第一个元素的字段, 因为每个元素可能修改的字段都不一样, 这样会导致其他元素漏掉更新某些字段
        //val props = (items.first() as OrmEntity).getData().keys
        val props = ArrayList(this.props)
        props.removeAll(primaryProp.columns)
        for(prop in props)
            query.set(prop2Column(prop), DbExpr.question)

        // where主键
        query.where(primaryKey, DbExpr.question)

        // 构建参数
        val params:ArrayList<Any?> = ArrayList()
        for (item in items){
            // 属性值
            for(prop in props)
                params.add(item[prop])
            // 主键值
            for(pk in primaryProp.columns)
                params.add(item[pk])
        }

        // 批量更新
        return query.batchUpdate(params);
    }

    /**
     * 单个删除
     *
     * @param pk 主键值, 可能是单主键(Any), 也可能是多主键(DbKey)
     * @return
     */
    public override fun delete(pk: Any): Boolean {
        // 构建delete语句
        val query = queryBuilder().where(primaryKey, DbExpr.question)

        // 构建参数
        val params:ArrayList<Any?> = ArrayList()
        if(pk is DbKey<*>)
            params.addAll(pk.columns)
        else
            params.add(pk)

        return query.delete(params)
    }

    /**
     * 批量删除
     *
     * @param pks 主键值列表, 主键值可能是单主键(Any), 也可能是多主键(DbKey)
     * @return
     */
    public override fun batchDelete(vararg pks: Any): IntArray {
        // 构建delete语句
        val query = queryBuilder().where(primaryKey, DbExpr.question)

        // 构建参数
        val params:ArrayList<Any?> = ArrayList()
        for (pk in pks){
            if(pk is DbKey<*>)
                params.addAll(pk.columns)
            else
                params.add(pk)
        }

        return query.batchDelete(params)
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

    /********************************* 关联关系 **************************************/
    /**
     * 是否有某个关联关系
     * @param name
     * @return
     */
    public override fun hasRelation(name: String): Boolean {
        //return name in relations; // 坑爹啊，ConcurrentHashMap下的 in 语义是调用 contains()，但是我想调用 containsKey()
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

}

