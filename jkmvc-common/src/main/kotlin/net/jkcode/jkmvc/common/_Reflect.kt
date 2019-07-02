package net.jkcode.jkmvc.common

import org.nustaq.serialization.util.FSTUtil
import sun.reflect.generics.reflectiveObjects.ParameterizedTypeImpl
import java.io.File
import java.lang.invoke.MethodHandle
import java.lang.invoke.MethodHandles
import java.lang.reflect.*
import java.util.concurrent.ConcurrentHashMap
import kotlin.properties.ReadWriteProperty
import kotlin.reflect.*
import kotlin.reflect.full.declaredMemberProperties
import kotlin.reflect.full.memberFunctions
import kotlin.reflect.full.staticFunctions
import kotlin.reflect.full.staticProperties
import kotlin.reflect.jvm.javaType

/**
 * 强制调用克隆方法
 */
public fun Any.forceClone():Any {
    if(!(this is Cloneable))
        throw IllegalArgumentException("非Cloneable对象，不能调用clone()方法")

    val f:KFunction<*> = this::class.getFunction("clone")!!
    return f.call(this) as Any;
}

/**
 * 类的相对路径转类
 * @return
 */
public fun String.classPath2class(): Class<*> {
    // 获得类名
    val className = this.substringBefore(".class").replace(File.separatorChar, '.')
    // 获得类
    return Class.forName(className)
}

/**
 * 获得指定类型的默认值
 * @return
 */
public inline val <T: Any> KClass<T>.defaultValue:T
    get(){
        return when (this) {
            Int::class -> 0
            Long::class -> 0L
            Float::class -> 0.0
            Double::class -> 0.0
            Boolean::class -> false
            Short::class -> 0
            Byte::class -> 0
            else -> null
        } as T
    }

/****************************** kotlin反射扩展: KClass *******************************/
/**
 * 匹配方法的名称与参数类型
 * @param name 方法名
 * @param paramTypes 参数类型
 * @return
 */
public fun KFunction<*>.matches(name:String, paramTypes:Array<out Class<*>>):Boolean{
    // 1 匹配名称
    if(name != this.name)
        return false

    // 2 匹配参数
    // 2.1 匹配参数个数
    if(paramTypes.size != this.parameters.size)
        return false;

    // 2.2 匹配参数类型
    for (i in paramTypes.indices){
        var targetType = this.parameters[i].type.javaType;
        if(targetType is ParameterizedTypeImpl) // 若是泛型类型，则去掉泛型，只保留原始类型
            targetType = targetType.rawType;

        if(paramTypes[i] != targetType)
            return false
    }

    return true;
}

/**
 * 查找方法
 * @param name 方法名
 * @param paramTypes 参数类型
 * @return
 */
public fun KClass<*>.getFunction(name:String, vararg paramTypes:Class<*>): KFunction<*>?{
    // 第一个参数为this
    val pt = toArray(this.java, *paramTypes)
    return memberFunctions.find {
        it.matches(name, pt);
    }
}

/**
 * 查找静态方法
 * @param name 方法名
 * @param paramTypes 参数类型
 * @return
 */
public fun KClass<*>.getStaticFunction(name:String, vararg paramTypes:Class<*>): KFunction<*>?{
    return staticFunctions.find {
        it.matches(name, paramTypes);
    }
}

/**
 * 查找构造函数
 * @param paramTypes 参数类型
 * @return
 */
public fun KClass<*>.getConstructor(vararg paramTypes:Class<*>): KFunction<*>?{
    return constructors.find {
        it.matches("<init>", paramTypes); // 构造函数的名称为 <init>
    }
}

/**
 * 查找属性
 * @param name 属性名
 * @return
 */
public fun <T: Any> KClass<T>.getProperty(name:String): KProperty1<T, *>?{
    return this.declaredMemberProperties.find {
        it.name == name;
    }
}

/**
 * 查找静态属性
 * @param name 属性名
 * @return
 */
public fun <T: Any> KClass<T>.getStaticProperty(name:String): KProperty0<*>? {
    return this.staticProperties.find {
        it.name == name;
    }
}

/**
 * 转换参数类型
 * @param value
 * @return
 */
public inline fun KParameter.convert(value: String): Any {
    return value.to(this.type)
}

/**
 * 创建类的实例
 *   参考 FSTDefaultClassInstantiator#newInstance()
 *
 * @param needInit 是否需要初始化, 即调用类自身的默认构造函数
 * @return
 */
public fun <T: Any> KClass<T>.newInstance(needInit: Boolean = true): Any? {
    // 无[无参数构造函数]
    if(!needInit && java.getConstructorOrNull() == null){
        // best effort. use Unsafe to instantiate.
        // Warning: if class contains transient fields which have default values assigned ('transient int x = 3'),
        // those will not be assigned after deserialization as unsafe instantiation does not execute any default
        // construction code.
        // Define a public no-arg constructor to avoid this behaviour (rarely an issue, but there are cases).
        if (FSTUtil.unFlaggedUnsafe != null)
            return FSTUtil.unFlaggedUnsafe.allocateInstance(java)

        throw RuntimeException("no suitable constructor found and no Unsafe instance avaiable. Can't instantiate " + this)
    }

    return java.newInstance()
}

/**
 * 获得指定属性的getter
 * @param prop
 * @return
 */
public fun <T: Any> KClass<T>.getGetter(prop: String): KProperty1.Getter<T, Any?>? {
    return getProperty(prop)?.getter
}

/**
 * 获得所有属性的getter
 * @return
 */
public fun <T: Any> KClass<T>.getGetters(): Map<String, KProperty1.Getter<T, Any?>> {
    return declaredMemberProperties.associate { prop ->
        prop.name to prop.getter
    }
}

/****************************** java反射扩展: Class *******************************/
/**
 * 是否静态方法
 */
public val Method.isStatic: Boolean
        get() = Modifier.isStatic(modifiers)

/**
 * 是否抽象类
 */
public val <T> Class<T>.isAbstract: Boolean
    get() =  Modifier.isAbstract(modifiers)

/**
 * 检查当前类 是否是 指定类的子类
 *
 * @param superClass 父类
 * @return
 */
public fun Class<*>.isSubClass(superClass: Class<*>): Boolean {
    return this != superClass && superClass.isAssignableFrom(this)
}

/**
 * 检查当前类 是否是 指定类的父类
 *     isSuperClass() 不包含当前类
 *     isAssignableFrom() 包含当前类
 *
 * @param subClass 子类
 * @return
 */
public fun Class<*>.isSuperClass(subClass: Class<*>): Boolean {
    return this != subClass && this.isAssignableFrom(subClass)
}

/**
 * 获得方法签名
 * @param withClass
 * @return
 */
public fun Method.getSignature(withClass: Boolean = false): String {
    val buffer = StringBuilder()
    // 类名
    if(withClass)
        buffer.append(this.declaringClass.name).append('.')
    // 方法名
    buffer.append(this.name)
    // 参数类型
    return this.parameterTypes.joinTo(buffer, ",", "(", ")"){
        it.name
    }.toString().replace("java.lang.", "")
}

/**
 * 类的方法缓存: <类 to <方法签名 to 方法>>
 */
private val class2methods: ConcurrentHashMap<String, Map<String, Method>> = ConcurrentHashMap();

/**
 * 获得当前类的方法哈希: <方法签名 to 方法>
 * @return
 */
public fun Class<*>.getMethodSignatureMaps(): Map<String, Method> {
    return class2methods.getOrPut(name){
        // 将该类的方法拼接成map
        methods.associate {
            it.getSignature() to it
        }
    }
}

/**
 * 根据方法签名来获得方法
 *
 * @param methodSignature
 * @return
 */
public fun Class<*>.getMethodBySignature(methodSignature: String): Method?{
    val methods = getMethodSignatureMaps()
    return methods[methodSignature]
}

/**
 * 查找构造函数
 * @param paramTypes 参数类型
 * @return
 */
public inline fun <T> Class<T>.getConstructorOrNull(vararg parameterTypes: Class<*>): Constructor<T>? {
    try{
        return this.getConstructor(*parameterTypes)
    }catch (e: NoSuchMethodException){
        return null
    }
}

/**
 * loop缓存: <类 to MethodHandles.Lookup>
 */
private val class2lookups: ConcurrentHashMap<Class<*>, MethodHandles.Lookup> = ConcurrentHashMap();

/**
 * 获得类对应的MethodHandles.Lookup对象
 * @return
 */
public fun Class<*>.getLookup(): MethodHandles.Lookup {
    return class2lookups.getOrPut(this) {
        // 反射调用 MethodHandles.Lookup 的私有构造方法
        val constructor = MethodHandles.Lookup::class.java.getDeclaredConstructor(this.javaClass)
        constructor.isAccessible = true
        constructor.newInstance(this)
    }
}

/**
 * 获得方法对应的MethodHandle对象
 * @return
 */
public fun Method.getMethodHandle(): MethodHandle {
    return declaringClass.getLookup().unreflectSpecial(this, declaringClass)
}

/**
 * 通过反射, 获得定义 Class 时声明的父类的泛型参数的类型
 *
 * @param index
 * @return
 */
fun Class<*>.getSuperClassGenricType(index: Int = 0): Class<*> {
    val genType = this.genericSuperclass

    if (genType !is ParameterizedType)
        return Any::class.java

    val params = genType.getActualTypeArguments()

    if (index >= params.size || index < 0)
        return Any::class.java

    if (params[index] !is Class<*>)
        return Any::class.java

    return params[index] as Class<*>
}

/**
 * 获得private的属性, 并使其可读
 * @param name 属性名
 * @return
 */
public fun Class<*>.getReadableField(name: String): Field? {
    try {
        val field = getDeclaredField(name)

        // 开放访问
        if (!field.isAccessible)
            field.isAccessible = true

        return field
    }catch (e: NoSuchFieldException){
        return null
    }
}

/**
 * 获得final的属性, 并使其可写
 * @param name 属性名
 * @return
 */
public fun Class<*>.getWritableFinalField(name: String): Field {
    val field = getDeclaredField(name)

    // 去掉final
    val modifiersField = Field::class.java!!.getDeclaredField("modifiers")
    modifiersField.setAccessible(true) //Field 的 modifiers 是私有的
    modifiersField.setInt(field, field.modifiers and Modifier.FINAL.inv())

    // 开放访问
    if (!field.isAccessible)
        field.isAccessible = true

    return field
}

/****************************** 代理实现接口 *******************************/
/**
 * 实现某接口的代理字段的迭代器
 *    如类定义如下:
 *    <code>class Test: CharSequence by "", IIdWorker by SnowflakeIdWorker()</code>
 *
 *    而kotlin编译代码如下, 他为代理对象生成的字段名为 $$delegate_0 / $$delegate_1/... 之类
 *    <code>
 *    public final class Test implements CharSequence, IIdWorker{
 *    	private final /* synthetic */ String $$delegate_0;
 *    	private final /* synthetic */ SnowflakeIdWorker $$delegate_1;
 *
 *    	public Test2() {
 *    		this.$$delegate_0 = "";
 *    		this.$$delegate_1 = new SnowflakeIdWorker();
 *    	}
 *		...
 *    }
 *    </code>
 */
class InterfaceDelegateFieldIterator(protected val clazz: Class<*>) : Iterator<Field> {

    protected var curr = -1

    override fun hasNext(): Boolean {
        return getInterfaceDelegateField(curr + 1) != null
    }

    override fun next(): Field {
        return getInterfaceDelegateField(++curr)!!
    }

    /**
     * 获得代理字段
     *    字段名为 $$delegate_0 / $$delegate_1/..., 逐个去试
     */
    protected fun getInterfaceDelegateField(i: Int): Field? {
        val name = "\$\$delegate_" + i
        return clazz.getReadableField(name)
    }
}

/**
 * 获得实现某接口的代理字段
 * @param `interface` 被代理的接口
 * @return 代理字段
 */
public fun Class<*>.getInterfaceDelegateField(`interface`: Class<*>): Field? {
    // 遍历代理字段
    for(field in InterfaceDelegateFieldIterator(this))
    // 匹配类型
        if(`interface`.isAssignableFrom(field.type))
            return field

    return null
}

/**
 * 获得所有的实现接口的代理字段
 * @return 代理字段
 */
public fun Class<*>.getInterfaceDelegateFields(): Collection<Field> {
    return InterfaceDelegateFieldIterator(this).map { it }
}

/**
 * 获得实现某接口的代理字段
 *    泛型T就是接口, 当前类使用代理对象来实现某接口
 * @return 代理对象
 */
public inline fun <reified T> Any.getInterfaceDelegate(): T? {
    return getInterfaceDelegate(T::class.java)
}

/**
 * 获得实现某接口的代理字段
 *    泛型T就是接口, 当前类使用代理对象来实现某接口
 * @return 代理对象
 */
public fun <T> Any.getInterfaceDelegate(`interface`: Class<T>): T? {
    // 获得代理字段
    val field = this.javaClass.getInterfaceDelegateField(`interface`)
    if(field == null)
        return null

    // 获得代理对象
    return field.get(this) as T
}

/****************************** 代理实现属性读写 *******************************/

/**
 * 实现属性读写的代理字段的迭代器
 *    如属性定义如下:
 *    <code>public var id:Int by property()</code>
 *
 *    而kotlin编译代码如下, 他为代理对象生成的字段名为: "属性名$delegate"
 *    <code>
 *    private final ReadWriteProperty id$delegate;
 *
 *    public final int getId() {
 *        return ((Number)this.id$delegate.getValue((Object)this, MessageEntity.$$delegatedProperties[0])).intValue();
 *    }
 *
 *    public final void setId(final int <set-?>) {
 *        this.id$delegate.setValue((Object)this, MessageEntity.$$delegatedProperties[0], (Object)<set-?>);
 *    }
 *
 *    // 构造函数
 *    public MessageEntity() {
 *        this.id$delegate = this.property();
 *    }
 *    </code>
 */
class PropDelegateFieldIterator(protected val clazz: Class<*>) : Iterator<Field> {

    // kotlin属性
    protected val props = clazz.kotlin.declaredMemberProperties

    protected var curr = -1

    override fun hasNext(): Boolean {
        return getPropDelegateField(curr + 1) != null
    }

    override fun next(): Field {
        return getPropDelegateField(++curr)!!
    }

    /**
     * 获得代理字段
     *    字段名为: "属性名$delegate"
     */
    protected fun getPropDelegateField(i: Int): Field? {
        if(i >= props.size)
            return null

        return clazz.getPropoDelegateField(props[i].name)
    }
}

/**
 * 获得实现属性读写的代理字段
 * @param prop 属性名
 * @return 代理字段
 */
public fun Class<*>.getPropoDelegateField(prop: String): Field? {
    val name = prop + "\$delegate"
    val field = this.getReadableField(name)
    if(field?.type == ReadWriteProperty::class.java)
        return field

    return null
}

/**
 * 获得所有的实现属性读写的代理字段
 * @return 代理字段
 */
public fun Class<*>.getPropDelegateFields(): Collection<Field> {
    return PropDelegateFieldIterator(this).map { it }
}

/**
 * 获得实现属性读写的代理字段
 * @param prop 属性名
 * @return 代理对象
 */
public fun Any.getPropDelegate(prop: String): ReadWriteProperty<*, *>? {
    // 获得代理字段
    val field = this.javaClass.getPropoDelegateField(prop)
    if(field == null)
        return null

    // 获得代理对象
    return field.get(this) as ReadWriteProperty<*, *>?
}