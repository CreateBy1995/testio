package client;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;

public class Client {
    public void initClient(){
        try {

            // 创建客户端Socket，指定服务器地址和端口
            Socket socket = new Socket("127.0.0.1", 8080);
            System.out.println("init Client");
            // 通过控制台输入数据
            BufferedReader br = new BufferedReader(new InputStreamReader(System.in));
            // 获取向服务端发送数据的输出流
            PrintWriter write = new PrintWriter(socket.getOutputStream());
            // 获取服务端返回数据的输入流
            BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            String line;
            while (true) {
                line = br.readLine();
                // 将从系统标准输入读入的字符串输出到Server
                write.println(line);
                // 刷新缓冲区 向服务器发送数据
                write.flush();
                // quit表示会话结束
                if ("quit".equals(line)){
                    break;
                }
                System.out.println(in.readLine());

            }
            //关闭资源
            write.close(); // 关闭Socket输出流
            socket.close(); // 关闭Socket
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
