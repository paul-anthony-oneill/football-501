$ fly logs -a backend-rosy-cloud-4618
Waiting for logs...
19:06:11 at org.springframework.beans.factory.support.SimpleInstantiationStrategy.instantiate(SimpleInstantiationStrategy.java:152) ~[spring-beans-7.0.7.jar!/:7.0.7]
19:06:11 at org.springframework.beans.factory.support.ConstructorResolver.instantiate(ConstructorResolver.java:653) ~[spring-beans-7.0.7.jar!/:7.0.7]
19:06:11 ... 25 common frames omitted
19:06:11 Caused by: org.flywaydb.core.internal.exception.sqlExceptions.FlywaySqlUnableToConnectToDbException: Unable to obtain connection from database: The server requested SCRAM-based authentication, but no password was provided.
19:06:11 -------------------------------------------------------------------------------------------------------------------------
19:06:11 SQL State : 08004
19:06:11 Error Code : 0
19:06:11 Message : The server requested SCRAM-based authentication, but no password was provided.
19:06:11 at org.flywaydb.core.internal.jdbc.JdbcUtils.openConnection(JdbcUtils.java:70) ~[flyway-core-11.14.1.jar!/:na]
19:06:11 at org.flywaydb.core.internal.jdbc.JdbcConnectionFactory.<init>(JdbcConnectionFactory.java:76) ~[flyway-core-11.14.1.jar!/:na]
19:06:11 at org.flywaydb.core.FlywayExecutor.execute(FlywayExecutor.java:142) ~[flyway-core-11.14.1.jar!/:na]
19:06:11 at org.flywaydb.core.Flyway.repair(Flyway.java:460) ~[flyway-core-11.14.1.jar!/:na]
19:06:11 at com.football501.config.FlywayConfig.flyway(FlywayConfig.java:54) ~[!/:0.0.1-SNAPSHOT]
19:06:11 at com.football501.config.FlywayConfig$$SpringCGLIB$$0.CGLIB$flyway$0(<generated>) ~[!/:0.0.1-SNAPSHOT]
19:06:11 	at com.football501.config.FlywayConfig$$SpringCGLIB$$FastClass$$1.invoke(<generated>) ~[!/:0.0.1-SNAPSHOT]
19:06:11 	at org.springframework.cglib.proxy.MethodProxy.invokeSuper(MethodProxy.java:258) ~[spring-core-7.0.7.jar!/:7.0.7]
19:06:11 	at org.springframework.context.annotation.ConfigurationClassEnhancer$BeanMethodInterceptor.intercept(ConfigurationClassEnhancer.java:398) ~[spring-context-7.0.7.jar!/:7.0.7]
19:06:11 at com.football501.config.FlywayConfig$$SpringCGLIB$$0.flyway(<generated>) ~[!/:0.0.1-SNAPSHOT]
19:06:11 at java.base/jdk.internal.reflect.DirectMethodHandleAccessor.invoke(Unknown Source) ~[na:na]
19:06:11 at java.base/java.lang.reflect.Method.invoke(Unknown Source) ~[na:na]
19:06:11 at org.springframework.beans.factory.support.SimpleInstantiationStrategy.lambda$instantiate$0(SimpleInstantiationStrategy.java:155) ~[spring-beans-7.0.7.jar!/:7.0.7]
19:06:11 	... 28 common frames omitted
19:06:11 Caused by: org.postgresql.util.PSQLException: The server requested SCRAM-based authentication, but no password was provided.
19:06:11 	at org.postgresql.core.v3.ConnectionFactoryImpl.lambda$doAuthentication$5(ConnectionFactoryImpl.java:939) ~[postgresql-42.7.10.jar!/:42.7.10]
19:06:11 at org.postgresql.core.v3.AuthenticationPluginManager.withPassword(AuthenticationPluginManager.java:82) ~[postgresql-42.7.10.jar!/:42.7.10]
19:06:11 at org.postgresql.core.v3.ConnectionFactoryImpl.doAuthentication(ConnectionFactoryImpl.java:936) ~[postgresql-42.7.10.jar!/:42.7.10]
19:06:11 at org.postgresql.core.v3.ConnectionFactoryImpl.tryConnect(ConnectionFactoryImpl.java:234) ~[postgresql-42.7.10.jar!/:42.7.10]
19:06:11 at org.postgresql.core.v3.ConnectionFactoryImpl.openConnectionImpl(ConnectionFactoryImpl.java:289) ~[postgresql-42.7.10.jar!/:42.7.10]
19:06:11 at org.postgresql.core.ConnectionFactory.openConnection(ConnectionFactory.java:57) ~[postgresql-42.7.10.jar!/:42.7.10]
19:06:11 at org.postgresql.jdbc.PgConnection.<init>(PgConnection.java:290) ~[postgresql-42.7.10.jar!/:42.7.10]
19:06:11 at org.postgresql.Driver.makeConnection(Driver.java:448) ~[postgresql-42.7.10.jar!/:42.7.10]
19:06:11 at org.postgresql.Driver.connect(Driver.java:298) ~[postgresql-42.7.10.jar!/:42.7.10]
19:06:11 at com.zaxxer.hikari.util.DriverDataSource.getConnection(DriverDataSource.java:144) ~[HikariCP-7.0.2.jar!/:na]
19:06:11 at com.zaxxer.hikari.pool.PoolBase.newConnection(PoolBase.java:373) ~[HikariCP-7.0.2.jar!/:na]
19:06:11 at com.zaxxer.hikari.pool.PoolBase.newPoolEntry(PoolBase.java:210) ~[HikariCP-7.0.2.jar!/:na]
19:06:11 at com.zaxxer.hikari.pool.HikariPool.createPoolEntry(HikariPool.java:488) ~[HikariCP-7.0.2.jar!/:na]
19:06:11 at com.zaxxer.hikari.pool.HikariPool.checkFailFast(HikariPool.java:576) ~[HikariCP-7.0.2.jar!/:na]
19:06:11 at com.zaxxer.hikari.pool.HikariPool.<init>(HikariPool.java:97) ~[HikariCP-7.0.2.jar!/:na]
19:06:11 at com.zaxxer.hikari.HikariDataSource.getConnection(HikariDataSource.java:111) ~[HikariCP-7.0.2.jar!/:na]
19:06:11 at org.flywaydb.core.internal.jdbc.JdbcUtils.openConnection(JdbcUtils.java:65) ~[flyway-core-11.14.1.jar!/:na]
19:06:11 ... 40 common frames omitted
19:06:12 INFO Main child exited normally with code: 1
19:06:12 INFO Starting clean up.
19:06:12 [ 215.543405] reboot: Restarting system
19:06:19 2026-06-02T19:06:19.026977964 [01KT4THW37XPQ9YN21CWY20ZHP:main] Running Firecracker v1.14.4
19:06:19 2026-06-02T19:06:19.027174856 [01KT4THW37XPQ9YN21CWY20ZHP:main] Listening on API socket ("/fc.sock").
19:06:20 INFO Starting init (commit: ea887ee)...
19:06:20 INFO Preparing to run: `java -jar app.jar` as app
19:06:20 INFO [fly api proxy] listening at /.fly/api
19:06:21 Machine started in 2.339s
19:06:26 2026/06/02 19:06:26 INFO SSH listening listen*address=[fdaa:79:7c71:a7b:76c:55d9:35c6:2]:22
19:07:09 . \_\_\_\_ * ** \_ _
19:07:09 /\\ / _**'_ \_\_ _ _(_)_ \_\_ \_\_ _ \ \ \ \
19:07:09 ( ( )\_** | '_ | '_| | '_ \/ _` | \ \ \ \
19:07:09 \\/ \_**)| |_)| | | | | || (_| | ) ) ) )
19:07:09 ' |\_**\_| .**|_| |_|_| |_\__, | / / / /
19:07:09 =========|_|==============|**_/=/_/_/_/
19:07:09 :: Spring Boot :: (v4.0.6)
19:07:13 2026-06-02T19:07:13.201Z INFO 637 --- [football-501] [ main] com.football501.Football501Application : Starting Football501Application v0.0.1-SNAPSHOT using Java 25.0.3 with PID 637 (/app/app.jar started by app in /app)
19:07:13 2026-06-02T19:07:13.362Z INFO 637 --- [football-501] [ main] com.football501.Football501Application : No active profile set, falling back to 1 default profile: "default"
19:07:22 [PC01] instance refused connection. is your app listening on 0.0.0.0:8080? make sure it is not only listening on 127.0.0.1 (hint: look at your startup logs, servers often print the address they are listening on)
19:07:22 Starting machine
19:07:22 2026-06-02T19:07:22.597881519 [01KT4THFGSYD1CENQVWXXYMTN9:main] Running Firecracker v1.14.4
19:07:22 2026-06-02T19:07:22.598022249 [01KT4THFGSYD1CENQVWXXYMTN9:main] Listening on API socket ("/fc.sock").
19:07:23 INFO Starting init (commit: ea887ee)...
19:07:24 INFO Preparing to run: `java -jar app.jar` as app
19:07:24 INFO [fly api proxy] listening at /.fly/api
19:07:24 Machine started in 1.81s
19:07:24 machine started in 1.919982287s
19:07:29 waiting for machine to be reachable on 0.0.0.0:8080 (waited 5.447829543s so far)
19:07:30 2026/06/02 19:07:30 INFO SSH listening listen_address=[fdaa:79:7c71:a7b:7fb:b35f:3104:2]:22
19:07:33 [PM05] failed to connect to machine: gave up after 15 attempts (in 8.606706227s)
19:07:33 [PC01] instance refused connection. is your app listening on 0.0.0.0:8080? make sure it is not only listening on 127.0.0.1 (hint: look at your startup logs, servers often print the address they are listening on)
19:07:33 [PC01] instance refused connection. is your app listening on 0.0.0.0:8080? make sure it is not only listening on 127.0.0.1 (hint: look at your startup logs, servers often print the address they are listening on)
19:07:44 [PC01] instance refused connection. is your app listening on 0.0.0.0:8080? make sure it is not only listening on 127.0.0.1 (hint: look at your startup logs, servers often print the address they are listening on)
19:07:45 [PC01] instance refused connection. is your app listening on 0.0.0.0:8080? make sure it is not only listening on 127.0.0.1 (hint: look at your startup logs, servers often print the address they are listening on)
19:07:54 [PC01] instance refused connection. is your app listening on 0.0.0.0:8080? make sure it is not only listening on 127.0.0.1 (hint: look at your startup logs, servers often print the address they are listening on)
19:07:55 [PC01] instance refused connection. is your app listening on 0.0.0.0:8080? make sure it is not only listening on 127.0.0.1 (hint: look at your startup logs, servers often print the address they are listening on)
19:08:04 [PC01] instance refused connection. is your app listening on 0.0.0.0:8080? make sure it is not only listening on 127.0.0.1 (hint: look at your startup logs, servers often print the address they are listening on)
19:08:05 . \_\_** _ \_\_ _ \_
19:08:05 /\\ / **_'_ ** \_ _(_)_ \_\_ \_\_ _ \ \ \ \
19:08:05 ( ( )\_** | '_ | '_| | '_ \/ _` | \ \ \ \
19:08:05 \\/ \_**)| |_)| | | | | || (_| | ) ) ) )
19:08:05 ' |\_**\_| .**|_| |_|_| |_\__, | / / / /
19:08:05 =========|_|==============|_\_\_/=/_/_/_/
19:08:05 :: Spring Boot :: (v4.0.6)
19:08:05 [PC01] instance refused connection. is your app listening on 0.0.0.0:8080? make sure it is not only listening on 127.0.0.1 (hint: look at your startup logs, servers often print the address they are listening on)
19:08:08 [PR04] could not find a good candidate within 40 attempts at load balancing
19:08:09 2026-06-02T19:08:09.278Z INFO 637 --- [football-501] [ main] com.football501.Football501Application : Starting Football501Application v0.0.1-SNAPSHOT using Java 25.0.3 with PID 637 (/app/app.jar started by app in /app)
19:08:09 2026-06-02T19:08:09.434Z INFO 637 --- [football-501] [ main] com.football501.Football501Application : No active profile set, falling back to 1 default profile: "default"
19:08:17 2026-06-02T19:08:17.761Z INFO 637 --- [football-501] [ main] .s.d.r.c.RepositoryConfigurationDelegate : Bootstrapping Spring Data JPA repositories in DEFAULT mode.
19:08:21 [PC01] instance refused connection. is your app listening on 0.0.0.0:8080? make sure it is not only listening on 127.0.0.1 (hint: look at your startup logs, servers often print the address they are listening on)
19:08:22 [PC01] instance refused connection. is your app listening on 0.0.0.0:8080? make sure it is not only listening on 127.0.0.1 (hint: look at your startup logs, servers often print the address they are listening on)
19:08:22 2026-06-02T19:08:22.720Z INFO 637 --- [football-501] [ main] .s.d.r.c.RepositoryConfigurationDelegate : Finished Spring Data repository scanning in 4559 ms. Found 14 JPA repository interfaces.
19:08:33 [PC01] instance refused connection. is your app listening on 0.0.0.0:8080? make sure it is not only listening on 127.0.0.1 (hint: look at your startup logs, servers often print the address they are listening on)
19:08:34 [PC01] instance refused connection. is your app listening on 0.0.0.0:8080? make sure it is not only listening on 127.0.0.1 (hint: look at your startup logs, servers often print the address they are listening on)
19:08:45 [PC01] instance refused connection. is your app listening on 0.0.0.0:8080? make sure it is not only listening on 127.0.0.1 (hint: look at your startup logs, servers often print the address they are listening on)
19:08:45 [PR04] could not find a good candidate within 40 attempts at load balancing
19:08:55 2026-06-02T19:08:55.919Z INFO 637 --- [football-501] [ main] .s.d.r.c.RepositoryConfigurationDelegate : Bootstrapping Spring Data JPA repositories in DEFAULT mode.
19:08:59 2026-06-02T19:08:59.758Z INFO 637 --- [football-501] [ main] .s.d.r.c.RepositoryConfigurationDelegate : Finished Spring Data repository scanning in 3283 ms. Found 14 JPA repository interfaces.
19:09:12 2026-06-02T19:09:12.799Z INFO 637 --- [football-501] [ main] o.s.boot.tomcat.TomcatWebServer : Tomcat initialized with port 8080 (http)
19:09:13 2026-06-02T19:09:13.598Z INFO 637 --- [football-501] [ main] o.apache.catalina.core.StandardService : Starting service [Tomcat]
19:09:13 2026-06-02T19:09:13.677Z INFO 637 --- [football-501] [ main] o.apache.catalina.core.StandardEngine : Starting Servlet engine: [Apache Tomcat/11.0.21]
19:09:18 2026-06-02T19:09:18.397Z INFO 637 --- [football-501] [ main] b.w.c.s.WebApplicationContextInitializer : Root WebApplicationContext: initialization completed in 117679 ms
19:09:35 2026-06-02T19:09:35.519Z INFO 637 --- [football-501] [ main] o.s.boot.tomcat.TomcatWebServer : Tomcat initialized with port 8080 (http)
19:09:35 2026-06-02T19:09:35.994Z INFO 637 --- [football-501] [ main] o.apache.catalina.core.StandardService : Starting service [Tomcat]
19:09:35 2026-06-02T19:09:35.995Z INFO 637 --- [football-501] [ main] o.apache.catalina.core.StandardEngine : Starting Servlet engine: [Apache Tomcat/11.0.21]
19:09:39 2026-06-02T19:09:39.513Z INFO 637 --- [football-501] [ main] b.w.c.s.WebApplicationContextInitializer : Root WebApplicationContext: initialization completed in 83918 ms
19:09:53 2026-06-02T19:09:53.597Z INFO 637 --- [football-501] [ main] com.zaxxer.hikari.HikariDataSource : HikariPool-1 - Starting...
