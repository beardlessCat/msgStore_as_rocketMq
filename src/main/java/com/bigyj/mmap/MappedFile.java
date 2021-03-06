package com.bigyj.mmap;

import com.bigyj.entity.SelectMappedBufferResult;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.util.concurrent.atomic.AtomicInteger;

public class MappedFile {
    /**
     * 当前文件指针，从0开始（内存映射中的文件指针）
     */
    protected final AtomicInteger wrotePosition = new AtomicInteger(0);
    /**
     * 当前文件的提交指针，如果开启 transientStorePoolEnable 则数据会存储在 TransientStorePool 中， 然后提交到内存映射 ByteBuffer中，
     * 再刷新到磁盘中
     */
    protected final AtomicInteger committedPosition = new AtomicInteger(0);


    private String fileName ;
    private int fileSize;
    private File file ;
    private FileChannel fileChannel;
    private long fileFromOffset;
    private MappedByteBuffer mappedByteBuffer;

    public int getFileSize() {
        return fileSize;
    }

    public void setFileSize(int fileSize) {
        this.fileSize = fileSize;
    }

    public AtomicInteger getWrotePosition() {
        return wrotePosition;
    }

    public long getFileFromOffset() {
        return fileFromOffset;
    }

    public void setFileFromOffset(long fileFromOffset) {
        this.fileFromOffset = fileFromOffset;
    }

    public MappedByteBuffer getMappedByteBuffer() {
        return mappedByteBuffer;
    }

    public void setMappedByteBuffer(MappedByteBuffer mappedByteBuffer) {
        this.mappedByteBuffer = mappedByteBuffer;
    }

    public MappedFile(final String fileName, final int fileSize) {
        init(fileName, fileSize);
    }

    private void init(String fileName, int fileSize) {
        this.fileName = fileName ;
        this.fileSize = fileSize ;
        this.file = new File(fileName);
        //文件名作为初始offSet
        this.fileFromOffset = Long.parseLong(this.file.getName());
        try {
            fileChannel = new RandomAccessFile(this.file,"rw").getChannel();
            this.mappedByteBuffer = this.fileChannel.map(FileChannel.MapMode.READ_WRITE, 0, fileSize);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    //消息写入
    public boolean appendMessage(byte[] message) {
        int currentOffSet = wrotePosition.get();
        //判断写入的文件是否大于大于剩余的文件大小
        if((this.fileSize-currentOffSet)>=message.length){
            //当前文件开始的offSet
            this.mappedByteBuffer.position(currentOffSet);
            //内容写入
            this.mappedByteBuffer.put(ByteBuffer.wrap(message));
            wrotePosition.addAndGet(message.length);
            return true;
        }
        return false;
    }

    //消息获取
    public SelectMappedBufferResult selectMappedBuffer(int pos) {
        ByteBuffer byteBuffer = this.mappedByteBuffer.slice();
        byteBuffer.position(pos);
        int readPosition = getReadPosition();
        int size = readPosition - pos;
        ByteBuffer byteBufferNew = byteBuffer.slice();
        byteBufferNew.limit(size);
        return new SelectMappedBufferResult(pos+this.fileFromOffset,size,byteBufferNew) ;
    }

    /**
     * 根据offset及size获取消息内容
     * @param pos
     * @param size
     * @return
     */
    public ByteBuffer selectMappedBuffer(int pos, int size) {
        ByteBuffer byteBuffer = this.mappedByteBuffer.slice();
        byteBuffer.position(pos);
        ByteBuffer byteBufferNew = byteBuffer.slice();
        byteBufferNew.limit(size);
        return byteBufferNew;
    }
    //判断文件是否已经写满
    public boolean isFull() {
        return false;
    }

    public int getReadPosition() {
        return committedPosition.get();
    }

    public void commit(int offSet){
        this.committedPosition.set(offSet);
    }

    public ByteBuffer sliceByteBuffer() {
        return this.mappedByteBuffer.slice();
    }
}
