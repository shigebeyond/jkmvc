package com.jkmvc.tests

import com.jkmvc.common.travel
import com.jkmvc.http.ControllerLoader
import org.junit.Test
import java.io.BufferedWriter
import java.io.OutputStreamWriter
import org.apache.velocity.app.Velocity
import java.io.File
import java.util.*


class ControllerTests{

    @Test
    fun testPath(){
        // 1 获取类加载的根路径   /oldhome/shi/code/java/jkmvc/build/classes/test
        println(this.javaClass.getResource("/").path)

        // 2 获取当前类的所在工程路径; 如果不加“/”  获取当前类的加载目录  /oldhome/shi/code/java/jkmvc/build/classes/test/com/jkmvc/tests
        println(this.javaClass.getResource("").path)

        // 3 获取项目路径    /oldhome/shi/code/java/jkmvc
        println(File("").canonicalPath)

        // 4 file:/oldhome/shi/code/java/jkmvc/build/classes/test/
        println(this.javaClass.classLoader.getResource(""))
        // 等同于
        //println(Thread.currentThread().contextClassLoader.getResource(""))

        // null
        // println(Thread.currentThread().contextClassLoader.getResource("/"))


        // 5 获取当前工程路径 /oldhome/shi/code/java/jkmvc
        println(System.getProperty("user.dir"))

        // 6 获取所有的类路径 包括jar包的路径: /home/shi/Applications/idea-IC-171.4249.39/lib/idea_rt.jar:/home/shi/Applications/idea-IC-171.4249.39/plugins/junit/lib/junit-rt.jar:/home/shi/Applications/jdk1.8.0_92/jre/lib/charsets.jar:/home/shi/Applications/jdk1.8.0_92/jre/lib/deploy.jar:/home/shi/Applications/jdk1.8.0_92/jre/lib/ext/cldrdata.jar:/home/shi/Applications/jdk1.8.0_92/jre/lib/ext/dnsns.jar:/home/shi/Applications/jdk1.8.0_92/jre/lib/ext/jaccess.jar:/home/shi/Applications/jdk1.8.0_92/jre/lib/ext/jfxrt.jar:/home/shi/Applications/jdk1.8.0_92/jre/lib/ext/localedata.jar:/home/shi/Applications/jdk1.8.0_92/jre/lib/ext/nashorn.jar:/home/shi/Applications/jdk1.8.0_92/jre/lib/ext/sunec.jar:/home/shi/Applications/jdk1.8.0_92/jre/lib/ext/sunjce_provider.jar:/home/shi/Applications/jdk1.8.0_92/jre/lib/ext/sunpkcs11.jar:/home/shi/Applications/jdk1.8.0_92/jre/lib/ext/zipfs.jar:/home/shi/Applications/jdk1.8.0_92/jre/lib/javaws.jar:/home/shi/Applications/jdk1.8.0_92/jre/lib/jce.jar:/home/shi/Applications/jdk1.8.0_92/jre/lib/jfr.jar:/home/shi/Applications/jdk1.8.0_92/jre/lib/jfxswt.jar:/home/shi/Applications/jdk1.8.0_92/jre/lib/jsse.jar:/home/shi/Applications/jdk1.8.0_92/jre/lib/management-agent.jar:/home/shi/Applications/jdk1.8.0_92/jre/lib/plugin.jar:/home/shi/Applications/jdk1.8.0_92/jre/lib/resources.jar:/home/shi/Applications/jdk1.8.0_92/jre/lib/rt.jar:/oldhome/shi/code/java/jkmvc/build/classes/test:/oldhome/shi/code/java/jkmvc/build/resources/test:/oldhome/shi/code/java/jkmvc/build/classes/main:/oldhome/shi/code/java/jkmvc/build/resources/main:/home/shi/.gradle/caches/modules-2/files-2.1/org.jetbrains.kotlin/kotlin-reflect/1.1.1/e68cf130c2dbdd68a72f4a750cf442dffcd877ce/kotlin-reflect-1.1.1.jar:/home/shi/.gradle/caches/modules-2/files-2.1/org.jetbrains.kotlinx/kotlinx-coroutines-core/0.16/96b56c8a7b6554494d611e2d71efe11d4a2d4608/kotlinx-coroutines-core-0.16.jar:/home/shi/.gradle/caches/modules-2/files-2.1/org.slf4j/slf4j-api/1.7.12/8e20852d05222dc286bf1c71d78d0531e177c317/slf4j-api-1.7.12.jar:/home/shi/.gradle/caches/modules-2/files-2.1/com.alibaba/druid/1.0.25/2057685b8d9bbd75242e920183ff647c68109579/druid-1.0.25.jar:/home/shi/.gradle/caches/modules-2/files-2.1/servlets.com/cos/05Nov2002/ae8a47c62f20d55ab3965ddd50fe0d127291e2b4/cos-05Nov2002.jar:/home/shi/.gradle/caches/modules-2/files-2.1/org.apache.velocity/velocity/1.7/2ceb567b8f3f21118ecdec129fe1271dbc09aa7a/velocity-1.7.jar:/home/shi/.gradle/caches/modules-2/files-2.1/javax.servlet/javax.servlet-api/3.1.0/3cd63d075497751784b2fa84be59432f4905bf7c/javax.servlet-api-3.1.0.jar:/home/shi/.gradle/caches/modules-2/files-2.1/javax.servlet.jsp/jsp-api/2.2.1-b03/c7205b380e9ceb4b96745656755f31f76ae01b74/jsp-api-2.2.1-b03.jar:/home/shi/.gradle/caches/modules-2/files-2.1/javax.servlet.jsp.jstl/javax.servlet.jsp.jstl-api/1.2.1/f072f63ab1689e885ac40c221df3e6bb3e64a84a/javax.servlet.jsp.jstl-api-1.2.1.jar:/home/shi/.gradle/caches/modules-2/files-2.1/org.jetbrains.kotlin/kotlin-test-junit/1.1.1/a1865f59b6f72597452e5bdfefeb14d13bc31c7d/kotlin-test-junit-1.1.1.jar:/home/shi/.gradle/caches/modules-2/files-2.1/org.slf4j/slf4j-log4j12/1.7.12/485f77901840cf4e8bf852f2abb9b723eb8ec29/slf4j-log4j12-1.7.12.jar:/home/shi/.gradle/caches/modules-2/files-2.1/log4j/log4j/1.2.17/5af35056b4d257e4b64b9e8069c0746e8b08629f/log4j-1.2.17.jar:/home/shi/.gradle/caches/modules-2/files-2.1/mysql/mysql-connector-java/5.1.6/380ef5226de2c85ff3b38cbfefeea881c5fce09d/mysql-connector-java-5.1.6.jar:/home/shi/.gradle/caches/modules-2/files-2.1/junit/junit/4.11/4e031bb61df09069aeb2bffb4019e7a5034a4ee0/junit-4.11.jar:/home/shi/.gradle/caches/modules-2/files-2.1/org.eclipse.jetty/jetty-webapp/9.2.22.v20170606/1b512e26860e651567a35abd12cfa3772bc61902/jetty-webapp-9.2.22.v20170606.jar:/home/shi/.gradle/caches/modules-2/files-2.1/org.jetbrains/annotations/13.0/919f0dfe192fb4e063e7dacadee7f8bb9a2672a9/annotations-13.0.jar:/home/shi/.gradle/caches/modules-2/files-2.1/commons-collections/commons-collections/3.2.1/761ea405b9b37ced573d2df0d1e3a4e0f9edc668/commons-collections-3.2.1.jar:/home/shi/.gradle/caches/modules-2/files-2.1/commons-lang/commons-lang/2.4/16313e02a793435009f1e458fa4af5d879f6fb11/commons-lang-2.4.jar:/home/shi/.gradle/caches/modules-2/files-2.1/org.jetbrains.kotlin/kotlin-test/1.1.1/5a852a554eb4f9fb93efdffa352b7983ed595e32/kotlin-test-1.1.1.jar:/home/shi/.gradle/caches/modules-2/files-2.1/org.eclipse.jetty/jetty-jsp/9.2.22.v20170606/a906c7384eeaf0f33bf526a3143bc83c6b15a349/jetty-jsp-9.2.22.v20170606.jar:/home/shi/.gradle/caches/modules-2/files-2.1/org.hamcrest/hamcrest-core/1.3/42a25dc3219429f0e5d060061f71acb49bf010a0/hamcrest-core-1.3.jar:/home/shi/.gradle/caches/modules-2/files-2.1/org.eclipse.jetty/jetty-xml/9.2.22.v20170606/3331ee02dcca4dd2f0a6bd864287b2a886e5e17e/jetty-xml-9.2.22.v20170606.jar:/home/shi/.gradle/caches/modules-2/files-2.1/org.eclipse.jetty/jetty-servlet/9.2.22.v20170606/db0b1b9965a7627e376f17af311cf01c18d20a2f/jetty-servlet-9.2.22.v20170606.jar:/home/shi/.gradle/caches/modules-2/files-2.1/org.eclipse.jetty/jetty-util/9.2.22.v20170606/747d17f6cd662f87d5ab5e08b572a1f1ce85ccb9/jetty-util-9.2.22.v20170606.jar:/home/shi/.gradle/caches/modules-2/files-2.1/org.eclipse.jetty/jetty-security/9.2.22.v20170606/489ec37fcbe2e7ed5d36f010cdc197c42e1181/jetty-security-9.2.22.v20170606.jar:/home/shi/.gradle/caches/modules-2/files-2.1/org.eclipse.jetty/jetty-server/9.2.22.v20170606/f7d36a5ee7e68a7bbd0f404af90b4c1269c65c1/jetty-server-9.2.22.v20170606.jar:/home/shi/.gradle/caches/modules-2/files-2.1/org.eclipse.jetty/jetty-http/9.2.22.v20170606/ba2028c83e4d54a86ee8d765659d56058b205da8/jetty-http-9.2.22.v20170606.jar:/home/shi/.gradle/caches/modules-2/files-2.1/org.eclipse.jetty/jetty-io/9.2.22.v20170606/4995c060104afeab9cedf9e4d0cfb1cacfeece8b/jetty-io-9.2.22.v20170606.jar:/home/shi/.gradle/caches/modules-2/files-2.1/org.eclipse.jetty.toolchain/jetty-schemas/3.1.M0/6179bafb6ed2eb029862356df6713078c7874f85/jetty-schemas-3.1.M0.jar:/home/shi/.gradle/caches/modules-2/files-2.1/org.jetbrains.kotlin/kotlin-stdlib/1.1.2/109c63008b2d569e38696a3178ee2493b0f6c776/kotlin-stdlib-1.1.2.jar:/home/shi/.gradle/caches/modules-2/files-2.1/javax.servlet.jsp/javax.servlet.jsp-api/2.3.1/95c630902565feda8155eb32d46064ef348435fc/javax.servlet.jsp-api-2.3.1.jar:/home/shi/.gradle/caches/modules-2/files-2.1/org.glassfish.web/javax.servlet.jsp/2.3.2/613f624102267b1397e845b3181a72273bd6f399/javax.servlet.jsp-2.3.2.jar:/home/shi/.gradle/caches/modules-2/files-2.1/org.eclipse.jetty.orbit/javax.servlet.jsp.jstl/1.2.0.v201105211821/db594f1c8fc00d536f6d135bd7f8a9a99a6b8eea/javax.servlet.jsp.jstl-1.2.0.v201105211821.jar:/home/shi/.gradle/caches/modules-2/files-2.1/org.glassfish.web/javax.servlet.jsp.jstl/1.2.2/5b2e83ef42b4eef0a7e41d43bb1d4b835f59ac7a/javax.servlet.jsp.jstl-1.2.2.jar:/home/shi/.gradle/caches/modules-2/files-2.1/org.eclipse.jetty.orbit/org.eclipse.jdt.core/3.8.2.v20130121/ebb04771ae21dec8682e4153e97404d9933a9c13/org.eclipse.jdt.core-3.8.2.v20130121.jar:/home/shi/.gradle/caches/modules-2/files-2.1/org.glassfish/javax.el/3.0.1-b08/8fa39d3901fc6ec8c0fff4ad4e48c26c4911c422/javax.el-3.0.1-b08.jar:/home/shi/Applications/idea-IC-171.4249.39/lib/idea_rt.jar
        println(System.getProperty("java.class.path"))
    }

    @Test
    fun testScanFile(){
        val path = "com/jkmvc/db"
//        val path = "org/junit/rules/"
        // 获得类加载器
        val cld = Thread.currentThread().contextClassLoader ?: throw ClassNotFoundException("Can't get class loader.")
        val urls = cld.getResources(path)
        for (url in urls){
            url.travel{ path:String, isDir:Boolean ->
                println(path)
            }
        }
    }

    @Test
    fun testScanControllerClass(){
        ControllerLoader.addPackage("com.jkmvc.example.controller");
        println(ControllerLoader.getControllerClass("user"))
    }
}