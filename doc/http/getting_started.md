# Get started

## 1 create project

### 1.1 create project directory

```
mkdir myproj
cd myproj
```

### 1.2 create buid.gradle

vim buid.gradle

```
// gradle's dependencies
buildscript {
    repositories {
        jcenter()
        mavenCentral()
        maven {
            url "http://dl.bintray.com/kotlin/kotlin"
        }
        maven {
            url 'http://oss.jfrog.org/artifactory/oss-snapshot-local'
        }
    }

    dependencies {
        classpath "org.jetbrains.kotlin:kotlin-gradle-plugin:$kotlin_version"
        classpath "org.jetbrains.kotlin:kotlin-allopen:$kotlin_version"

        classpath "org.jetbrains.dokka:dokka-gradle-plugin:${dokka_version}"

        classpath "org.akhikhl.gretty:gretty:${gretty_version}"
    }
}

// project name
group 'net.jkcode.jkmvc'
version '1.8.0'
name 'myproj'

// plugin
apply plugin: 'java'
apply plugin: 'kotlin'
apply plugin: 'maven'
apply plugin: 'org.akhikhl.gretty'

// project's dependencies
repositories {
    mavenCentral()
    maven {
        url "http://dl.bintray.com/kotlin/kotlin"
    }
}

dependencies {
	compile "net.jkcode.jkmvc:jkmvc-http:1.8.0"
}

// src directory
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

// jetty plugin
gretty{
    // server
    servletContainer 'jetty9' // 'tomcat8'
    httpPort 8080
    managedClassReload true // hot deploy
    scanInterval 1 // scan interval for hot deploy，if value = 0，no scan, no hot deploy

    // debug: gradle appRunDebug
    debugPort 5006
    debugSuspend true

    // webapp
    contextPath "/${project.name}"
    inplaceMode "hard" // default web directory: src/main/webapp
}
```

### 1.3 import into idea

Choose menu: File -> New -> Project From Existing Sources...

choose the file `build.gradle` which we created

### 1.4 edit web.xml

vim src/main/webapp/WEB-INF/web.xml

add the following text

```
<filter>
    <filter-name>jkmvc</filter-name>
    <filter-class>net.jkcode.jkmvc.http.JkFilter</filter-class>
    <!-- support servlet3.0: async servlet -->
    <async-supported>true</async-supported>
</filter>

<filter-mapping>
    <filter-name>jkmvc</filter-name>
    <url-pattern>/*</url-pattern>
</filter-mapping>
```

## 2 edit code

### 2.1 create the first `Controller` class

vim src/main/kotlin/com/jkmvc/example/controller/WelcomeController.kt

```
package net.jkcode.jkmvc.example.controller

import net.jkcode.jkmvc.http.Controller

/**
 * homepage
 */
class WelcomeController: Controller() {

    /**
     * homepage
     */
    public fun indexAction() {
        res.renderString("hello world");
    }

}
```

### 2.2 register controller class

Just register controller classes's package. And jkmvc suggests you to put all controller classes in a unified package.

vim src/main/resources/http.yaml

```
# controller classes's package paths
controllerPackages:
    - net.jkcode.jkmvc.example.controller
```

## 3 start project

### 3.1 start web server

```
gradle appRun -x test
```

### 3.2 visit webpage

open url in browser:　http://localhost:8081/myproj
