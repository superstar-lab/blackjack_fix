package ca.carleton.blackjack.selenium;

import org.junit.Test;
import org.openqa.selenium.WebElement;

import java.util.List;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

/**
 * Test the ability to connect and disconnect without getting any weird errors.
 * <p/>
 * Created by Mike on 11/8/2015.
 */
//@SeleniumTest
public class ConnectionBasicsTest extends AbstractSeleniumTest {


    @Test
    public void canConnect() {
        this.indexPage.connect();
        assertThat(this.indexPage.hasText("Successfully connected to the game with unique"), is(true));
        assertThat(this.indexPage.hasText("You have been designated the admin for this game."), is(true));
        this.indexPage.disconnect();
        assertThat(this.indexPage.hasText("Connection closed"), is(true));
    }

    @Test
    public void canOpenLobby() {
        this.indexPage.connect();
        assertThat(this.waitForDisplayed(this.indexPage.numberPlayers).isEnabled(), is(true));
        this.indexPage.open.click();
        assertThat(this.indexPage.hasText("Opening the lobby with specified settings."), is(true));
        this.indexPage.disconnect();
    }

    @Test
    public void canStartGame() {
        this.indexPage.connect();
        this.waitForDisplayed(this.indexPage.numberPlayers).isEnabled();
        this.indexPage.numberPlayers.sendKeys("1");
        this.indexPage.open.click();
        assertThat(this.indexPage.hasText("The game is now ready to begin. Press start when ready."), is(true));
        this.indexPage.start.click();
        assertThat(this.indexPage.hasText("The game has started! Please wait for your turn."), is(true));
        this.indexPage.disconnect();
    }

    @Test
    public void canSeeCardsAfterStart() throws InterruptedException {
        this.indexPage.quickStart();
        assertThat(this.indexPage.countNumberOfCardsFor(this.indexPage.playerCards), is(2));
        assertThat(this.indexPage.countNumberOfCardsFor(this.indexPage.dealerCards), is(2));
        assertThat(this.indexPage.countNumberOfCardsFor(this.indexPage.otherPlayer1Cards), is(2));
        assertThat(this.indexPage.countNumberOfCardsFor(this.indexPage.otherPlayer2Cards), is(2));

        // Want to see if first card is hidden as it should be
        final List<WebElement> dealerCards = this.indexPage.getAllCardsFor(this.indexPage.dealerCards);
        final String hiddenCardHTML = dealerCards.get(0).getAttribute("innerHTML");
        assertThat(hiddenCardHTML.contains("card back"), is(true));

        // However, our cards should always be visible.
        final List<WebElement> playerCards = this.indexPage.getAllCardsFor(this.indexPage.playerCards);
        final String visibleCardHTML = playerCards.get(0).getAttribute("innerHTML");
        assertThat(visibleCardHTML.contains("card back"), is(false));
        assertThat(visibleCardHTML.contains("<span class=\"rank\">"), is(true));
        this.indexPage.disconnect();
    }
}
