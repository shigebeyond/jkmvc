<%@ page language="java" import="java.util.*,net.jkcode.jkmvc.http.HttpRequest,net.jkcode.jkmvc.example.model.UserModel,net.jkcode.jkmvc.http.session.Auth" pageEncoding="UTF-8"%>
<%
    HttpRequest req = HttpRequest.current();
    UserModel user = (UserModel)Auth.instance().getCurrentUser();
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
      <p>
      <% if(user != null) { %>
            当前登录用户是 <%= username %>, <a href="<%= req.absoluteUrl("user/logout/") %>" class="btn btn-warning">注销</a>
      <% }else{ %>
            未登录
      <% } %>
      </p>

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

  </div>
  <!-- 最新的 Bootstrap 核心 JavaScript 文件 -->
  <script src="https://cdn.bootcss.com/bootstrap/3.3.7/js/bootstrap.min.js" integrity="sha384-Tc5IQib027qvyjSMfHjOMaLkfuWVxZxUPnCJA7l2mCWNIpG9mGCD8wGNIcPD7Txa" crossorigin="anonymous"></script>
</body>
</html>