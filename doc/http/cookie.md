# cookie

Remember to config `domain`

vim src/main/resources/cookie.properties

```
expiry = 604800
path = /
domain = localhost
# only use https/ssl
secure = false
# prevent js from reading cookie, defend XSS attack
httponly = false
```
