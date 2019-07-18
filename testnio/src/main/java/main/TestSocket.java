package main;

import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;

public class TestSocket {
    @Test
    public void Client() throws IOException {
        // 开启一个客户端通道
        SocketChannel socketChannel = SocketChannel.open(new InetSocketAddress("127.0.0.1",80)) ;
        // 文件路径
        File file = new File("");
        String filePath = file.getCanonicalPath();
        // 以读的模式开启一个文件通道，将此文件发送到服务端
        FileChannel fileChannel = FileChannel.open(Paths.get(filePath+"//testChannel.txt"), StandardOpenOption.READ) ;
        // 创建一个存放48个字节的缓冲区
        ByteBuffer byteBuffer = ByteBuffer.allocate(48) ;
        while (fileChannel.read(byteBuffer)!=-1){
            // 将缓冲区切换成读取数据的模式
            byteBuffer.flip() ;
            // 将缓冲区的数据发送给服务端
            socketChannel.write(byteBuffer) ;
            // 重置缓冲区状态
            byteBuffer.clear() ;
        }
        // 通知服务器数据传输完毕
        socketChannel.shutdownOutput() ;
        // 获取服务端响应
        int len = 0 ;
        while ((len=socketChannel.read(byteBuffer))!=-1){
            // 切换成读取模式
            byteBuffer.flip() ;
            // 打印服务端响应的信息
            System.out.println("received msg --> "+new String(byteBuffer.array(),0,len));
        }
        // 关闭资源
        socketChannel.close();
        fileChannel.close();
    }
    @Test
    public void blockingServer() throws IOException {
        // 开启一个服务端通道
        ServerSocketChannel serverSocketChannel = ServerSocketChannel.open() ;
        // 绑定监听端口
        serverSocketChannel.bind(new InetSocketAddress(80)) ;
        System.out.println("Blocking Server started on 80");
        // 文件路径
        File file = new File("");
        String filePath = file.getCanonicalPath();
        // 以读写的模式开启一个文件通道，将客户端传来的数据写入到文件
        FileChannel fileChannel = FileChannel.open(Paths.get(filePath+"//testChannelCopy.txt"), StandardOpenOption.READ,StandardOpenOption.WRITE) ;
        // 当有客户端发来数据时，开启一个通道接收
        SocketChannel socketChannel = serverSocketChannel.accept() ;
        // 创建一个24字节的缓冲区  此时我客户端创建的缓冲区是48字节
        // 也就是说客户端发送一次数据  服务端需要分两次读取，当48个字节读取完之后，客户端还未继续发送数据时（比如在客户端要发送数据的代码处打个断点就能看出效果）
        // 那么这个时候socketChannel.read(byteBuffer)这个方法会一直阻塞 ，等到客户端再次发送了48个字节过来
        // socketChannel.read(byteBuffer)方法会接收到数据 同样还是分成两次接收
        // 接收完48个字节之后 继续阻塞，直到客户端执行socketChannel.shutdownOutput()通知服务端传输完毕
        // 或者客户端执行socketChannel.close(); 也就是通道关闭
        // 则这个时候socketChannel.read(byteBuffer)会返回-1 才会跳出下面这个循环 否则会一直阻塞
        ByteBuffer byteBuffer = ByteBuffer.allocate(24) ;
        // 读取客户端发来的数据
        while (socketChannel.read(byteBuffer)!=-1){
//        socketChannel.read(byteBuffer);
            // 切换成读取数据模式
            byteBuffer.flip();
            // 将缓冲区的数据写入到文件
            fileChannel.write(byteBuffer) ;
            // 重置缓冲区状态
            byteBuffer.clear() ;
        }
        // 向客户端返回消息
        String msg = "ok" ;
        // 将字符串转成字节数组存入缓冲区
        byteBuffer.put(msg.getBytes()) ;
        // 切换读取模式
        byteBuffer.flip() ;
        // 写入客户端通道
        socketChannel.write(byteBuffer) ;
        byteBuffer.clear() ;
        // 关闭资源
        serverSocketChannel.close();
        socketChannel.close();
        fileChannel.close();
    }
}
