import java.io.FileWriter;
import java.time.Duration;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.openqa.selenium.By;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.Keys;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.interactions.Actions;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;


public class TokopediaScraper {
    static String url = "https://www.tokopedia.com/p/handphone-tablet/handphone?ob=5&page=1";
    static int delay = 20;
    static int numOfProducts = 2;
    public static void main(String[] args) {
        System.setProperty("webdriver.chrome.driver", "/Users/agungwicaksono/Downloads/chromedriver");
        WebDriver driver = new ChromeDriver();
        JavascriptExecutor jse = (JavascriptExecutor)driver;
        driver.get(url);
        WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(delay));
        String paginationClass = "css-txlndr-unf-pagination";
        String productClass = "css-bk6tzz";
        String nextAndPrevClass = "css-ad7yub-unf-pagination-item";
        List<Map<String, Object>> productList = new ArrayList<>();
        int index = 0;
        List<WebElement> productElementList = new ArrayList<>();
        while (productList.size() < numOfProducts) {
            jse.executeScript("window.scrollTo(0, 1080)");
            wait.until(ExpectedConditions.presenceOfElementLocated(By.className(paginationClass)));
            if (productElementList.size() == 0) {
                productElementList = driver.findElements(By.className(productClass));
            }
            WebElement elementClicked = productElementList.get(index);
            boolean isAds = true;
            try {
                elementClicked.findElement(By.className("css-nysll7"));
            } catch (Exception e) {
                isAds = false;
            }
            if (!isAds) {
                Actions actions = new Actions(driver);
                actions.keyDown(Keys.COMMAND).click(elementClicked).perform();
                driver.switchTo().window(driver.getWindowHandles().toArray()[1].toString());
                Map<String, Object> productDict = createDict(driver, wait);
                if (productDict != null) {
                    productList.add(productDict);
                }
                driver.close();
                driver.switchTo().window(driver.getWindowHandles().toArray()[0].toString());
            }
            if (index >= productElementList.size() - 1) {
                List<WebElement> nextAndPrevButton = driver.findElements(By.className(nextAndPrevClass));
                WebElement nextButton = nextAndPrevButton.get(1);
                nextButton.click();
                productElementList = new ArrayList<>();
                index = 0;
            }
            index += 1;
        }
        driver.close();
        // Save productList to csv
        // with ; as delimiter
        try {
            FileWriter writer = new FileWriter("./product.csv");
            for (Map<String, Object> product : productList) {
                // every key in product is a column name
                // only write the first row
                if (productList.indexOf(product) == 0) {
                    for (String key : product.keySet()) {
                        writer.write(key + ";");
                    }
                    writer.write("\n");
                }
                for (String key : product.keySet()) {
                    writer.write(product.get(key) + ";");
                }
                writer.write("\n");
            }
            writer.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static Map<String, Object> createDict(WebDriver driver, WebDriverWait wait) {
        String nameClass = "css-t9du53";
        String moreDetailButtonClass = "css-1n6vhqs";
        String descriptionClass = "css-1k1relq";
        String imageClass = "css-1c345mg";
        String priceClass = "css-aqsd8m";
        String ratingClass = "icon-star";
        String sellerClass = "css-12gb68h";


        String name = "";
        String description = "";
        List<String> imageList = new ArrayList<>();
        int price = 0;
        float rating = 0;
        String seller = "";

        // wait for the page to load
        wait.until(ExpectedConditions.presenceOfElementLocated(By.className(ratingClass)));

        try{
            WebElement moreDetailButton = driver.findElement(By.className(moreDetailButtonClass));
            moreDetailButton.click();
        } catch (Exception e) {
            descriptionClass = "css-17zm3l";
        }

        try {
            name = driver.findElement(By.className(nameClass)).getText();
            description = driver.findElement(By.className(descriptionClass)).getText();
            imageList = new ArrayList<>();
            List<WebElement> imageElementList = driver.findElements(By.className(imageClass));
            for (WebElement imageElement : imageElementList) {
                imageList.add(imageElement.getAttribute("src"));
            }
            price = Integer.parseInt(driver.findElement(By.className(priceClass)).getText().replace(".", "")
                    .replace("Rp", "").replace(" ", "").replace(".", ""));
            rating = Float.parseFloat(
                    driver.findElement(By.xpath("//img[@class='"+ratingClass+"']/following-sibling::span[@class='main']"))
                            .getText());
            seller = driver.findElement(By.className(sellerClass)).getText();
        } catch (Exception e) {
            System.out.println(e);
            return null;
        }
        Map<String, Object> productDict = new HashMap<>();
        productDict.put("name", name);
        productDict.put("description", description);
        productDict.put("image", imageList);
        productDict.put("price", price);
        productDict.put("rating", rating);
        productDict.put("seller", seller);
        return productDict;
    }
}