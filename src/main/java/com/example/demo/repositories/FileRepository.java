package com.example.demo.repositories;

import com.example.demo.model.UploadFileResponse;
import org.springframework.data.jpa.repository.JpaRepository;

public interface FileRepository extends JpaRepository<UploadFileResponse, Long> {
}
