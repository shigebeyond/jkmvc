# Deploy

## 1 Setting up a production environment

vim src/main/resources/application.properties

```
# environment: prod/dev/test
environment=dev
```
## 2 Setting up the production database

vim src/main/resources/dataSources.yaml

Write your database configure

## 3 Setting up the production upload directory and domain

vim src/main/resources/upload.properties

```
# upload directory, where the uploaded file save, without postfix "/"
uploadDirectory=/var/www/upload
# max file size, unit: B K M G T
maxPostSize=1M
encoding=gbk
# domain to visit uploaded file
uploadDomain=http://localhost:8081/jkmvc/upload
```

## 4 Setting up the controller classes's package

vim src/main/resources/http.yaml

```
# 是否调试
debug: true
# controller类所在的包路径
# controller classes's package paths
controllerPackages:
    - net.jkcode.jkmvc.example.controller
```