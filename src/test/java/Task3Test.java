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

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;


public class Task3Test {
    private WebDriver driver;
    private JLanguageTool languageTool;
    private Set<String> visitedPages;

    @BeforeEach
    public void setUp() {
        driver = new ChromeDriver();
        languageTool = new JLanguageTool(new Russian());
        visitedPages = new HashSet<>();
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
                if (!errorType.equals("Точка или заглавная буква")) {
                    errors.add("Слово: " + errorWord + ", Тип ошибки: " + errorType);
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return errors;
    }
}
