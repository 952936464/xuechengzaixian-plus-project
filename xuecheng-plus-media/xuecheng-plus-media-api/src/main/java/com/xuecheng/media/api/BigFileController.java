package com.xuecheng.media.api;


import com.xuecheng.base.model.RestResponse;
import com.xuecheng.media.model.dto.UploadFileParamsDto;
import com.xuecheng.media.service.MediaFileService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;

@Api(value = "大文件上传接口", tags = "大文件上传接口")
@RestController
public class BigFileController {
    @Autowired
    MediaFileService mediaFileService;

    @ApiOperation(value = "文件上传前检查文件")
    @PostMapping("/upload/checkfile")
    public RestResponse<Boolean> checkfile(@RequestParam("fileMd5") String fileMd5) throws Exception{
        return mediaFileService.checkfile(fileMd5);
    }

    @ApiOperation(value = "分块文件上传前的检测")
    @PostMapping("/upload/checkchunk")
    public RestResponse<Boolean> checkchunk(@RequestParam("fileMd5") String fileMd5, @RequestParam("chunk") int chunk) throws Exception{
        RestResponse<Boolean> checkchunk = mediaFileService.checkchunk(fileMd5, chunk);
        System.out.println("123");
        return checkchunk;
    }

    @ApiOperation(value = "上传分块文件")
    @PostMapping("/upload/uploadchunk")
    public RestResponse uploadchunk(@RequestParam("file") MultipartFile file,
                                    @RequestParam("fileMd5") String fileMd5,
                                    @RequestParam("chunk") int chunk) throws Exception{

        //创建一个临时文件
        File tempFile = File.createTempFile("minio", ".temp");
        file.transferTo(tempFile);
        String localFilePath = tempFile.getAbsolutePath();
        return mediaFileService.uploadchunk(fileMd5, chunk, localFilePath);
    }

    @ApiOperation(value = "合并文件")
    @PostMapping("/upload/mergechunks")
    public RestResponse mergechunks(@RequestParam("fileMd5") String fileMd5, @RequestParam("fileName") String fileName,
                                    @RequestParam("chunkTotal") int chunkTotal) throws Exception{
        UploadFileParamsDto uploadFileParamsDto = new UploadFileParamsDto();
        uploadFileParamsDto.setFileType("001002");//视频文件
        uploadFileParamsDto.setTags("视频文件");
        uploadFileParamsDto.setFilename(fileName);
        return mediaFileService.mergechunks(1232141425L, fileMd5, chunkTotal, uploadFileParamsDto);
    }


}
