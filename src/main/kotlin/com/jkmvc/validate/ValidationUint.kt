package com.jkmvc.validate

import com.jkmvc.common.replaces
import java.util.*
import kotlin.reflect.KFunction
import kotlin.reflect.memberExtensionFunctions
import kotlin.reflect.memberFunctions
import kotlin.reflect.staticFunctions

/**
 * 校验计算单位： 1 运算符 2 函数名 3 函数参数
 *
 * @Package packagename
 * @category
 * @author shijianhang
 * @date 2016-10-19 下午3:40:55
 *
 */
data class ValidationUint(public val operator:String? /* 运算符 */, public val func:String /* 函数名 */, public val params:List<String> /* 函数参数 */) {

    companion object{

        /**
         * 校验方法
         */
        public val validationFuncs: MutableMap<String, KFunction<*>> = HashMap<String, KFunction<*>>();

        /**
         * 字符串方法
         */
        public val stringFuncs: MutableMap<String, KFunction<*>> = HashMap<String, KFunction<*>>();

        init {
            // Validation的静态方法
            for (f in Validation::class.staticFunctions)
                validationFuncs[f.name] = f;

            // String的成员方法
            for (f in String::class.memberFunctions)
                stringFuncs[f.name] = f;
            for (f in String::class.memberExtensionFunctions)
                stringFuncs[f.name] = f;
        }

    }

    /**
     * 执行校验表达式
     *
     * @param unknown value 待校验的值
     * @param Map binds 变量
     * @return Anys
     */
    public fun execute(value:Any?, binds:Map<String, Any?> = emptyMap()): Any? {
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
        // 获得函数
        val f = getFunction(func)
        if(f == null)
            throw ValidationException("不存在校验方法$func");
        // 调用函数
        return f.call(*args)
    }

    /**
     * 获得校验方法
     */
    private fun getFunction(func: String): KFunction<*>? {
        // 优先调用 Validator 的校验方法, 再调用 String 的方法
        return validationFuncs[func] ?: stringFuncs[func];
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