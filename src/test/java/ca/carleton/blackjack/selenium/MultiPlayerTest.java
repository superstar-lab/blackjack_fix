package ca.carleton.blackjack.selenium;

import org.junit.Test;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;

import java.util.List;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.greaterThan;

/**
 * Tests surrounding multiple uesrs.
 * <p/>
 * Created by Mike on 11/8/2015.
 */
//@SeleniumTest
public class MultiPlayerTest extends AbstractSeleniumTest {

    @Test
    public void canMultiplePeopleConnect() {
        this.indexPage.connect();
        this.indexPage.setNumberPlayers(2);
        this.indexPage.open.click();
        // False, we want to wait for a second user.
        assertThat(this.indexPage.start.isEnabled(), is(false));

        // Lets connect a second player
        final WebDriver second = this.quickConnectSecondUser();
        this.delay(3);
        assertThat(this.indexPage.hasText("The game is now ready to begin. Press start when ready."), is(true));
        this.indexPage.disconnect();
        this.disconnectSecondUser(second);
    }

    @Test
    public void canMultiplePeoplePlayARound() throws Exception {
        this.indexPage.connect();
        this.indexPage.setNumberPlayers(2);
        this.indexPage.open.click();
        final WebDriver second = this.quickConnectSecondUser();
        this.delay(3);
        // We're now ready to play
        this.indexPage.start.click();
        // Admin should always go first.
        this.delay(2);
        this.indexPage.stay.click();
        this.delay(2);
        second.findElement(By.id("stay")).click();
        this.delay(3);
        // Now we should be resolved.
        assertThat(this.indexPage.hasText("To start another round, press the start button."), is(true));
        this.disconnectSecondUser(second);
        this.indexPage.disconnect();
    }

    @Test
    public void canSecondPlayerDisconnectInProgress() {
        this.indexPage.connect();
        this.indexPage.setNumberPlayers(2);
        this.indexPage.open.click();
        final WebDriver second = this.quickConnectSecondUser();
        // We're now ready to play
        this.indexPage.start.click();
        // Lets disconnect the second user.
        this.disconnectSecondUser(second);
        assertThat(this.indexPage.hasText("has disconnected from the game. He will be replaced by an AI"), is(true));
        this.indexPage.stay.click();
        assertThat(this.indexPage.hasText("To start another round, press the start button."), is(true));
        this.indexPage.disconnect();
    }

    @Test
    public void canAdminDisconnectGracefully() {
        this.indexPage.connect();
        this.indexPage.setNumberPlayers(2);
        this.indexPage.open.click();
        final WebDriver second = this.quickConnectSecondUser();
        // We're now ready to play
        this.indexPage.start.click();
        // Lets disconnect the admin
        this.indexPage.disconnect();
        final List<WebElement> result = second.findElements(By.xpath("//*[contains(text(),'" + "The administrator has left. Current sessions will be disconnected" + "')]"));
        assertThat(result.size(), is(greaterThan(0)));
        this.disconnectSecondUser(second);
    }

}
