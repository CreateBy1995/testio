package server;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.*;

public class Server {
    public void initServer(int port){
        try {
            ServerSocket serverSocket = new ServerSocket(port) ;
            System.out.println("Server started on "+port);
            // 线程池中空闲线程数
            int corePoolSize = 5 ;
            // 线程池中最大线程数
            int maximumPoolSize = 10 ;
            // 多余的空闲线程的存活世间     也就是除corePoolSize外的的空闲线程
            long keepAliveTime = 0L ;
            // 线程的任务队列长度 如果直接使用Executors.newFixedThreadPool 默认的长度是无限的，这样可能造成OOM
            LinkedBlockingQueue linkedBlockingQueue = new LinkedBlockingQueue(100) ;

            ExecutorService executorService = new ThreadPoolExecutor(corePoolSize,maximumPoolSize,keepAliveTime, TimeUnit.MILLISECONDS,linkedBlockingQueue) ;
            while (true){
                // 调用accept()方法开始监听，等待客户端的连接
                // 使用accept()阻塞等待客户请求，有一个客户端连接就开启一个线程去处理
                Socket socket = serverSocket.accept() ;
                executorService.execute(new SocketHandleThread(socket));
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
