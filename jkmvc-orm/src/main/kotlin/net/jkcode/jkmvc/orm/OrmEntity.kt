package net.jkcode.jkmvc.orm

import net.jkcode.jkmvc.common.*
import net.jkcode.jkmvc.db.MutableRow
import net.jkcode.jkmvc.serialize.ISerializer
import java.math.BigDecimal
import java.util.*
import kotlin.properties.ReadWriteProperty
import kotlin.reflect.KMutableProperty1
import kotlin.reflect.KProperty

/**
 * ORM之实体对象
 *  1. 本来想继承 MutableMap<String, Any?>, 但是得不偿失, 不值得做
 *    仅仅需要的是get()/put()
 *    可有可无的是size()/isEmpty()/containsKey()/containsValue()
 *    完全不需要的是remove()/clear()/keys/values/entries/MutableEntry
 *
 *  2. data 属性的改写
 *  2.1 子类 OrmValid 中改写
 *      改写为 net.jkcode.jkmvc.common.FixedKeyMapFactory.FixedKeyMap
 *      由于是直接继承 OrmEntity 来改写的, 因此直接覆写 data 属性, 因此能够应用到依赖 data 属性的方法
 *
 *  2.2 在实体类 XXXEntity 与模型类 XXXModel 分离的场景下改写, 如:
 *      XXXEntity: open class MessageEntity: OrmEntity()
 *      XXXModel: class MessageModel: MessageEntity(), IOrm by GeneralModel(m)
 *      而 XXXModel 继承于 XXXEntity 是为了继承与复用其声明的属性, 但是 IOrm 的方法全部交由 GeneralModel 代理来改写, 也就对应改写掉 XXXEntity/OrmEntity 中与 IOrm 重合的方法(即 IOrmEntity 的方法)
 *      但是注意某些方法与属性是 XXXEntity/OrmEntity 特有的, 没有归入 IOrm 接口, 也就是说 GeneralModel 不能改写这些方法与属性
 *      如 data 是内部属性无法被 IOrm 接口暴露
 *
 * @author shijianhang
 * @date 2016-10-10 上午12:52:34
 *
 */
open class OrmEntity : IOrmEntity {

    companion object{

        /**
         * orm配置
         */
        public val config: Config = Config.instance("orm")

        /**
         * 序列化
         */
        public val serializer: ISerializer = ISerializer.instance(config["serializeType"]!!)

        /**
         * 缓存属性代理
         */
        public val prop = (object : ReadWriteProperty<IOrmEntity, Any?> {
            // 获得属性
            public override operator fun getValue(thisRef: IOrmEntity, property: KProperty<*>): Any? {
                return thisRef[property.name]
            }

            // 设置属性
            public override operator fun setValue(thisRef: IOrmEntity, property: KProperty<*>, value: Any?) {
                thisRef[property.name] = value
            }
        })
    }

    /**
     * 最新的字段值：<字段名 to 最新字段值>
     * 1 子类会改写
     * 2 延迟加载, 对于子类改写是没有意义的, 但针对实体类 XXXEntity 与模型类 XXXModel 分离的场景下是有意义的, 也就是IOrm 的方法全部交由 GeneralModel 代理来改写, 也就用不到该类的 data 属性
     */
    protected open val data: MutableRow by lazy{
        HashMap<String, Any?>()
    }

    /**
     * 获得属性代理
     */
    public fun <T> property(): ReadWriteProperty<IOrmEntity, T> {
        return prop as ReadWriteProperty<IOrmEntity, T>;
    }

    /**
     * 判断是否有某字段
     *
     * @param column
     * @return
     */
    public override fun hasColumn(column: String): Boolean {
        return true;
    }

    /**
     * 设置对象字段值
     *    子类会改写
     *
     * @param column 字段名
     * @param  value  字段值
     */
    public override operator fun set(column: String, value: Any?) {
        data[column] = value;
    }

    /**
     * 判断属性值是否相等
     *    只在 set() 中调用，用于检查属性值是否修改
     *
     * @param oldValue
     * @param newValue
     * @return
     */
    protected fun equalsValue(oldValue: Any?, newValue: Any?): Boolean{
        if(oldValue == newValue) // 相等
            return true

        if(oldValue == null || newValue == null) // 不等，却有一个为空
            return false

        if(oldValue is BigDecimal && newValue !is BigDecimal) // 不等，却是 BigDecimal 与 其他数值类型
            return oldValue.toNumber(newValue.javaClass) == newValue // 由于只在 set() 调用，所以假定oldValue转为newValue的类型时，不丢失精度

        return false
    }

    /**
     * 获得对象字段
     *    子类会改写
     *
     * @param column 字段名/home/shi/Applications/jdk1.8.0_172/bin/java -ea -Didea.test.cyclic.buffer.size=1048576 -javaagent:/home/shi/Applications/idea-IC-181.4668.68/lib/idea_rt.jar=34459:/home/shi/Applications/idea-IC-181.4668.68/bin -Dfile.encoding=UTF-8 -classpath /home/shi/Applications/idea-IC-181.4668.68/lib/idea_rt.jar:/home/shi/Applications/idea-IC-181.4668.68/plugins/junit/lib/junit-rt.jar:/home/shi/Applications/idea-IC-181.4668.68/plugins/junit/lib/junit5-rt.jar:/home/shi/Applications/jdk1.8.0_172/jre/lib/charsets.jar:/home/shi/Applications/jdk1.8.0_172/jre/lib/deploy.jar:/home/shi/Applications/jdk1.8.0_172/jre/lib/ext/cldrdata.jar:/home/shi/Applications/jdk1.8.0_172/jre/lib/ext/dnsns.jar:/home/shi/Applications/jdk1.8.0_172/jre/lib/ext/jaccess.jar:/home/shi/Applications/jdk1.8.0_172/jre/lib/ext/jfxrt.jar:/home/shi/Applications/jdk1.8.0_172/jre/lib/ext/localedata.jar:/home/shi/Applications/jdk1.8.0_172/jre/lib/ext/nashorn.jar:/home/shi/Applications/jdk1.8.0_172/jre/lib/ext/sunec.jar:/home/shi/Applications/jdk1.8.0_172/jre/lib/ext/sunjce_provider.jar:/home/shi/Applications/jdk1.8.0_172/jre/lib/ext/sunpkcs11.jar:/home/shi/Applications/jdk1.8.0_172/jre/lib/ext/zipfs.jar:/home/shi/Applications/jdk1.8.0_172/jre/lib/javaws.jar:/home/shi/Applications/jdk1.8.0_172/jre/lib/jce.jar:/home/shi/Applications/jdk1.8.0_172/jre/lib/jfr.jar:/home/shi/Applications/jdk1.8.0_172/jre/lib/jfxswt.jar:/home/shi/Applications/jdk1.8.0_172/jre/lib/jsse.jar:/home/shi/Applications/jdk1.8.0_172/jre/lib/management-agent.jar:/home/shi/Applications/jdk1.8.0_172/jre/lib/plugin.jar:/home/shi/Applications/jdk1.8.0_172/jre/lib/resources.jar:/home/shi/Applications/jdk1.8.0_172/jre/lib/rt.jar:/oldhome/shi/code/java/jkmvc/jkmvc-orm/out/test/classes:/oldhome/shi/code/java/jkmvc/jkmvc-orm/out/production/classes:/oldhome/shi/code/java/jkmvc/jkmvc-common/out/production/classes:/home/shi/.gradle/caches/modules-2/files-2.1/com.alibaba/druid/1.1.6/996fccd9e30c10e113d4893f77aaddf88c819f8b/druid-1.1.6.jar:/home/shi/.gradle/caches/modules-2/files-2.1/org.jetbrains.kotlin/kotlin-stdlib-jdk8/1.3.21/d0634d54452abc421db494ad32dd215e6591c49f/kotlin-stdlib-jdk8-1.3.21.jar:/home/shi/.gradle/caches/modules-2/files-2.1/org.jetbrains.kotlin/kotlin-reflect/1.3.21/d0d5ff2ac2ebd8a42697af41e20fc225a23c5d3b/kotlin-reflect-1.3.21.jar:/home/shi/.gradle/caches/modules-2/files-2.1/commons-collections/commons-collections/3.2.1/761ea405b9b37ced573d2df0d1e3a4e0f9edc668/commons-collections-3.2.1.jar:/home/shi/.gradle/caches/modules-2/files-2.1/mysql/mysql-connector-java/5.1.6/380ef5226de2c85ff3b38cbfefeea881c5fce09d/mysql-connector-java-5.1.6.jar:/home/shi/.gradle/caches/modules-2/files-2.1/com.google.guava/guava/27.0.1-jre/bd41a290787b5301e63929676d792c507bbc00ae/guava-27.0.1-jre.jar:/home/shi/.gradle/caches/modules-2/files-2.1/dom4j/dom4j/1.6.1/5d3ccc056b6f056dbf0dddfdf43894b9065a8f94/dom4j-1.6.1.jar:/home/shi/.gradle/caches/modules-2/files-2.1/sax/sax/2.0.1/483ed610719e5d9c5777d65a5760eaa4238eae1b/sax-2.0.1.jar:/home/shi/.gradle/caches/modules-2/files-2.1/org.yaml/snakeyaml/1.18/e4a441249ade301985cb8d009d4e4a72b85bf68e/snakeyaml-1.18.jar:/home/shi/.gradle/caches/modules-2/files-2.1/com.alibaba/fastjson/1.2.39/24a08b8de679614110d1a5a7e6524eb8f9c64f2/fastjson-1.2.39.jar:/home/shi/.gradle/caches/modules-2/files-2.1/org.slf4j/slf4j-log4j12/1.7.12/485f77901840cf4e8bf852f2abb9b723eb8ec29/slf4j-log4j12-1.7.12.jar:/home/shi/.gradle/caches/modules-2/files-2.1/org.slf4j/slf4j-api/1.7.12/8e20852d05222dc286bf1c71d78d0531e177c317/slf4j-api-1.7.12.jar:/home/shi/.gradle/caches/modules-2/files-2.1/redis.clients/jedis/2.9.0/292bc9cc26553acd3cccc26f2f95620bf88a04c2/jedis-2.9.0.jar:/home/shi/.gradle/caches/modules-2/files-2.1/de.ruedigermoeller/fst/2.55/feec3c7d315686d14c7014855a0357cde630a4ab/fst-2.55.jar:/home/shi/.gradle/caches/modules-2/files-2.1/com.caucho/hessian/4.0.51/29047eb08639a98e4b9437d7cbe81fb0471ba7/hessian-4.0.51.jar:/home/shi/.gradle/caches/modules-2/files-2.1/com.esotericsoftware/kryo-shaded/4.0.0/4ae3bacfaea6459d8d63c6cf17c3718422fb2def/kryo-shaded-4.0.0.jar:/home/shi/.gradle/caches/modules-2/files-2.1/com.dyuproject.protostuff/protostuff-core/1.0.8/f57270c6219aa606d4be907f6ae12cc35a8ad7b2/protostuff-core-1.0.8.jar:/home/shi/.gradle/caches/modules-2/files-2.1/com.dyuproject.protostuff/protostuff-runtime/1.0.8/852c60a527a1d0815c2690e26a29f501c7cd752d/protostuff-runtime-1.0.8.jar:/home/shi/.gradle/caches/modules-2/files-2.1/org.apache.httpcomponents/httpclient/4.5.4/2f8a3c0c53550b237d8f7a98a417397395af8b80/httpclient-4.5.4.jar:/home/shi/.gradle/caches/modules-2/files-2.1/io.netty/netty-all/4.1.14.Final/3e2d12bfcd85fb65c3a4d4c0432152935d1228aa/netty-all-4.1.14.Final.jar:/home/shi/.gradle/caches/modules-2/files-2.1/org.jetbrains.kotlin/kotlin-stdlib-jdk7/1.3.21/d207ce2c9bcf17dc8e51bab4dbfdac4d013e7138/kotlin-stdlib-jdk7-1.3.21.jar:/home/shi/.gradle/caches/modules-2/files-2.1/org.jetbrains.kotlin/kotlin-stdlib/1.3.21/4bcc2012b84840e19e1e28074284cac908be0295/kotlin-stdlib-1.3.21.jar:/home/shi/.gradle/caches/modules-2/files-2.1/com.google.guava/failureaccess/1.0.1/1dcf1de382a0bf95a3d8b0849546c88bac1292c9/failureaccess-1.0.1.jar:/home/shi/.gradle/caches/modules-2/files-2.1/com.google.guava/listenablefuture/9999.0-empty-to-avoid-conflict-with-guava/b421526c5f297295adef1c886e5246c39d4ac629/listenablefuture-9999.0-empty-to-avoid-conflict-with-guava.jar:/home/shi/.gradle/caches/modules-2/files-2.1/com.google.code.findbugs/jsr305/3.0.2/25ea2e8b0c338a877313bd4672d3fe056ea78f0d/jsr305-3.0.2.jar:/home/shi/.gradle/caches/modules-2/files-2.1/org.checkerframework/checker-qual/2.5.2/cea74543d5904a30861a61b4643a5f2bb372efc4/checker-qual-2.5.2.jar:/home/shi/.gradle/caches/modules-2/files-2.1/com.google.errorprone/error_prone_annotations/2.2.0/88e3c593e9b3586e1c6177f89267da6fc6986f0c/error_prone_annotations-2.2.0.jar:/home/shi/.gradle/caches/modules-2/files-2.1/com.google.j2objc/j2objc-annotations/1.1/ed28ded51a8b1c6b112568def5f4b455e6809019/j2objc-annotations-1.1.jar:/home/shi/.gradle/caches/modules-2/files-2.1/org.codehaus.mojo/animal-sniffer-annotations/1.17/f97ce6decaea32b36101e37979f8b647f00681fb/animal-sniffer-annotations-1.17.jar:/home/shi/.gradle/caches/modules-2/files-2.1/xml-apis/xml-apis/1.0.b2/3136ca936f64c9d68529f048c2618bd356bf85c9/xml-apis-1.0.b2.jar:/home/shi/.gradle/caches/modules-2/files-2.1/log4j/log4j/1.2.17/5af35056b4d257e4b64b9e8069c0746e8b08629f/log4j-1.2.17.jar:/home/shi/.gradle/caches/modules-2/files-2.1/org.apache.commons/commons-pool2/2.4.2/e5f4f28f19d57716fbc3989d7a357ebf1e454fea/commons-pool2-2.4.2.jar:/home/shi/.gradle/caches/modules-2/files-2.1/com.fasterxml.jackson.core/jackson-core/2.8.8/d478fb6de45a7c3d2cad07c8ad70c7f0a797a020/jackson-core-2.8.8.jar:/home/shi/.gradle/caches/modules-2/files-2.1/org.javassist/javassist/3.21.0-GA/598244f595db5c5fb713731eddbb1c91a58d959b/javassist-3.21.0-GA.jar:/home/shi/.gradle/caches/modules-2/files-2.1/org.objenesis/objenesis/2.5.1/272bab9a4e5994757044d1fc43ce480c8cb907a4/objenesis-2.5.1.jar:/home/shi/.gradle/caches/modules-2/files-2.1/com.esotericsoftware/minlog/1.3.0/ff07b5f1b01d2f92bb00a337f9a94873712f0827/minlog-1.3.0.jar:/home/shi/.gradle/caches/modules-2/files-2.1/com.dyuproject.protostuff/protostuff-collectionschema/1.0.8/dae5cac42d1832ba15767608bfacdf6ca09f93c9/protostuff-collectionschema-1.0.8.jar:/home/shi/.gradle/caches/modules-2/files-2.1/com.dyuproject.protostuff/protostuff-api/1.0.8/850dcf054785a0939de65f923eb7a0b4b42bfdd6/protostuff-api-1.0.8.jar:/home/shi/.gradle/caches/modules-2/files-2.1/org.apache.httpcomponents/httpcore/4.4.7/5442c20f3568da63b17e0066b06cd88c2999dc14/httpcore-4.4.7.jar:/home/shi/.gradle/caches/modules-2/files-2.1/commons-logging/commons-logging/1.2/4bfc12adfe4842bf07b657f0369c4cb522955686/commons-logging-1.2.jar:/home/shi/.gradle/caches/modules-2/files-2.1/commons-codec/commons-codec/1.10/4b95f4897fa13f2cd904aee711aeafc0c5295cd8/commons-codec-1.10.jar:/home/shi/.gradle/caches/modules-2/files-2.1/org.jetbrains.kotlin/kotlin-stdlib-common/1.3.21/f30e4a9897913e53d778f564110bafa1fef46643/kotlin-stdlib-common-1.3.21.jar:/home/shi/.gradle/caches/modules-2/files-2.1/org.jetbrains/annotations/13.0/919f0dfe192fb4e063e7dacadee7f8bb9a2672a9/annotations-13.0.jar:/home/shi/Applications/idea-IC-181.4668.68/lib/junit-4.12.jar:/home/shi/Applications/idea-IC-181.4668.68/lib/hamcrest-core-1.3.jar com.intellij.rt.execution.junit.JUnitStarter -ideVersion5 -junit4 net.jkcode.jkmvc.tests.EntityTests,testOrmSerialize
class net.jkcode.jkmvc.model.GeneralModel: {fromUid=7, toUid=8, content=hello orm}
{toUid=8, fromUid=7, id=null, content=hello orm}

     * @param defaultValue 默认值
     * @return
     */
    public override operator fun <T> get(column: String, defaultValue: T?): T {
        if (!hasColumn(column))
            throw OrmException("类 ${this.javaClass} 没有字段 $column");

        return (data[column] ?: defaultValue) as T
    }

    /**
     * 暴露data属性, 仅限orm模块使用
     * @return
     */
    internal fun getData(): MutableRow {
        return data
    }

    /**
     * 从其他实体对象中设置字段值
     *   子类会改写
     * @param from
     */
    public override fun from(from: IOrmEntity): Unit{
        from.toMap(data)
    }

    /**
     * 从map中设置字段值
     *   子类会改写
     * @param from   字段值的哈希：<字段名 to 字段值>
     * @param expected 要设置的字段名的列表
     */
    public override fun fromMap(from: Map<String, Any?>, expected: List<String>): Unit {
        copyMap(from, data, expected)
    }

    /**
     * 获得字段值 -- 转为Map
     *     子类会改写
     * @param to
     * @param expected 要设置的字段名的列表
     * @return
     */
    public override fun toMap(to: MutableMap<String, Any?>, expected: List<String>): MutableMap<String, Any?> {
        return copyMap(data, to, expected)
    }

    /**
     * 从from中复制字段值到to
     *
     * @param from 源map
     * @param to 目标map
     * @param expected 要设置的字段名的列表
     * @return
     */
    protected fun copyMap(from: Map<String, Any?>, to: MutableMap<String, Any?>, expected: List<String>): MutableMap<String, Any?> {
        val columns = if (expected.isEmpty())
                        from.keys
                    else
                        expected

        for (column in columns)
            to[column] = from[column]

        return to
    }

    /**
     * 智能设置属性
     *    1 智能化
     *    在不知属性类型的情况下，将string赋值给属性
     *    => 需要将string转换为属性类型
     *    => 需要显式声明属性
     *
     *    2 不确定性
     *    一般用于从Request对象中批量获得属性，即获得与请求参数同名的属性值
     *    但是请求中可能带其他参数，不一定能对应到该对象的属性名，因此不抛出异常，只返回bool
     *
     * <code>
     *     class UserModel(id:Int? = null): Orm(id) {
     *          ...
     *          public var id:Int by property<Int>(); //需要显式声明属性
     *     }
     *
     *     val user = UserModel()
     *     user.id = String.parseInt("123")
     *     // 相当于
     *     user.setIntelligent("id", "123")
     * </code>
     *
     * @param column
     * @param value 字符串
     * @return
     */
    public override fun setIntelligent(column:String, value:String): Boolean {
        if (!hasColumn(column))
            return false;

        // 1 获得属性
        val prop = this::class.getProperty(column) as KMutableProperty1?
        if(prop == null)
            return false

        try {
            // 2 准备参数: 转换类型
            val param = value.to(prop.setter.parameters[1].type)
            // 3 调用setter方法
            prop.setter.call(this, param);
            return true
        }catch (e: Exception){
            throw OrmException("智能设置属性[$column=$value]错误: ${e.message}", e)
        }
    }

    /**
     * 序列化
     * @return
     */
    public override fun serialize(): ByteArray? {
        return serializer.serialize(this.toMap())
    }

    /**
     * 序列化
     *
     * @param bytes
     */
    public override fun unserialize(bytes: ByteArray): Unit {
        data.putAll(serializer.unserizlize(bytes) as Map<String, Any?>)
    }

    /**
     * 编译字符串模板
     *
     * @param template 字符串模板，格式 "name=:name, age=:age"，用:来修饰字段名
     * @return 将模板中的字段名替换为字段值
     */
    public override fun compileTemplate(template:String):String{
        // 1 编译模板
        if(template.contains(':'))
            return template.replaces(data);

        // 2 输出单个字段
        return data[template].toString()
    }

    /**
     * 改写 toString()
     *   在实体类 XXXEntity 与模型类 XXXModel 分离的场景下改写 OrmEntity.toString(), 如:
     *   XXXEntity: open class MessageEntity: OrmEntity()
     *   XXXModel: class MessageModel: MessageEntity(), IOrm by GeneralModel(m)
     *   而 XXXModel 继承于 XXXEntity 是为了继承与复用其声明的属性, 但是 IOrm 的方法全部交由 GeneralModel 代理来改写, 也就对应改写掉 XXXEntity/OrmEntity 中与 IOrm 重合的方法(即 IOrmEntity 的方法)
     *   但是某些方法与属性是 XXXEntity/OrmEntity 特有的, 没有归入 IOrm 接口, 也就是说 GeneralModel 不能改写这些方法与属性
     *   => 将 toString() 归入 IOrm 接口
     */
    public override fun toString(): String{
        return "${this.javaClass}: $data"
    }

}
