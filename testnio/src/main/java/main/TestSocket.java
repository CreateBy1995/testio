package main;

import org.junit.Test;
import sun.nio.ch.SelectorProviderImpl;

import java.io.File;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.*;
import java.nio.channels.spi.SelectorProvider;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.Iterator;
import java.util.Scanner;
import java.util.Set;

public class TestSocket {
    @Test
    public void client() throws IOException {
        // 开启一个客户端通道
        SocketChannel socketChannel = SocketChannel.open(new InetSocketAddress("127.0.0.1",90)) ;
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
    @Test
    public void nioServer() throws IOException {
        // 开启服务端通道 并且绑定端口
        ServerSocketChannel serverSocketChannel = ServerSocketChannel.open() ;
        // 将通道设置为非阻塞
        // 设置成非阻塞后  serverSocketChannel.accept() 这个方法会立即返回 而不会阻塞
        // 同理可得 如果 socketChannel 也是设置成非阻塞后 socketChannel.read 尽管没有读取到数据也会立即返回
        // 这就是非阻塞式IO 但其实是将阻塞的时机放到 select方法中
        // (selectNow方法是立即返回 select(timeout)是超时返回  但不管什么时候返回 还是需要一直轮询)
        // 只是说这样和BIO的区别就在于 NIO 可以通过select去获取那些socket准备就绪 可以直接读写
        // 而不是说像BIO那样无法知道哪个socket准备就绪 只能一直阻塞等待
        serverSocketChannel.configureBlocking(false) ;
        serverSocketChannel.bind(new InetSocketAddress(90)) ;
        System.out.println("server started on 90 and waiting for client connection");
        // 开启一个选择器
        // 两种不同操作系统实现 NIO 的方式， selector 和epoll
        // 在windows中select()方法最终调用的是poll0()这个本地方法
        // 因为是在windows 系统下 所以 此处的默认实现是WindowsSelectorImpl
        // epoll是只有在linux下才支持  所以还需要在配置这个参数
        // Djava.nio.channels.spi.SelectorProvider=sun.nio.ch.EPollSelectorProvider
        Selector selector = Selector.open() ;
        // 将服务端通道注册到选择器上  并且让选择器监听 OP_ACCEPT 事件 也就是客户端连接事件
        // 该方法会返回一个 SelectionKey 每一个 SelectionKey都是唯一的 里面记录了SelectionKey是哪一个通道注册到哪一个选择器上
        // 并且通过index 记录这是选择器上的第几个 SelectionKey
        serverSocketChannel.register(selector, SelectionKey.OP_ACCEPT) ;
        // 轮询获取选择器上的 准备就绪 的事件
        // select 返回的是准备就绪的 SelectionKey的数量 例如有两个客户端可读 就返回2
        // selector有两个属性 一个叫publicKeys存放所有注册到选择器上的key 一个叫publicSelectedKeys 存放所有准备完毕的key

        // select 是水平触发的(一直触发) 应用程序如果没有完成对一个已经就绪的文件描述符进行IO操作，那么之后每次select调用还是会将这些文件描述符通知进程
        // 也就是说如果 我检测到一个socket有可读事件  但是并没有对他进行IO处理 那么在下次select中还是会一直返回
        // 对应水平触发的就是边沿触发 也就是只触发一次 就是说不管你有没有IO处理 我只返回一次  也就是epoll中 ET(边沿触发)和LT(水平触发)
        while (selector.select() > 0){
            // 获取所有准备就绪的事件
            // selector.selectedKeys()方法返回的是 publicSelectedKeys  表示所有已经准备就绪的的
            Iterator<SelectionKey> iterator = selector.selectedKeys().iterator() ;
            while (iterator.hasNext()){
                SelectionKey selectionKey = iterator.next();
                // 如果准备好的事件是 客户端连接事件
                if(selectionKey.isAcceptable()){
                    // 获取客户端连接通道 并且将其注册进选择器 同时监听多种事件
                    SocketChannel socketChannel = serverSocketChannel.accept() ;
                    System.out.println("client -- "+socketChannel.getRemoteAddress()+" -- connect");
                    // 例如此处我监听了读事件  那么select 去遍历套接字列表的时候就会去检查这个socket的读事件准备好了没有
                    // 如果好了就把它加到事件列表中 而不会去检查他的写事件
                    int keys = SelectionKey.OP_READ; // SelectionKey.OP_READ|SelectionKey.OP_WRITE; 监听多种事件
                    socketChannel.configureBlocking(false) ;
                    socketChannel.register(selector,keys) ;
                    // 如果准备好的事件 是可读事件
                }else if(selectionKey.isReadable()){
                    // 获取通道并打印读取到的数据
                    SocketChannel socketChannel = (SocketChannel) selectionKey.channel();
                    ByteBuffer byteBuffer = ByteBuffer.allocate(1024) ;
                    // 此处的socketChannel.read方法是不会阻塞的
                    // 也就是说尽管读取到的数据为空  该方法也会返回
                    // 比如在客户端要发送数据的时候打个断点就能看到效果
                    int i ;
                    while ((i=socketChannel.read(byteBuffer)) > 0){
                        byteBuffer.flip();
                        String data = new String(byteBuffer.array(),0,byteBuffer.limit()) ;
                        System.out.println("read the client "+socketChannel.getRemoteAddress()+" data : "+data);
                        byteBuffer.clear();
                    }
                    // 当客户端主动切断连接的时候，服务端 Socket 的读事件（FD_READ）仍然起作用，也就是说，服务端 Socket 的状态仍然是有东西可读
                    // socketChannel.read(buffer) 是有返回值的，这种情况下返回值是 -1，所以如果 read 方法返回的是 -1，就可以关闭和这个客户端的连接了。
                    // 如果不关闭 则selector.select()每次都会检测到这个关闭的客户端的 读事件 仍然可用  但是socketChannel.read(byteBuffer)始终为-1
                    // 导致服务器一直进行轮询
                    // 这点与在linux下的epoll空轮询bug有点类似  最终都会导致CPU占用率上升
                    if (i == -1){
                        socketChannel.close();
                    }
                }
                // 每次处理完SelectionKey  都将其清除  否则会一直获取到处理过的SelectionKey
                // 因为publicSelectedKeys是一个set select()方法底层会将准备完毕的key一直塞到这个set中
                // 比如有一个客户端连接事件已经处理完毕了  而没有将其清除掉 然后又有一个客户端连接事件触发 那么这时候publicSelectedKeys会在增加一个准备完毕的key
                // 此时在调用serverSocketChannel.accept()方法的时候 由于是非阻塞模式
                // 尽管没有客户端连接也会立即返回null，那么后续的代码就会出错
                iterator.remove();
            }
        }
    }
}
