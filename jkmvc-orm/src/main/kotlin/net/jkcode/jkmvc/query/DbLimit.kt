package net.jkcode.jkmvc.query

import net.jkcode.jkmvc.db.DbType
import net.jkcode.jkmvc.db.IDb

/**
 * 查询分页, sql中的limit参数: limit + offset
 *    为了兼容不同db的特殊的limit语法，不使用 DbQueryBuilderDecorationClausesSimple("LIMIT", arrayOf<KFunction3<DbQueryBuilderDecoration, IDb, *, String>?>(null));
 *    直接硬编码
 *
 * @author shijianhang
 * @create 2020-3-19 下午1:47
 */
data class DbLimit(
        public val limit: Int,
        public val offset: Int
) {

    /**
     * 编译
     * @param db
     * @param sql
     */
    public fun compile(db: IDb, sql: StringBuilder) {
        if (db.dbType == DbType.Oracle) { // oracle
            // select * from ( select t1_.*, rownum rownum_ from ( select * from USER ) t1_ where rownum <  $end ) t2_ where t2_.rownum_ >=  $limit
            sql.insert(0, "SELECT t1_.*, rownum rownum_ FROM ( ").append(") t1_ WHERE rownum <  ").append(limit + offset)
            if (offset > 0)
                sql.insert(0, "SELECT * FROM ( ").append(" ) t2_ WHERE t2_.rownum_ >=  ").append(offset)
            return
        }

        if (db.dbType == DbType.SqlServer) { // sqlserver
            val iSelect = "SELECT".length
            if (offset == 0) {
                //select top $limit * from user
                sql.insert(iSelect, " TOP $limit") // 在 select 之后插入 top
            } else {
                // 截取 order by 子句
                val iOrderBy = sql.indexOf("ORDER BY")
                var orderBy = "ORDER BY ID" // 由于排名函数 "ROW_NUMBER" 必须有 ORDER BY 子句, 因此默认给一个, 但是最好是开发者自己提供
                if (iOrderBy != -1) {
                    orderBy = sql.substring(iOrderBy)
                    sql.delete(iOrderBy, sql.length)
                }
                // SELECT * FROM ( SELECT ROW_NUMBER() OVER (ORDER BY name) as rownum_, * FROM "user" ) a WHERE rownum_ >= $limit and rownum_ < $end;
                sql.insert(iSelect, "* FROM ( SELECT ROW_NUMBER() OVER ( $orderBy ) as rownum_, ").append(") t_ WHERE rownum_ >= ").append(limit).append(" AND rownum_ < ").append(offset + limit)
            }

            return
        }

        if (db.dbType == DbType.Postgresql) { // psql
            // select * from user limit $limit  offset $offset;
            sql.append(" LIMIT ").append(limit)
            if (offset > 0)
                sql.append(" OFFSET ").append(offset)
            return
        }

        // 其他：mysql / sqlite
        // select * from user limit $offset, $limit;
        if (offset == 0)
            sql.append(" LIMIT ").append(limit)
        else
            sql.append(" LIMIT ").append(offset).append(", ").append(limit)
    }
}