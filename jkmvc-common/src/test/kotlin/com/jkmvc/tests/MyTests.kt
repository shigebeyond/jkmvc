package com.jkmvc.tests

import com.jkmvc.cache.JedisFactory
import com.jkmvc.common.*
import com.jkmvc.validate.Validation
import getIntranetHost
//import kotlinx.coroutines.experimental.*
import org.dom4j.Attribute
import org.dom4j.DocumentException
import org.dom4j.Element
import org.dom4j.io.SAXReader
import org.junit.Test
import java.io.File
import java.util.*
import kotlin.reflect.KClass
import kotlin.reflect.KMutableProperty1
import java.util.Calendar
import java.util.GregorianCalendar




open class A() {}
class B():A() {
    protected val name:String = "test";
}

fun A.echo(){
    println("hi, I'm A")
}

fun B.echo(){
    println("hi, I'm B")
}

enum class NumType {
    Byte,
    Short,
    INT,
    LONG
}

class MyTests{

    @Test
    fun testBig(){
//        var a= BigDecimal("100")
//        var b=BigDecimal("3")
//        var c=a/b;
//        println(c)
//        var d=a.divide(b);
//        println(d)
    }

    @Test
    fun testMap(){
        //val map = HashMap<Int, String>() // {1=a, 2=b}
        val map = TreeMap<Int, String>() // {1=a, 2=b}
        map[2] = "a"
        map[1] = "b"
        for((k, v) in map){
            println("$k = $v")
        }
    }

    @Test
    fun testList(){
        val list:MutableList<String> = LinkedList()
        list.add("a")
        list += "a"
        list.add("b")
        list.add("c")
        println(list)
        /*list.remove("c")
        println(list)
        list.add(0, "d")
        println(list)*/

        var n = list.size
        val it = list.iterator()
        while (--n > 0){
            it.next()
        }
        println(it.next())
    }

    @Test
    fun testIp(){
        // 127.0.1.1
        //val addr = InetAddress.getLocalHost().hostAddress
        //val addr = InetAddress.getLoopbackAddress().hostAddress
        //println(addr)

        /*for(netInterface in NetworkInterface.getNetworkInterfaces()){
            for(ip in netInterface.inetAddresses){
                if (ip != null && ip is Inet4Address) {
                    println("本机的IP = " + ip.hostAddress)
                }
            }
        }*/
        // 内网ip
        println(getIntranetHost())
    }

    @Test
    fun testPerform(){
        val start = System.currentTimeMillis()
        val n = 1000000
        val map = HashMap<String, Long>(n, 0.75f)
        val sb = StringBuilder(100)


        for (i in 0..n - 1) {
            val time = System.currentTimeMillis()
            map.put(sb.append(i).append("_").append(time).toString(), time)
            sb.delete(0, sb.length)
        }
        println((System.currentTimeMillis() - start) / 1000.0)
        Thread.sleep(150000)
    }

    @Test
    fun testString(){
//        val m = "jdbc:mysql://[^/]+/([^\\?]+)".toRegex().find("jdbc:mysql://127.0.0.1/test?useUnicode=true&characterEncoding=utf-8")
//        if(m != null)
//            println(m.groups[1]!!.value)
        /*val funname = "indexAction"
        val name = funname.substring(0, funname.length - 6) // 去掉Action结尾
        println(name)*/
//        println("my_favorite_food".underline2Camel())
//        println("myFavoriteFood".camel2Underline())
//        println("2017-09-27 08:47:04".to(Date::class))
        /*val str:Any? = null
        println(str.toString())//null
        println(arrayOf("a", "b").joinToString(", ", "(", ")") {
            "[$it]"
        })*/
        //println("01".toInt())
        // 长文本
        /*val str = """
        hello
        world
        """
        println(str)*/

        // 去空格
        println("hello world".trim())
        println("hello world".replace(" ", ""))
    }

    @Test
    fun testFile(){
        /*val f = File("/home/shi/test/wiki.txt")
        val ms = "<span.*>([^<]*)</span>".toRegex().findAll(f.readText())
        for(m in ms){
            println(m.groups[0]!!.value)
        }
        f.replaceText {
            "<span.*>([^<]*)</span>".toRegex().replace(it){ result: MatchResult ->
                result.groups[1]!!.value
            }
        }*/

        // http://yipeiwu.com/getvideo.html
        // 下载网易公开课
        /*val f = File("/home/shi/test/course.html")
        val ms = "<tr>\\s*<td>(.+)\\s*</td>\\s*<td><a href=\"([^\"]+)\".+</td>\\s*</tr>".toRegex().findAll(f.readText())
        for(m in ms){
            val title = m.groups[1]!!.value
            val url = m.groups[2]!!.value
            val ext = url.substringAfterLast('.')
            // 服务器拒绝 aria2c 下载，只能用curl
            println("aria2c -s 2 '$url' -o '$title.$ext'")
        }*/

        // 添加行号
        val f = File("/home/shi/test/voice.txt")
        var i = 1
        f.forEachLine {
            println((i++).toString() + " " + it)
        }
    }

    @Test
    fun testDate(){
        // 日期比较
        /*val a = Date()
        val b = a.add(Calendar.MINUTE, 1)
        val c = a.add(Calendar.DATE, -1)
        println(a > b)
        println(a > c)*/

        // 字符串转date
//        "2016-12-21".toDate().print()
//        "2016-12-21 12:00:06".toDate().print()

        // 获得一日/一周/一月/一年的开始与结束时间
//        val c = GregorianCalendar()
//        c.time.print() // 当前时间
        // 一日的时间
//        c.dayStartTime.print()
//        c.dayEndTime.print()
        // 一周的时间
//        c.weekStartTime.print()
//        c.weekEndTime.print()
//        c.weekStartTime2.print()
//        c.weekEndTime2.print()
        // 一月的时间
//        c.monthStartTime.print()
//        c.monthEndTime.print()
        // 一季度的时间
//        c.quarterStartTime.print()
//        c.quarterEndTime.print()
        // 一年的时间
//        c.yearStartTime.print()
//        c.yearEndTime.print()

        // 跨第2月的月份运算
        // 从01-29到01-31,加一个月后, 都是2017-02-28 00:00:00
        val startTime = "2018-02-01".toDate()
        startTime.print()

        val month = 1
        val cl = GregorianCalendar()
        cl.time = startTime
        cl.add(Calendar.MONTH, month)
        cl.time.print()
        val endTime = cl.timeInMillis / 1000 - 1 // 秒
        Date(endTime * 1000).print()
    }

    @Test
    fun testLog(){
//        testLogger.info("打信息日志")
//        testLogger.debug("打调试日志")
//        testLogger.error("打错误日志")

        // 去掉短信的异常
        val dir = File("/home/shi/test/szdl/logs/cn")
        val reg = "短信发送失败: null\\s\njava\\.lang\\.NullPointerException".toRegex() //
        dir.travel { file ->
            /*val txt = file.readText()
            val m = reg.find(txt)
            println(m?.value)*/
            file.replaceText {
                reg.replace(it, "")
            }
        }
    }

    @Test
        fun testCode(){
        val singleReg = "(?!property\\(\\) )//.*\\n".toRegex() // 单行注释
        val multipleReg = "/\\*.+?\\*/".toRegex(setOf(RegexOption.DOT_MATCHES_ALL)) // 单行注释
        val blank2Reg = "\n\\s*\n\\s*\n".toRegex() // 双空行
        val firstReg = "\\{\\s*\n\\s*\n".toRegex() // {下的第一个空行
       /*
       var content = File("/home/shi/code/java/szpower/szpower2/src/main/kotlin/com/jkmvc/szpower/controller/AlarmController.kt").readText()
//        println(multipleReg.findAll(content).joinToString {
//            it.value
//        })
        content = singleReg.replace(content, "\n")
        content = multipleReg.replace(content, "")
        println(content)
        */

        val dir = File("/home/shi/code/java/szpower/szpower2/src")
        dir.travel { file ->
            if(file.name.endsWith(".kt")){
                file.replaceText {
                    var content = singleReg.replace(it, "\n")
                    content = multipleReg.replace(content, "")
                    content = blank2Reg.replace(content, "\n\n")
                    firstReg.replace(content, "{\n")
                }
            }
        }
    }

    @Test
    fun testResource(){
        val f = File("jkmvc.properties")
        println(f.absolutePath + " : " +  f.exists()) // /home/shi/code/java/jkmvc/jkmvc-common/jkmvc.properties : false
        // 不能识别正则，如 jkmvc.*
        val res = Thread.currentThread().contextClassLoader.getResource("jkmvc.properties")
        println(res)
    }

    @Test
    fun testXml(){
        // https://www.cnblogs.com/longqingyang/p/5577937.html
        // 解析books.xml文件
        // 创建SAXReader的对象reader
        val reader = SAXReader();
        try {
            // 通过reader对象的read方法加载books.xml文件,获取docuemnt对象。
            val document = reader.read(Thread.currentThread().contextClassLoader.getResourceAsStream("books.xml"));
            // 通过document对象获取根节点bookstore
            val bookStore = document.getRootElement();
            // 通过element对象的elementIterator方法获取迭代器
            // 遍历迭代器，获取根节点中的信息（书籍）
            for(ib in bookStore.elementIterator()){
                val book = ib as Element
                System.out.println("=====开始遍历某一本书=====");
                // 获取book的属性名以及 属性值
                for (ia in book.attributes()) {
                    val attr = ia as Attribute
                    println("属性：" + attr.name + "=" + attr.value);
                }
                for(ie in book.elementIterator()){
                    val child = ie as Element
                    println("节点：" + child.name + "=" + child.getStringValue());
                }
                System.out.println("=====结束遍历某一本书=====");
            }
        } catch (e: DocumentException) {
            e.printStackTrace();
        }
    }

    @Test
    fun testYaml(){
        val config = Config.instance("test", "yaml")
        println(config.props)
        println(config.props["age"] is Int)
        // 数组字段的类型是： ArrayList
        val books = config.props["favoriteBooks"]
        println(books)
        println(books is List<*>)
    }

    @Test
    fun testSnowflakeId(){
//        val idWorker = SnowflakeIdWorker(0, 0)
        val idWorker = SnowflakeIdWorker.instance()
        for (i in 0..999) {
            val id = idWorker.nextId()
            println(java.lang.Long.toBinaryString(id))
            println(id)
        }
    }

    @Test
    fun testClass(){
//        println(MyTests::class)
//        println(this.javaClass)
//        println(this.javaClass.kotlin)
//        println(this::class)
       /* println(this::class.simpleName)
        println(this::class.qualifiedName)
        println(this::class.jvmName)
        println(this.javaClass.name)
        println(this.javaClass.canonicalName)
        println(this.javaClass.simpleName)
        println(this.javaClass.typeName)

        println(MyTests::class.qualifiedName)
        println(Int::class.qualifiedName)
        println(String::class.qualifiedName)
        */
        //val m = this.javaClass.getModifiers()
       /* val m = IConfig::class.java.modifiers
        println(Modifier.isAbstract(m))
        println(Modifier.isInterface(m))*/

        /*val clazz = IConfig::class
        println(clazz === IConfig::class) // true
        val java = clazz.java
        println(java === IConfig::class.java) // true
        println(java.isAssignableFrom(java)) // true*/

//        val method = Config::class.java.getMethod("containsKey", String::class.java)
//        println(method)

//        val constructor = Config::class.java.getConstructor(String::class.java, String::class.java)
//        println(constructor)

        //println(Config::class.java.isInterface)
        //println(IValidation::class.java.isInterface)
    }

    @Test
    fun testParamName(){
        /*val method = Config::class.java.findMethod("containsKey", arrayListOf(String::class.java))
        // 获得方法的参数名
        val clazz = method.getDeclaringClass()
        val methodName = method.getName()
        val pool = ClassPool.getDefault()
        pool.insertClassPath(ClassClassPath(clazz))
        val cc = pool.get(clazz.getName())
        val cm = cc.getDeclaredMethod(methodName)
        val methodInfo = cm.getMethodInfo()
        val codeAttribute = methodInfo.getCodeAttribute()
        val paramNames = arrayOfNulls<String>(cm.getParameterTypes().size)
        val pos = if (java.lang.reflect.Modifier.isStatic(cm.getModifiers())) 0 else 1
        for (i in paramNames.indices)
            println(codeAttribute.getAttribute(LocalVariableAttribute.tag).variableName(i + pos))
        */
    }

    @Test
    fun testExtend(){
        val a:A = A()
        a.echo() // A
        val b:B = B()
        b.echo() // B
        val c:A = B()
        c.echo() // A
    }

    @Test
    fun testMethod(){
        for(m in IConfig::class.java.methods)
            println(m.getSignature())
    }

    @Test
    fun testFunc(){
        val f: Lambda = { it:String ->
            it
        } as Lambda
        println(f.javaClass)
        println(f.javaClass.kotlin)
//        println(f is KFunction<*>) // false
//        println(f is KCallable<*>) // false
        println(f is Lambda) // true
        println(f.javaClass.superclass) // class kotlin.jvm.internal.Lambda
        println(f.javaClass.superclass.superclass) // Object
    }

    @Test
    fun testMember(){
        // kotlin.reflect.jvm.internal.KotlinReflectionInternalError: Reflection on built-in Kotlin types is not yet fully supported. No metadata found for public open val length: kotlin.Int defined in kotlin.String
        /*
        for(m in  String::class.declaredFunctions)
            println(m.name)
        */
        for(m in String.javaClass.methods)
            println(m.name)
    }

    @Test
    fun testType(){
        val type = Validation::trim.parameters[0].type
        println(type::class)
        println(type.javaClass)
        println(type.classifier)
    }

    inline fun <reified T> to(value:String): T
    {
        val clazz:KClass<*> = T::class
        return value.to(clazz) as T
    }

    @Test
    fun testTo(){
       /*
       println("123".to(Int::class))
        println("123.45".to(Float::class))
        println("123.4567".to(Double::class))
        println("true".to(Boolean::class))
        */
        val i:Int? = to("1");
        println(i)
        val b:Boolean? = to("true")
        println(b)
        val f:Float? = to("1.23")
        println(f)
    }

    @Test
    fun testPattern(){
        val reg = "^\\d+$".toRegex()
//        println(reg.matches("123"));
//        println(reg.matches("123#"));
        println("hello".endsWith("")); // true
    }

    @Test
    fun testEnum(){
        for (v in NumType.values())
            println("$v => ${v.ordinal}")
    }

    /**
     * 测试操作符
     * 原来像支持如linq之类的dsl，但是在处理 where(a > b) 的时候，你是无法修改操作符 > 的语义，他本来的语义就是2个对象做对比（调用compareTo()），返回boolean，但是我却想让他做字符串拼接，返回string
     * => 语法上就不支持，不可行
     */
    @Test
    fun testOperator(){
        println("a" < "b") // 调用compareTo()
        println("a" in "b") // 调用contains()
    }

    @Test
    fun testFileSizeUnit(){
        println('K'.convertBytes())
        println('M'.convertBytes())
        println('G'.convertBytes())
        println('T'.convertBytes())
    }

    @Test
    fun testDir(){
        val dir = File("upload")
        println(dir.absolutePath)  // /oldhome/shi/code/java/jkmvc/upload

        // 创建不存在的目录
        if(!dir.exists())
            dir.mkdirs();

        val f = File(dir, "test.txt")
        f.writeText("hello")
    }

    @Test
    fun testTravelFile(){

        val dir = File("/home/shi/code/java/szpower/szpower/jkmvc-szpower/src/main/kotlin/com/jkmvc/szpower/controller")
        dir.travel { f ->
            println("处理文件: " + f.name)
            f.forEachLine { l ->
                // println("\"\\w+\\.\\w+\\s=\"".toRegex().matches("\"a.b =\""))
                if("\"\\w+\\.\\w+\\s=\"".toRegex().matches(l))
                    println("\\t" + l)
            }
        }
    }

    @Test
    fun testLine(){
        var count:Int = 0
        val dir = File("/home/shi/code/java/jkmvc/")
        val reg = "^\\s*(//|/\\*|\\*|\\*/).*".toRegex() // 注释
        dir.travel { file ->
            if(file.name.endsWith(".kt")){
                file.forEachLine { line ->
                    if(line.isNotBlank() && !reg.matches(line)){ // 非空 + 非注释
                        count++
                    }else{
                        //println(line)
                    }
                }
            }
        }
        println(count)
    }

    @Test
    fun testGetFileContent(){
        // 扫描出id自增的代码
        /*val dir = File("/home/shi/Downloads/电网项目/source/szdl/0103_Code/NNDYPT/src/main/java")
        dir.travel {
            //println(it)
            it.forEachLine {
                if(it.contains("ID_SEQ")){
                    println(it)
                }
            }
        }*/

        // controller的action方法改名
        // 由actionIndex, 改为indexAction
        val dir = File("/oldhome/shi/code/java/jkmvc/jkmvc-example/src/main/kotlin/com/jkmvc/example")
        dir.travel {
            if(it.name.indexOf("Controller.kt") > 0){
                it.replaceText {
                    "fun\\s+action([\\w\\d]+)".toRegex().replace(it){ result: MatchResult ->
                        "fun " + result.groups[1]!!.value.decapitalize() + "Action"
                    }
                }
                println("处理文件: " + it.name)
            }
        }

        // 扫描出分析模型的字段与注释
        /*val dir = File("/home/shi/下载/电网项目/source/021RecoverPowerFast_AlarmAnalyse/src/com/yingkai/lpam/pojo")
        var i = 0;
        var clazz = ""
        dir.travel {
            //println(it)
            it.forEachLine {
                // 获得类注释
                val matches = "^\\s+\\*\\s+([^@]+)$".toRegex().find(it)
                if(matches != null){
                    i++
                    clazz = matches.groups[1]!!.value
                }else{
                    // 获得表名
                    val matches = "^@Table\\(name=\"(.+)\"\\)$".toRegex().find(it)
                    if(matches != null){
                        println("\n-------------------------------------------------\n")
                        println(i.toString() + matches.groups[1]!!.value + "\t" + clazz)
                        println("-- 字段")
                    }else{
                        // 获得字段
                        val matches = "^\\s+private\\s+(.+)$".toRegex().find(it)
                        if(matches != null){
                            val field = matches.groups[1]!!.value
                            val arr = field.split("\\s+".toRegex())
                            var (type, name) = arr
                            name = name.trim(";")
                            var comment = if(arr.size > 2) arr[2] else ""
                            comment = comment.trim("//")
                            println("$name\t$type\t$comment")
                        }
                    }
                }

            }
        }*/
    }

    @Test
    fun testProp(){
        // 获得不了getter/setter方法
//        println(MyTests::class.getFunction("getId")) // null
//        println(MyTests::class.getFunction("setName")) // null

        // 获得属性
        val p = MyTests::class.getProperty("id")!!
        println(p)
        println(p is KMutableProperty1) // true
        println(p::class) // class kotlin.reflect.jvm.internal.KMutableProperty1Impl

        // 获得参数类型
        println(p.getter.parameters)
        println(p.getter.parameters[0])

        // 不能修改属性的访问权限
//        val prop: KProperty1<B, *> = B::class.getProperty("name") as KProperty1<B, *>
//        println(prop.get(B()))

        // kotlin类
//        val field:Field = B::class.java.getDeclaredField("name").apply { isAccessible = true } // 修改属性的访问权限
//        println(field.get(B()))

        // 试试java原生类
//        for(f in LinkedList::class.java.declaredFields)
//            println(f)

//        val field:Field = LinkedList::class.java.getDeclaredField("size").apply { isAccessible = true } // 修改属性的访问权限
//        val o:LinkedList<String> = LinkedList()
//        println(field.get(o))

        println(System.currentTimeMillis() / 100)

    }

    @Test
    fun testRedis(){
        val jedis = JedisFactory.instance()
        println(jedis.get("name"))
        jedis.set("name", "shi")
        println(jedis.get("name"))
    }

    /*@Test
    fun testCoroutine(){
        val mainThread = Thread.currentThread()
        println("Start")

        // Start a coroutine
        launch(CommonPool) {
            val coroutineThead = Thread.currentThread()
            println("Same Thread: " + (coroutineThead === mainThread))

            delay(1000)
            println("Hello")
        }

        Thread.sleep(2000) // wait for 2 seconds
        println("Stop")
    }

    @Test
    fun testAsync(){
        val deferred = (1..10).map { n ->
            async (CommonPool) {
                n
            }
        }
        println(deferred)

    }

    suspend fun doSomething(): Int {
        return 10;
    }*/
}

class Lambda {

}




