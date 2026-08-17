package co.istad.ai_interview_app.features.file;

import co.istad.ai_interview_app.features.file.dto.FileUploadResponse;
import co.istad.ai_interview_app.features.file.service.FileStorageService;
import co.istad.ai_interview_app.features.file.service.FileStorageServiceImpl;
import io.minio.MinioClient;
import io.minio.PutObjectArgs;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.http.HttpStatus;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

/**
 * Covers the rules that decide whether an object is stored at all and, once
 * stored, which endpoint is allowed to hand out a presigned URL for it.
 */
class FileStorageTest {

    private static final long MAX_BYTES = 5L * 1024 * 1024;

    private MinioClient minioClient;
    private FileStorageServiceImpl service;

    @BeforeEach
    void setUp() {
        minioClient = mock(MinioClient.class);
        service = new FileStorageServiceImpl(minioClient);
        ReflectionTestUtils.setField(service, "bucket", "ai-career");
        ReflectionTestUtils.setField(service, "maxFileSize", MAX_BYTES);
        ReflectionTestUtils.setField(service, "presignedExpiryMinutes", 15);
    }

    private static MultipartFile file(String contentType, byte[] content) {
        return new MockMultipartFile("file", "upload.bin", contentType, content);
    }

    @Test
    void publicUploadIsAddressedByThePublicEndpoint() throws Exception {
        FileUploadResponse response =
                service.upload(file("image/png", new byte[]{1, 2, 3}), FileVisibility.PUBLIC);

        assertThat(response.key()).startsWith("public/").endsWith(".png");
        assertThat(response.url()).isEqualTo("/api/v1/public/files/" + response.key());
        assertThat(response.size()).isEqualTo(3L);
        assertThat(response.contentType()).isEqualTo("image/png");

        ArgumentCaptor<PutObjectArgs> args = ArgumentCaptor.forClass(PutObjectArgs.class);
        verify(minioClient).putObject(args.capture());
        assertThat(args.getValue().bucket()).isEqualTo("ai-career");
        assertThat(args.getValue().object()).isEqualTo(response.key());
    }

    @Test
    void privateUploadIsAddressedByTheAuthenticatedEndpoint() {
        FileUploadResponse response =
                service.upload(file("application/pdf", new byte[]{1}), FileVisibility.PRIVATE);

        assertThat(response.key()).startsWith("private/").endsWith(".pdf");
        assertThat(response.url()).isEqualTo("/api/v1/files/" + response.key());
    }

    @Test
    void rejectsAnEmptyFile() {
        assertThatThrownBy(() -> service.upload(file("image/png", new byte[0]), FileVisibility.PUBLIC))
                .isInstanceOf(ResponseStatusException.class)
                .extracting(e -> ((ResponseStatusException) e).getStatusCode())
                .isEqualTo(HttpStatus.BAD_REQUEST);
    }

    @Test
    void rejectsAFileOverTheSizeLimit() {
        MultipartFile oversized = file("image/png", new byte[(int) MAX_BYTES + 1]);

        assertThatThrownBy(() -> service.upload(oversized, FileVisibility.PUBLIC))
                .isInstanceOf(ResponseStatusException.class)
                .extracting(e -> ((ResponseStatusException) e).getStatusCode())
                .isEqualTo(HttpStatus.CONTENT_TOO_LARGE);
    }

    @Test
    void rejectsAnUnsupportedContentType() {
        assertThatThrownBy(() ->
                service.upload(file("application/zip", new byte[]{1}), FileVisibility.PRIVATE))
                .isInstanceOf(ResponseStatusException.class)
                .extracting(e -> ((ResponseStatusException) e).getStatusCode())
                .isEqualTo(HttpStatus.UNSUPPORTED_MEDIA_TYPE);
    }

    @Test
    void normalizesKeysAndRejectsTraversal() {
        assertThat(FileController.normalizeKey("/public/2026/08/a.png"))
                .isEqualTo("public/2026/08/a.png");

        for (String bad : new String[]{"/", "", null, "/public/../private/secret.pdf"}) {
            assertThatThrownBy(() -> FileController.normalizeKey(bad))
                    .isInstanceOf(ResponseStatusException.class)
                    .extracting(e -> ((ResponseStatusException) e).getStatusCode())
                    .isEqualTo(HttpStatus.NOT_FOUND);
        }
    }

    /**
     * The guard that stops the unauthenticated endpoint from presigning a
     * private object.
     */
    @Test
    void publicEndpointRefusesPrivateKeys() {
        FileStorageService storage = mock(FileStorageService.class);
        PublicFileController controller = new PublicFileController(storage);

        assertThatThrownBy(() -> controller.read("/private/2026/08/secret.pdf"))
                .isInstanceOf(ResponseStatusException.class)
                .extracting(e -> ((ResponseStatusException) e).getStatusCode())
                .isEqualTo(HttpStatus.NOT_FOUND);

        verify(storage, org.mockito.Mockito.never()).presignedUrl(any());
    }

    @Test
    void publicEndpointRedirectsToAPresignedUrl() {
        FileStorageService storage = mock(FileStorageService.class);
        org.mockito.Mockito.when(storage.presignedUrl("public/2026/08/logo.png"))
                .thenReturn("http://localhost:9000/ai-career/public/2026/08/logo.png?X-Amz-Signature=abc");

        var response = new PublicFileController(storage).read("/public/2026/08/logo.png");

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FOUND);
        assertThat(response.getHeaders().getLocation()).hasToString(
                "http://localhost:9000/ai-career/public/2026/08/logo.png?X-Amz-Signature=abc");
    }
}
