package com.xuecheng.media;

import com.j256.simplemagic.ContentInfo;
import com.j256.simplemagic.ContentInfoUtil;
import io.minio.*;
import io.minio.errors.*;
import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.io.IOUtils;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import sun.net.www.content.image.png;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.List;

public class MinioTest {
    MinioClient minioClient =
            MinioClient.builder()
                    .endpoint("http://192.168.101.65:9000")
                    .credentials("minioadmin", "minioadmin")
                    .build();

    @Test
    public void testUpload() throws IOException, ServerException, InsufficientDataException, ErrorResponseException, NoSuchAlgorithmException, InvalidKeyException, InvalidResponseException, XmlParserException, InternalException {
        ContentInfo extensionMatch = ContentInfoUtil.findExtensionMatch(".rar");
        String mimeType = MediaType.APPLICATION_OCTET_STREAM_VALUE;
        if (extensionMatch != null) {
            mimeType = extensionMatch.getMimeType();
        }

        UploadObjectArgs uploadObjectArgs = UploadObjectArgs.builder()
                .bucket("testbucket")
                .filename("C:\\Users\\Johnny\\Downloads\\RJ01102532.rar")
                .object("Surah")
                .contentType(mimeType)
                .build();

        minioClient.uploadObject(uploadObjectArgs);
    }

    @Test
    public void testDelete() throws ServerException, InsufficientDataException, ErrorResponseException, IOException, NoSuchAlgorithmException, InvalidKeyException, InvalidResponseException, XmlParserException, InternalException {
        RemoveObjectArgs removeObjectArgs = RemoveObjectArgs.builder()
                .bucket("testbucket")
                .object("Surah")
                .build();

        minioClient.removeObject(removeObjectArgs);
    }

    @Test
    public void testGetFile() throws ServerException, InsufficientDataException, ErrorResponseException, IOException, NoSuchAlgorithmException, InvalidKeyException, InvalidResponseException, XmlParserException, InternalException {
        GetObjectArgs getObjectArgs = GetObjectArgs.builder()
                .bucket("testbucket")
                .object("WutheringWaves-overseas-setup-1.5.2.0.exe")
                .build();

        GetObjectResponse object = minioClient.getObject(getObjectArgs);
        FileOutputStream outputStream = new FileOutputStream(new File("C:\\Users\\Johnny\\Downloads\\game.exe"));
        IOUtils.copy(object, outputStream);
    }


    // upload chunk to MinIO
    @Test
    public void uploadChunk() throws IOException, ServerException, InsufficientDataException, ErrorResponseException, NoSuchAlgorithmException, InvalidKeyException, InvalidResponseException, XmlParserException, InternalException {
        for (int i = 0; i < 18; i++) {
            UploadObjectArgs uploadObjectArgs = UploadObjectArgs.builder()
                    .bucket("testbucket")
                    .filename("C:\\Users\\Johnny\\Downloads\\chunk\\" + i)
                    .object("chunk/" + i)
                    .build();

            minioClient.uploadObject(uploadObjectArgs);
            System.out.println("Uploaded chunk " + i + "successfully");
        }
        

    }

    // merge chunk in MinIO
    @Test
    public void testMerge() throws ServerException, InsufficientDataException, ErrorResponseException, IOException, NoSuchAlgorithmException, InvalidKeyException, InvalidResponseException, XmlParserException, InternalException {
        List<ComposeSource> sources = new ArrayList<>();
        for (int i = 0; i < 18; i++) {
            ComposeSource composeSource = ComposeSource.builder()
                    .bucket("testbucket")
                    .object("chunk/" + i)
                    .build();
            sources.add(composeSource);

        }

        ComposeObjectArgs composeObjectArgs = ComposeObjectArgs.builder()
                .bucket("testbucket")
                .object("WutheringWaves-overseas-setup-1.5.2.0.exe")
                .sources(sources)
                .build();

        minioClient.composeObject(composeObjectArgs);
    }


}
