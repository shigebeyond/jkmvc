# 开始

## 1 创建工程

### 1.1 创建工程目录

```
mkdir myproj
cd myproj
```

### 1.2 新建 buid.gradle

buid.gradle 的内容如下

```
//gradle脚本自身需要使用的资源
buildscript {
    // 仓库
    repositories {
        jcenter()
        mavenCentral()
        maven {
            url 'http://maven.aliyun.com/nexus/content/repositories/snapshots'
        }
        maven {
            url "http://dl.bintray.com/kotlin/kotlin"
        }
        maven {
            url 'http://oss.jfrog.org/artifactory/oss-snapshot-local'
        }
    }

    // 依赖
    dependencies {
        classpath "org.jetbrains.kotlin:kotlin-gradle-plugin:$kotlin_version"
        classpath "org.jetbrains.kotlin:kotlin-allopen:$kotlin_version"

        classpath "org.jetbrains.dokka:dokka-gradle-plugin:${dokka_version}"

        classpath "org.akhikhl.gretty:gretty:${gretty_version}"
    }
}

// 工程标识
group 'net.jkcode.jkmvc'
version '1.0-SNAPSHOT'
name 'myproj'

// gradle插件
apply plugin: 'java'
apply plugin: 'kotlin'
apply plugin: 'maven'
apply plugin: 'org.akhikhl.gretty'

// 工程需要使用的资源
// 仓库
repositories {
    mavenCentral()
    maven {
        url 'http://maven.aliyun.com/nexus/content/repositories/snapshots'
    }
    maven {
        url "http://dl.bintray.com/kotlin/kotlin"
    }
}

// 依赖
dependencies {
	compile "net.jkcode.jkmvc:jkmvc-http:1.0-SNAPSHOT"
}

// 源码目录
sourceSets {
    main {
        java {
            srcDirs = ['src/main/java', 'src/main/kotlin']
        }
        resources {
            srcDirs = ['src/main/resources']
        }
    }
    test {
        java {
            srcDirs = ['src/test/java', 'src/test/kotlin']
        }
        resources {
            srcDirs = ['src/test/resources']
        }
    }
}

// 启动jetty
gretty{
    inplaceMode "hard" // 资源目录 src/main/webapp
    httpPort 8080
    contextPath project.name
    scanInterval 1 // jetty 热部署，当值为0时，不扫描新class，不热部署
}
```

### 1.3 导入idea

选择菜单: File -> New -> Project From Existing Sources...

选择我们在上一步创建build.gradle

### 1.4 修改web.xml

vim src/main/webapp/WEB-INF/web.xml

添加以下内容

```
<filter>
    <filter-name>jkmvc</filter-name>
    <filter-class>net.jkcode.jkmvc.http.JkFilter</filter-class>
</filter>

<filter-mapping>
    <filter-name>jkmvc</filter-name>
    <url-pattern>/*</url-pattern>
</filter-mapping>
```
## 2 编写代码

### 2.1 创建第一个Controller类

vim src/main/kotlin/com/jkmvc/example/controller/WelcomeController.kt

```
package net.jkcode.jkmvc.example.controller

import net.jkcode.jkmvc.http.Controller

/**
 * 主页
 */
class WelcomeController: Controller() {

    /**
     * 主页
     */
    public fun indexAction() {
        res.renderString("hello world");
    }

}
```

### 2.2 注册Controller类

其实是注册Controller类所在的包，jkmvc建议你在所有的controller都放在统一的包下


vim src/main/resources/http.yaml

```
# controller类所在的包路径
# controller classes's package paths
controllerPackages:
    - net.jkcode.jkmvc.example.controller
```

## 3 启动项目

### 3.1 启动web server

```
gradle :jkmvc-example:appRun -x test
```

### 3.2 访问网页

在浏览器中访问　http://localhost:8081/
