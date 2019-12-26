package net.jkcode.jkmvc.tags.form

import net.jkcode.jkutil.ttl.AllRequestScopedTransferableThreadLocal
import org.apache.commons.lang.StringEscapeUtils
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicLong
import javax.servlet.jsp.JspWriter
import kotlin.properties.ReadWriteProperty
import kotlin.reflect.KProperty

/**
 * 属性代理
 *
 * @author shijianhang<772910474@qq.com>
 * @date 2019-12-20 18:57:59
 */
object AttrDelegater: ReadWriteProperty<HtmlTag, Any?> {
    // 获得属性
    public override operator fun getValue(thisRef: HtmlTag, property: KProperty<*>): Any? {
        return thisRef.attrs[property.name]
    }

    // 设置属性
    public override operator fun setValue(thisRef: HtmlTag, property: KProperty<*>, value: Any?) {
        thisRef.attrs[property.name] = value
    }
}

/**
 * html标签
 *
 * @author shijianhang<772910474@qq.com>
 * @date 2019-12-20 18:57:59
 */
open class HtmlTag(
        public var tag: String?, // 标签名
        public var hasBody: Boolean // 是否有标签体
) : IBoundTag() {

    companion object{

        /**
         * id生成器
         */
        protected val idGenerators: AllRequestScopedTransferableThreadLocal<ConcurrentHashMap<String, AtomicLong>> = AllRequestScopedTransferableThreadLocal {
            ConcurrentHashMap<String, AtomicLong>()
        }
    }

    /**
     * 是否转义html
     */
    public var htmlEscape: Boolean = false

    /**
     * 自定义的标签体
     *   优先于 jspBody
     */
    protected var body: String? = null

    /**
     * 属性
     */
    internal val attrs = HashMap<String, Any?>()

    /**
     * 获得属性代理
     * @return
     */
    public fun <T> property(): ReadWriteProperty<HtmlTag, T> {
        return AttrDelegater as ReadWriteProperty<HtmlTag, T>;
    }

    public var cssClass: String? = null

    public var cssErrorClass: String? = null

    public var cssStyle: String? = null

    public var id: String? by property()

    public var name: String? by property()

    public var disabled: Boolean by property()

    public var lang: String? by property()

    public var title: String? by property()

    public var dir: String? by property()

    public var tabindex: String? by property()

    public var onclick: String? by property()

    public var ondblclick: String? by property()

    public var onmousedown: String? by property()

    public var onmouseup: String? by property()

    public var onmouseover: String? by property()

    public var onmousemove: String? by property()

    public var onmouseout: String? by property()

    public var onkeypress: String? by property()

    public var onkeyup: String? by property()

    public var onkeydown: String? by property()

    /**
     * 生成id
     */
    protected fun nextId(): String {
        val tag = this::class.simpleName!!.substringBefore("Tag")
        return tag + idGenerators.get().getOrPut(tag){
            AtomicLong(0)
        }.incrementAndGet()
    }

    /**
     * 输出标签前处理
     */
    protected open fun beforeWriteTag(writer: JspWriter) {
    }

    /**
     * 输出标签后处理
     */
    protected open fun afterWriteTag(writer: JspWriter) {
    }

    /**
     * 输出标签
     */
    public override fun doTag() {
        writeTag(jspContext.out)
    }

    /**
     * 输出标签
     */
    public fun writeTag(writer: JspWriter) {
        // 默认id
        if(id == null)
            id = nextId()

        // 默认name
        if(name == null && path != null)
            name = path

        // 样式
        if(isError && cssErrorClass != null) // 错误样式类
            attrs["class"] = cssErrorClass
        if(attrs["class"] == null && cssClass != null) // 默认样式类
            attrs["class"] = cssClass
        if(cssStyle != null) // 样式
            attrs["style"] = cssStyle

        beforeWriteTag(writer)

        // 标签头
        if(tag != null) {
            writer.append("<").append(tag)
            // 属性
            for ((name, value) in attrs)
                writeAttr(writer, name, value)

            if (!hasBody) {
                writer.append("/>")
                return
            }

            writer.append(">")
        }

        // 标签体
        writeBody(writer)

        // 标签尾
        if(tag != null)
            writer.append("<").append(tag).append("/>")

        afterWriteTag(writer)
    }

    /**
     * 标签体
     * @param writer
     */
    protected open fun writeBody(writer: JspWriter) {
        if (body != null)
            writer.append(body)
        else if (jspBody != null)
            jspBody.invoke(writer)
    }

    /**
     * 写属性
     * @param writer
     * @param name
     * @param value
     */
    protected fun writeAttr(writer: JspWriter, name: String, value: Any?){
        writer.append(" ").append(name).append("=\"")
                .append(toDisplayString(value)).append("\"")
    
    }

    /**
     * 转字符串
     * @param obj
     * @return
     */
    protected fun toDisplayString(obj: Any?): String {
        val str = toString(obj)
        return if(htmlEscape) StringEscapeUtils.escapeHtml(str) else str
    }

    /**
     * 转字符串
     * @param obj
     * @return
     */
    protected fun toString(obj: Any?): String {
        if (obj == null) 
            return ""

        if(obj is Array<*>)
            return obj.joinToString(",", "[", "]") { it.toString() }
        
        if (obj is String) 
            return obj
        
        if (obj is BooleanArray)
            return obj.joinToString(",", "[", "]") { it.toString() }
        
        if (obj is ByteArray) 
            return obj.joinToString(",", "[", "]") { it.toString() }
        
        if (obj is CharArray) 
            return obj.joinToString(",", "[", "]") { it.toString() }
        
        if (obj is DoubleArray) 
            return obj.joinToString(",", "[", "]") { it.toString() }
        
        if (obj is FloatArray) 
            return obj.joinToString(",", "[", "]") { it.toString() }
        
        if (obj is IntArray) 
            return obj.joinToString(",", "[", "]") { it.toString() }
        
        if (obj is LongArray) 
            return obj.joinToString(",", "[", "]") { it.toString() }
        
        if (obj is ShortArray) 
            return obj.joinToString(",", "[", "]") { it.toString() }

        return obj.toString()
    }

    /**
     * 清空, 以便复用
     */
    public fun clear(){
        attrs.clear()
        jspContext = null
        parent = null
    }

}