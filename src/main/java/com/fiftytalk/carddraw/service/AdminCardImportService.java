package com.fiftytalk.carddraw.service;

import com.fiftytalk.carddraw.common.BusinessException;
import com.fiftytalk.carddraw.config.CardDrawProperties;
import com.fiftytalk.carddraw.domain.CardStatus;
import com.fiftytalk.carddraw.domain.Rarity;
import com.fiftytalk.carddraw.dto.AdminCardImportResponse;
import com.fiftytalk.carddraw.entity.Card;
import com.fiftytalk.carddraw.repository.CardRepository;
import com.fiftytalk.carddraw.util.ResourceUrlUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;

import javax.xml.parsers.DocumentBuilderFactory;
import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

@Service
public class AdminCardImportService {
    private final CardRepository cardRepository;
    private final CardDrawProperties properties;

    public AdminCardImportService(CardRepository cardRepository, CardDrawProperties properties) {
        this.cardRepository = cardRepository;
        this.properties = properties;
    }

    @Transactional
    public AdminCardImportResponse importExcel(MultipartFile file, String poolVersion, boolean skipDuplicates) {
        if (file == null || file.isEmpty()) {
            throw new BusinessException("يرجى اختيار ملف Excel أولًا");
        }
        String normalizedPoolVersion = poolVersion == null || poolVersion.isBlank() ? "v1" : poolVersion.trim();
        try {
            List<Map<String, String>> rows = readWorkbookRows(file.getBytes());
            int maxSortNo = cardRepository.findAll().stream()
                    .filter(card -> card.getPoolVersion().equals(normalizedPoolVersion))
                    .map(Card::getSortNo)
                    .filter(Objects::nonNull)
                    .max(Integer::compareTo)
                    .orElse(0);
            Set<String> existingKeys = new HashSet<>();
            for (Card card : cardRepository.findAll()) {
                if (card.getPoolVersion().equals(normalizedPoolVersion)) {
                    existingKeys.add(cardKey(card.getWord(), card.getRarity()));
                }
            }

            List<String> errors = new ArrayList<>();
            Map<String, Integer> createdByRarity = new LinkedHashMap<>();
            int totalRows = 0;
            int createdCount = 0;
            int skippedCount = 0;

            for (int i = 0; i < rows.size(); i++) {
                Map<String, String> row = rows.get(i);
                String word = first(row, "word", "الكلمة");
                if (word.isBlank()) {
                    continue;
                }
                totalRows++;
                String meaning = first(row, "meaning", "المعنى");
                String arabicMeaning = first(row, "arabicMeaning", "arabic", "المعنى بالعربية");
                String rarityText = first(row, "rarity", "الندرة");
                String source = first(row, "source", "المصدر");
                String category = first(row, "category", "الفئة");
                List<Rarity> rarities = parseRarities(rarityText);
                if (rarities.isEmpty()) {
                    errors.add("رقم " + (i + 2) + " صف بندرة غير معروفة: " + rarityText);
                    continue;
                }
                for (Rarity rarity : rarities) {
                    String key = cardKey(word, rarity);
                    if (skipDuplicates && existingKeys.contains(key)) {
                        skippedCount++;
                        continue;
                    }
                    Card card = new Card(
                            word.trim(),
                            meaning.trim(),
                            arabicMeaning.trim(),
                            audioUrl(word.trim()),
                            "https://cdn.example.com/card/" + rarity.name().toLowerCase() + "/" + word.trim() + ".png",
                            rarity,
                            normalizedPoolVersion,
                            ++maxSortNo
                    );
                    card.setStatus(CardStatus.ONLINE);
                    cardRepository.save(card);
                    existingKeys.add(key);
                    createdCount++;
                    createdByRarity.merge(rarity.name(), 1, Integer::sum);
                }
            }
            return new AdminCardImportResponse(totalRows, createdCount, skippedCount, createdByRarity, errors);
        } catch (BusinessException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new BusinessException("فشل تحليل Excel، يرجى التأكد من أن الملف .xlsx ويتضمن: word, meaning, rarity");
        }
    }

    private List<Map<String, String>> readWorkbookRows(byte[] bytes) throws Exception {
        Map<String, byte[]> entries = unzip(bytes);
        byte[] sheetBytes = entries.get("xl/worksheets/sheet1.xml");
        if (sheetBytes == null) {
            sheetBytes = entries.entrySet().stream()
                    .filter(entry -> entry.getKey().startsWith("xl/worksheets/sheet") && entry.getKey().endsWith(".xml"))
                    .sorted(Map.Entry.comparingByKey())
                    .map(Map.Entry::getValue)
                    .findFirst()
                    .orElseThrow(() -> new BusinessException("لا توجد ورقة قابلة للقراءة في Excel"));
        }
        List<String> sharedStrings = readSharedStrings(entries.get("xl/sharedStrings.xml"));
        Document sheet = parseXml(sheetBytes);
        NodeList rowNodes = sheet.getElementsByTagName("row");
        List<String> headers = new ArrayList<>();
        List<Map<String, String>> rows = new ArrayList<>();
        boolean headerFound = false;

        for (int i = 0; i < rowNodes.getLength(); i++) {
            Element row = (Element) rowNodes.item(i);
            Map<Integer, String> cells = readCells(row, sharedStrings);
            if (!headerFound) {
                if (cells.values().stream().anyMatch(value -> value.contains("word") || value.contains("الكلمة"))) {
                    int maxColumn = cells.keySet().stream().max(Integer::compareTo).orElse(0);
                    for (int col = 0; col <= maxColumn; col++) {
                        headers.add(cells.getOrDefault(col, "").trim());
                    }
                    headerFound = true;
                }
                continue;
            }
            Map<String, String> mapped = new LinkedHashMap<>();
            for (int col = 0; col < headers.size(); col++) {
                String header = headers.get(col);
                if (!header.isBlank()) {
                    mapped.put(header, cells.getOrDefault(col, "").trim());
                }
            }
            rows.add(mapped);
        }
        if (!headerFound) {
            throw new BusinessException("لم يتم العثور على عمود word في رأس Excel");
        }
        return rows;
    }

    private Map<String, byte[]> unzip(byte[] bytes) throws Exception {
        Map<String, byte[]> entries = new HashMap<>();
        try (ZipInputStream zip = new ZipInputStream(new ByteArrayInputStream(bytes))) {
            ZipEntry entry;
            while ((entry = zip.getNextEntry()) != null) {
                if (!entry.isDirectory()) {
                    entries.put(entry.getName(), zip.readAllBytes());
                }
            }
        }
        return entries;
    }

    private List<String> readSharedStrings(byte[] bytes) throws Exception {
        if (bytes == null) {
            return List.of();
        }
        Document document = parseXml(bytes);
        NodeList items = document.getElementsByTagName("si");
        List<String> strings = new ArrayList<>();
        for (int i = 0; i < items.getLength(); i++) {
            Element item = (Element) items.item(i);
            NodeList textNodes = item.getElementsByTagName("t");
            StringBuilder value = new StringBuilder();
            for (int j = 0; j < textNodes.getLength(); j++) {
                value.append(textNodes.item(j).getTextContent());
            }
            strings.add(value.toString());
        }
        return strings;
    }

    private Map<Integer, String> readCells(Element row, List<String> sharedStrings) {
        Map<Integer, String> cells = new TreeMap<>();
        NodeList cellNodes = row.getElementsByTagName("c");
        for (int i = 0; i < cellNodes.getLength(); i++) {
            Element cell = (Element) cellNodes.item(i);
            int column = columnIndex(cell.getAttribute("r"));
            cells.put(column, cellValue(cell, sharedStrings));
        }
        return cells;
    }

    private String cellValue(Element cell, List<String> sharedStrings) {
        String type = cell.getAttribute("t");
        if ("inlineStr".equals(type)) {
            NodeList textNodes = cell.getElementsByTagName("t");
            StringBuilder value = new StringBuilder();
            for (int i = 0; i < textNodes.getLength(); i++) {
                value.append(textNodes.item(i).getTextContent());
            }
            return value.toString();
        }
        NodeList values = cell.getElementsByTagName("v");
        if (values.getLength() == 0) {
            return "";
        }
        String raw = values.item(0).getTextContent();
        if ("s".equals(type)) {
            int index = Integer.parseInt(raw);
            return index >= 0 && index < sharedStrings.size() ? sharedStrings.get(index) : "";
        }
        return raw;
    }

    private Document parseXml(byte[] bytes) throws Exception {
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
        factory.setFeature("http://xml.org/sax/features/external-general-entities", false);
        factory.setFeature("http://xml.org/sax/features/external-parameter-entities", false);
        return factory.newDocumentBuilder().parse(new InputSource(new ByteArrayInputStream(new String(bytes, StandardCharsets.UTF_8).getBytes(StandardCharsets.UTF_8))));
    }

    private int columnIndex(String cellReference) {
        int result = 0;
        for (int i = 0; i < cellReference.length(); i++) {
            char ch = cellReference.charAt(i);
            if (ch < 'A' || ch > 'Z') {
                break;
            }
            result = result * 26 + (ch - 'A' + 1);
        }
        return Math.max(0, result - 1);
    }

    private String first(Map<String, String> row, String... keys) {
        for (String key : keys) {
            String value = row.get(key);
            if (value != null && !value.isBlank()) {
                return value.trim();
            }
        }
        return "";
    }

    private List<Rarity> parseRarities(String text) {
        if (text == null || text.isBlank()) {
            return List.of();
        }
        List<Rarity> rarities = new ArrayList<>();
        String normalized = text.toUpperCase(Locale.ROOT).replaceAll("[，、/;；]+", " ");
        for (String token : normalized.split("[,\\s]+")) {
            if (token.isBlank()) {
                continue;
            }
            try {
                rarities.add(Rarity.valueOf(token));
            } catch (IllegalArgumentException ignored) {
                // Ignore non-rarity notes in mixed cells.
            }
        }
        return rarities;
    }

    private String cardKey(String word, Rarity rarity) {
        return word.trim().toLowerCase(Locale.ROOT) + "|" + rarity.name();
    }

    private String audioUrl(String word) {
        return ResourceUrlUtils.cdnUrl(properties.getCdnBaseUrl(), "uploads/card-draw/audio/v1/" + word + ".mp3");
    }
}
