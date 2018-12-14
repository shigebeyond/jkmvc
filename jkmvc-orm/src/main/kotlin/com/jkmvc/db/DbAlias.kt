package com.jkmvc.db

/**
 * Db别名
 *
 * @author shijianhang
 * @create 2017-11-19 下午1:47
 **/
data class DbAlias(public val name:CharSequence /* 原名 */, public val alias:String? = null /* 别名 */) : CharSequence by "" {

    /**
     * 转字符串
     * @return
     */
    public override fun toString(): String {
        if(alias == null)
            return name.toString();

        return "$name $alias"
    }
}