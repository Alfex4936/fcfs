package csw.fcfs.storage;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.nio.file.Path;
import java.util.UUID;
import java.util.stream.Stream;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Profile;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

@Service
@Qualifier("S3Storage")
@Profile("!test")
public class S3StorageService implements StorageService {

    private final S3Client s3Client;
    private final StorageProperties properties;

    @Autowired
    public S3StorageService(S3Client s3Client, StorageProperties properties) {
        this.s3Client = s3Client;
        this.properties = properties;
    }

    @Override
    public void init() {
        // No init needed for S3
    }

    @Override
    public String store(MultipartFile file) {
        String filename = StringUtils.cleanPath(file.getOriginalFilename());
        String newFilename = UUID.randomUUID().toString() + "_" + filename;
        try {
            if (file.isEmpty()) {
                throw new StorageException("Failed to store empty file " + filename);
            }
            if (filename.contains("..")) {
                // This is a security check
                throw new StorageException(
                        "Cannot store file with relative path outside current directory "
                                + filename);
            }
            try (InputStream inputStream = file.getInputStream()) {
                s3Client.putObject(PutObjectRequest.builder()
                        .bucket(properties.getS3().getBucketName())
                        .key(newFilename)
                        .build(),
                        RequestBody.fromInputStream(inputStream, file.getSize()));
                return newFilename;
            }
        }
        catch (IOException e) {
            throw new StorageException("Failed to store file " + filename, e);
        }
    }

    @Override
    public Stream<Path> loadAll() {
        // Not practical for S3, returning empty stream
        return Stream.empty();
    }

    @Override
    public Path load(String filename) {
        // Not applicable for S3
        return null;
    }

    @Override
    public Resource loadAsResource(String filename) {
        try {
            URL url = s3Client.utilities().getUrl(builder -> builder.bucket(properties.getS3().getBucketName()).key(filename));
            Resource resource = new UrlResource(url);
            if (resource.exists() || resource.isReadable()) {
                return resource;
            }
            else {
                throw new StorageFileNotFoundException(
                        "Could not read file: " + filename);

            }
        }
        catch (Exception e) {
            throw new StorageFileNotFoundException("Could not read file: " + filename, e);
        }
    }

    @Override
    public void deleteAll() {
        // Not implemented for safety
    }

    @Override
    public void delete(String filename) {
        s3Client.deleteObject(DeleteObjectRequest.builder()
                .bucket(properties.getS3().getBucketName())
                .key(filename)
                .build());
    }
}
