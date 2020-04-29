#!/bin/sh
# 设置生产环境
sed -i 's/env=dev/env=pro/g' gradle.properties

# 安装到本地库
#gradle install -x test
gradle :jkmvc-orm:install -x test
gradle :jkmvc-http:install -x test
gradle :jkmvc-server-jetty:install -x test
gradle :jkmvc-tag:install -x test

# 恢复开发环境
sed -i 's/env=pro/env=dev/g' gradle.properties
