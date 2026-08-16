package co.istad.ai_interview_app.features.file;

import co.istad.ai_interview_app.features.file.service.FileStorageService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.net.URI;

/**
 * Unauthenticated read access for objects uploaded as {@link FileVisibility#PUBLIC}
 * — avatars, company logos and portfolio covers, which are rendered on public
 * pages and by the Next.js image optimizer.
 *
 * <p>Sits under {@code /api/v1/public/**}, which {@code SecurityConfig} already
 * permits for GET.
 */
@RestController
@RequestMapping("/api/v1/public/files")
@RequiredArgsConstructor
public class PublicFileController {

    private static final String PUBLIC_PREFIX = FileVisibility.PUBLIC.prefix() + "/";

    private final FileStorageService fileStorageService;

    @GetMapping("/{*key}")
    public ResponseEntity<Void> read(@PathVariable String key) {
        String objectKey = FileController.normalizeKey(key);

        // Without this check the public endpoint would happily presign a private
        // key such as `private/2026/08/<uuid>.pdf`.
        if (!objectKey.startsWith(PUBLIC_PREFIX)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "File not found");
        }

        return ResponseEntity.status(HttpStatus.FOUND)
                .header(HttpHeaders.CACHE_CONTROL, "public, max-age=300")
                .location(URI.create(fileStorageService.presignedUrl(objectKey)))
                .build();
    }
}
