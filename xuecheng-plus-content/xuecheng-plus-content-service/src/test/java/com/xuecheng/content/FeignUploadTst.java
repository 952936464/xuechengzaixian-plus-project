package com.xuecheng.content;

import com.xuecheng.content.config.MultipartSupportConfig;
import com.xuecheng.content.feignclient.MediaServiceClient;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;

/**
 * 测试远程调用meida
 */
@SpringBootTest
public class FeignUploadTst {
    @Autowired
    MediaServiceClient mediaServiceClient;
    @Test
    public void test() throws IOException {
        MultipartFile multipartFile = MultipartSupportConfig.getMultipartFile(new File("C:\\Users\\95293\\Desktop\\uploadtest\\120.html"));
        String upload = mediaServiceClient.upload(multipartFile, "course/109.html");
        if(upload == null){
            System.out.println("走了降级逻辑");
        }
    }
}
