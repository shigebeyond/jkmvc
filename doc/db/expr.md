# Db表达式

见类 `DbExpr`

```
data class DbExpr(public val exp:CharSequence /* 表达式, 可以是 String | DbQueryBuilder */,
                  public val alias:String?, /* 别名 */
                  public val expQuoting:Boolean = (exp !is IDbQueryBuilder) /* 是否转义exp, 只要不是子查询, 默认都转 */
) : CharSequence by ""
```

主要有两个作用:
1. 带别名
`DbQueryBuilder().select(DbExpr("username", "u"), DbExpr("password", "p")).from("user");`

2. 控制是否转义
`DbQueryBuilder().select("username", DbExpr("COUNT(`id`)", "total_posts", false))`


