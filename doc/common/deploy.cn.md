# 部署

## 1 设置生成环境

vim src/main/resources/application.properties

```
# 环境：prod/dev/test
environment=dev
```
## 2 设置生成库

vim src/main/resources/database.yaml

修改你的生成库的连接配置

## 3 设置生产环境的上传目录与域名

vim src/main/resources/upload.properties

```
# 上传文件的保存目录，末尾不要带/
uploadDirectory=upload
# 上传文件的大小限制，单位 B K M G T
maxPostSize=1M
# 编码
encoding=gbk
# 上传文件的域名
uploadDomain=http://localhost:8081/jkmvc/upload

```