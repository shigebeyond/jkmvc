package com.jkmvc.validate

/**
 * 校验计算单位： 1 运算符 2 函数名 3 函数参数
 *
 * @author shijianhang
 * @date 2016-10-19 下午3:40:55
 *
 */
interface IValidationUint{

    /**
     * 运算符
     */
    val operator:String?

    /**
     * 函数名
     */
    val func:String

    /**
     * 函数参数
     */
    val params:List<String>

    /**
     * 执行校验表达式
     *
     * @param value 待校验的值
     * @param binds 变量
     * @returns
     */
    fun execute(value:Any?, binds:Map<String, Any?> = emptyMap()): Any?

    /**
     * 构建结果消息
     * @return
     */
    fun message():String;
}