# read-write-split
基于ShardingShphere-JDBC读写分离的样版工程。通过该工程快速了解ShardingShphere-JDBC框架。

---
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

### 配置MySQL slave节点
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