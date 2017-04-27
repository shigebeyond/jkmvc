package com.jkmvc.validate

import com.jkmvc.common.findFunction
import com.jkmvc.common.replaces

/**
 * 校验计算单位： 1 运算符 2 函数名 3 函数参数
 *
 * @Package packagename
 * @category
 * @author shijianhang
 * @date 2016-10-19 下午3:40:55
 *
 */
data class ValidationUint(public val operator:String /* 运算符 */, public val func:String /* 函数名 */, public val params:List<String> /* 函数参数 */) {

    /**
     * 执行校验表达式
     *
     * @param unknown value 待校验的值
     * @param array|ArrayAccess binds 变量
     * @return mixed
     */
    public fun execute(value:Any?, binds:Map<String, Any> = emptyMap())
    {
        // 构造参数
        val args:Array<Any?> = Array<Any?>(params.size + 1){ i ->
            if(i == 0) // 待校验的值: 作为第一参数
                value
            else {
                val param = params[i - 1]
                if (param[0] == ':') // 参数
                    binds[param.substring(1)]
                else // 值
                    param
            }
        }
        // 调用函数
        val f = Validation::class.findFunction(func); // 优先调用 Validator 中的校验方法
        f?.call(*args)
    }
    /**
     * 构建结果消息
     * @return string
     */
    public fun message():String
    {
        val msg = Validation.messages[func];
        return if (msg == null)
                    "校验[$func]规则失败";
                else
                    msg.replaces(params);
    }
}