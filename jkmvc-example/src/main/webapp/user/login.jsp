<%@ page language="java" import="com.jkmvc.http.Request,com.jkmvc.example.model.UserModel,com.jkmvc.session.Auth" pageEncoding="UTF-8"%>
<%
    Request req = (Request) request;
    UserModel user = (UserModel)Auth.getUser();
    String username = user == null ? "" : user.getUsername();
%>
<!DOCTYPE html>
<html lang="en">
<head>
  <meta charset="UTF-8">
  <title>todolist</title>
  <!-- 最新版本的 Bootstrap 核心 CSS 文件 -->
  <link rel="stylesheet" href="https://cdn.bootcss.com/bootstrap/3.3.7/css/bootstrap.min.css" integrity="sha384-BVYiiSIFeK1dGmJRAkycuHAHRg32OmUcww7on3RYdg4Va+PmSTsz/K68vbdEjh4u" crossorigin="anonymous">
  <!-- 可选的 Bootstrap 主题文件（一般不用引入） -->
  <link rel="stylesheet" href="https://cdn.bootcss.com/bootstrap/3.3.7/css/bootstrap-theme.min.css" integrity="sha384-rHyoN1iRsVXV4nD0JutlnGaslCJuC7uwjduW9SVrLvRYooPp2bWYgmgJQIXwl/Sp" crossorigin="anonymous">
</head>
<body>
  <div class="panel panel-default">
    <!-- Default panel contents -->
    <div class="panel-heading">登录</div>
    <div class="panel-body">

      <p>当前登录用户是 <%= username %></p>
    </div>

    <!-- Form -->
    <form action="<%= req.absoluteUrl("user/login") %>" method="post">
      <div class="form-group">
        <label for="username">username</label>
        <input type="text" class="form-control" id="username" placeholder="username" name="username">
      </div>
      <div class="form-group">
        <label for="password">password</label>
        <input type="text" class="form-control" id="password" placeholder="password" name="password">
      </div>
      <button type="submit" class="btn btn-default">Submit</button>
    </form>

  </div>
  <!-- 最新的 Bootstrap 核心 JavaScript 文件 -->
  <script src="https://cdn.bootcss.com/bootstrap/3.3.7/js/bootstrap.min.js" integrity="sha384-Tc5IQib027qvyjSMfHjOMaLkfuWVxZxUPnCJA7l2mCWNIpG9mGCD8wGNIcPD7Txa" crossorigin="anonymous"></script>
</body>
</html>