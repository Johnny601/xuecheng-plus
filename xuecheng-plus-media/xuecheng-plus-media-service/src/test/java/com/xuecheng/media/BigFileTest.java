package com.xuecheng.media;

import org.apache.commons.codec.digest.DigestUtils;
import org.junit.jupiter.api.Test;

import java.io.*;
import java.lang.reflect.Array;
import java.util.*;

public class BigFileTest {
    @Test
    public void testChunk() throws IOException {
        File sourceFile = new File("C:\\Users\\Johnny\\Downloads\\WutheringWaves-overseas-setup-1.5.2.0.exe");
        // location where the chunk file will be stored
        String chunkFilePath = "C:\\Users\\Johnny\\Downloads\\chunk\\";
        // chunk size, 1Mb
        int chunkSize = 1024 * 1024 * 5;
        int chunkNum = (int) Math.ceil(sourceFile.length() * 1.0 / chunkSize);

        RandomAccessFile raf_r = new RandomAccessFile(sourceFile, "r");
        byte[] bytes = new byte[1024];
        for (int i = 0; i < chunkNum; i++) {
            File chunkFile = new File(chunkFilePath + i);
            RandomAccessFile raf_rw = new RandomAccessFile(chunkFile, "rw");
            int len = -1;
            while ((len = raf_r.read(bytes)) != -1) {
                raf_rw.write(bytes, 0, len);
                if (chunkFile.length() >= chunkSize) {
                    break;
                }
            }
            raf_rw.close();
        }
        raf_r.close();
    }

    @Test
    public void testMerge() throws IOException {
        // location of the chunk file
        File chunkFileFolder = new File("C:\\Users\\Johnny\\Downloads\\chunk\\");
        // location of the source file
        File sourceFile = new File("C:\\Users\\Johnny\\Downloads\\WutheringWaves-overseas-setup-1.5.2.0.exe");
        // location of the merged file
        File mergeFile = new File("C:\\Users\\Johnny\\Downloads\\WutheringWaves-overseas-setup-1.5.2.0_2.exe");

        // get all the chunk files
        File[] files = chunkFileFolder.listFiles();
        // sort the chunk files
        List<File> fileList = Arrays.asList(files);
        Collections.sort(fileList, new Comparator<File>() {
            @Override
            public int compare(File o1, File o2) {
                return Integer.parseInt(o1.getName()) - Integer.parseInt(o2.getName());
            }
        });

        RandomAccessFile raf_rw = new RandomAccessFile(mergeFile, "rw");
        byte[] bytes = new byte[1024];
        for (File file : fileList) {
            RandomAccessFile raf_r = new RandomAccessFile(file, "r");
            int len = -1;
            while ((len = raf_r.read(bytes)) != -1) {
                raf_rw.write(bytes, 0, len);
            }
            raf_r.close();
        }
        raf_rw.close();

        // verify the integrity of the merged file
        FileInputStream fileInputStream_merge = new FileInputStream(mergeFile);
        FileInputStream fileInputStream_source = new FileInputStream(sourceFile);
        String md5_merge = DigestUtils.md5Hex(fileInputStream_merge);
        String md5_source = DigestUtils.md5Hex(fileInputStream_source);
        if (md5_merge.equals(md5_source)) {
            System.out.println("The merged file is correct.");
        } else {
            System.out.println("The merged file is incorrect.");
        }

    }
}
