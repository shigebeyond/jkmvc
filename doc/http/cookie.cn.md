# cookie

主要是注意修改cookie的域名配置

vim src/main/resources/cookie.properties

```
expiry = 604800
path = /
domain = localhost
# 仅用于https/ssl
secure = false
# 禁止js脚本读取到cookie, 防止XSS攻击
httponly = false
```