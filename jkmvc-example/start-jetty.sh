#!/bin/sh
JAVA_VERSION=`java -fullversion 2>&1 | awk -F[\"\.] '{print $2$3$4}' |awk -F"_" '{print $1}'`
if [ $JAVA_VERSION -lt 180 ]; then
	echo "Error: Java version should >= 1.8.0 "
    exit 1
fi

cd `dirname $0`
DIR=`pwd`

WAR=`ls | grep .war`
# 去掉.war后缀, 即可工程名
PRO=${WAR%%.war}

if [ ! -d $PRO ]; then
	mkdir $PRO
	cd $PRO
	echo "解押"$WAR
	#unzip ../$WAR
	jar -xvf ../$WAR

	# 移动servlet.jar
	mv ../javax.servlet-api-3.1.0.jar WEB-INF/lib/
fi

cd $DIR

# 将 jetty.yaml 中的 webDir 配置项修改为当前项目路径
#sed -i "s/webDir: .*src\/main\/webapp/webDir: $PRO/g" $PRO/WEB-INF/classes/jetty.yaml
sed -i "s/webDir: .*src\/main\/webapp/webDir: $PRO/g" conf/jetty.yaml

echo "启动jetty"
JAVA_OPTS="-Djava.net.preferIPv4Stack=true -server -Xms1g -Xmx1g -XX:MetaspaceSize=128m -Djava.util.concurrent.ForkJoinPool.common.parallelism=32"
#JAVA_OPTS="-Djava.net.preferIPv4Stack=true -server"

JAVA_DEBUG_OPTS=""
if [ "$1" = "debug" ]; then
    JAVA_DEBUG_OPTS=" -Xdebug -Xnoagent -Djava.compiler=NONE -Xrunjdwp:transport=dt_socket,address=5005,server=y,suspend=n "
fi

SERVER_CLASS='net.jkcode.jkmvc.server.JettyServerLauncher'

java $JAVA_OPTS $JAVA_DEBUG_OPTS -cp $DIR/conf:$DIR/$PRO/WEB-INF/classes:$DIR/$PRO/WEB-INF/lib/* $SERVER_CLASS
