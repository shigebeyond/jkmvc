import com.jkmvc.db.Record
import java.util.*

/**
 * ORM之实体对象
 *
 * @Package packagename
 * @category
 * @author shijianhang
 * @date 2016-10-10 上午12:52:34
 *
 */
abstract class OrmEntity(data: MutableMap<String, Any?> = LinkedHashMap<String, Any?>()): Record(data)
{
	/**
	 * 判断是否有某字段
	 *
	 * @param string column
	 * @return
	 */
	public fun hasColumn(column:String): Boolean {
		return true;
	}

	/**
	 * 变化的字段值：<字段名 => 字段值>
	 * @var array
	*/
	protected val dirty:MutableSet<String> by lazy{
		HashSet<String>()
	};

	/**
	 * 设置对象字段值
	 *
	 * @param  string column 字段名
	 * @param  mixed  value  字段值
	 */
	public override operator fun set(column: String, value: Any?) {
		if(!hasColumn(column))
			throw OrmException("类 class 没有字段 column");

		dirty.add(column);
		super.set(column, value);
	}

	/**
	 * 获得对象字段
	 *
	 * @param   string column 字段名
	 * @return  mixed
	 */
	public override operator fun <T> get(column: String, defaultValue: Any?): T {
		if(!hasColumn(column))
			throw OrmException("类 class 没有字段 column");

		return super.get(column, defaultValue);
	}

	/**
	 * 设置多个字段值
	 *
	 * @param  array values   字段值的数组：<字段名 => 字段值>
	 * @param  array expected 要设置的字段名的数组
	 * @return ORM
	 */
	public fun values(values:Map<String, Any?>, expected:List<String>? = null): IOrm {
		val columns = if (expected === null)
							values.keys
						else
							expected;

		for (column in columns)
			this[column] = values[column];

		return this;
	}

	/**
	 * 获得变化的字段值
	 * @return array
	 */
	public fun dirty(): List<String> {
		return this.dirty;
	}

	/**
	 * 获得字段值
	 * @return array
	 */
	public fun asArray()
	{
		return data;
	}

}
