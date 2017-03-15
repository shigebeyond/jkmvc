/**
 * ORM之关联对象操作
 *
 * @Package packagename
 * @category
 * @author shijianhang
 * @date 2016-10-10 上午12:52:34
 *
 */
abstract class OrmRelated : OrmPersistent
{
	/**
	 * 关联关系 - 有一个
	 *    当前表是主表, 关联表是从表
	 */
	const RELATIONBELONGSTO = "belongsto";

	/**
	 * 关联关系 - 有多个
	 * 	当前表是主表, 关联表是从表
	 */
	const RELATIONHASMANY = "hasmany";

	/**
	 * 关联关系 - 从属于
	 *    当前表是从表, 关联表是主表
	 */
	const RELATIONHASONE = "hasone";

	/**
	 * 自定义关联关系
	 * @var array
	 */
	protected static relations = array();

	/**
	 * 获得关联关系
	 *
	 * @param string name
	 * @return array
	 */
	public static fun relation(name = null)
	{
		if(name === null)
			return relations;

		return Arr.get(relations, name);
	}

	/**
	 * 缓存关联对象
	 * @var array <name => Orm>
	 */
	protected related = array();
	
	/**
	 * 返回要序列化的属性
	 * @return array
	 */
	public fun sleep()
	{
		props = super.sleep();
		props[] = "related";
		return props;
	}

	/**
	 * 判断对象是否存在指定字段
	 *
	 * @param  string column Column name
	 * @return boolean
	 */
	public fun isset(column)
	{
		return isset(relations[column]) && super.isset(column);
	}
	
	/**
	 * 尝试获得对象字段
	 *
	 * @param   string column 字段名
	 * @param   mixed value 字段值，引用传递，用于获得值
	 * @return  bool
	 */
	public fun tryGet(column, &value)
	{
		// 获得关联对象
		if (isset(relations[column]))
		{
			value = this.related(column);
			return true;
		}

		return super.tryGet(column, value);
	}

	/**
	 * 尝试设置字段值
	 *
	 * @param  string column 字段名
	 * @param  mixed  value  字段值
	 * @return ORM
	 */
	public fun trySet(column, value)
	{
		// 设置关联对象
		if (isset(relations[column]))
		{
			this.related[column] = value;
			// 如果关联的是主表，则更新从表的外键
			extract(relations[column]);
			if(type == RELATIONBELONGSTO)
				this.foreignkey = value.pk();
			return true;
		}

		return super.trySet(column, value);
	}
	
	/**
	 * 删除某个字段值
	 *
	 * @param  string column 字段名
	 * @return
	 */
	public fun unset(column)
	{
		super.unset(column);
		unset(this.related[column]);
	}

	/**
	 * 获得/设置原始的字段值
	 *
	 * @param array data
	 * @return Orm|array
	 */
	public fun data(array data = null)
	{
		// getter
		if (data === null)
			return this.data;

		// setter
		related = array();
		foreach (data as column => value)
		{
			// 关联查询时，会设置关联表字段的列别名（列别名 = 表别名 : 列名），可以据此来设置关联对象的字段值
			if(strpos(column, ":") === false) // 自身字段
			{
				this.data[column] = value;
			}
			elseif(value !== null) // 关联对象字段: 不处理null的值, 因为left join查询时, 关联对象可能没有匹配的行
			{
				list(name, column) = explode(":", column);
				obj = this.related(name, true); // 创建关联对象
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
	public fun related(name, = false, columns = null)
	{
		// 已缓存
		if(isset(this.related[name]))
			return this.related[name];

		// 获得关联关系
		extract(relations[name]);
		
		// 获得关联模型类
		class = "Model".ucfirst(model);

		// 创建新对象
		if(new)
			return this.related[name] = class;

		// 根据关联关系来构建查询
		obj = null;
		switch (type)
		{
			case RELATIONBELONGSTO: // belongsto: 查主表
				obj = this.querymaster(class, foreignkey).select(columns).find();
				break;
			case RELATIONHASONE: // hasxxx: 查从表
				obj = this.queryslave(class, foreignkey).select(columns).find();
				break;
			case RELATIONHASMANY: // hasxxx: 查从表
				obj = this.queryslave(class, foreignkey).select(columns).findall();
				break;
		}

		return this.related[name] = obj;
	}

	/**
	 * 查询关联的从表
	 *
	 * @param string class 从类
	 * @param string foreignkey 外键
	 * @return OrmQueryBuilder
	 */
	protected fun queryslave(class, foreignkey)
	{
		return class.querybuilder().where(foreignkey, this.pk()); // 从表.外键 = 主表.主键
	}

	/**
	 * 查询关联的主表
	 *
	 * @param string class 主类
	 * @param string foreignkey 外键
	 * @return OrmQueryBuilder
	 */
	protected fun querymaster(class, foreignkey)
	{
		return class.querybuilder().where(class.primaryKey, this.foreignkey); // 主表.主键 = 从表.外键
	}

	/**
	 * 获得字段值
	 * @return array
	 */
	public fun asArray()
	{
		result = super.asArray();

		// 包含已加载的关联对象
		foreach (this.related as name => model)
			result[name] = model.asArray();

		return result;
	}

}
