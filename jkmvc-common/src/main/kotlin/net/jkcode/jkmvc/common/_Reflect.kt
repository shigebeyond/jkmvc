package net.jkcode.jkmvc.common

import org.nustaq.serialization.util.FSTUtil
import sun.reflect.generics.reflectiveObjects.ParameterizedTypeImpl
import java.io.File
import java.lang.invoke.MethodHandle
import java.lang.invoke.MethodHandles
import java.lang.reflect.*
import java.util.*
import java.util.concurrent.CompletableFuture
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.Future
import kotlin.properties.ReadWriteProperty
import kotlin.reflect.*
import kotlin.reflect.full.declaredMemberProperties
import kotlin.reflect.full.memberFunctions
import kotlin.reflect.full.staticFunctions
import kotlin.reflect.full.staticProperties
import kotlin.reflect.jvm.javaType
import java.util.ArrayList

/**
 * 尝试调用克隆方法
 *    1 如果是集合+数组, 则复制为新的集合+数组
 *    2 如果实现了 Cloneable 接口, 则调用并返回 clone(), 否则直接返回 this
 *
 * @param cloningArrayCollectionElement 是否克隆集合+数组的元素
 * @return
 */
public fun Any.tryClone(cloningArrayCollectionElement: Boolean = false):Any {
    // 1 集合
    if(this is Collection<*>)
        return this.cloneCollection(cloningArrayCollectionElement)

    // 2 数组
    if(this.isArray())
        return this.cloneArray(cloningArrayCollectionElement)

    // 3 其他
    if(this !is Cloneable)
        return this

    /*
    val f:KFunction<*> = this::class.getFunction("clone")!!
    return f.call(this) as Any
    */
    val method = this.javaClass.getMethod("clone")
    method.isAccessible = true
    return method.invoke(this)
}

/**
 * 包装clone()的调用
 * @param cloning 是否需要克隆
 * @return
 */
private fun Any.wrapClone(cloning: Boolean): Any{
    return if(cloning) this.tryClone() else this
}

// 单元素list
private val singleListClass = Class.forName("java.util.Collections\$SingletonList")

/**
 * 克隆集合
 * @param cloningElement 是否克隆元素
 * @return
 */
private fun Collection<Any?>.cloneCollection(cloningElement: Boolean = false): Collection<Any?>{
    // 1 特殊处理单元素list
    if(this.javaClass == singleListClass)
        return Collections.singletonList(this.first()?.wrapClone(cloningElement))

    // 2 其他
    val dest = this.javaClass.newInstance() as MutableCollection<Any?>
    for(e in this)
        dest.add(e?.wrapClone(cloningElement))
    return dest
}

/**
 * 克隆数组
 * @param cloningElement 是否克隆元素
 * @return
 */
private fun Any.cloneArray(cloningElement: Boolean = false): Any{
    // 1 对象类型的数据
    if(this is Array<*>) {
        // return Arrays.copyOf(this, this.size) // 元素没clone

        // 由于 T[] != Object[], 就算你能复制属性值, 但是设置属性值时会报错, 因为属性类型与属性值类型不匹配
        //val copy = arrayOfNulls<Any?>(this.size) // Object[]
        val copy = java.lang.reflect.Array.newInstance(this.javaClass.getComponentType(), this.size) as Array<Any?>
        for(i in 0 until this.size)
            copy[i] = this[i]?.wrapClone(cloningElement)
        return copy
    }

    // 2 基础类型的数组
    if(this is IntArray) {
        return Arrays.copyOf(this, this.size)
    }
    if(this is ShortArray)
        return Arrays.copyOf(this, this.size)

    if(this is LongArray)
        return Arrays.copyOf(this, this.size)

    if(this is FloatArray)
        return Arrays.copyOf(this, this.size)

    if(this is DoubleArray)
        return Arrays.copyOf(this, this.size)

    if(this is BooleanArray)
        return Arrays.copyOf(this, this.size)

    throw IllegalArgumentException("Not Array")
}

/**
 * 克隆对象属性
 * @param props 要克隆的属性名
 */
public fun Any.cloneProperties(vararg props: String){
    cloneProperties(false, *props)
}

/**
 * 克隆对象属性
 * @param cloningArrayCollectionElement 是否克隆集合+数组的元素
 * @param props 要克隆的属性名
 */
public fun Any.cloneProperties(cloningArrayCollectionElement: Boolean, vararg props: String){
    cloneProperties(this, this, cloningArrayCollectionElement, *props)
}

/**
 * 克隆对象多个属性
 * @param src 源对象
 * @param dest 目标对象
 * @param cloningArrayCollectionElement 是否克隆集合+数组的元素
 * @param props 要克隆的属性名
 */
private fun cloneProperties(src: Any, dest: Any, cloningArrayCollectionElement: Boolean, vararg props: String){
    if(src.javaClass != dest.javaClass)
        throw IllegalArgumentException("克隆属性中的双方不是同类型")

    for(prop in props)
        cloneProperty(src, dest, prop, cloningArrayCollectionElement)
}

/**
 * 克隆对象单个属性
 *    调用属性值的clone()
 *
 * @param src 源对象
 * @param dest 目标对象
 * @param props 要克隆的属性名
 * @param cloningArrayCollectionElement 是否克隆集合+数组的元素
 */
private fun cloneProperty(src: Any, dest: Any, prop: String, cloningArrayCollectionElement: Boolean) {
    val clazz = src.javaClass
    // 获得字段
    val field = clazz.getWritableFinalField(prop, true)
    // 获得源对象的字段值
    val srcValue = field.get(src)
    if (srcValue == null)
        return

    // 克隆字段值
    val destValue = srcValue.tryClone(cloningArrayCollectionElement)

    // 获得目标对象的字段值
    field.set(dest, destValue)
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
public inline val <T: Any> KClass<T>.defaultValue:T?
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
        } as T?
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

/****************************** java反射扩展: Method *******************************/
/**
 * 是否静态方法
 */
public val Method.isStatic: Boolean
        get() = Modifier.isStatic(modifiers)

/**
 * 从CompletableFuture获得方法结果值
 *
 * @param resFuture
 * @return
 */
public fun Method.resultFromFuture(resFuture: CompletableFuture<*>): Any? {
    // 1 异步结果
    //if (Future::class.java.isAssignableFrom(method.returnType))
    if(this.returnType == Future::class.java
            || this.returnType == CompletableFuture::class.java)
        return resFuture

    // 2 同步结果
    return resFuture.get()
}

/**
 * 获得方法的默认结果值
 * @return
 */
public val Method.defaultResult: Any?
    get(){
        return this.returnType.kotlin.defaultValue
    }


/****************************** java反射扩展: Class *******************************/
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
 * 根据类名+方法签名来获得方法
 *
 * @param methodSignature
 * @return
 */
public fun getMethodByClassAndSignature(clazz: String, methodSignature: String): Method{
    val c = Class.forName(clazz) // ClassNotFoundException
    val m = c.getMethodBySignature(methodSignature)
    if(m == null)
        throw IllegalArgumentException("Class [$clazz] has no method [$methodSignature]") // 无函数
    return m
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
 * 根据方法名来获得方法
 *   忽略参数类型, 一般只用在没有重载的方法中, 方便非java语言(如php)客户端的调用, 不用关心方法签名
 *
 * @param name
 * @return
 */
public fun Class<*>.getMethodByName(name: String): Method?{
    return methods.firstOrNull {
        it.name == name
    }
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
 * @param genTypeIndex 第几个泛型
 * @return
 */
public fun Class<*>.getSuperClassGenricType(genTypeIndex: Int = 0): Class<*>? {
    val genType = this.genericSuperclass
    return getGenricType(genType, genTypeIndex)
}

/**
 * 通过反射, 获得定义 Class 时声明的接口的泛型参数的类型
 *
 * @param interfaceIndex 第几个接口
 * @param genTypeIndex 第几个泛型
 * @return
 */
public fun Class<*>.getInterfaceGenricType(interfaceIndex: Int = 0, genTypeIndex: Int = 0): Class<*>? {
    val genType = this.genericInterfaces.getOrNull(interfaceIndex)
    return getGenricType(genType, genTypeIndex)
}

/**
 * 通过反射, 获得定义 Class 时声明的接口的泛型参数的类型
 *
 * @param interfaceIndex 第几个接口
 * @param genTypeIndex 第几个泛型
 * @return
 */
public fun Class<*>.getInterfaceGenricType(`interface`: Class<*>, genTypeIndex: Int = 0): Class<*>? {
    val genType = this.genericInterfaces.firstOrNull{
        it is ParameterizedType && it.rawType == `interface`
    }
    return getGenricType(genType, genTypeIndex)
}

/**
 * 获得泛型的类型
 * @param genType 泛型类型信息
 * @param genTypeIndex 第几个泛型
 * @return
 */
private fun getGenricType(genType: Type?, genTypeIndex: Int): Class<out Any>? {
    if(genType == null)
        return null

    if (genType !is ParameterizedType)
        return genType as Class<out Any>

    // 泛型参数
    val params = genType.getActualTypeArguments()
    return params.getOrElse(genTypeIndex){
        Any::class.java
    } as Class<*>
}

/**
 * 获得可访问的属性
 * @param name 属性名
 * @return
 */
public fun Class<*>.getAccessibleField(name: String): Field? {
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
 * @param inherited 是否包含继承的属性
 * @return
 */
public fun Class<*>.getWritableFinalField(name: String, inherited: Boolean = false): Field {
    val field = if(inherited)
                    getInheritField(name) // 本类+父类, 可获得父类的protected属性
                else
                    getDeclaredField(name) // 本类, 可获得本类的private属性

    // 去掉final
    if (Modifier.isFinal(field.getModifiers())) {
        val modifiersField = Field::class.java!!.getDeclaredField("modifiers")
        modifiersField.setAccessible(true) //Field 的 modifiers 是私有的
        modifiersField.setInt(field, field.modifiers and Modifier.FINAL.inv())
    }

    // 开放访问
    if (!field.isAccessible)
        field.isAccessible = true

    return field
}

/**
 * 获得属性, 包含继承的
 * @param name
 * @return
 */
public fun Class<*>.getInheritField(name: String): Field{
    var c: Class<*>? = this
    while (c != null) {
        try{
            return c.getDeclaredField(name)
        }catch(e: NoSuchFieldException){
            c = c.superclass
        }
    }
    throw NoSuchFieldException(name)
}

/**
 * 获得可访问的方法
 * @param name 方法名
 * @param parameterTypes 参数类型
 * @return
 */
public fun Class<*>.getAccessibleMethod(name: String, vararg parameterTypes: Class<*>): Method {
    val method = this.getDeclaredMethod(name, *parameterTypes)
    method.setAccessible(true)
    return method
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
        return clazz.getAccessibleField(name)
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
    val field = this.getAccessibleField(name)
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