# 引入缺失的jar
# gradle war插件在打包时会忽略掉javax.servlet-api-3.1.0.jar, 而我直接启动jetty又需要他

# 在build/app/中执行
# cd build/app/
pwd
WAR=`ls | grep .war`
mkdir tmp
mv $WAR tmp/
cd tmp/
# 解压
jar -xvf $WAR
rm $WAR
# 复制javax.servlet-api-3.1.0.jar
if [ -d "/home/shi/.m2" ]; then # 本地
    cp /home/shi/.m2/repository/javax/servlet/javax.servlet-api/3.1.0/javax.servlet-api-3.1.0.jar WEB-INF/lib/
else # 测试服
	cp /root/java/javax.servlet-api-3.1.0.jar WEB-INF/lib/
fi
# 重新打包
jar -cvf $WAR *
mv $WAR ..
cd ..
rm -rf tmp