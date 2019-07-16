package server;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;

/**
 * 处理客户端socket的线程
 */
public class SocketHandleThread implements Runnable{
    private Socket socket ;

    /**
     * 初始化一个socket
     * @param socket
     */
    public SocketHandleThread(Socket socket){
        this.socket = socket ;
    }
    public void run() {
        try {
            // 获取socket输入流 用于接受客户端发来的数据
            BufferedReader in=new BufferedReader(new InputStreamReader(socket.getInputStream()));
            // 获取socket输出流，用于向客户端返回数据
            PrintWriter writer=new PrintWriter(socket.getOutputStream());

            String line;
            while(true){
                // 读取客户端传来的数据 如果尚未接收到数据 则此方法会阻塞
                line=in.readLine();
                // 如果发送的是quit 则结束本次回话
                if(("quit").equals(line)){
                    break;
                }
                System.out.println("收到客户端--"+socket.getInetAddress()+":"+socket.getPort()+"  发来的数据 --"+line);
                // 服务端返回信息
                writer.println("服务端返回数据"+line);
                // 刷新缓冲区  向客户端发送该数据
                writer.flush();
            }
            // 关闭资源
            writer.close();
            in.close();
            socket.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
