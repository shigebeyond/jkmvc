# Deploy

## 1 Setting up a production environment

vim src/main/resources/jkmvc.properties

```
# environment: prod/dev/test
environment=dev
```
## 2 Setting up the production database

vim src/main/resources/database.yaml

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