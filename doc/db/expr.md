# Db Expression

see class `DbExpr`

```
data class DbExpr(public val exp:CharSequence /* Expression: String | DbQueryBuilder */,
                  public val alias:String?, /* alias */
                  public val expQuoting:Boolean = (exp !is IDbQueryBuilder) /* Whether `exp` is quoting, If `exp` is not sub query, default value is true */
) : CharSequence by ""
```

It has 2 uses:

1. With alias

`DbQueryBuilder().select(DbExpr("username", "u"), DbExpr("password", "p")).from("user");`

2. Controller quoting expression

`DbQueryBuilder().select("username", DbExpr("COUNT(`id`)", "total_posts", false))`


