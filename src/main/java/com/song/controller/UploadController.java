package com.song.controller;

import cn.hutool.core.io.FileUtil;
import cn.hutool.core.util.StrUtil;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.model.ObjectMetadata;
import com.song.dto.Result;
import com.song.entity.Blog;
import com.song.utils.SystemConstants;

import com.song.utils.UserHolder;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import javax.annotation.Resource;
import java.io.File;
import java.io.IOException;
import java.util.UUID;

@Slf4j
@RestController
@RequestMapping("upload")
public class UploadController {
    @Resource
    private AmazonS3 amazonS3;
    // dont import @Value from lombok!
    @Value("${aws.bucket.name}")
    private String bucketName;

    @PostMapping("blog")
    public Result uploadImage(@RequestParam("file") MultipartFile image) {
        try {
            // get the original file name
            String originalFilename = image.getOriginalFilename();
            // create a new file name
            String fileName = createNewFileName(originalFilename);

            // save the file
//            image.transferTo(new File(SystemConstants.IMAGE_UPLOAD_DIR, fileName));
//            // return the result
//            log.debug("save file successfully,{}", fileName);
//            return Result.ok(fileName);
//        } catch (IOException e) {
//            throw new RuntimeException("upload file failed", e);
//        }
            ObjectMetadata metadata = new ObjectMetadata();
            metadata.setContentLength(image.getSize());
            metadata.setContentType(image.getContentType());
            amazonS3.putObject(bucketName, fileName, image.getInputStream(), metadata);

            log.debug("File uploaded successfully to S3, {}", fileName);
            String fileUrl = "https://songs-project.s3.eu-west-1.amazonaws.com/" + fileName;


            return Result.ok(fileUrl);
        } catch (IOException e) {
            throw new RuntimeException("Upload file failed", e);
        }
    }

    @GetMapping("/blog/delete")
    public Result deleteBlogImg(@RequestParam("name") String filename) {
//        File file = new File(SystemConstants.IMAGE_UPLOAD_DIR, filename);
//        if (file.isDirectory()) {
//            return Result.fail("wrong file name");
//        }
//        FileUtil.del(file);
//        return Result.ok();
        try {
            amazonS3.deleteObject(bucketName, filename);
            return Result.ok();
        } catch (Exception e) {
            log.error("Error deleting file from S3", e);
            return Result.fail("Failed to delete file");
        }
    }

    private String createNewFileName(String originalFilename) {
        Long userId = UserHolder.getUser().getId();
        String suffix = StrUtil.subAfter(originalFilename, ".", true);
        String randomString = UUID.randomUUID().toString();

        // 构建S3对象键为 "用户ID/blogs/随机字符串.后缀"
        return userId + "/blogs/" + randomString + "." + suffix;

    }
}


//   private String createNewFileName(String originalFilename) {
//       //  get the suffix
//        String suffix = StrUtil.subAfter(originalFilename, ".", true);
//        // generate a random name
//        String name = UUID.randomUUID().toString();
//        int hash = name.hashCode();
//        int d1 = hash & 0xF;
//        int d2 = (hash >> 4) & 0xF;
//        // check if the directory is existed
//        File dir = new File(SystemConstants.IMAGE_UPLOAD_DIR, StrUtil.format("/blogs/{}/{}", d1, d2));
//        if (!dir.exists()) {
//            dir.mkdirs();
//        }
//        return StrUtil.format("/blogs/{}/{}/{}.{}", d1, d2, name, suffix);

//}