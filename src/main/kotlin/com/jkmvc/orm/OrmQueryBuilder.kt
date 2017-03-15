import com.jkmvc.db.DbQueryBuilder

/**
 * 面向orm对象的sql构建器
 *
 * @Package packagename
 * @category
 * @author shijianhang
 * @date 2016-10-16 下午8:02:28
 *
 */
class OrmQueryBuilder : DbQueryBuilder
{
	/**
	 * model的类
	 * @var string
	 */
	protected val cls;

	/**
	 * 构造函数
	 *
	 * string class model类名，其基类为Orm
	 */
	public fun construct(cls)
	{
		// 检查是否是orm子类
		if(!issubclassof(cls, "Orm"))
			throw OrmException("OrmQueryBuilder.class 必须是 Orm 的子类");
		
		super.construct(array(class, "db")/* 获得db的回调  */, class.table());
		this.cls = cls;
	}

	/**
	 * 查询单个: select　语句
	 * 
	 * @param bool|int|string|Orm fetchvalue fetchvalue 如果类型是int，则返回某列FETCHCOLUMN，如果类型是string，则返回指定类型的对象，如果类型是object，则给指定对象设置数据, 其他返回关联数组
	 * @return Orm
	 */
	public fun find(fetchvalue = false)
	{
		data = super.find();
		if(!data)
			return null;
		
		if(fetchvalue instanceof Orm) // 已有对象
			model = fetchvalue;
		else // 新对象
			model = this.class;
		
		//　设置原始属性值
		return model.data(data);
	}

	/**
	 * 查找多个： select 语句
	 *
	 * @param bool|int|string|Orm fetchvalue fetchvalue 如果类型是int，则返回某列FETCHCOLUMN，如果类型是string，则返回指定类型的对象，如果类型是object，则给指定对象设置数据, 其他返回关联数组
	 * @return array
	 */
	public fun findall(fetchvalue = false)
	{
		rows = super.findall(fetchvalue);
		foreach (rows as key => row)
			rows[key] = (this.class).data(row);
		return rows;
	}

	/**
	 * 联查表
	 *
	 * @param string name 关联关系名
	 * @param array columns 字段名数组: array(column1, column2, alias => column3), 
	 * 													如 array("name", "age", "birt" => "birthday"), 其中 name 与 age 字段不带别名, 而 birthday 字段带别名 birt
	 * @return OrmQueryBuilder
	 */
	public fun with(name, array columns = null)
	{
		val cls = this.class;

		// select当前表字段
		if(this.data.isEmpty())
			this.select(array(cls.table().".*"));

		// 获得关联关系
		relation = class.relation(name);
		if(relation)
		{
			// 根据关联关系联查表
			extract(relation);
			class = "Model".ucfirst(model);
			when (type)
			{
				"belongsto"-> // belongsto: 查主表
					this.joinMaster(cls, foreignkey, name);
				"hasmany" -> // hasxxx: 查从表
				"hasone" -> // hasxxx: 查从表
					this.joinSlave(cls, foreignkey, name);
			}
			// select关联表字段
			this.selectrelated(cls, name);
		}

		return this;
	}

	/**
	 * 联查从表
	 *     从表.外键 = 主表.主键
	 *
	 * @param string slave 从类
	 * @param string foreignkey 外键
	 * @param string tablealias 表别名
	 * @return OrmQueryBuilder
	 */
	protected fun joinSlave(slave, foreignkey, tablealias)
	{
		// 联查从表
		master = this.class;
		masterpk = master.table().".".master.primaryKey();
		slavefk = tablealias.".".foreignkey;
		return this.join(array(tablealias => slave.table()), "LEFT").on(slavefk, "=", masterpk); // 从表.外键 = 主表.主键
	}

	/**
	 * 联查主表
	 *     主表.主键 = 从表.外键
	 *
	 * @param string master 主类
	 * @param string foreignkey 外键
	 * @param string tablealias 表别名
	 * @return OrmQueryBuilder
	 */
	protected fun joinMaster(master, foreignkey, tablealias)
	{
		// 联查从表
		slave = this.class;
		masterpk = master.table().".".master.primaryKey();
		slavefk = slave.table().".".foreignkey;
		return this.join(master.table(), "LEFT").on(masterpk, "=", slavefk); // 主表.主键 = 从表.外键
	}

	/**
	 * select关联表的字段
	 *
	 * @param string class 关联类
	 * @param string tablealias 表别名
	 * @param array columns 查询的列
	 */
	protected fun selectrelated(class, tablealias, array columns = null)
	{
		// 默认查询全部列
		if(columns === null)
			columns = arraykeys(class.columns());

		// 构建列别名
		select = array();
		foreach (columns as column)
		{
			columnalias = tablealias.":".column; // 列别名 = 表别名 : 列名，以便在设置orm对象字段值时，可以逐层设置关联对象的字段值
			column = tablealias.".".column;
			select[columnalias] = column;
		}
		return this.select(select);
	}

}
