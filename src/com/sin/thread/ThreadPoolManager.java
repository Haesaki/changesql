package com.sin.thread;

import com.sin.entity.DatabaseEntity;
import com.sin.entity.TableEntity;
import com.sin.service.DBConnection;
import com.sin.service.DBManager;

import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.Future;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

public class ThreadPoolManager {
    // 线程池的参数
    private static final int CORE_POOL_SIZE = 8;
    private static final int MAX_POOL_SIZE = 8192;
    private static final int QUEUE_CAPACITY = 8192;
    private static final long KEEP_ALIVE_TIME = 100 * 60;

    // 由主线程传入的database的相关信息
    private DBManager dbManager;
    // 新建的连接池
    DBConnection dbconn;

    public ThreadPoolManager(DBManager dbManager, DBConnection dbconn) {
        this.dbManager = dbManager;
        this.dbconn = dbconn;
        assert dbconn != null;
    }

    // 利用线程池去管理多线程任务
    public void runInsertTaskByTableHash() throws InterruptedException {
        // 多线程启动
        ThreadPoolExecutor poolExecutor = new ThreadPoolExecutor(
                CORE_POOL_SIZE,
                MAX_POOL_SIZE,
                KEEP_ALIVE_TIME,
                TimeUnit.SECONDS,
                new ArrayBlockingQueue<>(QUEUE_CAPACITY),
                new ThreadPoolExecutor.CallerRunsPolicy()
        );
        int runTasks = 0;
        TableEntity[] tableEntities = new TableEntity[CORE_POOL_SIZE];
        Future<Boolean>[] result = new Future[CORE_POOL_SIZE];
        for (DatabaseEntity databaseEntity : dbManager.dbList) {
            for(TableEntity tableEntity : databaseEntity.tableEntityMap.values()){
                if (runTasks < CORE_POOL_SIZE) {
                    tableEntities[runTasks] = tableEntity;
                    result[runTasks++] = poolExecutor.submit(new InsertByTableHash(tableEntity, databaseEntity.name, dbconn));
                } else {
                    int finishPos = -1;
                    while (finishPos == -1) {
                        for (int i = 0; i < CORE_POOL_SIZE; i++) {
                            if (result[i].isCancelled()) {
                                result[i] = poolExecutor.submit(new InsertByTableHash(tableEntity, databaseEntity.name, dbconn));
                            }
                            if (result[i].isDone()) {
                                finishPos = i;
                                break;
                            }
                        }
                        // 休眠一会 避免一直循环，占用CPU
                        Thread.sleep(1000);
                    }
                    tableEntities[finishPos] = tableEntity;
                    result[finishPos] = poolExecutor.submit(new InsertByTableHash(tableEntity, databaseEntity.name,dbconn));
                }
            }
        }
        poolExecutor.shutdown();
        while (!poolExecutor.awaitTermination(10, TimeUnit.SECONDS)) {
            System.out.println("Wait for poolExecutor finished");
        }
    }

    // 利用线程池去管理多线程任务
    public void runInsertTaskByTable() throws InterruptedException {
        // 多线程启动
        ThreadPoolExecutor poolExecutor = new ThreadPoolExecutor(
                CORE_POOL_SIZE,
                MAX_POOL_SIZE,
                KEEP_ALIVE_TIME,
                TimeUnit.SECONDS,
                new ArrayBlockingQueue<>(QUEUE_CAPACITY),
                new ThreadPoolExecutor.CallerRunsPolicy()
        );
        int runTasks = 0;
        TableEntity[] tableEntities = new TableEntity[CORE_POOL_SIZE];
        Future<Boolean>[] result = new Future[CORE_POOL_SIZE];
        for (DatabaseEntity databaseEntity : dbManager.dbList) {
            for(TableEntity tableEntity : databaseEntity.tableEntityMap.values()){
                if (runTasks < CORE_POOL_SIZE) {
                    tableEntities[runTasks] = tableEntity;
                    result[runTasks++] = poolExecutor.submit(new InsertByTable(tableEntity, databaseEntity.name, dbconn));
                } else {
                    int finishPos = -1;
                    while (finishPos == -1) {
                        for (int i = 0; i < CORE_POOL_SIZE; i++) {
                            if (result[i].isCancelled()) {
                                result[i] = poolExecutor.submit(new InsertByTable(tableEntity, databaseEntity.name, dbconn));
                            }
                            if (result[i].isDone()) {
                                finishPos = i;
                                break;
                            }
                        }
                        // 休眠一会 避免一直循环，占用CPU
                        Thread.sleep(1000);
                    }
                    tableEntities[finishPos] = tableEntity;
                    result[finishPos] = poolExecutor.submit(new InsertByTable(tableEntity, databaseEntity.name,dbconn));
                }
            }
        }
        poolExecutor.shutdown();
        while (!poolExecutor.awaitTermination(10, TimeUnit.SECONDS)) {
            System.out.println("Wait for poolExecutor finished");
        }
    }

    // 利用线程池去管理多线程任务
    public void runInsertTaskByDB() throws InterruptedException {
        // 多线程启动
        ThreadPoolExecutor poolExecutor = new ThreadPoolExecutor(
                CORE_POOL_SIZE,
                MAX_POOL_SIZE,
                KEEP_ALIVE_TIME,
                TimeUnit.SECONDS,
                new ArrayBlockingQueue<>(QUEUE_CAPACITY),
                new ThreadPoolExecutor.CallerRunsPolicy()
        );
        int runTasks = 0;
        DatabaseEntity[] dbEntity = new DatabaseEntity[CORE_POOL_SIZE];
        Future<Boolean>[] result = new Future[CORE_POOL_SIZE];
        for (DatabaseEntity databaseEntity : dbManager.dbList) {
            if (runTasks < CORE_POOL_SIZE) {
                dbEntity[runTasks] = databaseEntity;
                result[runTasks++] = poolExecutor.submit(new InsertByDatabase(databaseEntity, dbconn));
            } else {
                int finishPos = -1;
                while (finishPos == -1) {
                    for (int i = 0; i < CORE_POOL_SIZE; i++) {
                        if (result[i].isCancelled()) {
                            result[i] = poolExecutor.submit(new InsertByDatabase(dbEntity[i], dbconn));
                        }
                        if (result[i].isDone()) {
                            finishPos = i;
                            break;
                        }
                    }
                    // 休眠一会 避免一直循环，占用CPU
                    Thread.sleep(1000);
                }
                dbEntity[finishPos] = databaseEntity;
                result[finishPos] = poolExecutor.submit(new InsertByDatabase(databaseEntity, dbconn));
//                for (int i = 0; i < CORE_POOL_SIZE; i++) {
//                    if (result[i].isCancelled()) {
//                        result[i] = poolExecutor.submit(new InsertByDatabase(dbEntity[i], dbconn));
//                    }
//                    if (result[i].isDone()) {
//                        finishPos = i;
//                        break;
//                    }
//                }
//                if (finishPos != -1) {
//                    dbEntity[finishPos] = databaseEntity;
//                    result[finishPos] = poolExecutor.submit(new InsertByDatabase(databaseEntity, dbconn));
//                }
            }
        }
        poolExecutor.shutdown();
        while (!poolExecutor.awaitTermination(10, TimeUnit.SECONDS)) {
            System.out.println("Wait for poolExecutor finished");
        }
    }
}
