package org.springframework.samples.petclinic.selenium;
```

4. Scroll to bottom, click **Commit changes**
   - Commit message: `Fix test package declaration`
   - Click green **Commit changes** button

---

### Step 2: Move the File to Correct Location

Now we need to move the file to match the package structure.

1. Go to the same file again: `src/test/java/selenium/PetClinicSeleniumTest.java`

2. Click the **pencil icon** ✏️ to edit

3. Look at the **file path bar** at the top of the editor where it shows:
```
   SpringPetClinic / src / test / java / selenium / PetClinicSeleniumTest.java
```

4. **Click on the filename** `PetClinicSeleniumTest.java` in that path bar

5. **Backspace** to delete `selenium/PetClinicSeleniumTest.java`

6. **Type the new path:**
```
   org/springframework/samples/petclinic/selenium/PetClinicSeleniumTest.java

import org.openqa.selenium.WebDriver;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.WebDriverWait;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;
import io.github.bonigarcia.wdm.WebDriverManager;
import java.time.Duration;

public class PetClinicSeleniumTest {
    
    private WebDriver driver;
    private String baseUrl = "http://localhost:8080";
    private WebDriverWait wait;
    
    @BeforeClass
    public void setUp() {
        // Setup ChromeDriver
        WebDriverManager.chromedriver().setup();
        
        // Configure Chrome options for headless mode (important for Jenkins)
        ChromeOptions options = new ChromeOptions();
        options.addArguments("--headless");
        options.addArguments("--no-sandbox");
        options.addArguments("--disable-dev-shm-usage");
        options.addArguments("--disable-gpu");
        options.addArguments("--window-size=1920,1080");
        
        driver = new ChromeDriver(options);
        wait = new WebDriverWait(driver, Duration.ofSeconds(10));
        driver.manage().timeouts().implicitlyWait(Duration.ofSeconds(10));
    }
    
    @Test(priority = 1)
    public void testHomePageTitle() {
        driver.get(baseUrl);
        String title = driver.getTitle();
        Assert.assertTrue(title.contains("PetClinic"), 
            "Home page title should contain 'PetClinic'");
        System.out.println("✓ Test 1 Passed: Home page title is correct");
    }
    
    @Test(priority = 2)
    public void testHomePageWelcomeMessage() {
        driver.get(baseUrl);
        WebElement welcomeElement = wait.until(
            ExpectedConditions.presenceOfElementLocated(By.tagName("h2"))
        );
        String welcomeText = welcomeElement.getText();
        Assert.assertTrue(welcomeText.contains("Welcome"), 
            "Welcome message should be present on home page");
        System.out.println("✓ Test 2 Passed: Welcome message is displayed");
    }
    
    @Test(priority = 3)
    public void testFindOwnersPage() {
        driver.get(baseUrl);
        WebElement findOwnersLink = wait.until(
            ExpectedConditions.elementToBeClickable(
                By.xpath("//a[@title='find owners']")
            )
        );
        findOwnersLink.click();
        
        String currentUrl = driver.getCurrentUrl();
        Assert.assertTrue(currentUrl.contains("/owners/find"), 
            "Should navigate to find owners page");
        System.out.println("✓ Test 3 Passed: Find Owners page loads successfully");
    }
    
    @Test(priority = 4)
    public void testVeterinariansPage() {
        driver.get(baseUrl);
        WebElement veterinariansLink = wait.until(
            ExpectedConditions.elementToBeClickable(
                By.xpath("//a[@title='veterinarians']")
            )
        );
        veterinariansLink.click();
        
        String currentUrl = driver.getCurrentUrl();
        Assert.assertTrue(currentUrl.contains("/vets"), 
            "Should navigate to veterinarians page");
        
        // Verify table is present
        WebElement vetsTable = wait.until(
            ExpectedConditions.presenceOfElementLocated(By.tagName("table"))
        );
        Assert.assertNotNull(vetsTable, "Veterinarians table should be displayed");
        System.out.println("✓ Test 4 Passed: Veterinarians page loads with table");
    }
    
    @Test(priority = 5)
    public void testNavigationLinks() {
        driver.get(baseUrl);
        
        // Check if navigation bar exists
        WebElement navbar = wait.until(
            ExpectedConditions.presenceOfElementLocated(By.tagName("nav"))
        );
        Assert.assertNotNull(navbar, "Navigation bar should be present");
        
        // Verify at least 3 navigation links exist
        int linkCount = driver.findElements(By.cssSelector("nav a")).size();
        Assert.assertTrue(linkCount >= 3, 
            "Should have at least 3 navigation links");
        System.out.println("✓ Test 5 Passed: Navigation structure is correct");
    }
    
    @AfterClass
    public void tearDown() {
        if (driver != null) {
            driver.quit();
        }
    }
}
