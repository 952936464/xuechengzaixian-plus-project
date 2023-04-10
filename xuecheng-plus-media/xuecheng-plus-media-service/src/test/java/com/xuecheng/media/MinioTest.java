package com.xuecheng.media;

import io.minio.MinioClient;
import io.minio.RemoveObjectArgs;
import io.minio.UploadObjectArgs;
import io.minio.errors.*;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;

/**
 * 测试minio的sdk
 */
public class MinioTest {

    MinioClient minioClient =
            MinioClient.builder().
                endpoint("http://192.168.101.65:9000")
                .credentials("minioadmin","minioadmin")
                .build();

    @Test
    public void test_upload() throws IOException, ServerException, InsufficientDataException, ErrorResponseException, NoSuchAlgorithmException, InvalidKeyException, InvalidResponseException, XmlParserException, InternalException {
        UploadObjectArgs uploadObjectArgs = UploadObjectArgs.builder()
                .bucket("testbucket")//桶
                .filename("C:\\Users\\95293\\Desktop\\隐私\\noble22a.pdf")
                .object("noble")
                .build();
        minioClient.uploadObject(uploadObjectArgs);
    }
    @Test
    public void test_delete() throws Exception{
        RemoveObjectArgs removeObjectArgs = RemoveObjectArgs.builder().bucket("testbucket").object("noble").build();

        minioClient.removeObject(removeObjectArgs);
    }
}
