package main;


import org.junit.Test;

import java.nio.ByteBuffer;

public class TestBuffer {
    // 1、分配一个指定大小的缓冲区
    ByteBuffer byteBuffer = ByteBuffer.allocate(1024) ;
    @Test
    public void test(){
        System.out.println("存放数据前position --- "+ byteBuffer.position());
        System.out.println("存放数据前limit --- "+ byteBuffer.limit());
        byte []bytes = new byte[]{1,2,3,4,5} ;
        // 存入数据
        byteBuffer.put(bytes) ;
        System.out.println("存放入5个字节后position --- "+ byteBuffer.position());
        System.out.println("存放入5个字节后limit --- "+ byteBuffer.limit());
        // flip 读取数据模式 将limit的位置执行有效数据的位置  此处也就是5

        byteBuffer.flip() ;
        System.out.println("flip后get之前position --- "+ byteBuffer.position());
        System.out.println("flip后get之前limit --- "+ byteBuffer.limit());
        for (int i = 0; i < byteBuffer.limit() ; i++) {
            // 每次get方法执行后 position的偏移量会自动+1
            byteBuffer.get();
        }
        System.out.println("get后position --- "+ byteBuffer.position());
        System.out.println("get后limit --- "+ byteBuffer.limit());
    }
}
