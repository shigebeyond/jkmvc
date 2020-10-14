package net.jkcode.jkmvc.orm.serialize

import com.thoughtworks.xstream.XStream
import com.thoughtworks.xstream.converters.Converter
import com.thoughtworks.xstream.converters.MarshallingContext
import com.thoughtworks.xstream.converters.UnmarshallingContext
import com.thoughtworks.xstream.io.HierarchicalStreamReader
import com.thoughtworks.xstream.io.HierarchicalStreamWriter
import net.jkcode.jkmvc.orm.Orm
import net.jkcode.jkmvc.orm.modelOrmMeta
import net.jkcode.jkutil.common.getInheritPropertyClass
import net.jkcode.jkutil.common.isSuperClass
import net.jkcode.jkutil.common.setPropertyValue

/**
 * orm的xstream转换器
 *    主要是将_data序列化成xml, 支持属性别名
 *
 * @Description:
 * @author shijianhang<772910474@qq.com>
 * @date 2020-2-12 11:22 AM
 */
class OrmConverter(protected val xstream: XStream): Converter {

    init {
        /**
         * fix bug: unmarshal() 中 context.convertAnother(reader.value, propType) 解析属性值出错
         * 异常: Caused by: java.lang.IndexOutOfBoundsException: only START_TAG can have attributes END_TAG seen <net.jkcode.jkmvc.tests.model.UserModel>\n  <name>shi</name>... @2:19
         * 原因:
         * AbstractReferenceUnmarshaller.convert()
         * <code>
         *   final String attributeName = getMapper().aliasForSystemAttribute("reference"); // 不为null
         *   final String reference = attributeName == null ? null : reader.getAttribute(attributeName); // 读取失败
         * </code>
         * 解决: 不引用
         */
        xstream.setMode(XStream.NO_REFERENCES)
    }

    override fun canConvert(type: Class<*>): Boolean {
        return Orm::class.java.isSuperClass(type)
    }

    // 序列化
    override fun marshal(source: Any, writer: HierarchicalStreamWriter, context: MarshallingContext) {
        val item = source as Orm
        // 遍历map创建子节点
        for((key, value) in item.getData()){
            // fix bug: 属性值为null, 直接不输出, 否则就算输出空节点 <xxx />, 在反序列化时 `reader.value` 读到的值是空字符串, 也是无法反序列化的
            if(value == null)
                continue

            // 忽略属性
            if(!xstream.mapper.shouldSerializeMember(source.javaClass, key))
                continue

            // key作为节点名
            // 支持别名
            //val key = xstream.mapper.aliasForAttribute(source.javaClass, key) // @deprecated
            val key = xstream.mapper.serializedMember(source.javaClass, key)
            writer.startNode(key)

            // value作为节点值
            //writer.setValue(value?.toString())
            context.convertAnother(value)
            writer.endNode()
        }
    }

    // 反序列化
    override fun unmarshal(reader: HierarchicalStreamReader, context: UnmarshallingContext): Any {
        val type = context.requiredType as Class<Orm> // 当前类

        // fix bug: 实例化不能仅仅用默认构造函数, 还是参数是数组的构造函数
        //val item = type.newInstance() as Orm
        val ormMeta = type.kotlin.modelOrmMeta
        val item = ormMeta.newInstance() as Orm

        // 遍历子节点来构建map
        while (reader.hasMoreChildren()) {
            reader.moveDown()
            // key作为节点名
            var key = reader.nodeName
            // 支持别名
//            key = xstream.mapper.attributeForAlias(type, key) // @deprecated
            key = xstream.mapper.realMember(type, key)

            // value作为节点值
            var value: Any? = reader.value
            // 转换值
            val propType = item::class.getInheritPropertyClass(key)?.java // 属性类
//            val converter = xstream.mapper.getConverterFromItemType(key, propType, type)
//            value = converter.fromString(value)
            if(propType != null)
                value = context.convertAnother(value, propType)

            // fix bug: AppDefinition.id 只是setter, 不是字段
            //item[key] = value
            if(ormMeta.propsAndRelations.contains(key))
                item[key] = value
            else
                item.setPropertyValue(key, value)
            reader.moveUp()
        }
        return item
    }
}  