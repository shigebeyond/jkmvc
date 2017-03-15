import com.jkmvc.orm.MetaRelation
import com.jkmvc.orm.RelationType
import java.util.*

/**
 * ORM之关联对象操作
 *
 * @Package packagename
 * @category
 * @author shijianhang
 * @date 2016-10-10 上午12:52:34
 *
 */
open abstract class OrmRelated : OrmPersistent
{

	companion object{
		/**
		 * 自定义关联关系
		 * @var array
		 */
		protected val relations:Map<String, MetaRelation> = LinkedHashMap<String, MetaRelation>()

		/**
		 * 获得关联关系
		 *
		 * @param string name
		 * @return array
		 */
		public fun relation(name:String): MetaRelation? {
			return relations.get(name);
		}
	}

	/**
	 * 设置对象字段值
	 *
	 * @param  string column 字段名
	 * @param  mixed  value  字段值
	 */
	public override operator fun set(column: String, value: Any?) {
		if(!hasColumn(column))
			throw OrmException("类 class 没有字段 column");

		// 设置关联对象
		if (relations.containsKey(column)) {
			this[column] = value;
			// 如果关联的是主表，则更新从表的外键
			val (type, model, foreignKey) = relations[column]!!;
			if(type == RelationType.BELONGS_TO)
				this[foreignKey] = (value as Orm).pk();
			return;
		}

		super.set(column, value);
	}

	/**
	 * 获得对象字段
	 *
	 * @param   string column 字段名
	 * @return  mixed
	 */
	public override operator fun <T> get(name: String, defaultValue: Any?): T {
		// 获得关联对象
		if (relations.containsKey(name))
			return related(name);

		return super.get(name, defaultValue);
	}

	/**
	 * 获得/设置原始的字段值
	 *
	 * @param array data
	 * @return Orm|array
	 */
	public fun original(data:Map<String, Any?>):Orm
	{
		for ((column, value) in data)
		{
			// 关联查询时，会设置关联表字段的列别名（列别名 = 表别名 : 列名），可以据此来设置关联对象的字段值
			if(!column.contains(":")) // 自身字段
			{
				this.data[column] = value;
			}

			if(value !== null) // 关联对象字段: 不处理null的值, 因为left join查询时, 关联对象可能没有匹配的行
			{
				val (name, column) = column.split(":");
				val obj = this.related(name, true); // 创建关联对象
				obj.data[column] = value;
			}
		}

		return this;
	}

	/**
	 * 获得关联对象
	 *
	 * @param string name 关联对象名
	 * @param boolean 是否创建新对象：在查询db后设置原始字段值data()时使用
	 * @param array columns 字段名数组: array(column1, column2, alias => column3), 
	 * 													如 array("name", "age", "birt" => "birthday"), 其中 name 与 age 字段不带别名, 而 birthday 字段带别名 birt
	 * @return Orm
	 */
	public fun related(name:String, newed:Boolean, vararg columns:String):Orm
	{
		// 已缓存
		if(this.data.contains(name))
			return this.data[name];

		// 获得关联关系
		val (type, model, foreignKey) = relations[name]!!;
		
		// 创建新对象
		if(newed){
			val item = model.java.newInstance() as Orm;
			this.data[name] = item;
			return item;
		}

		// 根据关联关系来构建查询
		var obj:Orm = null;
		when (type)
		{
			RelationType.BELONGS_TO -> // belongsto: 查主表
				obj = this.querymaster(model, foreignkey).select(columns).find();
			RelationType.HAS_ONE -> // hasxxx: 查从表
				obj = this.queryslave(model, foreignkey).select(columns).find();
			RelationType.HAS_MANY -> // hasxxx: 查从表
				obj = this.queryslave(model, foreignkey).select(columns).findall();
		}

		this.related[name] = obj;
		return obj;
	}

	/**
	 * 查询关联的从表
	 *
	 * @param string class 从类
	 * @param string foreignkey 外键
	 * @return OrmQueryBuilder
	 */
	protected fun queryslave(model, foreignkey)
	{
		return model.queryBuilder().where(foreignkey, this.pk()); // 从表.外键 = 主表.主键
	}

	/**
	 * 查询关联的主表
	 *
	 * @param string class 主类
	 * @param string foreignkey 外键
	 * @return OrmQueryBuilder
	 */
	protected fun querymaster(model, foreignkey)
	{
		return model.queryBuilder().where(class.primaryKey, this.foreignkey); // 主表.主键 = 从表.外键
	}

}
