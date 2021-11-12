# read-write-split
基于ShardingShphere-JDBC读写分离的样版工程。通过该工程快速了解ShardingShphere-JDBC框架。

## 项目框架
* Spring Boot
* Mybatis 
* tkMapper
* PageHelper
* HikariCP
* MySQL
* knife4j
* ShardingShphere-JDBC

## 基础设施搭建
MySQL数据库一个``master``节点一个``slave``节点均部署在Docker容器中，服务器使用``CentOS7``。

### 安装Docker
```bash
yum -y install docker
```

### 启动Docker服务
```bash
systemctl enable docker.service
systemctl start docker.service
```

### 创建Docker macvlan网络
```bash
docker network create -d macvlan --subnet=<局域网网段> --gateway=<网关地址> -o parent=<物理网卡名称> <给该网络取一个漂亮的名称>
```
举个例子
```bash
docker network create -d macvlan --subnet=192.168.0.0/24 --gateway=192.168.210.0.1 -o parent=eth0 macvlan_net
```

### 部署MySQL Master节点
``$PWD``参数表示你当前命令行所处的路径，那我们``cd``到Docker的``volumes``路径下，来部署我们的``master``节点。
```bash
cd /var/lib/docker/volumes
```
```bash
docker run -itd \
 --name mysql-01 \
 --hostname mysql-01 \
 -v $PWD/mysql_01/conf:/etc/mysql/conf.d \
 -v $PWD/mysql_01/data:/var/lib/mysql \
 -e TZ=Asia/Shanghai \
 -e MYSQL_ROOT_PASSWORD=123456 \
 --net macvlan_net \
 --ip 192.168.0.40 \
 mysql:5.7 \
 --character-set-server=utf8mb4 \
 --collation-server=utf8mb4_unicode_ci
```

### 配置MySQL Master节点
进入``$PWD/mysql_01/conf``目录，创建或修改``my.cnf``文件。
```bash
vim /var/lib/docker/volumes/mysql_01/conf/my.cnf
```
```ini
[mysqld]
server-id=1
log-bin=master-bin
binlog-format=row
binlog-ignore-db=information_schema
binlog-ignore-db=mysql
binlog-ignore-db=performance_schema
binlog-ignore-db=sys
expire_logs_days=7
```

### 部署MySQL Slave节点
``$PWD``参数表示你当前命令行所处的路径，那我们``cd``到Docker的``volumes``路径下，来部署我们的``slave``节点。
```bash
cd /var/lib/docker/volumes
```
```bash
docker run -itd \
 --name mysql-02 \
 --hostname mysql-02 \
 -v $PWD/mysql_02/conf:/etc/mysql/conf.d \
 -v $PWD/mysql_02/data:/var/lib/mysql \
 -e TZ=Asia/Shanghai \
 -e MYSQL_ROOT_PASSWORD=123456 \
 --net macvlan_net \
 --ip 192.168.0.41 \
 mysql:5.7 \
 --character-set-server=utf8mb4 \
 --collation-server=utf8mb4_unicode_ci
```

### 配置MySQL Slave节点
进入``$PWD/mysql_02/conf``目录，创建或修改``my.cnf``文件。
```bash
vim /var/lib/docker/volumes/mysql_02/conf/my.cnf
```
```ini
[mysqld]
server-id=2
read-only=1
```

### 配置主从复制
使用``mysql``命令行工具连接``master``节点
```bash
mysql -uroot -p123456 -h'192.168.0.40'
```
查看当前``master``节点状态信息
```bash
show master status;
```
```master```节点状态信息回显
```text
mysql> show master status;
+-------------------+----------+--------------+-------------------------------------------------+-------------------+
| File              | Position | Binlog_Do_DB | Binlog_Ignore_DB                                | Executed_Gtid_Set |
+-------------------+----------+--------------+-------------------------------------------------+-------------------+
| master-bin.000001 |   154    |              | information_schema,mysql,performance_schema,sys |                   |
+-------------------+----------+--------------+-------------------------------------------------+-------------------+
1 row in set (0.00 sec)
```
使用``mysql``命令行工具连接``slave``节点
```bash
mysql -uroot -p123456 -h'192.168.0.40'
```
配置主从复制命令
* ``master_log_file``：``master``节点回显信息中的``File``列的值。
* ``master_log_pos``：``master``节点回显信息中的``Position``列的值。
```bash
change master to
master_host='192.168.0.40',
master_user='root',
master_password='123456',
master_log_file='master-bin.000001',
master_log_pos=154;
```
启动主从复制命令
```bash
start slave;
```
查看当前```slave```节点状态信息
```bash
show slave status \G;
```
``slave``节点状态信息回显
```text
mysql> show slave status \G;
*************************** 1. row ***************************
               Slave_IO_State: Waiting for master to send event
                  Master_Host: 192.168.0.40
                  Master_User: root
                  Master_Port: 3306
                Connect_Retry: 60
              Master_Log_File: master-bin.000001
          Read_Master_Log_Pos: 116304
               Relay_Log_File: mysql-02-relay-bin.000002
                Relay_Log_Pos: 113214
        Relay_Master_Log_File: master-bin.000001
             Slave_IO_Running: Yes
            Slave_SQL_Running: Yes
              Replicate_Do_DB: 
          Replicate_Ignore_DB: 
           Replicate_Do_Table: 
       Replicate_Ignore_Table: 
      Replicate_Wild_Do_Table: 
  Replicate_Wild_Ignore_Table: 
                   Last_Errno: 0
                   Last_Error: 
                 Skip_Counter: 0
          Exec_Master_Log_Pos: 116304
              Relay_Log_Space: 113424
              Until_Condition: None
               Until_Log_File: 
                Until_Log_Pos: 0
           Master_SSL_Allowed: No
           Master_SSL_CA_File: 
           Master_SSL_CA_Path: 
              Master_SSL_Cert: 
            Master_SSL_Cipher: 
               Master_SSL_Key: 
        Seconds_Behind_Master: 0
Master_SSL_Verify_Server_Cert: No
                Last_IO_Errno: 0
                Last_IO_Error: 
               Last_SQL_Errno: 0
               Last_SQL_Error: 
  Replicate_Ignore_Server_Ids: 
             Master_Server_Id: 1
                  Master_UUID: 36902060-410b-11ec-b5e5-0242c0a8d228
             Master_Info_File: /var/lib/mysql/master.info
                    SQL_Delay: 0
          SQL_Remaining_Delay: NULL
      Slave_SQL_Running_State: Slave has read all relay log; waiting for more updates
           Master_Retry_Count: 86400
                  Master_Bind: 
      Last_IO_Error_Timestamp: 
     Last_SQL_Error_Timestamp: 
               Master_SSL_Crl: 
           Master_SSL_Crlpath: 
           Retrieved_Gtid_Set: 
            Executed_Gtid_Set: 
                Auto_Position: 0
         Replicate_Rewrite_DB: 
                 Channel_Name: 
           Master_TLS_Version: 
1 row in set (0.00 sec)
```

## 关于SharindShphere-JDBC框架主键回显报错问题
本项目数据访问层使用tkMapper框架，主键自增在字段上使用``@GeneratedValue(generator = "JDBC")``注解，
当调用tkMapper自带的``insertSelective``方法会导致空指针异常。

### 异常堆栈
```java
java.lang.NullPointerException: ResultSet should call next or has no more data.
	at com.google.common.base.Preconditions.checkNotNull(Preconditions.java:897) ~[guava-29.0-jre.jar:na]
	at org.apache.shardingsphere.driver.jdbc.core.resultset.GeneratedKeysResultSet.checkStateForGetData(GeneratedKeysResultSet.java:243) ~[shardingsphere-jdbc-core-5.0.0.jar:5.0.0]
	at org.apache.shardingsphere.driver.jdbc.core.resultset.GeneratedKeysResultSet.getLong(GeneratedKeysResultSet.java:142) ~[shardingsphere-jdbc-core-5.0.0.jar:5.0.0]
	at org.apache.ibatis.type.LongTypeHandler.getNullableResult(LongTypeHandler.java:44) ~[mybatis-3.5.7.jar:3.5.7]
	at org.apache.ibatis.type.LongTypeHandler.getNullableResult(LongTypeHandler.java:26) ~[mybatis-3.5.7.jar:3.5.7]
	at org.apache.ibatis.type.BaseTypeHandler.getResult(BaseTypeHandler.java:94) ~[mybatis-3.5.7.jar:3.5.7]
	at org.apache.ibatis.executor.keygen.Jdbc3KeyGenerator$KeyAssigner.assign(Jdbc3KeyGenerator.java:270) ~[mybatis-3.5.7.jar:3.5.7]
	at org.apache.ibatis.executor.keygen.Jdbc3KeyGenerator.lambda$assignKeysToParam$0(Jdbc3KeyGenerator.java:124) ~[mybatis-3.5.7.jar:3.5.7]
	at java.util.ArrayList.forEach(ArrayList.java:1259) ~[na:1.8.0_301]
	at org.apache.ibatis.executor.keygen.Jdbc3KeyGenerator.assignKeysToParam(Jdbc3KeyGenerator.java:124) ~[mybatis-3.5.7.jar:3.5.7]
	at org.apache.ibatis.executor.keygen.Jdbc3KeyGenerator.assignKeys(Jdbc3KeyGenerator.java:104) ~[mybatis-3.5.7.jar:3.5.7]
	at org.apache.ibatis.executor.keygen.Jdbc3KeyGenerator.processBatch(Jdbc3KeyGenerator.java:85) ~[mybatis-3.5.7.jar:3.5.7]
	at org.apache.ibatis.executor.keygen.Jdbc3KeyGenerator.processAfter(Jdbc3KeyGenerator.java:71) ~[mybatis-3.5.7.jar:3.5.7]
	at org.apache.ibatis.executor.statement.PreparedStatementHandler.update(PreparedStatementHandler.java:51) ~[mybatis-3.5.7.jar:3.5.7]
	at org.apache.ibatis.executor.statement.RoutingStatementHandler.update(RoutingStatementHandler.java:74) ~[mybatis-3.5.7.jar:3.5.7]
	at org.apache.ibatis.executor.SimpleExecutor.doUpdate(SimpleExecutor.java:50) ~[mybatis-3.5.7.jar:3.5.7]
	at org.apache.ibatis.executor.BaseExecutor.update(BaseExecutor.java:117) ~[mybatis-3.5.7.jar:3.5.7]
	at sun.reflect.NativeMethodAccessorImpl.invoke0(Native Method) ~[na:1.8.0_301]
	at sun.reflect.NativeMethodAccessorImpl.invoke(NativeMethodAccessorImpl.java:62) ~[na:1.8.0_301]
	at sun.reflect.DelegatingMethodAccessorImpl.invoke(DelegatingMethodAccessorImpl.java:43) ~[na:1.8.0_301]
	at java.lang.reflect.Method.invoke(Method.java:498) ~[na:1.8.0_301]
	at org.apache.ibatis.plugin.Plugin.invoke(Plugin.java:64) ~[mybatis-3.5.7.jar:3.5.7]
	at com.sun.proxy.$Proxy278.update(Unknown Source) ~[na:na]
	at org.apache.ibatis.session.defaults.DefaultSqlSession.update(DefaultSqlSession.java:194) ~[mybatis-3.5.7.jar:3.5.7]
	at org.apache.ibatis.session.defaults.DefaultSqlSession.insert(DefaultSqlSession.java:181) ~[mybatis-3.5.7.jar:3.5.7]
	at sun.reflect.NativeMethodAccessorImpl.invoke0(Native Method) ~[na:1.8.0_301]
	at sun.reflect.NativeMethodAccessorImpl.invoke(NativeMethodAccessorImpl.java:62) ~[na:1.8.0_301]
	at sun.reflect.DelegatingMethodAccessorImpl.invoke(DelegatingMethodAccessorImpl.java:43) ~[na:1.8.0_301]
	at java.lang.reflect.Method.invoke(Method.java:498) ~[na:1.8.0_301]
	at org.mybatis.spring.SqlSessionTemplate$SqlSessionInterceptor.invoke(SqlSessionTemplate.java:427) ~[mybatis-spring-2.0.6.jar:2.0.6]
	at com.sun.proxy.$Proxy230.insert(Unknown Source) ~[na:na]
	at org.mybatis.spring.SqlSessionTemplate.insert(SqlSessionTemplate.java:272) ~[mybatis-spring-2.0.6.jar:2.0.6]
	at org.apache.ibatis.binding.MapperMethod.execute(MapperMethod.java:62) ~[mybatis-3.5.7.jar:3.5.7]
	at org.apache.ibatis.binding.MapperProxy$PlainMethodInvoker.invoke(MapperProxy.java:145) ~[mybatis-3.5.7.jar:3.5.7]
	at org.apache.ibatis.binding.MapperProxy.invoke(MapperProxy.java:86) ~[mybatis-3.5.7.jar:3.5.7]
	at com.sun.proxy.$Proxy236.insertSelective(Unknown Source) ~[na:na]
	at sun.reflect.NativeMethodAccessorImpl.invoke0(Native Method) ~[na:1.8.0_301]
	at sun.reflect.NativeMethodAccessorImpl.invoke(NativeMethodAccessorImpl.java:62) ~[na:1.8.0_301]
	at sun.reflect.DelegatingMethodAccessorImpl.invoke(DelegatingMethodAccessorImpl.java:43) ~[na:1.8.0_301]
	at java.lang.reflect.Method.invoke(Method.java:498) ~[na:1.8.0_301]
	at org.springframework.aop.support.AopUtils.invokeJoinpointUsingReflection(AopUtils.java:344) ~[spring-aop-5.2.5.RELEASE.jar:5.2.5.RELEASE]
	at org.springframework.aop.framework.ReflectiveMethodInvocation.invokeJoinpoint(ReflectiveMethodInvocation.java:198) ~[spring-aop-5.2.5.RELEASE.jar:5.2.5.RELEASE]
	at org.springframework.aop.framework.ReflectiveMethodInvocation.proceed(ReflectiveMethodInvocation.java:163) ~[spring-aop-5.2.5.RELEASE.jar:5.2.5.RELEASE]
	at org.springframework.dao.support.PersistenceExceptionTranslationInterceptor.invoke(PersistenceExceptionTranslationInterceptor.java:139) ~[spring-tx-5.2.5.RELEASE.jar:5.2.5.RELEASE]
	at org.springframework.aop.framework.ReflectiveMethodInvocation.proceed(ReflectiveMethodInvocation.java:186) ~[spring-aop-5.2.5.RELEASE.jar:5.2.5.RELEASE]
	at org.springframework.aop.framework.JdkDynamicAopProxy.invoke(JdkDynamicAopProxy.java:212) ~[spring-aop-5.2.5.RELEASE.jar:5.2.5.RELEASE]
	at com.sun.proxy.$Proxy238.insertSelective(Unknown Source) ~[na:na]
	at com.github.xuchengen.rws.biz.UserService.createUser(UserService.java:19) ~[classes/:na]
	at com.github.xuchengen.rws.biz.UserService$$FastClassBySpringCGLIB$$49f8d808.invoke(<generated>) ~[classes/:na]
	at org.springframework.cglib.proxy.MethodProxy.invoke(MethodProxy.java:218) ~[spring-core-5.2.5.RELEASE.jar:5.2.5.RELEASE]
	at org.springframework.aop.framework.CglibAopProxy$DynamicAdvisedInterceptor.intercept(CglibAopProxy.java:687) ~[spring-aop-5.2.5.RELEASE.jar:5.2.5.RELEASE]
	at com.github.xuchengen.rws.biz.UserService$$EnhancerBySpringCGLIB$$488bcf4f.createUser(<generated>) ~[classes/:na]
	at com.github.xuchengen.rws.web.UserController.createUser(UserController.java:43) ~[classes/:na]
	at sun.reflect.NativeMethodAccessorImpl.invoke0(Native Method) ~[na:1.8.0_301]
	at sun.reflect.NativeMethodAccessorImpl.invoke(NativeMethodAccessorImpl.java:62) ~[na:1.8.0_301]
	at sun.reflect.DelegatingMethodAccessorImpl.invoke(DelegatingMethodAccessorImpl.java:43) ~[na:1.8.0_301]
	at java.lang.reflect.Method.invoke(Method.java:498) ~[na:1.8.0_301]
	at org.springframework.web.method.support.InvocableHandlerMethod.doInvoke(InvocableHandlerMethod.java:190) ~[spring-web-5.2.5.RELEASE.jar:5.2.5.RELEASE]
	at org.springframework.web.method.support.InvocableHandlerMethod.invokeForRequest(InvocableHandlerMethod.java:138) ~[spring-web-5.2.5.RELEASE.jar:5.2.5.RELEASE]
	at org.springframework.web.servlet.mvc.method.annotation.ServletInvocableHandlerMethod.invokeAndHandle(ServletInvocableHandlerMethod.java:105) ~[spring-webmvc-5.2.5.RELEASE.jar:5.2.5.RELEASE]
	at org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerAdapter.invokeHandlerMethod(RequestMappingHandlerAdapter.java:879) ~[spring-webmvc-5.2.5.RELEASE.jar:5.2.5.RELEASE]
	at org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerAdapter.handleInternal(RequestMappingHandlerAdapter.java:793) ~[spring-webmvc-5.2.5.RELEASE.jar:5.2.5.RELEASE]
	at org.springframework.web.servlet.mvc.method.AbstractHandlerMethodAdapter.handle(AbstractHandlerMethodAdapter.java:87) ~[spring-webmvc-5.2.5.RELEASE.jar:5.2.5.RELEASE]
	at org.springframework.web.servlet.DispatcherServlet.doDispatch(DispatcherServlet.java:1040) ~[spring-webmvc-5.2.5.RELEASE.jar:5.2.5.RELEASE]
	at org.springframework.web.servlet.DispatcherServlet.doService(DispatcherServlet.java:943) ~[spring-webmvc-5.2.5.RELEASE.jar:5.2.5.RELEASE]
	at org.springframework.web.servlet.FrameworkServlet.processRequest(FrameworkServlet.java:1006) ~[spring-webmvc-5.2.5.RELEASE.jar:5.2.5.RELEASE]
	at org.springframework.web.servlet.FrameworkServlet.doPost(FrameworkServlet.java:909) ~[spring-webmvc-5.2.5.RELEASE.jar:5.2.5.RELEASE]
	at javax.servlet.http.HttpServlet.service(HttpServlet.java:660) ~[tomcat-embed-core-9.0.33.jar:9.0.33]
	at org.springframework.web.servlet.FrameworkServlet.service(FrameworkServlet.java:883) ~[spring-webmvc-5.2.5.RELEASE.jar:5.2.5.RELEASE]
	at javax.servlet.http.HttpServlet.service(HttpServlet.java:741) ~[tomcat-embed-core-9.0.33.jar:9.0.33]
	at org.apache.catalina.core.ApplicationFilterChain.internalDoFilter(ApplicationFilterChain.java:231) ~[tomcat-embed-core-9.0.33.jar:9.0.33]
	at org.apache.catalina.core.ApplicationFilterChain.doFilter(ApplicationFilterChain.java:166) ~[tomcat-embed-core-9.0.33.jar:9.0.33]
	at org.apache.tomcat.websocket.server.WsFilter.doFilter(WsFilter.java:53) ~[tomcat-embed-websocket-9.0.33.jar:9.0.33]
	at org.apache.catalina.core.ApplicationFilterChain.internalDoFilter(ApplicationFilterChain.java:193) ~[tomcat-embed-core-9.0.33.jar:9.0.33]
	at org.apache.catalina.core.ApplicationFilterChain.doFilter(ApplicationFilterChain.java:166) ~[tomcat-embed-core-9.0.33.jar:9.0.33]
	at org.springframework.web.filter.RequestContextFilter.doFilterInternal(RequestContextFilter.java:100) ~[spring-web-5.2.5.RELEASE.jar:5.2.5.RELEASE]
	at org.springframework.web.filter.OncePerRequestFilter.doFilter(OncePerRequestFilter.java:119) ~[spring-web-5.2.5.RELEASE.jar:5.2.5.RELEASE]
	at org.apache.catalina.core.ApplicationFilterChain.internalDoFilter(ApplicationFilterChain.java:193) ~[tomcat-embed-core-9.0.33.jar:9.0.33]
	at org.apache.catalina.core.ApplicationFilterChain.doFilter(ApplicationFilterChain.java:166) ~[tomcat-embed-core-9.0.33.jar:9.0.33]
	at org.springframework.web.filter.FormContentFilter.doFilterInternal(FormContentFilter.java:93) ~[spring-web-5.2.5.RELEASE.jar:5.2.5.RELEASE]
	at org.springframework.web.filter.OncePerRequestFilter.doFilter(OncePerRequestFilter.java:119) ~[spring-web-5.2.5.RELEASE.jar:5.2.5.RELEASE]
	at org.apache.catalina.core.ApplicationFilterChain.internalDoFilter(ApplicationFilterChain.java:193) ~[tomcat-embed-core-9.0.33.jar:9.0.33]
	at org.apache.catalina.core.ApplicationFilterChain.doFilter(ApplicationFilterChain.java:166) ~[tomcat-embed-core-9.0.33.jar:9.0.33]
	at org.springframework.web.filter.CharacterEncodingFilter.doFilterInternal(CharacterEncodingFilter.java:201) ~[spring-web-5.2.5.RELEASE.jar:5.2.5.RELEASE]
	at org.springframework.web.filter.OncePerRequestFilter.doFilter(OncePerRequestFilter.java:119) ~[spring-web-5.2.5.RELEASE.jar:5.2.5.RELEASE]
	at org.apache.catalina.core.ApplicationFilterChain.internalDoFilter(ApplicationFilterChain.java:193) ~[tomcat-embed-core-9.0.33.jar:9.0.33]
	at org.apache.catalina.core.ApplicationFilterChain.doFilter(ApplicationFilterChain.java:166) ~[tomcat-embed-core-9.0.33.jar:9.0.33]
	at org.apache.catalina.core.StandardWrapperValve.invoke(StandardWrapperValve.java:202) ~[tomcat-embed-core-9.0.33.jar:9.0.33]
	at org.apache.catalina.core.StandardContextValve.invoke(StandardContextValve.java:96) [tomcat-embed-core-9.0.33.jar:9.0.33]
	at org.apache.catalina.authenticator.AuthenticatorBase.invoke(AuthenticatorBase.java:541) [tomcat-embed-core-9.0.33.jar:9.0.33]
	at org.apache.catalina.core.StandardHostValve.invoke(StandardHostValve.java:139) [tomcat-embed-core-9.0.33.jar:9.0.33]
	at org.apache.catalina.valves.ErrorReportValve.invoke(ErrorReportValve.java:92) [tomcat-embed-core-9.0.33.jar:9.0.33]
	at org.apache.catalina.core.StandardEngineValve.invoke(StandardEngineValve.java:74) [tomcat-embed-core-9.0.33.jar:9.0.33]
	at org.apache.catalina.valves.RemoteIpValve.invoke(RemoteIpValve.java:747) [tomcat-embed-core-9.0.33.jar:9.0.33]
	at org.apache.catalina.connector.CoyoteAdapter.service(CoyoteAdapter.java:343) [tomcat-embed-core-9.0.33.jar:9.0.33]
	at org.apache.coyote.http11.Http11Processor.service(Http11Processor.java:373) [tomcat-embed-core-9.0.33.jar:9.0.33]
	at org.apache.coyote.AbstractProcessorLight.process(AbstractProcessorLight.java:65) [tomcat-embed-core-9.0.33.jar:9.0.33]
	at org.apache.coyote.AbstractProtocol$ConnectionHandler.process(AbstractProtocol.java:868) [tomcat-embed-core-9.0.33.jar:9.0.33]
	at org.apache.tomcat.util.net.NioEndpoint$SocketProcessor.doRun(NioEndpoint.java:1594) [tomcat-embed-core-9.0.33.jar:9.0.33]
	at org.apache.tomcat.util.net.SocketProcessorBase.run(SocketProcessorBase.java:49) [tomcat-embed-core-9.0.33.jar:9.0.33]
	at java.util.concurrent.ThreadPoolExecutor.runWorker(ThreadPoolExecutor.java:1149) [na:1.8.0_301]
	at java.util.concurrent.ThreadPoolExecutor$Worker.run(ThreadPoolExecutor.java:624) [na:1.8.0_301]
	at org.apache.tomcat.util.threads.TaskThread$WrappingRunnable.run(TaskThread.java:61) [tomcat-embed-core-9.0.33.jar:9.0.33]
	at java.lang.Thread.run(Thread.java:748) [na:1.8.0_301]
```

### 异常分析
当我们调用tkMapper框架自带的``insertSelective``方法时生成的SQL语句如下：
```sql
INSERT INTO `t_user` ( `id`,`name`,`phone` ) VALUES( ?,?,? )
```
``PreparedStatement``绑定参数如下：
```text
Parameters: null, 凡尔赛(String), 17811111113(String)
```
tkMapper框架对``insertSelective``方法的官方解释：
> 保存一个实体，null的属性不会保存，会使用数据库默认值，无默认值则使用null

问题就出在这里，自增主键的null会当作绑定参数传递。ShardingShphere框架在底层处理时将该null值作为插入的主键值暂存，
执行回填逻辑时将该值作为可回填主键的值最终导致了异常。

### 重点关注的方法
```java
public InsertStatementContext(final Map<String, ShardingSphereMetaData> metaDataMap, final List<Object> parameters, final InsertStatement sqlStatement, final String defaultSchemaName) {
    super(sqlStatement);
    AtomicInteger parametersOffset = new AtomicInteger(0);
    insertValueContexts = getInsertValueContexts(parameters, parametersOffset);
    insertSelectContext = getInsertSelectContext(metaDataMap, parameters, parametersOffset, defaultSchemaName).orElse(null);
    onDuplicateKeyUpdateValueContext = getOnDuplicateKeyUpdateValueContext(parameters, parametersOffset).orElse(null);
    tablesContext = new TablesContext(getAllSimpleTableSegments());
    ShardingSphereSchema schema = getSchema(metaDataMap, defaultSchemaName);
    List<String> insertColumnNames = getInsertColumnNames();
    columnNames = useDefaultColumns() ? schema.getAllColumnNames(sqlStatement.getTable().getTableName().getIdentifier().getValue()) : insertColumnNames;
    generatedKeyContext = new GeneratedKeyContextEngine(sqlStatement, schema).createGenerateKeyContext(insertColumnNames, getAllValueExpressions(sqlStatement), parameters).orElse(null);
    this.schemaName = defaultSchemaName;
}
```

```java
public Optional<GeneratedKeyContext> createGenerateKeyContext(final List<String> insertColumnNames, final List<List<ExpressionSegment>> valueExpressions, final List<Object> parameters) {
    String tableName = insertStatement.getTable().getTableName().getIdentifier().getValue();
    return findGenerateKeyColumn(tableName).map(optional -> containsGenerateKey(insertColumnNames, optional)
            ? findGeneratedKey(insertColumnNames, valueExpressions, parameters, optional) : new GeneratedKeyContext(optional, true));
}
```

```java
private GeneratedKeyContext findGeneratedKey(final List<String> insertColumnNames, final List<List<ExpressionSegment>> valueExpressions, 
                                                 final List<Object> parameters, final String generateKeyColumnName) {
    GeneratedKeyContext result = new GeneratedKeyContext(generateKeyColumnName, false);
    for (ExpressionSegment each : findGenerateKeyExpressions(insertColumnNames, valueExpressions, generateKeyColumnName)) {
        if (each instanceof ParameterMarkerExpressionSegment) {
            result.getGeneratedValues().add((Comparable<?>) parameters.get(((ParameterMarkerExpressionSegment) each).getParameterMarkerIndex()));
        } else if (each instanceof LiteralExpressionSegment) {
            result.getGeneratedValues().add((Comparable<?>) ((LiteralExpressionSegment) each).getLiterals());
        }
    }
    return result;
}
```
