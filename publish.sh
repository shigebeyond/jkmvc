#!/bin/sh
# 设置生产环境
sed -i 's/env=dev/env=pro/g' gradle.properties

# 发布到本地库, 用于检查
gradle publishToMavenLocal -x test

# 发布到指定的maven仓库
gradle publish -x test

# 恢复开发环境
sed -i 's/env=pro/env=dev/g' gradle.properties