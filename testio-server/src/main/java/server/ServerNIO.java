package server;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.nio.ByteBuffer;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;

public class ServerNIO {
    // 多路复用器 用于注册channel
    private Selector selector ;
    // 缓存从客户端读取到的流
    private ByteBuffer readBuffer = ByteBuffer.allocate(1024) ;
    // 缓存要写入到客户端的流
    private ByteBuffer writeBuffer = ByteBuffer.allocate(1024) ;
    public void initServer(int port){
        System.out.println("NIOServer started on "+port);
        try {
            // 开启多路复用器
            this.selector = Selector.open() ;
            // 开启服务器通道 类似于BIO中的 ServerSocket
            ServerSocketChannel serverSocketChannel = ServerSocketChannel.open() ;
            // 绑定端口 类似于BIO中的 ServerSocket serverSocket = new ServerSocket(port)
            serverSocketChannel.bind(new InetSocketAddress(port));
            // 设置为非阻塞
            serverSocketChannel.configureBlocking(false) ;

        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
