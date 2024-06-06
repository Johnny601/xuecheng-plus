package com.xuecheng.media.service.jobhandler;

import com.xuecheng.base.utils.Mp4VideoUtil;
import com.xuecheng.media.model.po.MediaProcess;
import com.xuecheng.media.service.MediaFileProcessService;
import com.xuecheng.media.service.MediaFileService;
import com.xxl.job.core.context.XxlJobHelper;
import com.xxl.job.core.handler.annotation.XxlJob;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

@Slf4j
@Component
public class VideoTask {
    @Autowired
    MediaFileProcessService mediaFileProcessService;

    @Autowired
    MediaFileService mediaFileService;

    @Value("${videoprocess.ffmpegpath}")
    private String ffmpegPath;

    @XxlJob("videoJobHandler")
    public void videoJobHandler() throws Exception {

        // 分片参数
        int shardIndex = XxlJobHelper.getShardIndex();
        int shardTotal = XxlJobHelper.getShardTotal();

        //find the number of core of the CPU
        int processorNum = Runtime.getRuntime().availableProcessors();
        //find all videos that need to be processed
        List<MediaProcess> mediaProcessList = mediaFileProcessService.getMediaProcessList(shardIndex, shardTotal, processorNum);
        int size = mediaProcessList.size();
        if (size == 0) {
            log.debug("no video need to be processed");
            return;
        }

        //create thread pool
        ExecutorService executorService = Executors.newFixedThreadPool(size);
        //create a counter for thread
        CountDownLatch countDownLatch = new CountDownLatch(size);
        mediaProcessList.forEach(mediaProcess -> {
            executorService.execute(() -> {
                try {
                    Long taskId = mediaProcess.getId();
                    //Md5 as the Id of file to be processed
                    String fileId = mediaProcess.getFileId();
                    //start the task and indicate that the task is in progress in the DB
                    boolean b = mediaFileProcessService.startTask(taskId);
                    if (!b) {
                        log.debug("start task failed, taskId:{}", taskId);
                        return;
                    }

                    //bucket the file is stored in
                    String bucket = mediaProcess.getBucket();
                    //file path the file is stored in
                    String filePath = mediaProcess.getFilePath();

                    File file = mediaFileService.downloadFileFromMinIO(bucket, filePath);
                    if (file == null) {
                        log.debug("download file failed, taskId:{}, bucket: {}, objectName: {}", taskId, bucket, filePath);
                        mediaFileProcessService.saveProcessFinishStatus(taskId, "3", fileId, null, "download file failed");
                        return;
                    }

                    String video_path = file.getAbsolutePath();
                    String mp4_name = fileId + ".mp4";
                    File mp4File = null;
                    try {
                        mp4File = File.createTempFile("minio", ".mp4");
                    } catch (IOException e) {
                        log.debug("create temp file failed, {}", e.getMessage());
                        mediaFileProcessService.saveProcessFinishStatus(taskId, "3", fileId, null, "create file failed");
                        return;
                    }

                    String mp4_path = mp4File.getAbsolutePath();
                    Mp4VideoUtil videoUtil = new Mp4VideoUtil(ffmpegPath, video_path, mp4_name, mp4_path);
                    String result = videoUtil.generateMp4();
                    if (!result.equals("success")) {
                        log.debug("generate mp4 failed,result:{}, bucket:{}, objectName: {}",result, bucket, filePath);
                        mediaFileProcessService.saveProcessFinishStatus(taskId, "3", fileId, null, result);
                        return;
                    }

                    //将mp4上传至minio
                    //mp4在minio的存储路径
                    String objectName = getFilePath(fileId, ".mp4");
                    //访问url
                    String url = "/" + bucket + "/" + objectName;


                    boolean b1 = mediaFileService.addMediaFilesToMinIO(mp4File.getAbsolutePath(), "video/mp4", bucket, objectName);
                    if (!b1) {
                        log.debug("upload mp4 failed, taskId:{}", taskId);
                        mediaFileProcessService.saveProcessFinishStatus(taskId, "3", fileId, null, "upload mp4 failed");
                        return;
                    }

                    //url of the mp4 file
                    mediaFileProcessService.saveProcessFinishStatus(taskId, "2", fileId, url, null);
                } finally {
                    countDownLatch.countDown();
                }



            });

        });

        countDownLatch.await(30, TimeUnit.MINUTES);
    }

    private String getFilePath(String fileMd5,String fileExt){
        return fileMd5.substring(0,1) + "/" + fileMd5.substring(1,2) + "/" + fileMd5 + "/" +fileMd5 +fileExt;
    }

}
