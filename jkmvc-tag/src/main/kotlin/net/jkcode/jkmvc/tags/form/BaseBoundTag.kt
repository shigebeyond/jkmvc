package net.jkcode.jkmvc.tags.form

import net.jkcode.jkutil.common.Config
import net.jkcode.jkutil.common.PropertyUtil
import net.jkcode.jkutil.common.trim
import javax.servlet.http.HttpServletRequest
import javax.servlet.jsp.tagext.TagSupport

/**
 * 绑定值的标签
 *    由于web容器(tomcat/jetty)会服用 Tag 实例, 因此本地属性不能用by lazy(有延迟取值需求的, 只能在get()中自行实现), 而且必须在 doEndTag() 中重置
 * @author shijianhang<772910474@qq.com>
 * @date 2019-12-25 3:35 PM
 */
abstract class BaseBoundTag : TagSupport() {

    companion object{

        val config = Config.instance("tag", "properties")

        /**
         * 请求中存储错误的属性名
         */
        val requestErrorAttrName: String = config["requestErrorAttrName"] ?: "errors"
    }

    /**
     * 会话
     */
    protected val session
        get() = pageContext.session

    /**
     * 请求
     */
    protected val request
        get() = pageContext.request as HttpServletRequest

    /**
     * 属性路径
     */
    public var path: String? = null

    /**
     * 属性的绝对路径
     *   暂时只支持2层
     */
    public var absolutePath: String? = null
        get() {
            // 初始化
            if (field == null && path != null) { // 2层全路径
                // 子路径
                var absolutePath = path!!
                // 父路径
                val parent = findAncestorWithClass(this, BaseBoundTag::class.java) as BaseBoundTag?
                if (parent?.path != null)
                    absolutePath = "${parent.path}.$absolutePath"
                field = absolutePath
            }

            return field
        }

    /**
     * 绑定值
     */
    public var boundValue: Any? = null
        get() {
            if (field == null && absolutePath != null) {
                val keys = absolutePath!!.split("\\.".toRegex(), 2)
                var value = request.getAttribute(keys[0])
                if (value != null && keys.size == 2)
                    value = PropertyUtil.getPath(value, keys[1])
                field = value
            }

            return field
        }

    /**
     * 绑定值类型
     */
    public var boundType: Class<*>? = null
        get() {
            if (field == null && absolutePath != null) {
                val keys = absolutePath!!.split("\\.".toRegex(), 2)
                var value = request.getAttribute(keys[0])
                field = if (keys.size == 2)
                            PropertyUtil.getPathType(value, keys[1])
                        else
                            value.javaClass
            }

            return field
        }

    /**
     * 绑定的错误
     */
    public var boundError: Any? = null
        get() {
            if (field == null && absolutePath != null) {
                val errors = request.getAttribute(requestErrorAttrName)
                val path = if (absolutePath!!.endsWith(".*")) // 所有错误
                                absolutePath!!.trim(".*")
                            else // 单个错误
                                absolutePath!!
                field = PropertyUtil.getPath(errors, path)
            }

            return field
    }

    /**
     * 检查绑定值是否等于指定值
     */
    public fun isBoundValueEquals(value: Any?): Boolean {
        return value != null && boundValue != null && boundValue!!.equals(value)
    }

    /**
     * 是否有错
     */
    public val isError: Boolean
        get() {
            return !(boundError == null
                    || boundError is Map<*, *> && (boundError as Map<*, *>).isEmpty() // 不为空
                    || boundError is Collection<*> && (boundError as Collection<*>).isEmpty()) // 不为空
        }

    /**
     * 重置本地属性
     */
    public open fun reset(){
        // 父类的属性
        pageContext = null
        parent = null
        id = null

        // 本类的属性
        path = null
        absolutePath = null
        boundValue = null
        boundType = null
        boundError = null
    }
}