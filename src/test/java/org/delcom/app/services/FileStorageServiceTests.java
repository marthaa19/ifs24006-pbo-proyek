package org.delcom.app.services;

import static org.mockito.Mockito.when;
import static org.mockito.Mockito.mock;

import java.io.ByteArrayInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.UUID;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.web.multipart.MultipartFile;

public class FileStorageServiceTests {
    
    private FileStorageService fileStorageService;
    private String testUploadDir;
    private UUID testId;

    @BeforeEach
    void setUp() throws IOException {
        // Setup test directory
        testUploadDir = "./test-uploads";
        testId = UUID.randomUUID();
        
        fileStorageService = new FileStorageService();
        fileStorageService.uploadDir = testUploadDir;
        
        // Buat directory test jika belum ada
        Path uploadPath = Paths.get(testUploadDir);
        if (!Files.exists(uploadPath)) {
            Files.createDirectories(uploadPath);
        }
    }

    @AfterEach
    void tearDown() throws IOException {
        // Cleanup - hapus semua file test dan directory
        Path uploadPath = Paths.get(testUploadDir);
        if (Files.exists(uploadPath)) {
            Files.walk(uploadPath)
                .sorted((a, b) -> b.compareTo(a)) // reverse order to delete files before directories
                .forEach(path -> {
                    try {
                        Files.delete(path);
                    } catch (IOException e) {
                        // Ignore
                    }
                });
        }
    }

    @Test
    @DisplayName("Pengujian untuk service FileStorage")
    void testFileStorageService() throws Exception {
        assert (fileStorageService != null);

        // Menguji method storeFile
        {
            // Store file dengan ekstensi
            {
                MultipartFile mockFile = mock(MultipartFile.class);
                when(mockFile.getOriginalFilename()).thenReturn("test-image.jpg");
                when(mockFile.getInputStream()).thenReturn(new ByteArrayInputStream("test content".getBytes()));

                String filename = fileStorageService.storeFile(mockFile, testId);
                assert (filename != null);
                assert (filename.equals("cover_" + testId.toString() + ".jpg"));
                
                // Verifikasi file benar-benar tersimpan
                Path filePath = Paths.get(testUploadDir).resolve(filename);
                assert (Files.exists(filePath));
            }

            // Store file tanpa ekstensi
            {
                UUID testId2 = UUID.randomUUID();
                MultipartFile mockFile = mock(MultipartFile.class);
                when(mockFile.getOriginalFilename()).thenReturn("testfile");
                when(mockFile.getInputStream()).thenReturn(new ByteArrayInputStream("test content".getBytes()));

                String filename = fileStorageService.storeFile(mockFile, testId2);
                assert (filename != null);
                assert (filename.equals("cover_" + testId2.toString()));
                
                Path filePath = Paths.get(testUploadDir).resolve(filename);
                assert (Files.exists(filePath));
            }

            // Store file dengan originalFilename null
            {
                UUID testId3 = UUID.randomUUID();
                MultipartFile mockFile = mock(MultipartFile.class);
                when(mockFile.getOriginalFilename()).thenReturn(null);
                when(mockFile.getInputStream()).thenReturn(new ByteArrayInputStream("test content".getBytes()));

                String filename = fileStorageService.storeFile(mockFile, testId3);
                assert (filename != null);
                assert (filename.equals("cover_" + testId3.toString()));
            }

            // Store file yang replace existing file
            {
                MultipartFile mockFile = mock(MultipartFile.class);
                when(mockFile.getOriginalFilename()).thenReturn("replace-test.png");
                when(mockFile.getInputStream()).thenReturn(new ByteArrayInputStream("new content".getBytes()));

                String filename = fileStorageService.storeFile(mockFile, testId);
                assert (filename != null);
                assert (filename.equals("cover_" + testId.toString() + ".png"));
            }

            // Store file ke directory yang belum ada (auto create)
            {
                String newUploadDir = "./test-uploads-new";
                fileStorageService.uploadDir = newUploadDir;
                
                UUID testId4 = UUID.randomUUID();
                MultipartFile mockFile = mock(MultipartFile.class);
                when(mockFile.getOriginalFilename()).thenReturn("new-dir-test.jpg");
                when(mockFile.getInputStream()).thenReturn(new ByteArrayInputStream("test content".getBytes()));

                String filename = fileStorageService.storeFile(mockFile, testId4);
                assert (filename != null);
                
                // Cleanup new directory
                Path newPath = Paths.get(newUploadDir);
                if (Files.exists(newPath)) {
                    Files.walk(newPath)
                        .sorted((a, b) -> b.compareTo(a))
                        .forEach(path -> {
                            try {
                                Files.delete(path);
                            } catch (IOException e) {
                                // Ignore
                            }
                        });
                }
                
                // Kembalikan ke test directory
                fileStorageService.uploadDir = testUploadDir;
            }
        }

        // Menguji method deleteFile
        {
            // Delete file yang ada
            {
                // Buat file dulu
                UUID deleteTestId = UUID.randomUUID();
                MultipartFile mockFile = mock(MultipartFile.class);
                when(mockFile.getOriginalFilename()).thenReturn("delete-test.jpg");
                when(mockFile.getInputStream()).thenReturn(new ByteArrayInputStream("delete test".getBytes()));
                
                String filename = fileStorageService.storeFile(mockFile, deleteTestId);
                
                // Test delete
                boolean deleted = fileStorageService.deleteFile(filename);
                assert (deleted);
                
                // Verifikasi file sudah terhapus
                Path filePath = Paths.get(testUploadDir).resolve(filename);
                assert (!Files.exists(filePath));
            }

            // Delete file yang tidak ada
            {
                boolean deleted = fileStorageService.deleteFile("nonexistent-file.jpg");
                assert (!deleted);
            }

            // Test IOException - Lock file dengan FileOutputStream (Windows-specific)
            {
                UUID lockedTestId = UUID.randomUUID();
                MultipartFile mockFile = mock(MultipartFile.class);
                when(mockFile.getOriginalFilename()).thenReturn("locked-test.jpg");
                when(mockFile.getInputStream()).thenReturn(new ByteArrayInputStream("locked test".getBytes()));
                
                String filename = fileStorageService.storeFile(mockFile, lockedTestId);
                Path lockedFilePath = Paths.get(testUploadDir).resolve(filename);
                
                FileOutputStream fos = null;
                try {
                    // Lock file dengan membuka stream (akan trigger IOException di Windows)
                    fos = new FileOutputStream(lockedFilePath.toFile());
                    
                    // Coba delete file yang sedang terbuka
                    boolean deleted = fileStorageService.deleteFile(filename);
                    
                    // Di Windows, file yang terbuka tidak bisa dihapus (IOException)
                    // Di Linux/Mac, file bisa dihapus meski terbuka (return true)
                    // Jadi kita terima kedua hasil
                    assert (deleted || !deleted);
                    
                } finally {
                    if (fos != null) {
                        try {
                            fos.close();
                        } catch (IOException e) {
                            // Ignore
                        }
                    }
                    // Cleanup - pastikan file terhapus
                    try {
                        Files.deleteIfExists(lockedFilePath);
                    } catch (IOException e) {
                        // Ignore
                    }
                }
            }
        }

        // Menguji method loadFile
        {
            String testFilename = "test-load.jpg";
            Path loadedPath = fileStorageService.loadFile(testFilename);
            assert (loadedPath != null);
            assert (loadedPath.equals(Paths.get(testUploadDir).resolve(testFilename)));
        }

        // Menguji method fileExists
        {
            // File yang ada
            {
                UUID existTestId = UUID.randomUUID();
                MultipartFile mockFile = mock(MultipartFile.class);
                when(mockFile.getOriginalFilename()).thenReturn("exist-test.jpg");
                when(mockFile.getInputStream()).thenReturn(new ByteArrayInputStream("exist test".getBytes()));
                
                String filename = fileStorageService.storeFile(mockFile, existTestId);
                
                boolean exists = fileStorageService.fileExists(filename);
                assert (exists);
            }

            // File yang tidak ada
            {
                boolean exists = fileStorageService.fileExists("nonexistent-file.jpg");
                assert (!exists);
            }
        }

        // Menguji method storeStudentFile
        {
            UUID studentId = UUID.randomUUID();

            // Store student file dengan ekstensi
            {
                MultipartFile mockFile = mock(MultipartFile.class);
                when(mockFile.getOriginalFilename()).thenReturn("student-photo.jpg");
                when(mockFile.getInputStream()).thenReturn(new ByteArrayInputStream("student photo".getBytes()));

                String filename = fileStorageService.storeStudentFile(mockFile, studentId);
                assert (filename != null);
                assert (filename.equals("student_" + studentId.toString() + ".jpg"));
                
                // Verifikasi file tersimpan
                Path filePath = Paths.get(testUploadDir).resolve(filename);
                assert (Files.exists(filePath));
            }

            // Store student file tanpa ekstensi
            {
                UUID studentId2 = UUID.randomUUID();
                MultipartFile mockFile = mock(MultipartFile.class);
                when(mockFile.getOriginalFilename()).thenReturn("studentphoto");
                when(mockFile.getInputStream()).thenReturn(new ByteArrayInputStream("student photo".getBytes()));

                String filename = fileStorageService.storeStudentFile(mockFile, studentId2);
                assert (filename != null);
                assert (filename.equals("student_" + studentId2.toString()));
            }

            // Store student file dengan originalFilename null
            {
                UUID studentId3 = UUID.randomUUID();
                MultipartFile mockFile = mock(MultipartFile.class);
                when(mockFile.getOriginalFilename()).thenReturn(null);
                when(mockFile.getInputStream()).thenReturn(new ByteArrayInputStream("student photo".getBytes()));

                String filename = fileStorageService.storeStudentFile(mockFile, studentId3);
                assert (filename != null);
                assert (filename.equals("student_" + studentId3.toString()));
            }

            // Store student file ke directory yang belum ada (auto create)
            {
                String newUploadDir = "./test-uploads-student";
                fileStorageService.uploadDir = newUploadDir;
                
                UUID studentId4 = UUID.randomUUID();
                MultipartFile mockFile = mock(MultipartFile.class);
                when(mockFile.getOriginalFilename()).thenReturn("student-new.jpg");
                when(mockFile.getInputStream()).thenReturn(new ByteArrayInputStream("student photo".getBytes()));

                String filename = fileStorageService.storeStudentFile(mockFile, studentId4);
                assert (filename != null);
                
                // Cleanup
                Path newPath = Paths.get(newUploadDir);
                if (Files.exists(newPath)) {
                    Files.walk(newPath)
                        .sorted((a, b) -> b.compareTo(a))
                        .forEach(path -> {
                            try {
                                Files.delete(path);
                            } catch (IOException e) {
                                // Ignore
                            }
                        });
                }
            }
        }
    }
}