package com.ecocook.service;

import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.text.Normalizer;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Service
public class IngredientIconService {

    private static final String ICON_RELATIVE_DIR = "src/main/resources/static/uploads/ingredient-icons";
    private static final String ICON_WEB_PREFIX = "/uploads/ingredient-icons/";
    private static final DateTimeFormatter TIMESTAMP_FORMAT = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");
    private static final List<String> AUTHORIZED_EXTENSIONS = List.of(".svg", ".png", ".jpg", ".jpeg", ".webp");

    public IngredientIconService() throws IOException {
        ensureDirectoryExists();
    }

    public List<String> listIcons() throws IOException {
        ensureDirectoryExists();

        try (Stream<Path> stream = Files.list(getIconDirectory())) {
            return stream
                .filter(Files::isRegularFile)
                .filter(path -> AUTHORIZED_EXTENSIONS.stream().anyMatch(ext -> path.getFileName().toString().toLowerCase(Locale.ROOT).endsWith(ext)))
                .sorted(Comparator.comparing(path -> path.getFileName().toString().toLowerCase(Locale.ROOT)))
                .map(path -> ICON_WEB_PREFIX + path.getFileName().toString())
                .collect(Collectors.toList());
        }
    }

    public String saveIcon(MultipartFile file) throws IOException {
        if (file == null || file.isEmpty()) {
            return null;
        }

        ensureDirectoryExists();

        String originalFilename = file.getOriginalFilename();
        if (originalFilename == null || originalFilename.isBlank()) {
            throw new IOException("Nom de fichier invalide pour l'icône.");
        }

        String extension = extractExtension(originalFilename);
        if (extension == null || AUTHORIZED_EXTENSIONS.stream().noneMatch(ext -> ext.equalsIgnoreCase(extension))) {
            throw new IOException("Format d'icône non supporté. Formats autorisés : " + String.join(", ", AUTHORIZED_EXTENSIONS));
        }

        String baseName = sanitizeFilename(removeExtension(originalFilename));
        String timestamp = TIMESTAMP_FORMAT.format(LocalDateTime.now());
        String finalName = baseName + "-" + timestamp + extension.toLowerCase(Locale.ROOT);

        Path target = getIconDirectory().resolve(finalName);
        Files.copy(file.getInputStream(), target, StandardCopyOption.REPLACE_EXISTING);

        return ICON_WEB_PREFIX + finalName;
    }

    private void ensureDirectoryExists() throws IOException {
        Path dir = getIconDirectory();
        if (!Files.exists(dir)) {
            Files.createDirectories(dir);
        }
    }

    private Path getIconDirectory() {
        return Paths.get(ICON_RELATIVE_DIR);
    }

    private String extractExtension(String filename) {
        int lastDot = filename.lastIndexOf('.');
        if (lastDot == -1) {
            return null;
        }
        return filename.substring(lastDot).toLowerCase(Locale.ROOT);
    }

    private String removeExtension(String filename) {
        int lastDot = filename.lastIndexOf('.');
        if (lastDot == -1) {
            return filename;
        }
        return filename.substring(0, lastDot);
    }

    private String sanitizeFilename(String input) {
        if (input == null) {
            return "icone-ingredient";
        }
        String normalized = Normalizer.normalize(input.toLowerCase(Locale.ROOT), Normalizer.Form.NFD)
            .replaceAll("\\p{InCombiningDiacriticalMarks}+", "");
        normalized = normalized.replaceAll("[^a-z0-9]+", "-");
        normalized = normalized.replaceAll("-{2,}", "-");
        normalized = normalized.replaceAll("(^-|-$)", "");

        return normalized.isBlank() ? "icone-ingredient" : normalized;
    }
}



