package main;

import org.junit.Test;

import java.io.*;
import java.nio.ByteBuffer;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;

public class TestChannel {


    /**
     * 利用通道进行文件复制,这种方式使用的也是直接缓冲区
     */
    @Test
    public void testChannelV4() throws IOException {
        // 文件路径
        File file = new File("");
        String filePath = file.getCanonicalPath();
        FileChannel inputChannel = FileChannel.open(Paths.get(filePath+"//testChannel.txt"), StandardOpenOption.READ) ;
        FileChannel outputChannel = FileChannel.open(Paths.get(filePath+"//testChannelCopy.txt"), StandardOpenOption.WRITE,StandardOpenOption.READ) ;
        // 3个参数分别为  传输的起始偏移量，传输的终止偏移量，传输的目的地
        inputChannel.transferTo(0,inputChannel.size(),outputChannel) ;
    }

    /**
     * 开启一个读写通道
     * @throws IOException
     */
    @Test
    public void testChannelV3() throws IOException {
        // 文件路径
        File file = new File("");
        String filePath = file.getCanonicalPath();
        // 以读写的模式开启一个通道，也就是这个通道是同时支持读和写两种操作
        FileChannel fileChannel = FileChannel.open(Paths.get(filePath+"//testChannel.txt"), StandardOpenOption.WRITE,StandardOpenOption.READ) ;
        // 创建一个缓冲区
        ByteBuffer byteBuffer = ByteBuffer.allocate(1024) ;
        // 将文件的内容读取到缓冲区中
        fileChannel.read(byteBuffer) ;
        // 将缓冲区切换成读取模式
        byteBuffer.flip() ;
        for (int i = 0; i <byteBuffer.limit(); i++) {
            // 输出文件内容
            System.out.println((char)byteBuffer.get());
        }
        // 清空缓冲区状态
        byteBuffer.clear() ;
        // 将数据存入缓冲区
        String content = "a" ;
        byteBuffer.put(content.getBytes()) ;
        // 切换成读取模式
        byteBuffer.flip();
        // 将数据写入到文件
        fileChannel.write(byteBuffer) ;
    }



    /**
     * 使用直接缓冲区（内存映射文件）复制文件
     */
    @Test
    public void testChannelV2(){
        File file = new File("");
        FileChannel inputChannel = null ;
        FileChannel outputChannel = null ;
        // 项目路径
        try {
            String filePath = file.getCanonicalPath();
            // 因为此处使用的是直接缓冲区 所以不必通过一个流来开启通道
            // 以读取的模式打开一个通道
            inputChannel = FileChannel.open(Paths.get(filePath+"//testChannel.txt"), StandardOpenOption.READ) ;
            // 以读和写的模式打开一个通道
            // 如果只用写的方式打开
            // 则在 outputChannel.map(FileChannel.MapMode.READ_WRITE,0,inputChannel.size()) 这个阶段会报错
            // 因为在这里我是以 读写模式去打开一个缓冲区 而在此处获取通道时，通道也要支持读和写的模式
            // 也就是说此时这个通道是同时支持写和读的
            outputChannel = FileChannel.open(Paths.get(filePath+"//testChannelCopy.txt"), StandardOpenOption.WRITE,StandardOpenOption.READ) ;
            // 此处获取一个只读的Buffer 大小为 通道的大小 此处通道的大小也就是打开的文件的内容长度
            // 获取的原理同 ByteBuffer.allocateDirect(size)方式一样
            // MappedByteBuffer 为 ByteBuffer的子类
            MappedByteBuffer inputMapBuffer = inputChannel.map(FileChannel.MapMode.READ_ONLY,0,inputChannel.size()) ;
            // 获取一个读写的Buffer 因为是复制  所以大小还是要以 inChannel为例
            MappedByteBuffer outputMapBuffer = outputChannel.map(FileChannel.MapMode.READ_WRITE,0,inputChannel.size()) ;
//            // 直接对缓冲区进行读写 不用借助于通道，因为此处是直接操作物理内存
            byte []bytes = new byte[outputMapBuffer.capacity()] ;
            // 读取缓冲区的数据
            inputMapBuffer.get(bytes) ;
            // 写入数据
            outputMapBuffer.put(bytes) ;
        } catch (IOException e) {
            e.printStackTrace();
        }finally {
            // 关闭资源
            try {
                if (outputChannel != null){
                    outputChannel.close();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
            try {
                if (inputChannel != null){
                    inputChannel.close();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

    }

    /**
     * 使用非直接缓冲区复制文件
     */
    @Test
    public void testChannel(){
        FileInputStream fileInputStream = null ;
        FileOutputStream fileOutputStream = null ;
        FileChannel inputChannel = null ;
        FileChannel outputChannel = null ;
        try {
            File file = new File("");
            // 项目路径
            String filePath = file.getCanonicalPath();
            // 获取通道
            fileInputStream = new FileInputStream(filePath+"//testChannel.txt") ;
            fileOutputStream = new FileOutputStream(filePath+"//testChannelCopy.txt") ;
            inputChannel = fileInputStream.getChannel();
            outputChannel = fileOutputStream.getChannel() ;
            // 创建一个缓冲区 负责在通道间进行传输
            ByteBuffer byteBuffer = ByteBuffer.allocate(1024) ;
            // 使用input通道将数据从文件读取到缓冲区中
            while ((inputChannel.read(byteBuffer)) != -1){
                // 将缓冲区切换为读取模式
                byteBuffer.flip() ;
                // 使用output通道读取缓冲区的数据并写入文件
                outputChannel.write(byteBuffer) ;
                // 每次写入后都要清空缓冲区状态
                byteBuffer.clear() ;
            }
        }  catch (IOException e) {
            e.printStackTrace();
        }finally {
            // 关闭资源
            try {
                if (outputChannel != null){
                    outputChannel.close();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
            try {
                if (inputChannel != null){
                    inputChannel.close();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
            try {
                if (fileInputStream != null){
                    fileInputStream.close();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
            try {
                if (fileOutputStream != null){
                    fileOutputStream.close();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}
