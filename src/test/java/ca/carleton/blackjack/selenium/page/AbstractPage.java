package ca.carleton.blackjack.selenium.page;

import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.PageFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Represents a selenium.page.
 *
 * Created by Mike on 10/6/2015.
 */

public abstract class AbstractPage<T extends AbstractPage<T>> {

    private static final Logger LOG = LoggerFactory.getLogger(AbstractPage.class);

    @Autowired
    protected WebDriver webDriver;

    protected abstract String getPageName();

    @PostConstruct
    public void init() {
        LOG.warn("Initializing elements for page {}.", this.getClass());
        PageFactory.initElements(this.webDriver, this);
    }

    public AbstractPage() {
        super();
    }

    public AbstractPage(final WebDriver webDriver) {
        this.webDriver = webDriver;
      LOG.warn("Initializing elements for page {}.", this.getClass());
        PageFactory.initElements(this.webDriver, this);
    }

    public T open() {
        final Map<String, Object> parameters = new HashMap<>();
        return this.openPage(parameters);
    }

    private T openPage(final Map<String, Object> parameters) {
        this.webDriver.get(String.format("http://localhost:8080/%s", this.getPageName()));
        return (T) this;
    }

    public String getTitle() {
        return this.webDriver.getTitle();
    }

    public String getUrl() {
        return this.webDriver.getCurrentUrl();
    }

    /**
     * Check whether or not the web driver can find the given text.
     *
     * @param searchKey the text.
     * @return true if yes.
     */
    public boolean hasText(final String searchKey) {
        final List<WebElement> result = this.webDriver.findElements(By.xpath("//*[contains(text(),'" + searchKey + "')]"));
        return result != null && result.size() > 0;
    }

    public void refreshWithNewDriver(WebDriver webDriver){
      this.webDriver = webDriver;
      LOG.warn("Refreshing elements for page {}.", this.getClass());
      PageFactory.initElements(this.webDriver, this);
    }
}

