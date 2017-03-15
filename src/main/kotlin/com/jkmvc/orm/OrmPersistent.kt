/**
 * ORM之持久化，主要是负责数据库的增删改查
 *
 * @Package packagename
 * @category
 * @author shijianhang
 * @date 2016-10-10
 *
 */
abstract class OrmPersistent : OrmMetaData
{
	/**
	 * 获得sql构建器
	 * @return OrmQueryBuilder
	 */
	public static fun querybuilder()
	{
		return OrmQueryBuilder(getcalledclass());
	}

	/**
	 * 构造函数
	 * @param string|array id 主键/查询条件
	 */
	public fun construct(id = null)
	{
		if(id === null)
			return;

		// 根据id来查询结果
		query = querybuilder();
		if(isarray(id)) // id是多个查询条件
			query.wheres(id);
		else // id是主键
			query.where(primaryKey, id);
		query.find(this);
	}

	/**
	 * 判断当前记录是否存在于db: 有原始数据就认为它是存在的
	 */
	public fun exists()
	{
		return !empty(this.data);
	}

	/**
	 * 保存数据
	 *
	 * @return int 对insert返回新增数据的主键，对update返回影响行数
	 */
	public fun save()
	{
		if(this.exists())
			return this.update();

		return this.create();
	}

	/**
	 * 插入数据: insert sql
	 *
	 * <code>
	 *    user = ModelUser();
	 *    user.name = "shi";
	 *    user.age = 24;
	 *    user.create();
	 * </code>
	 * 
	 * @return int 新增数据的主键
	 */
	public fun create()
	{
		if(empty(this.dirty))
			throw OrmException("没有要创建的数据");

		// 校验
		this.check();

		// 插入数据库
		pk = querybuilder().value(this.dirty).insert();

		// 更新内部数据
		this.data = this.dirty + this.data; //　原始字段值
		this.data[primaryKey] = pk; // 主键
		this.dirty = array(); // 变化的字段值

		return pk;
	}

	/**
	 * 更新数据: update sql
	 *
	 * <code>
	 *    user = ModelUser.querybuilder().where("id", 1).find();
	 *    user.name = "li";
	 *    user.update();
	 * </code>
	 * 
	 * @return int 影响行数
	 */
	public fun update()
	{
		if(!this.exists())
			throw OrmException("更新对象[".name."#".this.pk()."]前先检查是否存在");

		if (empty(this.dirty))
			throw OrmException("没有要更新的数据");

		// 校验
		this.check();

		// 更新数据库
		result = querybuilder().sets(this.dirty).where(primaryKey, this.pk()).update();

		// 更新内部数据
		this.data = this.dirty + this.data;
		this.dirty = array();

		return result;
	}

	/**
	 * 删除数据: delete sql
	 *
	 *　<code>
	 *    user = ModelUser.querybuilder().where("id", "=", 1).find();
	 *    user.delete();
	 *　</code>
	 *
	 * @return int 影响行数
	 */
	public fun delete()
	{
		if(!this.exists())
			throw OrmException("删除对象[".name."#".this.pk()."]前先检查是否存在");

		//　校验
		if(!this.check())
			return;

		// 删除数据
		result = querybuilder().where(primaryKey, this.pk()).delete();

		// 更新内部数据
		this.data = this.dirty = array();

		return result;
	}
}
