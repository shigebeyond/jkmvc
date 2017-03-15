import com.jkmvc.db.Db
import com.jkmvc.db.DbQueryBuilder
import com.jkmvc.orm.Orm
import java.util.*

/**
 * 面向orm对象的sql构建器
 *
 * @Package packagename
 * @category
 * @author shijianhang
 * @date 2016-10-16 下午8:02:28
 *
 */
class OrmQueryBuilder(db: Db = Db.getDb(), table:String = "" /*表名*/) : DbQueryBuilder(db, table) {

	/**
	 * 联查表
	 *
	 * @param string name 关联关系名
	 * @param array columns 字段名数组: array(column1, column2, alias => column3), 
	 * 													如 array("name", "age", "birt" => "birthday"), 其中 name 与 age 字段不带别名, 而 birthday 字段带别名 birt
	 * @return OrmQueryBuilder
	 */
	public fun with(name, vararg columns:String)
	{
		// select当前表字段
		if(this.data.isEmpty())
			this.select(array(cls.table().".*"));

		// 获得关联关系
		val (type, model, foreignKey) = model.relation(name);
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
			this.selectRelated(cls, name);
		}

		return this;
	}

	/**
	 * 联查从表
	 *     从表.外键 = 主表.主键
	 *
	 * @param string slave 从类
	 * @param string foreignkey 外键
	 * @param string tableAlias 表别名
	 * @return OrmQueryBuilder
	 */
	protected fun joinSlave(slave, foreignkey, tableAlias)
	{
		// 联查从表
		master = this.class;
		masterpk = master.table().".".master.primaryKey();
		slavefk = tableAlias.".".foreignkey;
		return this.join(array(tableAlias => slave.table()), "LEFT").on(slavefk, "=", masterpk); // 从表.外键 = 主表.主键
	}

	/**
	 * 联查主表
	 *     主表.主键 = 从表.外键
	 *
	 * @param string master 主类
	 * @param string foreignkey 外键
	 * @param string tableAlias 表别名
	 * @return OrmQueryBuilder
	 */
	protected fun joinMaster(master, foreignkey, tableAlias)
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
	 * @param string tableAlias 表别名
	 * @param array columns 查询的列
	 */
	protected fun selectRelated(model:Class<*>, tableAlias:String, vararg columns:String)
	{
		// 默认查询全部列
		if(columns === null)
			columns = model.columns();

		// 构建列别名
		val select:MutableList<Pair<String, String>> = LinkedList<Pair<String, String>>();
		for (column in columns)
		{
			val columnAlias = tableAlias + ":" + column; // 列别名 = 表别名 : 列名，以便在设置orm对象字段值时，可以逐层设置关联对象的字段值
			val column = tableAlias + "." + column;
			select.add(column to columnAlias);
		}
		return this.select(select);
	}

}
