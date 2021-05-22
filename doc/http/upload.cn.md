# 上传文件

jkmvc对上传包servlet3.0的`javax.servlet.http.Part`进行了二次封装，并提供了便捷的api来处理上传文件。

## 1 上传配置

vim src/main/resources/upload.properties

```
# 上传文件的保存目录，末尾不要带/
uploadRootDirectory=/var/www/upload
# 编码
encoding=gbk
# 禁止上传的文件扩展名, 以逗号分隔
forbiddenExt = jsp,jspx,exe,sh,php,py
# 访问上传文件的域名
uploadDomain=http://localhost:8081/jkmvc/upload
```

配置项 | 作用
--- | ---
uploadRootDirectory | 上传的根目录，由jkmvc接收的上传文件都保存到该目录下，同时为了能访问这些文件，你需要基于该目录建立http文件服务器
uploadDomain | 访问上传文件的域名，结合它可以获得访问上传文件的url

## 2 处理上传文件

### 2.1 上传的表单

表单用 `enctype="multipart/form-data"` 来修饰

```
<form class="form-inline" action="<%= req.absoluteUrl("user/uploadAvatar/" + user.getId()) %>" method="post" enctype="multipart/form-data">
    <div class="form-group">
        <label for="avatar">avatar</label>
        <input type="file" class="form-control" id="avatar" placeholder="avatar" name="avatar">
    </div>
    <button type="submit" class="btn btn-default">Upload</button>
</form>
```

### 2.2 接收上传文件

```
/**
 * 上传头像
 */
public fun uploadAvatar()
{
    // 查询单个用户
    val id: Int = req["id"]!!
    val user = UserModel(id)
    if(!user.isLoaded()){
        res.renderHtml("用户[" + req["id"] + "]不存在")
        return
    }

    // 检查并处理上传文件
    if(req.isUpload){ // 检查上传请求
        user.avatar = req.storePartFileAndGetRelativePath("avatar")
        user.update()
    }

    // 重定向到详情页
    redirect("user/detail/$id");
}
```

## 3 下载文件

文件上传后，当然需要被下载。文件下载有２种方式

### 3.1 java提供的下载

直接在Controller中调用`res.renderFile(file: File)` 来向浏览器响应文件

### 3.2 文件服务器提供的下载

我们可以使用apache/nginx来提供文件下载服务，直接指定服务目录为上传目录

```
location ~ \.(gif|jpg|jpeg|.js|.css)$ {
    root   /var/www/upload;
    index  index.html index.htm;
}

```

