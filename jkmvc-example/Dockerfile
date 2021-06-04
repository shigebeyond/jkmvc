# 拷贝到 build/app目录下, 执行: sudo docker build -t jkmvcapp .; sudo docker #run --name jkmvcapp -d jkmvcapp:latest

# 基础镜像
FROM ubuntu

# 描述
MAINTAINER jkmvcapp

# 定义匿名数据卷, 挂到/var/lib/docker/tmp/
# 必须是容器内部路径, 不能是宿主路径
# 如果要使用宿主路径, 请在 docker run时用-v做好宿主与容器的路径映射
# https://blog.csdn.net/fangford/article/details/88873104
#VOLUME "/data1"

# 解压与复制jdk.tar.gz
# 由于add/copy的文件必须使用上下文目录的内容, 因此要先将jdk.tar.gz拷贝到当前目录
# https://www.367783.net/hosting/5025.html
ADD jdk-8u172-linux-x64.tar.gz /usr/local
# 由于目录名不一定是 jkmvc-example, 则不能写死
#COPY jkmvc-example-1.9.0.war /app/
COPY *.war /app/
COPY start-jetty.sh /app/

# 配置 JDK 的环境变量和字符集
ENV JAVA_HOME /usr/local/jdk1.8.0_172
ENV PATH $JAVA_HOME/bin:$PATH
ENV CLASSPATH .:$JAVA_HOME/lib/dt.jar:$JAVA_HOME/lib/tools.jar
ENV LANG C.UTF-8

# 暴露端口, 跟jetty.yaml端口一样
EXPOSE 8082

# 安装unzip -- 安装太久, jenkins ssh连接断开了
#RUN apt-get update
#RUN apt-get install unzip

# 启动命令
ENTRYPOINT ["/app/start-jetty.sh"]