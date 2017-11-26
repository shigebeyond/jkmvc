# Upload files

Jkmvc uses `servlets.com: cos: 05Nov2002` for uploading files, and improves into a convenient API to handle the upload file.

## 1 Upload configuration

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

Configuration item | usage
--- | ---
The root directory uploadDirectory | upload, received by the jkmvc upload files are saved to the directory, at the same time in order to be able to access these files, you need to set the directory file server based on HTTP
UploadDomain | access to upload files with domain name, it can get access to upload files URL


配置项 | 作用
--- | ---
uploadDirectory | The root directory for upload, which jkmvc saves the upload files. To access these files, you need to setup a file server on this diretory.
uploadDomain | Domain name to access to upload files

## 2 Process upload files

### 2.1 upload form

The form is defined with `enctype =" multipart / form-data "` property.

```
<form class="form-inline" action="<%= req.absoluteUrl("user/uploadAvatar/" + user.getId()) %>" method="post" enctype="multipart/form-data">
    <div class="form-group">
        <label for="avatar">avatar</label>
        <input type="file" class="form-control" id="avatar" placeholder="avatar" name="avatar">
    </div>
    <button type="submit" class="btn btn-default">Upload</button>
</form>
```

### 2.2 Receive upload files


### 2.2 接收上传文件

```
/**
 * upload avatar
 */
public fun uploadAvatarAction()
{
    // set uploadSubdir which uploaded file is saved, you must set it before calling req's other api, or it's useless
    req.uploadSubdir = "avatar/" + Date().format("yyyy/MM/dd")

    // find a user
    val id: Int = req["id"]!!
    val user = UserModel(id)
    if(!user.isLoaded()){
        res.render("use [" + req["id"] + "] not exists")
        return
    }

    // check and handle upload request
    if(req.isUpload()){ // check upload request
        user.avatar = req.getFileRelativePath("avatar")
        user.update()
    }

    // redirect to detail page
    redirect("user/detail/$id");
}
```

## 3 Download file

After the file upload, it needs to be downloaded. There are 2 ways to download the file:

### 3.1 Download by java

Call `res.render (file: File)` in the Controller, to return file to the browser

### 3.2 Download by the file server

We can use apache / nginx to provide file download service, by specify the upload directory as http root directory

```
location ~ \.(gif|jpg|jpeg|.js|.css)$ {
    root   /var/www/upload;
    index  index.html index.htm;
}

```

