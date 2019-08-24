package net.jkcode.jkmvc.tests

import com.alibaba.fastjson.JSON
import com.alibaba.fastjson.JSONObject
import com.alibaba.fastjson.serializer.SerializerFeature
import net.jkcode.jkmvc.bit.SetBitIterator
import net.jkcode.jkmvc.common.*
import net.jkcode.jkmvc.elements.ElementCollection
import net.jkcode.jkmvc.idworker.SnowflakeId
import net.jkcode.jkmvc.idworker.SnowflakeIdWorker
import net.jkcode.jkmvc.iterator.ArrayFilteredIterator
import net.jkcode.jkmvc.redis.JedisFactory
import net.jkcode.jkmvc.serialize.ISerializer
import net.jkcode.jkmvc.validator.ValidateFuncDefinition
import org.dom4j.Attribute
import org.dom4j.DocumentException
import org.dom4j.Element
import org.dom4j.io.SAXReader
import org.junit.Test
import java.io.BufferedReader
import java.io.File
import java.io.IOException
import java.io.InputStreamReader
import java.lang.reflect.ParameterizedType
import java.net.HttpURLConnection
import java.net.URL
import java.util.*
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.ConcurrentLinkedQueue
import kotlin.collections.ArrayList
import kotlin.reflect.KClass
import kotlin.reflect.KMutableProperty1
import kotlin.reflect.full.memberFunctions
import kotlin.reflect.jvm.javaMethod
import kotlin.reflect.jvm.reflect

open class A() {
    open fun echo(){}
}
class B():A() {
    /*override fun echo(){
        println("Ah")
    }*/
}

fun A.sayHi(){
    println("hi, I'm A")
}

fun B.sayHi(){
    println("hi, I'm B")
}

enum class NumType {
    Byte,
    Short,
    INT,
    LONG
}

class Lambda {
}

data class Man(val name: String, val age: Int)

/**
 * 基本测试
 * @Description:
 * @author shijianhang<772910474@qq.com>
 * @date 2017-12-14 3:11 PM
 */
class MyTests{

    @Test
    fun testSys(){
        // val prop = System.getenv()
        val prop = System.getProperties()
        printProps(prop)
    }

    fun printProps(props: Map<*, *>) {
        println(props.entries.joinToString("\n\n") {
            "${it.key}\n\t${it.value}"
        })
    }

    @Test
    fun testNullSafe() {
        val field = String::class.java.getReadableField("test")
        val b = field?.type == String::class.java
        println(b) // false
    }

    @Test
    fun testExpr() {
        println(1.1.toExpr())
        println(1.1F.toExpr())
        println(1.toExpr())
        println(1L.toExpr())
        println(1.1.toExpr().exprTo(Double::class))
        println(1.1F.toExpr().exprTo(Float::class))
        println(1.toExpr().exprTo(Int::class))
        //println("1L".toLong()) // java.lang.NumberFormatException: For input string: "1L"
        println(1L.toExpr().exprTo(Long::class))
    }

    @Test
    fun testTime(){
        for(i in 0..10000){
            println(currMillis())
            Thread.sleep(1)
        }
    }

    @Test
    fun testConcurrentHashMap(){
        val map = ConcurrentHashMap<String, Int>()
        map["a"] = 1
        map["b"] = 2
        println(map)
        println(map.keys) // keySet()
        println(map.values) // values()
    }

    @Test
    fun testSerialize(){
        //val obj = "hello world"
        //val obj = LongArray(3)
        //val obj = BitSet.valueOf(words)
        val obj = BitSet()
        obj.set(100)
        println(obj)
        val instance = ISerializer.instance("fst")
        val bs = instance.serialize(obj)
        if(bs != null) {
            val obj2 = instance.unserialize(bs!!)
            println(obj2)
        }
    }

    @Test
    fun testMath(){
        println(Math.pow(3.0, 2.0)) // 9
        println(Math.pow(3.0, 3.0)) // 27
        println(Math.sqrt(4.0)) // 2
        println(Math.sqrt(9.0)) // 3
    }

    @Test
    fun testElements(){
        //val eles = ElementArray(arrayOf(1, 2, 3))
        val eles = ElementCollection(listOf(1, 2, 3))
        for(i in 0 until eles.size())
            println(eles.getElement(i))
    }

    @Test
    fun testPool(){
        val pool = SimpleObjectPool(){
            ArrayList<Int>()
        }
        pool.printIdleObjects()
        makeThreads(100){
            var list: ArrayList<Int>? = null
            try{
                list = pool.borrowObject()
                //pool.printIdleObjects()
                list.add(it)
                //println("list: " + System.identityHashCode(list)+ " - " + list)
            }finally {
                if(list != null) {
                    //list.clear()
                    pool.returnObject(list)
                    pool.printIdleObjects()
                }
            }
        }
        Thread.sleep(10000)
        println("-----------")
        pool.printIdleObjects()

    }

    @Test
    fun testIterator(){
        val itr = object: ArrayFilteredIterator<Int>(arrayOf(1, 2, 3)){
        //val itr = object: CollectionFilteredIterator<Int>(listOf(1, 2, 3)){
            override fun filter(ele: Int): Boolean {
                return ele > 1
            }
        }
        println(itr.toDesc())
    }

    @Test
    fun testBit(){
        val bs = BitSet(10)
        bs.set(0)
        bs.set(5)
        println(bs.size()) // 此 BitSet 表示位值时实际使用空间的位数
        println(bs.length()) // 此 BitSet 的“逻辑大小”：BitSet 中最高设置位的索引加 1 -- 6
        println(bs.cardinality()) // 此 BitSet 中设置为 true 的位数 -- 2

        println("迭代1: ")
        var i = bs.nextSetBit(0)
        while (i >= 0) {
            println(i)
            i = bs.nextSetBit(i + 1)
        }

        println("迭代2: ")
        for(j in SetBitIterator(bs)){
            println(j)
        }
    }

    @Test
    fun testDoneFlagList(){
        val list = DoneFlagList<Int>()
        list.add(1)
        list.add(2)
        list.add(3)

        list.setDone(0, true)
        list.setDone(2, true)

        // 迭代已完成
        println("迭代已完成")
        for(done in list.doneIterator(true))
            println(done)

        // 迭代未完成
        println("迭代未完成")
        for(undone in list.doneIterator(false))
            println(undone)
    }

    @Test
    fun testNumber(){
        //val io:Integer = 1
        //val i:Int = io
//        val map = mapOf("a" to 111)
//        val i:Int = map["a"]!!
//        val io:Integer = map["a"]!! as Integer

        // 外部的i vs 内部的i, 虽同名, 但不是同一个变量
        var i = 0
        for(i in 0 until 10){
            if(i == 5) {
                println(i) // 5
                break
            }
        }
        println(i) // 0
    }

    @Test
    fun testBig(){
        println(-1%10)
        // var a= BigDecimal("100")
        // var b=BigDecimal("3")
        // var c=a/b;
        // println(c)
        // var d=a.divide(b);
        // println(d)
    }

    @Test
    fun testArray(){
        val s = arrayOf<String>("test")
        val a: Array<Any> = s as Array<Any>
        val b: Array<String> = a as Array<String>
        println(b)
    }

    @Test
    fun testPair(){
        println(Pair("a", "b"))
        println(Pair("a", "b") == Pair("a", "b")) // true
        println(Pair(1, 2) == Pair(1, 2)) // true
        println(Pair(Integer(1), String(charArrayOf('a'))) == Pair(Integer(1), String(charArrayOf('a')))) // true

        val map = HashMap<Pair<Integer, String>, String>()
        var k = Pair(Integer(1), String(charArrayOf('a')))
        map[k] = "test"
        k = Pair(Integer(1), String(charArrayOf('a')))
        println(map[k]) // test
    }

    @Test
    fun testMap(){
        val map = HashMap<Int, String>() // {1=a, 2=b}
        val tableField = HashMap::class.java.getReadableField("table")!!
        println(tableField.get(map))

        //val map = TreeMap<Int, String>() // {1=a, 2=b}
        map[2] = "a"
        println(tableField.get(map))
        map[1] = "b"
        for((k, v) in map){
            println("$k = $v")
        }

        // 边遍历, 边删除:
        // 1 HashMap 抛异常 ConcurrentModificationException
        // 2 ConcurrentHashMap 成功
        for(k in map.keys) {
            val v = map.remove(k)
            println("$k = $v")
        }

        println(map)
        //println( emptyMap<String, Any?>() as MutableMap<String, Any?>)
    }

    @Test
    fun testFixedKeyMap(){
        val mf = FixedKeyMapFactory("name", "id", "age", "sex")
        val map = mf.createMap()
        map["name"] = "shi"
        map["id"] = 1
        map["age"] = 13
        map["sex"] = 1
        println(map)

        println("\nremove: ")
        println(map.remove("age"))
        println(map)

        println("\nkeys: ")
        val keys = map.keys
        println(keys)

        println("\nremovekey: ")
        println(keys.remove("sex"))
        println(keys)

        println("\ncontainskey: ")
        println(map.containsKey("id")) // true
        println(map.containsKey("sex")) // false

        println("\ncontainsvalue: ")
        println(map.containsValue("shi")) // true
        println(map.containsValue(1)) // true
        println(map.containsValue(2)) // false
        println(map.containsValue(null)) // false

        println("\ntravel: ")
        for((k, v) in map){
            println("$k = $v")
        }

    }

    @Test
    fun testList(){
        val list = listOf<Int>(1, 2, 3)
        println(list.javaClass) // ArrayList

        /*val list:MutableList<String> = LinkedList()
        list.add("a")
        list += "a"
        list.add("b")
        list.add("c")
        println(list)*/
        /*list.remove("c")
        println(list)
        list.add(0, "d")
        println(list)*/

        /*var n = list.size
        val it = list.iterator()
        while (--n > 0){
            it.next()
        }
        println(it.next())*/
    }

    @Test
    fun testFinally(){
        fun getMsg(): String {
            try{
                throw IllegalArgumentException("fuck")
            }catch (e: Exception){
                e.printStackTrace()
                return "fuck"
            }finally {
                // finally 代码块中的 return 会直接替换 try / catch 代码块中的 return
                return "hello"
            }
        }

        println(getMsg())
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
    fun testObject(){
        val clazz = Application::class.java.name
        println(clazz)
        val exist = try{
                        Class.forName(clazz) != null
                    }catch (e: Exception){
                        false
                    }
        println("exist= $exist")
    }

    @Test
    fun testPerform(){
        val start = currMillis()
        val n = 1000000
        val map = HashMap<String, Long>(n, 0.75f)
        val sb = StringBuilder(100)


        for (i in 0..n - 1) {
            val time = currMillis()
            map.put(sb.append(i).append("_").append(time).toString(), time)
            sb.delete(0, sb.length)
        }
        println((currMillis() - start) / 1000.0)
        Thread.sleep(150000)
    }

    @Test
    fun testString(){
        // val m = "jdbc:mysql://[^/]+/([^\\?]+)".toRegex().find("jdbc:mysql://127.0.0.1/test?useUnicode=true&characterEncoding=utf-8")
        // if(m != null)
        //     println(m.groups[1]!!.value)

        /*val funname = "indexAction"
        val name = funname.substring(0, funname.length - 6) // 去掉Action结尾
        println(name)*/

        // println("my_favorite_food".underline2Camel())
        // println("myFavoriteFood".camel2Underline())
        // println("2017-09-27 08:47:04".to(Date::class))

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
        // println("hello world".trim())
        // println("hello world".replace(" ", ""))

        println("a/b/c".substringBeforeLast('/'))
    }

    @Test
    fun testHashCode(){
//        var i: Any? = null
//        println(i.hashCode())
//        println("server-Jt0H".hashCode() and Integer.MAX_VALUE)

//        val strs = arrayOf("abcdefg", "hijkmn")
//        println(joinHashCode(*strs))
//        println(strs.joinToString("").hashCode())

        val str1 = "ABCDEa123abc"
        val str2 = "ABCDFB123abc"
        println(str1.hashCode());  // 165374702
        println(str1.longHashCode()) // 1707477902391208686
        println(str1.longHashCode().toInt()) // 165374702

        println(str2.hashCode()); //  165374702
        println(str2.longHashCode()) // 1707477902391208686
        println(str2.longHashCode().toInt())
    }

    @Test
    fun testFile(){
        /*println(File.separator) // "/"
        println(File.separatorChar) // "/"
        println(File.pathSeparator) // ":"
        println(File.pathSeparatorChar) // ":"
        */

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
        f.forEachLine { line ->
            if(needLineNo(line))
                println((i++).toString() + ". " + line)
            else
                println(line)
        }
    }

    fun needLineNo(line: String): Boolean {
        // 空行
        if(line.isBlank())
            return false

        // 标题
        if(line.startsWith("#"))
            return false

        // 已有行号
        if("^\\d+".toRegex().matches(line))
            return false

        return true
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
        // "2016-12-21".toDate().print()
        // "2016-12-21 12:00:06".toDate().print()

        // 获得一日/一周/一月/一年的开始与结束时间
        // val c = GregorianCalendar()
        // c.time.print() // 当前时间

        // 一日的时间
        // c.dayStartTime.print()
        // c.dayEndTime.print()

        // 一周的时间
        // c.weekStartTime.print()
        // c.weekEndTime.print()
        // c.weekStartTime2.print()
        // c.weekEndTime2.print()

        // 一月的时间
        // c.monthStartTime.print()
        // c.monthEndTime.print()

        // 一季度的时间
        // c.quarterStartTime.print()
        // c.quarterEndTime.print()

        // 一年的时间
        // c.yearStartTime.print()
        // c.yearEndTime.print()

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
        // testLogger.info("打信息日志")
        // testLogger.debug("打调试日志")
        // testLogger.error("打错误日志")

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
    fun testDomainCheck() {
        val dir = File("/home/shi/code/php/sk")
        val domainReg = "http://([\\w\\d-_\\.]+)\\.(sk(\\d)?|shikee)\\.com".toRegex() //
        val subDomains = HashSet<String>()
        dir.travel { file ->
            if (!file.name.endsWith(".php"))
                return@travel

            // 配置文件处理
            if(file.name == "shikee.php")
                return@travel

            // 收集域名
            val txt = file.readText()
            val matches = domainReg.findAll(txt)
            for(m in matches){
                subDomains.add(m.groupValues.get(1)) // 子域名
            }
        }

        val f = File(dir, "common/config/sk0.com/shikee.php")
        val txt = f.readText()
        val configReg = "domain_([\\w\\d-_]+)".toRegex()
        val matches = configReg.findAll(txt)
        val configDomains = HashSet<String>()
        for(m in matches){
            configDomains.add(m.groupValues.get(1))
        }
        println("****用到子域名****")
        println(subDomains.joinToString("\n"))
        println("****配置子域名****")
        println(configDomains.joinToString("\n"))
        println("****没有配置的子域名****")
        subDomains.removeAll(configDomains)
        println(subDomains.joinToString("\n"))

    }

    @Test
    fun testDomainReplace(){
        val dir = File("/home/shi/code/php/sk")
        val domainPattern = "http://([\\w\\d-_\\.]+)\\.(sk(\\d)?|shikee)\\.com/?" // 1 子域名
        val domainReg = domainPattern.toRegex() // 1 子域名
        val assignReg = "((=|=>|\\?)\\s*)('|\")$domainPattern".toRegex()  // 1 赋值号+空格 2 赋值号 3 引号 4 子域名
        dir.travel { file ->
            if(!file.name.endsWith(".php"))
                return@travel

            // 配置文件处理
            if(file.name == "shikee.php")
                return@travel

            // view文件处理
            if(file.absolutePath.contains("/views/")) {
                /*file.replaceText { txt ->
                    domainReg.replace(txt) { result: MatchResult ->
                        val subDomain = result.groupValues.get(1)
                        "<?= config_item('domain_$subDomain') ?>"
                    };
                }*/

                return@travel
            }

            // 业务文件处理
            file.replaceText { txt ->
                assignReg.replace(txt) { result: MatchResult ->
                    val assign = result.groupValues.get(1) // 1 赋值号
                    val quote = result.groupValues.get(3) // 2 引号
                    val subDomain = result.groupValues.get(4) // 3 子域名
                    "${assign}config_item('domain_$subDomain').$quote"
                };
            }
        }
        println("over")
    }

    @Test
    fun testCode(){
        val singleReg = "(?!property\\(\\) )//.*\\n".toRegex() // 单行注释
        val multipleReg = "/\\*.+?\\*/".toRegex(setOf(RegexOption.DOT_MATCHES_ALL)) // 单行注释
        val blank2Reg = "\n\\s*\n\\s*\n".toRegex() // 双空行
        val firstReg = "\\{\\s*\n\\s*\n".toRegex() // {下的第一个空行
       /*
       var content = File("/home/shi/code/java/szpower/szpower2/src/main/kotlin/com/jkmvc/szpower/controller/AlarmController.kt").readText()
        // println(multipleReg.findAll(content).joinToString {
        //     it.value
        // })
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
    fun testJson(){
        val o = HashMap<String, Any?>()
        o["notify_url"] = "http://baidu.com" // 原始值
        var json = JSON.toJSONString(o)
        println(json) // 输出 {"notify_url":"http://baidu.com"}

        json = JSON.toJSONString(o, SerializerFeature.WriteSlashAsSpecial)
        println(json) // 输出: {"notify_url":"http:\/\/baidu.com"}

        val o2 = JSONObject.parse(json) as JSONObject
        println(o2["notify_url"])

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
            // 遍历迭代器，获取根节点中的信息(书籍)
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
    fun testConfig(){
        //val config = Config.instance("man", "yaml")
        val config = Config.instance("man", "json")
        println(config.props)
        println(config.props["age"] is Int)
        // 数组字段的类型是： ArrayList
        val books = config.props["favoriteBooks"]
        println(books)
        println(books is List<*>)
    }

    @Test
    fun testSnowflakeId(){
        // val idWorker = SnowflakeIdWorker(0, 0)
        val idWorker = SnowflakeIdWorker()
        for (i in 0..40) {
            val id = idWorker.nextId()
            //println(java.lang.Long.toBinaryString(id))
            println(id)
        }

        //println(generateId("test"))
    }

    @Test
    fun testSnowflakeIdParse(){
        val id = SnowflakeId(0, currMillis(), 1, 2)
        println("id: $id")
        //val l = id.toLong()
        for(i in 0..10) {
            val l = generateId("test")
            println("long: $l")
            val id2 = SnowflakeId.fromLong(l)
            println("id2: $id2")
        }
    }

    @Test
    fun testClass(){
        // println(MyTests::class)
        // println(this.javaClass)
        // println(this.javaClass.kotlin)
        // println(this::class)

       println(this::class.simpleName) // MyTests
        println(this::class.qualifiedName) // net.jkcode.jkmvc.tests.MyTests
        //println(this::class.jvmName)

        println(this.javaClass.name) // net.jkcode.jkmvc.tests.MyTests
        println(this.javaClass.canonicalName) // net.jkcode.jkmvc.tests.MyTests
        println(this.javaClass.simpleName) // MyTests
        println(this.javaClass.typeName) // net.jkcode.jkmvc.tests.MyTests

        println(MyTests::class.qualifiedName)
        println(Int::class.qualifiedName)
        println(String::class.qualifiedName)

        //val m = this.javaClass.getModifiers()
       /* val m = IConfig::class.java.modifiers
        println(Modifier.isAbstract(m))
        println(Modifier.isInterface(m))*/

        /*val clazz = IConfig::class
        println(clazz === IConfig::class) // true
        val java = clazz.java
        println(java === IConfig::class.java) // true
        println(java.isAssignableFrom(java)) // true*/

        // val method = Config::class.java.getMethod("containsKey", String::class.java)
        // println(method)

        // val constructor = Config::class.java.getConstructorOrNull(String::class.java, String::class.java)
        // println(constructor)

        //println(Config::class.java.isInterface)
        //println(IValidation::class.java.isInterface)
    }

    @Test
    fun testField(){
        val map = HashMap<String, String>()
        map["a"] = "b"
        // 获得字段
        val f = map.javaClass.getReadableField("table")!! // transient Node<K,V>[] table
        println(f)
        println(f.declaringClass) // HashMap
        println(f.name) // table
        println(f.modifiers) // transient
        println(f.type) // HashMap$Node
        println(f.declaredAnnotations)

        // 获得字段值
        println(f.get(map))
    }

    @Test
    fun testParamName(){
        val method = Config::class.java.getMethod("containsKey", String::class.java)
        // 获得方法的参数名
        val clazz = method.getDeclaringClass()
        val methodName = method.getName()
        for(p in method.parameters)
            println(p.name)

        /*
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
    fun testSubClass(){
        // 测试方法从属于子类还是父类
        //val funs = B::class.declaredFunctions // 当前类实现的方法
        val funs = B::class.memberFunctions // 当前类实现的方法
        //val funs = B::class.functions // 当前类实现的方法
        funs.forEach {
            println(it.javaMethod!!.declaringClass)
        }
    }

    @Test
    fun testExtend(){
        val a:A = A()
        a.sayHi() // A
        val b:B = B()
        b.sayHi() // B
        val c:A = B()
        c.sayHi() // A
    }

    @Test
    fun testMethod(){
        for(m in IConfig::class.java.methods)
            println(m.getSignature())
    }

    @Test
    fun testFunc(){
        /*val f: Lambda = { it:String ->
            it
        } as Lambda
        println(f.javaClass)
        println(f.javaClass.kotlin)
        println(f::class)
        // println(f is KFunction<*>) // false
        // println(f is KCallable<*>) // false
        println(f is Lambda) // true
        println(f.javaClass.superclass) // class kotlin.jvm.internal.Lambda
        println(f.javaClass.superclass.superclass) // Object
        */
        val s = mutableListOf<Int>(1, 2)
        // 检查无返回值的函数调用的结果
        val f = s::clear
        val r = f.invoke()
        println(r)
    }

    @Test
    fun testCloneThreadLocalMap(){
        // ThreadLocalMap的类, 是ThreadLocal的内部私有类
        // 编码时使用内部类 "ThreadLocal.ThreadLocalMap", 但是Java在编译代码时为了区分内部类, 会将内部类名改为 "ThreadLocal$ThreadLocalMap"
        val mapClazz = Class.forName("java.lang.ThreadLocal\$ThreadLocalMap")
        println(mapClazz)
        val mapConstructor = mapClazz.getDeclaredConstructor(mapClazz)
        println(mapConstructor)
        mapConstructor.isAccessible = true
        println(mapConstructor)

        // 获得当前线程拥有的ThreadLocalMap实例
        val threadLocalProp = Thread::class.getProperty("threadLocals") as KMutableProperty1<Thread, Any?>
        val value = threadLocalProp.get(Thread.currentThread())

        // 克隆实例
        //val o = value.forceClone() // wrong: ThreadLocalMap没有实现Cloneable接口
        val o = mapConstructor.newInstance(value) // wrong: ThreadLocalMap(ThreadLocalMap parentMap) 构造函数只能用于对 InheritableThreadLocal 中的ThreadLocalMap进行复制
    }

    @Test
    fun testFuncReflect(){
        val f = { it:String ->
            it
        }
        println(f)
        val r = f.reflect()!!.returnType
        println(r) // kotlin.String
        println(r.classifier as KClass<*>) // class kotlin.String
        println((r.classifier as KClass<*>).java) // class java.lang.String
        // println(r.javaClass) // class kotlin.reflect.jvm.internal.KTypeImpl
        // println(r.javaType) // 报错: kotlin.reflect.jvm.internal.KotlinReflectionInternalError: Introspecting local functions, lambdas, anonymous functions and local variables is not yet fully supported in Kotlin reflection
    }

    @Test
    fun testMember(){
        // 报错: kotlin.reflect.jvm.internal.KotlinReflectionInternalError: Reflection on built-in Kotlin types is not yet fully supported. No metadata found for public open val length: kotlin.Int defined in kotlin.String
        /*
        for(m in  String::class.declaredFunctions)
            println(m.name)
        */
        for(m in String.javaClass.methods)
            println(m.name)
    }

    @Test
    fun testType(){
        val type = ValidateFuncDefinition::trim.parameters[0].type
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
    fun testLambda(){
        val l = { it: String ->
            println(it)
        }
        println(l::class)
        println(l.javaClass)
    }

    @Test
    fun testDataClass(){
        var m = Man("shi", 1)
        var i = 0
        println("${++i}: m=$m")
        m = m.copy()
        println("${++i}: m=$m")
        m = m.copy(age = 12)
        println("${++i}: m=$m")
        m = m.copy(name = "li")
        println("${++i}: m=$m")
    }

    @Test
    fun testPattern(){
        /*
        val reg = "^\\d+$".toRegex()
        println(reg.matches("123"));
        println(reg.matches("123#"));
        println("hello".endsWith("")); // true
        */

        /*
        // https://www.cnblogs.com/dplearning/p/5897316.html
        // 前瞻： exp1(?=exp2)    查找exp2前面的exp1
        // 后顾:  (?<=exp2)exp1   查找exp2后面的exp1
        // 负前瞻:  exp1(?!exp2)   查找后面不是exp2的exp1
        // 负后顾:  (?<!exp2)exp1   查找前面不是exp2的exp1
        val str = "abcacad"
        val reg = "a(?=b)".toRegex() // 只匹配后面接有b的a
        println(str.replace(reg, "*") )// *bcacad

        val reg2 = "(?<=c)a".toRegex() // 只匹配前面面接有c的a
        println(str.replace(reg2, "*") )// abc*c*d
        */

        val argExpr = "(\"第0个分片的参数\"),(\"第1个分片的参数\"),(\"第2个分片的参数\")"
        //val argses = argExpr.split("),(")
        val reg = "(?<=\\)),(?=\\()".toRegex()
        println(argExpr.replace(reg, "--------") )//
        val argses = argExpr.split(reg)
        for (args in argses){
            println(args)
        }
    }

    @Test
    fun testEnum(){
        val t = NumType.valueOf("Byte")
        println(t)
        println(t is Enum<*>)
        for (v in NumType.values())
            println("$v => ${v.ordinal}")
    }

    /**
     * 测试操作符
     * 原来像支持如linq之类的dsl，但是在处理 where(a > b) 的时候，你是无法修改操作符 > 的语义，他本来的语义就是2个对象做对比(调用compareTo())，返回boolean，但是我却想让他做字符串拼接，返回string
     * => 语法上就不支持，不可行
     */
    @Test
    fun testOperator(){
        println("a" < "b") // 调用compareTo()
        println("a" in "b") // 调用contains()
    }

    @Test
    fun testFileSiz(){
        println(fileSize2Bytes("1K"))
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

        val dir = File("/home/shi/test")
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
        // println(MyTests::class.getFunction("getId")) // null
        // println(MyTests::class.getFunction("setName")) // null

        // 获得属性
        val p = MyTests::class.getProperty("id")!!
        println(p)
        println(p is KMutableProperty1) // true
        println(p::class) // class kotlin.reflect.jvm.internal.KMutableProperty1Impl

        // 获得参数类型
        println(p.getter.parameters)
        println(p.getter.parameters[0])

        // 不能修改属性的访问权限
        // val prop: KProperty1<B, *> = B::class.getProperty("name") as KProperty1<B, *>
        // println(prop.get(B()))

        // kotlin类
        // val field:Field = B::class.java.getDeclaredField("name").apply { isAccessible = true } // 修改属性的访问权限
        // println(field.get(B()))

        // 试试java原生类
        // for(f in LinkedList::class.java.declaredFields)
        //     println(f)

        // val field:Field = LinkedList::class.java.getDeclaredField("size").apply { isAccessible = true } // 修改属性的访问权限
        // val o:LinkedList<String> = LinkedList()
        // println(field.get(o))

        println(currMillis() / 100)

    }

    @Test
    fun testRedis(){
        val jedis = JedisFactory.instance()
        println(jedis.get("name"))
        jedis.set("name", "shi")
        println(jedis.get("name"))
    }

    @Test
    fun testGenericSuperclass(){
        val obj = ArrayList<String>()
        val clazz = obj.javaClass

//        println(clazz.getSuperClassGenricType())

        //getSuperclass()获得该类的父类
        println(clazz.getSuperclass())
        //getGenericSuperclass()获得带有泛型的父类
        //Type是 Java 编程语言中所有类型的公共高级接口。它们包括原始类型、参数化类型、数组类型、类型变量和基本类型。
        val type = clazz.getGenericSuperclass()
        System.out.println(type)
        //ParameterizedType参数化类型，即泛型
        val p = type as ParameterizedType
        //getActualTypeArguments获取参数化类型的数组，泛型可能有多个
        val c = p.actualTypeArguments[0]
        println(c)
    }

    @Test
    fun testConcurrentLinkedQueue(){
        val q = ConcurrentLinkedQueue<Int>()
        for(i in 0..1000){
            q.add(i)
        }
        println("全部: " + q)

        makeThreads(3){
            while(q.isNotEmpty()){
                val list = ArrayList<Int>()
                q.drainTo(list, 10)
                println("出队:"  + list)
            }
        }

        println("全部: " + q)
    }

    @Test
    fun testRandom() {
        val start = "curl 'http://www.shibiantian.com/coupon/exchange_coupon_code' -H 'Cookie: Hm_lvt_792d6c5d77fe8d4bc5dccb134f9ab44f=1561691832; shibiantian_uid=466; ci_session=a%3A12%3A%7Bs%3A10%3A%22session_id%22%3Bs%3A32%3A%222df26e42a079a4848292d8e4ac9d3712%22%3Bs%3A10%3A%22ip_address%22%3Bs%3A13%3A%22192.168.10.35%22%3Bs%3A10%3A%22user_agent%22%3Bs%3A104%3A%22Mozilla%2F5.0+%28X11%3B+Linux+x86_64%29+AppleWebKit%2F537.36+%28KHTML%2C+like+Gecko%29+Chrome%2F70.0.3538.77+Safari%2F537.36%22%3Bs%3A13%3A%22last_activity%22%3Bi%3A1561697041%3Bs%3A9%3A%22user_data%22%3Bs%3A0%3A%22%22%3Bs%3A3%3A%22uid%22%3Bs%3A3%3A%22466%22%3Bs%3A8%3A%22nickname%22%3Bs%3A11%3A%22shigebeyond%22%3Bs%3A5%3A%22phone%22%3Bs%3A11%3A%2213471156021%22%3Bs%3A10%3A%22hide_phone%22%3Bs%3A11%3A%22134%2A%2A%2A%2A6021%22%3Bs%3A6%3A%22uc_uid%22%3Bi%3A2091194700%3Bs%3A8%3A%22cart_num%22%3Bi%3A0%3Bs%3A11%3A%22message_num%22%3Bi%3A19%3B%7D27b6cdbd2071537fa4fb04ab0d69cbfc4fb2098e; Hm_lpvt_792d6c5d77fe8d4bc5dccb134f9ab44f=1561697309' -H 'Origin: http://www.shibiantian.com' -H 'Accept-Encoding: gzip, deflate' -H 'Accept-Language: zh-CN,zh;q=0.9' -H 'User-Agent: Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/70.0.3538.77 Safari/537.36' -H 'Content-Type: application/x-www-form-urlencoded; charset=UTF-8' -H 'Accept: application/json, text/javascript, */*; q=0.01' -H 'Referer: http://www.shibiantian.com/coupon/user_coupon' -H 'X-Requested-With: XMLHttpRequest' -H 'Connection: keep-alive' --data 'cuopon_code=YHQ"
        val end = "' --compressed"
        val sb = StringBuffer()
        for (i in 0..1000)
            sb.appendln(start + randomNumberString(18) + end);
        File("/home/shi/test/shibiantian.txt").writeText(sb.toString())
    }

    @Test
    fun testConsistentHash(){
        val servers = (0..10).map {
            "server-${randomString(4)}"
        }
        val ch = ConsistentHash(2, 100, servers)
        ch.add("server4")

        ch.dumpVirtualNodes()

        println("虚拟节点个数: " + ch.size)
        (0..9).forEach {
            val key = randomString(5)
            println("key[$key]哈希[${key.hashCode()}]命中节点: " + ch.get(key))
        }
    }

    @Test
    fun testBioHttp(){
        val url = URL("https://www.zhonghuasuan.com/")
        val document = StringBuffer()
        try {
            val urlCon = url.openConnection() as HttpURLConnection
            val reader = BufferedReader(InputStreamReader(urlCon.getInputStream(), "UTF-8"))
            do{
                val line = reader.readLine()
                if(line == null)
                    break
                document.append(line)
            }while(true)
            reader.close()
            println(document)
        } catch (e: IOException) {
            e.printStackTrace();
        }

    }

}

