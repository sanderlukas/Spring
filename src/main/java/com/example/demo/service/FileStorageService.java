package com.example.demo.service;

import com.example.demo.exception.FileStorageException;
import com.example.demo.exception.StorageFileNotFoundException;
import com.example.demo.model.UploadFileResponse;
import com.example.demo.property.FileStorageProperties;
import com.example.demo.repositories.FileRepository;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.stereotype.Service;
import org.springframework.util.FileSystemUtils;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.List;

@Service
public class FileStorageService implements StorageService {

    private final FileRepository fileRepository;
    private final Path rootLocation;

    public FileStorageService(FileRepository fileRepository, FileStorageProperties properties) {
        this.fileRepository = fileRepository;
        this.rootLocation = Paths.get(properties.getUploadDir())
                .toAbsolutePath().normalize();
    }

    private static Dimension getImageDimensions(MultipartFile imageFile) throws IOException {
        File convertedFile = new File(imageFile.getOriginalFilename());
        convertedFile.createNewFile();
        FileOutputStream fos = new FileOutputStream(convertedFile);
        fos.write(imageFile.getBytes());
        fos.close();
        BufferedImage bufferedImage = ImageIO.read(convertedFile);
        int width = bufferedImage.getWidth();
        int height = bufferedImage.getHeight();
        return new Dimension(width, height);
    }


    @Override
    public void storeFile(MultipartFile file) {
        String filename = StringUtils.cleanPath(file.getOriginalFilename());

        try {
            String mimeType = Files.probeContentType(Paths.get(filename));
            String[] fileType = mimeType.split("/");

            if (!fileType[0].equals("image")) {
                throw new FileStorageException("File is not an image " + filename);
            }

            if (file.isEmpty()) {
                throw new FileStorageException("Failed to store empty file " + filename);
            }

            if (filename.contains("..")) {
                throw new FileStorageException(
                        "Cannot store file with relative path outside current directory "
                                + filename);
            }

            try (InputStream inputStream = file.getInputStream()) {
                Files.copy(inputStream, this.rootLocation.resolve(filename),
                        StandardCopyOption.REPLACE_EXISTING);
            }

            String fileUrl = ServletUriComponentsBuilder.fromCurrentContextPath()
                    .path("/files/")
                    .path(filename)
                    .toUriString();

            Dimension imageDimensions = getImageDimensions(file);
            fileRepository.save(new UploadFileResponse(filename, fileUrl, fileType[1],
                    imageDimensions.getHeight(), imageDimensions.getWidth()));
        } catch (IOException e) {
            throw new FileStorageException("Failed to store file " + filename, e);
        }
    }

    @Override
    public List<UploadFileResponse> loadAll() {
        try {
            return fileRepository.findAll();
        } catch (Exception e) {
            throw new FileStorageException("Failed to read stored files", e);
        }
    }

    @Override
    public Path load(String filename) {
        return rootLocation.resolve(filename);
    }

    @Override
    public Resource loadAsResource(String filename) {
        try {
            Path file = load(filename);
            Resource resource = new UrlResource(file.toUri());
            if (resource.exists() || resource.isReadable()) {
                return resource;
            } else {
                throw new StorageFileNotFoundException(
                        "Could not read file: " + filename);
            }
        } catch (MalformedURLException e) {
            throw new StorageFileNotFoundException("Could not read file: " + filename, e);
        }
    }

    @Override
    public void init() {
        try {
            Files.createDirectories(rootLocation);
        } catch (IOException e) {
            throw new FileStorageException("Could not initialize storage", e);
        }
    }

    @Override
    public void deleteAll() {
        FileSystemUtils.deleteRecursively(rootLocation.toFile());
    }
}
