package com.milktea.bot.service;

import com.milktea.bot.model.MenuItem;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class MenuService {

    private static final Logger log = LoggerFactory.getLogger(MenuService.class);

    private final List<MenuItem> allItems = new ArrayList<>();
    private final Map<String, List<MenuItem>> categoryMap = new LinkedHashMap<>();
    private final Map<String, MenuItem> itemIdMap = new HashMap<>();

    @PostConstruct
    public void loadMenu() throws Exception {
        ClassPathResource resource = new ClassPathResource("Menu.csv");
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(resource.getInputStream(), StandardCharsets.UTF_8))) {

            reader.readLine(); // skip header

            String line;
            while ((line = reader.readLine()) != null) {
                line = line.trim();
                if (line.isEmpty()) continue;

                String[] parts = line.split(",");
                if (parts.length < 7) continue;

                MenuItem item = new MenuItem(
                        parts[0].trim(),
                        parts[1].trim(),
                        parts[2].trim(),
                        parts[3].trim(),
                        Integer.parseInt(parts[4].trim()),
                        Integer.parseInt(parts[5].trim()),
                        Boolean.parseBoolean(parts[6].trim())
                );

                allItems.add(item);
                itemIdMap.put(item.getItemId(), item);
                categoryMap.computeIfAbsent(item.getCategory(), k -> new ArrayList<>()).add(item);
            }
        }

        log.info("Loaded {} menu items in {} categories", allItems.size(), categoryMap.size());
    }

    public List<String> getDrinkCategories() {
        return categoryMap.keySet().stream()
                .filter(cat -> !"Topping".equals(cat))
                .collect(Collectors.toList());
    }

    public List<MenuItem> getItemsByCategory(String category) {
        return categoryMap.getOrDefault(category, Collections.emptyList()).stream()
                .filter(MenuItem::isAvailable)
                .collect(Collectors.toList());
    }

    public MenuItem getItemById(String itemId) {
        return itemIdMap.get(itemId);
    }

    public List<MenuItem> getAvailableToppings() {
        return categoryMap.getOrDefault("Topping", Collections.emptyList()).stream()
                .filter(MenuItem::isAvailable)
                .collect(Collectors.toList());
    }

    public String getCategoryEmoji(String category) {
        return switch (category) {
            case "Trà Sữa" -> "🧋";
            case "Trà Trái Cây" -> "🍹";
            case "Cà Phê" -> "☕";
            case "Đá Xay" -> "🧊";
            default -> "📋";
        };
    }
}
