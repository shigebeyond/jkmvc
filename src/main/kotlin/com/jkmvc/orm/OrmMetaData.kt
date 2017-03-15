import com.jkmvc.db.Db
import java.util.*

/**
 * ORM之元数据
 *
 * @Package packagename
 * @category
 * @author shijianhang
 * @date 2016-10-10
 *
 */
abstract class OrmMetaData : OrmValid {
    companion object {
        /****************** 部分元数据有不一样的默认值, 不能在基类定义 => 默认值不能保存在类结构中, 因此只能缓存默认值 ********************/
        /**
         * 缓存所有model类的表名: <类名 => 表名>
         * @var string
         */
        protected val tables: MutableMap<Class<*>, String> by lazy {
            LinkedHashMap<Class<*>, String>()
        }

        /****************** 部分元数据有一样的默认值, 可在基类定义 => 默认值直接保存在类结构中 ********************/
        /**
         * 数据库
         * 	默认一样, 基类给默认值, 子类可自定义
         * @var Db
         */
        protected val db = "default";

        /**
         * 自定义的表名
         *     默认不一样, 基类不能给默认值, 子类可自定义
         * @var string
         */
        //protected val table;

        /**
         * 自定义的表字段
         *     默认不一样, 基类不能给默认值, 但子类可自定义
         * @var array
         */
        //protected val columns;

        /**
         * 主键
         *     默认一样, 基类给默认值, 子类可自定义
         * @var string
         */
        protected val primaryKey = "id";

        /**
         * 获得数据库
         * @return Db
         */
        public fun db(): Db {
            return Db.getDb(db);
        }

        /**
         * 获得模型名
         *    假定model类名, 都是以"Model"作为前缀
         *
         * @return string
         */
        public fun name() {
            return strtolower(substr(getcalledclass(), 6));
        }

        /**
         * 获得表名
         *
         * @return  string
         */
        public fun table() {
            // 先查缓存
            if (!isset(tables[class]))
            {
                if (propertyexists(class, "table")) // 自定义表名
                tables[class] = table;
                else // 默认表名 = 模型名
                tables[class] = name();
            }

            return tables[class];
        }

        /**
         * 判断是否有某字段
         *
         * @param string column
         * @return
         */
        public fun hasColumn(column:String): Boolean {
            return columns().contains(column)
        }

        /**
         * 获得字段列表
         * @return array
         */
        public fun columns(): List<String> {
            return db().listColumns(table());
        }

        /**
         * 获得主键
         * @return string
         */
        public fun primaryKey() {
            return primaryKey;
        }
    }


    /**
     * 获得主键值
     * @return int|string
     */
    public fun pk() {
        return this[primaryKey];
    }
}