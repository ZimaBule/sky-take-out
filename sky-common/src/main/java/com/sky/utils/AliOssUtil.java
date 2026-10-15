package com.sky.utils;

import com.aliyun.oss.ClientException;
import com.aliyun.oss.OSS;
import com.aliyun.oss.OSSClientBuilder;
import com.aliyun.oss.OSSException;
import com.sky.properties.AliOssProperties;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.ByteArrayInputStream;

//@AllArgsConstructor
@Component
@Slf4j
public class AliOssUtil {

    @Autowired
    private AliOssProperties aliOssProperties;

    //文件上传
    public String upload(byte[] bytes, String objectName) {

        String endpoint = aliOssProperties.getEndpoint();
        String accessKeyId = aliOssProperties.getAccessKeyId();
        String accessKeySecret = aliOssProperties.getAccessKeySecret();
        String bucketName = aliOssProperties.getBucketName();

        // 创建OSSClient实例。
        OSS ossClient = new OSSClientBuilder().build(endpoint, accessKeyId, accessKeySecret);

        try {
            // 创建PutObject请求。
            ossClient.putObject(bucketName, objectName, new ByteArrayInputStream(bytes));
        } catch (OSSException oe) {
            log.error("OSS拒绝请求, ErrorMessage:{}, ErrorCode:{}, RequestId:{}, HostId:{}",
                    oe.getErrorMessage(), oe.getErrorCode(), oe.getRequestId(), oe.getHostId());
            throw new RuntimeException("OSS上传失败: " + oe.getErrorCode() + " - " + oe.getErrorMessage(), oe);
        } catch (ClientException ce) {
            System.out.println("Caught an ClientException, which means the client encountered "
                    + "a serious internal problem while trying to communicate with OSS, "
                    + "such as not being able to access the network.");
            System.out.println("Error Message:" + ce.getMessage());
        } finally {
            if (ossClient != null) {
                ossClient.shutdown();
            }
        }

        //文件访问路径规则 https://BucketName.Endpoint/ObjectName
        StringBuilder stringBuilder = new StringBuilder("https://");
        stringBuilder
                .append(bucketName)
                .append(".")
                .append(endpoint.split("//")[1])
                .append("/")
                .append(objectName);
        log.info("文件上传到:{}", stringBuilder.toString());
        return stringBuilder.toString();
    }
}


