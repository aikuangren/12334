package com.fiftytalk.carddraw.controller;

import com.fiftytalk.carddraw.common.ApiResponse;
import com.fiftytalk.carddraw.common.BusinessException;
import com.fiftytalk.carddraw.config.CardDrawProperties;
import com.fiftytalk.carddraw.dto.AudioSyncResponse;
import com.fiftytalk.carddraw.dto.ResourceUploadResponse;
import com.fiftytalk.carddraw.entity.Card;
import com.fiftytalk.carddraw.repository.CardRepository;
import com.fiftytalk.carddraw.util.ResourceUrlUtils;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Stream;

@RestController
@RequestMapping("/admin/resources")
public class AdminResourceController {
    private static final Set<String> IMAGE_EXTENSIONS = Set.of("png", "jpg", "jpeg", "webp", "gif");
    private static final Set<String> AUDIO_EXTENSIONS = Set.of("mp3", "wav", "m4a", "aac", "ogg");
    private final CardDrawProperties properties;
    private final CardRepository cardRepository;

    public AdminResourceController(CardDrawProperties properties, CardRepository cardRepository) {
        this.properties = properties;
        this.cardRepository = cardRepository;
    }

    @PostMapping("/upload-image")
    public ApiResponse<ResourceUploadResponse> uploadImage(@RequestParam MultipartFile file) {
        return ApiResponse.ok(storeResource(
                file,
                IMAGE_EXTENSIONS,
                "يرجى اختيار صورة للرفع",
                "يدعم فقط صور png وjpg وjpeg وwebp وgif",
                "images"
        ));
    }

    @PostMapping("/upload-audio")
    public ApiResponse<ResourceUploadResponse> uploadAudio(@RequestParam MultipartFile file) {
        return ApiResponse.ok(storeResource(
                file,
                AUDIO_EXTENSIONS,
                "يرجى اختيار ملف صوتي للرفع",
                "يدعم فقط ملفات mp3 وwav وm4a وaac وogg",
                "audio"
        ));
    }

    @PostMapping("/sync-audio-by-word")
    @Transactional
    public ApiResponse<AudioSyncResponse> syncAudioByWord() {
        Path audioDir = Path.of(properties.getUploadDir(), "card-draw", "audio", "v1").toAbsolutePath().normalize();
        if (!Files.isDirectory(audioDir)) {
            throw new BusinessException("لم يتم العثور على مجلد الصوت: " + audioDir);
        }

        Map<String, Path> audioByWord = new LinkedHashMap<>();
        try (Stream<Path> files = Files.list(audioDir)) {
            files.filter(Files::isRegularFile)
                    .filter(path -> AUDIO_EXTENSIONS.contains(extension(path.getFileName().toString())))
                    .sorted()
                    .forEach(path -> audioByWord.putIfAbsent(wordKey(stripExtension(path.getFileName().toString())), path));
        } catch (IOException exception) {
            throw new BusinessException("تعذرت قراءة مجلد الصوت: " + exception.getMessage());
        }

        int updatedCardCount = 0;
        List<String> matchedWords = new ArrayList<>();
        for (Card card : cardRepository.findAll()) {
            Path audioPath = audioByWord.get(wordKey(card.getWord()));
            if (audioPath == null) {
                continue;
            }
            String relativePath = Path.of("uploads", "card-draw", "audio", "v1", audioPath.getFileName().toString())
                    .toString()
                    .replace('\\', '/');
            String audioUrl = ResourceUrlUtils.cdnUrl(properties.getCdnBaseUrl(), relativePath);
            if (!audioUrl.equals(card.getAudioUrl())) {
                card.setAudioUrl(audioUrl);
                updatedCardCount++;
            }
            matchedWords.add(wordKey(card.getWord()));
        }

        Set<String> matchedWordSet = Set.copyOf(matchedWords);
        List<String> unmatchedFiles = audioByWord.entrySet().stream()
                .filter(entry -> !matchedWordSet.contains(entry.getKey()))
                .map(entry -> entry.getValue().getFileName().toString())
                .toList();

        return ApiResponse.ok(new AudioSyncResponse(
                audioByWord.size(),
                matchedWordSet.size(),
                updatedCardCount,
                unmatchedFiles
        ));
    }

    private ResourceUploadResponse storeResource(
            MultipartFile file,
            Set<String> allowedExtensions,
            String emptyMessage,
            String invalidMessage,
            String resourceType
    ) {
        if (file == null || file.isEmpty()) {
            throw new BusinessException(emptyMessage);
        }
        String extension = extension(file.getOriginalFilename());
        if (!allowedExtensions.contains(extension)) {
            throw new BusinessException(invalidMessage);
        }
        String dateDir = LocalDate.now().format(DateTimeFormatter.BASIC_ISO_DATE);
        String fileName = UUID.randomUUID().toString().replace("-", "") + "." + extension;
        Path relativePath = Path.of("card-draw", resourceType, dateDir, fileName);
        Path targetPath = Path.of(properties.getUploadDir()).resolve(relativePath).toAbsolutePath().normalize();
        try {
            Files.createDirectories(targetPath.getParent());
            file.transferTo(targetPath);
        } catch (IOException exception) {
            throw new BusinessException("فشل رفع المورد: " + exception.getMessage());
        }
        String localUrl = "/uploads/" + relativePath.toString().replace('\\', '/');
        String url = ResourceUrlUtils.cdnUrl(properties.getCdnBaseUrl(), localUrl);
        return new ResourceUploadResponse(url, localUrl, fileName, file.getSize());
    }

    private String extension(String originalFilename) {
        if (originalFilename == null || !originalFilename.contains(".")) {
            throw new BusinessException("الملف يفتقد الامتداد");
        }
        return originalFilename.substring(originalFilename.lastIndexOf('.') + 1).toLowerCase(Locale.ROOT);
    }

    private String stripExtension(String fileName) {
        int index = fileName.lastIndexOf('.');
        return index < 0 ? fileName : fileName.substring(0, index);
    }

    private String wordKey(String word) {
        return word == null ? "" : word.trim().toLowerCase(Locale.ROOT);
    }
}
