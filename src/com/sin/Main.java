package com.sin;

import com.beust.jcommander.JCommander;
import com.beust.jcommander.Parameter;
import com.sin.entity.DatabaseEntity;
import com.sin.service.DBConnection;
import com.sin.service.DBManager;

import javax.swing.text.html.HTMLEditorKit;
import java.io.File;
import java.io.IOException;
import java.net.DatagramPacket;
import java.sql.Connection;
import java.sql.DriverManager;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import com.sin.thread.InsertByDatabase;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;

public class Main {
    @Parameter(names = {"--data_path"}, description = "dir path of source data")
    public String DataPath = "";

    @Parameter(names = {"--dst_ip"}, description = "ip of dst database address")
    public String DstIP = "";

    @Parameter(names = {"--dst_port"}, description = "port of dst database address")
    public String DstPort = "";

    @Parameter(names = {"--dst_user"}, description = "user name of dst database")
    public String DstUser = "";

    @Parameter(names = {"--dst_password"}, description = "password of dst database")
    public String DstPassword = "";

    public static final int DEFAULT_THREADS = 4;
    // 线程池的参数
    private static final int CORE_POOL_SIZE = 4;
    private static final int MAX_POOL_SIZE = 8192;
    private static final int QUEUE_CAPACITY = 8192;
    private static final long KEEP_ALIVE_TIME = 100 * 60;


    public static void main(String[] args) throws IOException, InterruptedException {

        Main main = new Main();
        //parse cmd
        JCommander jc = JCommander.newBuilder().addObject(main).build();
        jc.parse(args);

        DBConnection dbconn = new DBConnection(main.DstIP, main.DstPort, main.DstUser, main.DstPassword);
        Connection conn = dbconn.connectDB();   // 其实后面可以到到多线程在建立连接

        DBManager dbManager = new DBManager(main.DataPath); // 获取所有的数据库信息
        dbManager.createDB(conn); // 创建数据表和数据库

        // 多线程启动
        ThreadPoolExecutor poolExecutor = new ThreadPoolExecutor(
                CORE_POOL_SIZE,
                MAX_POOL_SIZE,
                KEEP_ALIVE_TIME,
                TimeUnit.SECONDS,
                new ArrayBlockingQueue<>(QUEUE_CAPACITY),
                new ThreadPoolExecutor.CallerRunsPolicy()
        );
        for(DatabaseEntity databaseEntity : dbManager.dbList){
            poolExecutor.execute(new InsertByDatabase(databaseEntity));
        }
        poolExecutor.shutdown();
        poolExecutor.shutdownNow();
        while(!poolExecutor.awaitTermination(10, TimeUnit.SECONDS)){
            System.out.println("Await for thread pool to finish!");
        }
        System.out.println("Thread Pool Finish");
    }
    // tdsqlshard-gzh17qjo.sql.tencentcdb.com:135 实例地址
}
