package net.jkcode.jkmvc.tags.form

import net.jkcode.jkutil.ttl.AllRequestScopedTransferableThreadLocal
import org.apache.commons.lang.StringEscapeUtils
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicLong
import javax.servlet.jsp.JspWriter
import javax.servlet.jsp.tagext.DynamicAttributes
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
        public val tag: String?, // 标签名
        public val hasBody: Boolean // 是否有标签体
) : BaseBoundTag(), DynamicAttributes {

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

    // id属性与 TagSupport的属性 重名了
    //public override var id: String? by property()
    override fun setId(id: String?) {
        attrs["id"] = id
    }
    override fun getId(): String? {
        return attrs["id"] as String?
    }

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
     * 实现 DynamicAttributes 接口
     * 设置动态属性
     *
     * @param uri 属性的命名空间uri
     * @param localName 属性名
     * @param value 属性值
     */
    public override fun setDynamicAttribute(uri: String?, localName: String, value: Any?) {
        attrs[localName] = value
    }

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

    override fun doStartTag(): Int {
        // 标签头
        writeTagStart(pageContext.out)

        return if(hasBody) EVAL_BODY_INCLUDE else SKIP_BODY
    }

    override fun doEndTag(): Int {
        // 标签尾
        writeTagEnd(pageContext.out)

        // 重置本地属性
        reset()

        return EVAL_PAGE;
    }

    /**
     * 输出标签
     */
    public fun writeTag(writer: JspWriter) {
        // 标签头
        writeTagStart(writer)

        // 标签体
        writeBody(writer)

        // 标签尾
        writeTagEnd(writer)
    }

    /**
     * 写标签头
     * @param writer
     */
    protected fun writeTagStart(writer: JspWriter) {
        if (tag == null)
            return

        // 默认id
        if (id == null)
            id = nextId()

        // 默认name
        if (name == null && path != null)
            name = path

        // 样式
        if (isError && cssErrorClass != null) // 错误样式类
            attrs["class"] = cssErrorClass
        if (attrs["class"] == null && cssClass != null) // 默认样式类
            attrs["class"] = cssClass
        if (cssStyle != null) // 样式
            attrs["style"] = cssStyle

        beforeWriteTag(writer)

        // 标签头
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

    /**
     * 写标签尾
     * @param writer
     */
    protected fun writeTagEnd(writer: JspWriter) {
        if (tag != null)
            writer.append("</").append(tag).append(">")

        afterWriteTag(writer)
    }

    /**
     * 写标签体
     * @param writer
     */
    protected open fun writeBody(writer: JspWriter) {
        if (body != null)
            writer.append(body)
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
     * 重置本地属性
     */
    public override fun reset(){
        super.reset()

        htmlEscape = false
        body = null
        attrs.clear()

        cssClass = null
        cssErrorClass = null
        cssStyle = null
    }

}