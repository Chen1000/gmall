package com.atguigu.gmall.manage.controller;

import org.csource.common.MyException;
import org.csource.fastdfs.ClientGlobal;
import org.csource.fastdfs.StorageClient;
import org.csource.fastdfs.TrackerClient;
import org.csource.fastdfs.TrackerServer;
import org.junit.Test;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.net.URL;

@RestController
public class FileUploadController {

    @RequestMapping(value = "fileUpload",method = RequestMethod.POST)
    public String fileUpload(@RequestParam("file") MultipartFile file) throws IOException, MyException {
        if(file != null){
            System.out.println("multipartFile = " + file.getName()+"|"+file.getSize());
        }

        String conf = this.getClass().getResource("/tracker.conf").getFile();
        ClientGlobal.init(conf);
        TrackerClient trackerClient=new TrackerClient();
        TrackerServer trackerServer=trackerClient.getConnection();
        StorageClient storageClient=new StorageClient(trackerServer,null);

        String[] upload_file = storageClient.upload_file(file.getBytes(), "jpg", null);

        String fileUrl = "";
        for (int i = 0; i < upload_file.length; i++) {

            String s = upload_file[i];
            System.out.println("s = " + s);
            fileUrl += "/" + s;

        }


        return "http://file.server.com" + fileUrl;
    }


    @Test
    public void textFileUpload() throws IOException, MyException {
        /*Class<? extends FileUploadController> aClass = this.getClass();
        System.out.println(aClass);

        URL resource = aClass.getResource("tracker.conf");
        System.out.println(resource);

        String file = resource.getFile();
        System.out.println(file);*/

        String file = this.getClass().getResource("/tracker.conf").getFile();
        ClientGlobal.init(file);
        TrackerClient trackerClient=new TrackerClient();
        TrackerServer trackerServer=trackerClient.getConnection();
        StorageClient storageClient=new StorageClient(trackerServer,null);
        String orginalFilename="G://yoona.jpg";
        String[] upload_file = storageClient.upload_file(orginalFilename, "jpg", null);
        for (int i = 0; i < upload_file.length; i++) {
            String s = upload_file[i];
            System.out.println("s = " + s);
        }

    }



}

