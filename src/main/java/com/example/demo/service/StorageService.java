package com.example.demo.service;

import com.example.demo.model.UploadFileResponse;
import org.springframework.core.io.Resource;
import org.springframework.web.multipart.MultipartFile;

import java.nio.file.Path;
import java.util.List;

public interface StorageService {

    void storeFile(MultipartFile file);

    List<UploadFileResponse> loadAll();

    Path load(String filename);

    Resource loadAsResource(String filename);

    void init();

    void deleteAll();
}