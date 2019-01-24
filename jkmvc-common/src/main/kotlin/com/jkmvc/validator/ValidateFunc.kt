package com.jkmvc.validator

import com.jkmvc.common.*
import java.util.*
import kotlin.collections.Map
import kotlin.collections.MutableMap
import kotlin.collections.joinToString
import kotlin.collections.set
import kotlin.reflect.KFunction
import kotlin.reflect.KType
import kotlin.reflect.full.memberFunctions

/**
 * 校验函数
 *
 * @author shijianhang
 * @date 2016-10-19 下午3:40:55
 */
class ValidateFunc(protected val func: KFunction<*> /* 方法 */) : IValidateFunc {

    companion object{

        /**
         * 校验方法对应的错误消息
         */
        protected val messages: Config = Config.instance("validation-messages")

        /**
         * 校验方法
         */
        protected val funcs: MutableMap<String, ValidateFunc> = HashMap();

        init {
            /*// String的成员方法
            // 报错： kotlin.reflect.jvm.internal.KotlinReflectionInternalError: Reflection on built-in Kotlin types is not yet fully supported. No metadata found for public open val length: kotlin.Int defined in kotlin.String
            // 解决：在Validation中封装String的方法
            for (f in String::class.memberExtensionFunctions)
                funcs[f.name] = ValidationFunc(f);
            */

            // Validation的静态方法
            for (f in ValidateFuncDefinition::class.memberFunctions)
                if(f.name != "execute")
                    funcs[f.name] = ValidateFunc(f);
        }

        /**
         * 获得校验方法
         * @param name 方法名
         * @return
         */
        public fun get(name: String): ValidateFunc{
            val f = funcs[name]
            if(f == null)
                throw Exception("Class [Validation] has no method [$name()]")

            return f
        }
    }

    /**
     * 函数名
     */
    protected val name = func.name

    /**
     * 执行函数
     *   如果是预言函数 + 预言失败, 则抛 ValidateException 异常
     *
     * @param value 待校验的值
     * @param args 参数
     * @param variables 变量
     * @param label 值的标识, 如orm中的字段名, 如请求中的表单域名
     * @return 如果是预言函数, 返回原值, 否则返回执行结果
     */
    public override fun execute(value:Any?, args:Array<String>, variables:Map<String, Any?>, label:String): Any? {
        // 获得函数
        if(func == null)
            throw ValidateException("不存在校验方法${this.func}");

        try{
            // 其他参数
            var i = 2 // 从第三个参数开始
            val otherArgs:Array<Any?>  = args.mapToArray{ arg ->
                convertArg(arg, variables, func.parameters[i++].type)
            }

            // 调用函数
            val result = func.call(
                    ValidateFuncDefinition, // 对象
                    convertValue(value, func.parameters[1].type), // 待校验的值
                    *otherArgs // 其他参数
            )

            // 如果是预言函数, 返回原值
            if(isPredict() ){
                // 如果预言失败, 则抛 ValidateException 异常
                if(result == false) {
                    val message: String = messages[name]!!
                    throw ValidateException(label + message.replaces(args));
                }

                // 返回原值
                return value
            }

            // 否则返回执行结果
            return result
        }catch (e:Exception){
            throw Exception("调用校验方法出错：" + name + "(" + args.joinToString(",") + ")", e)
        }
    }

    /**
     * 是否预言函数, 如果是则预言失败抛异常
     */
    protected fun isPredict(): Boolean {
        return messages.containsKey(name)
    }


    /**
     * 根据校验值的类型，转换校验值
     *
     * @param value 校验值
     * @param type 值类型
     * @return
     */
    protected fun convertValue(value:Any?, type: KType): Any? {
        // 只对String类型的值进行转换，只针对请求参数的校验
        if(value is String)
            return value.to(type)
        return value
    }

    /**
     * 根据参数类型，转换参数值
     *
     * @param arg 字符串类型的参数值
     * @param variables 变量
     * @param type 参数类型
     * @return
     */
    protected fun convertArg(arg:String, variables: Map<String, Any?>, type: KType): Any? {
        return if (arg[0] == ':') // 变量: 从变量池中取值，都是正确类型，不需要转换类型
                    variables[arg.substring(1)];
                else // 值：转换类型
                    arg.exprTo(type)
    }

}