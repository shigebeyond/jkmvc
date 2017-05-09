package com.jkmvc.validate

import com.jkmvc.common.replaces
import com.jkmvc.common.to
import java.util.*
import kotlin.reflect.KFunction
import kotlin.reflect.KType
import kotlin.reflect.full.memberFunctions

/**
 * 校验计算单位： 1 运算符 2 函数名 3 函数参数
 *
 * @author shijianhang
 * @date 2016-10-19 下午3:40:55
 *
 */
data class ValidationUint(public override val operator:String? /* 运算符 */, public override val func:String /* 函数名 */, public override val params:List<String> /* 函数参数 */) :IValidationUint{

    companion object{

        /**
         * 校验方法
         */
        public val validationFuncs: MutableMap<String, KFunction<*>> = HashMap<String, KFunction<*>>();

        init {

            // 通过反射的方式来获得校验方法：报错
            // Validation的静态方法
            for (f in Validation::class.memberFunctions)
                if(f.name != "execute")
                    validationFuncs[f.name] = f;

            /*// String的成员方法
            // 报错： kotlin.reflect.jvm.internal.KotlinReflectionInternalError: Reflection on built-in Kotlin types is not yet fully supported. No metadata found for public open val length: kotlin.Int defined in kotlin.String
            // 解决：在Validation中封装String的方法
            for (f in String::class.memberExtensionFunctions)
                stringFuncs[f.name] = f;
            */
        }



    }

    /**
     * 执行校验表达式
     *
     * @param value 待校验的值
     * @param binds 变量
     * @returns
     */
    public override fun execute(value:Any?, binds:Map<String, Any?>): Any? {
        // 获得函数
        val f = validationFuncs[func]
        if(f == null)
            throw ValidationException("不存在校验方法$func");

        // 构造参数
        val args:Array<Any?> = Array<Any?>(params.size + 2){ i ->
            if(i == 0) // 对象：作为第1参数
                Validation
            else if(i == 1) // 待校验的值: 作为第2参数
                value // 正确类型，不需要转换类型
            else
                param(i - 2, binds, f.parameters[i].type)
        }

        // 调用函数
        return f.call(*args)
    }

    /**
     * 获得实际参数
     */
    protected fun param(i: Int, binds: Map<String, Any?>, type: KType): Any? {
        val param:String = params[i]
        return if (param[0] == ':') // 变量: 从变量池中取值，都是正确类型，不需要转换类型
                    binds[param.substring(1)];
                else // 值：转换类型
                    param.to(type);
    }

    /**
     * 构建结果消息
     * @return
     */
    public override fun message():String
    {
        val msg = Validation.messages[func];
        return if (msg == null)
                    "校验[$func]规则失败";
                else
                    msg.replaces(params);
    }
}