import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.chrome.ChromeDriver;
import org.languagetool.JLanguageTool;
import org.languagetool.language.Russian;
import org.languagetool.rules.RuleMatch;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/*
 * В данном проекте предоставлена более упрощенная версия реализации данного задания
 *
 * Так как много ручной работы в виде добавления списка исключений (причем одного типа, остальные виды исключений,
 * например, на определенную ошибку, необходимо также вручную вносить в коде), ручного поиска слов на странице
 *
 * Для улучшения данного проекта можно добавить и реализовать следующее
 *
 * class ErrorFromPage
 *
 * атрибуты:
 * String errorWord - сама ошибка
 * String errorType - тип ошибки
 * String errorUrl - адрес, где была найдена
 *
 * функции:
 * void printContextForError(ErrorFromPage errors) - напечатать контекст по ошибке для упрощения поиска на странице
 * void writeErrorsToFile(String filePath, List<ErrorFromPage> errors) - записать ошибки в файл
 * List<ErrorFromPage> readErrorsToFile(String filePath) - чтение ошибок из файла
 *
 *
 * Это позволит после первого запуска теста записать все найденные ошибки в файл.
 * Дальше создать файл с исключениями и внести туда те ошибки, которые не нужно учитывать
 * (придется просмотреть все найденные ошибки вручную, определить являются ли они ошибками).
 * Те ошибки, которые не удается найти по слову, можно будет найти благодаря выводу контекста.
 * Тем самым после второго запуска останутся только те ошибки, которые остались вне списка исключений.
 * По итогу список исключений будет указывать на конкретную ошибку на странице, а не в общем виде.
 * Соответственно после исправления ошибок не будет необходимости обновлять таблицу исключений,
 * если она того не требует после исправлений
 *
 */

public class Task3Test {
    private WebDriver driver;
    private JLanguageTool languageTool;
    private Set<String> visitedPages;
    private Set<String> exclusions;

    @BeforeEach
    public void setUp() {
        driver = new ChromeDriver();
        languageTool = new JLanguageTool(new Russian());
        visitedPages = new HashSet<>();
        exclusions = loadExclusionsFromFile("src/main/resources/exclusion.txt");
    }

    @AfterEach
    public void tearDown() {
        driver.quit();
    }

    @Test
    public void checkSpellAllPages() {
        String startUrl = "https://nexign.com/ru";
        driver.get(startUrl);

        visitedPages.add(startUrl);
        checkPage(startUrl);

        List<String> links = getAllLinks();
        for (String link : links) {
            if (!visitedPages.contains(link)) {
                visitedPages.add(link);
                checkPage(link);
            }
        }
    }

    private void checkPage(String url) {
        try {
            driver.get(url);
            WebElement body = driver.findElement(By.tagName("body"));
            String pageText = body.getText();
            List<String> errors = checkSpell(pageText);
            if (!errors.isEmpty()) {
                System.out.println("Ошибки на странице " + url + ":");
                for (String error : errors) {
                    System.out.println("  - " + error);
                }
            }
        } catch (Exception e) {
            System.err.println("Ошибка при обработке страницы " + url + ": " + e.getMessage());
        }
    }

    private List<String> getAllLinks() {
        List<WebElement> elements = driver.findElements(By.tagName("a"));
        List<String> links = new ArrayList<>();
        for (WebElement element : elements) {
            String href = element.getDomProperty("href");
            if (href != null && href.startsWith("https://nexign.com/ru")
                    && !visitedPages.contains(href)) {
                links.add(href);
            }
        }
        return links;
    }

    private List<String> checkSpell(String text) {
        List<String> errors = new ArrayList<>();
        try {
            List<RuleMatch> matches = languageTool.check(text);
            for (RuleMatch match : matches) {
                String errorWord = text.substring(match.getFromPos(), match.getToPos());
                String errorType = match.getRule().getDescription();
                if (!errorType.equals("Точка или заглавная буква")
                        && (!errorType.equals("Проверка орфографии с исправлениями")
                        || !isExclusion(errorWord.toLowerCase()))) {
                    errors.add("Слово: " + errorWord + ", Тип ошибки: " + errorType);
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return errors;
    }

    private Set<String> loadExclusionsFromFile(String filePath) {
        Set<String> exclusions = new HashSet<>();
        try(BufferedReader reader = new BufferedReader(new FileReader(filePath))) {
            String line;
            while ((line = reader.readLine()) != null) {
                String withoutSpaces = line.strip();
                if (!withoutSpaces.isEmpty())
                    exclusions.add(withoutSpaces.toLowerCase());
            }
        } catch (IOException e) {
            System.err.println("Ошибка при чтении файла исключений: " + e.getMessage());
        }
        return exclusions;
    }

    private boolean isExclusion(String word) {
        return exclusions.contains(word.toLowerCase());
    }
}
