package ca.carleton.blackjack.cucumber;

import ca.carleton.blackjack.BlackJackApplication;
import ca.carleton.blackjack.game.BlackJackGame;
import ca.carleton.blackjack.game.GameOption;
import ca.carleton.blackjack.game.entity.Player;
import ca.carleton.blackjack.game.entity.card.Card;
import ca.carleton.blackjack.game.entity.card.HandStatus;
import ca.carleton.blackjack.game.entity.card.Rank;
import ca.carleton.blackjack.game.entity.card.Suit;
import io.cucumber.java.en.And;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootContextLoader;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ContextConfiguration;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.lessThan;
import static org.hamcrest.core.Is.is;

/**
 * Step definitions for win conditions.
 * <p/>
 * Created by Mike on 11/3/2015.
 */
public class WinConditionStepDefs {

    final Player player = new Player(null);

    @Autowired
    private BlackJackGame blackJackGame;

    @Given("A/Another/a player playing the game")
    public void addPlayer() {
        this.blackJackGame.registerPlayer(null);
    }

    @Given("player {int} has a card with the rank '{}' and suit '{}' and hidden '{}'")
    public void addCardForPlayer(final int index, final Rank rank, final Suit suit, final boolean hidden) {
        this.blackJackGame.getConnectedPlayers().get(index - 1).getHand().addCard(new Card(rank, suit, hidden));
    }

    @Given("player {int} has another card with the rank '{}' and suit '{}' and hidden '{}'")
    public void addCardForPlayer2(final int index, final Rank rank, final Suit suit, final boolean hidden) {
        this.blackJackGame.getConnectedPlayers().get(index - 1).getHand().addCard(new Card(rank, suit, hidden));
    }

    @And("player {int} has his last option as '{}'")
    public void setOption(final int index, final GameOption option) {
        this.blackJackGame.getConnectedPlayers().get(index - 1).setLastOption(option);
    }

    @When("the game is resolved")
    public void resolve() {
        this.blackJackGame.resolveRound();
    }

    @Then("player {int} should have a hand status of '{}'")
    public void checkStatus(final int index, final HandStatus option) {
        assertThat(this.blackJackGame.getConnectedPlayers().get(index - 1).getHand().getHandStatus(), is(option));
    }

    @Then("player {int} should have a hand that isn't bust")
    public void checkNotBust(final int index) {
        assertThat(this.blackJackGame.getConnectedPlayers().get(index - 1).getHand().getHandValue(), is(lessThan(22L)));
    }

    @Given("a/another card in the player's hand with the rank '{}' and suit '{}' and hidden '{}'")
    public void addCard(final Rank rank, final Suit suit, final boolean hidden) {
        this.player.getHand().addCard(new Card(rank, suit, hidden));
    }

    @When("the player draws his seventh card with the rank '{}' and suit '{}' and hidden '{}'")
    public void drawLastCard(final Rank rank, final Suit suit, final boolean hidden) throws Throwable {
        this.player.getHand().addCard(new Card(rank, suit, hidden));
        assertThat(this.player.getHand().getCards().size(), is(7));
    }

    @And("the player's hand value doesn't exceed {int}")
    public void checkHandValue(final int handValue) throws Throwable {
        assertThat(this.player.getHand().getHandValue(), is(lessThan((long) handValue)));
    }

    @Then("^the player immediately wins with a seven card charlie")
    public void setWinner() throws Throwable {
        this.blackJackGame.registerPlayer(null);
        this.blackJackGame.resolveRoundSevenCardCharlie(this.player);
    }

    @And("^the other player's statuses should be set")
    public void checkLosers() throws Throwable {
        assertThat(this.blackJackGame.getConnectedPlayers()
                .stream()
                .filter(player -> player.getHand().getHandStatus() == HandStatus.LOSER)
                .count(), is(1L));
    }

    @And("^the player's status should be set")
    public void checkWinner() throws Throwable {
        assertThat(this.player.getHand().getHandStatus(), is(HandStatus.SEVEN_CARD_CHARLIE));
    }
}
