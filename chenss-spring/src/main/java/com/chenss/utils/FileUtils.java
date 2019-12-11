package com.chenss.utils;

import com.chenss.event.MyEvent;
import org.springframework.context.ApplicationContext;

import java.io.*;

public class FileUtils {
    public static final int STANDARD_BYTE_SIZE = 5000;

    public static void fileUploadWrite(FileInputStream fis, FileOutputStream fos, ApplicationContext context) {
        BufferedInputStream bis = new BufferedInputStream(fis);
        BufferedOutputStream bos = new BufferedOutputStream(fos);

        try {
            int fileSize = bis.available();
            int readSize = 0;
            byte[] bytes = new byte[STANDARD_BYTE_SIZE];
            boolean runFlag=true;
            while (runFlag) {
                if (fileSize <= STANDARD_BYTE_SIZE) {
                    bytes = new byte[fileSize];
                    bis.read(bytes);
                    bos.write(bytes);
                    readSize=fileSize;
                    runFlag=false;
                } else if (readSize + STANDARD_BYTE_SIZE >= fileSize) {
                    bytes = new byte[fileSize - readSize];
                    bis.read(bytes);
                    bos.write(bytes);
                    readSize=fileSize;
                    runFlag=false;
                } else {
                    bis.read(bytes);
                    bos.write(bytes);
                    readSize += STANDARD_BYTE_SIZE;
                }

				context.publishEvent(new MyEvent("文件上传", fileSize,readSize));
            }
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            try {
                bis.close();
                bos.close();
                fis.close();
                fos.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}
