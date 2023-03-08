
package ca.carleton.blackjack.cucumber;

import ca.carleton.blackjack.BlackJackApplication;
import ca.carleton.blackjack.game.BlackJackGame;
import ca.carleton.blackjack.game.GameOption;
import ca.carleton.blackjack.game.entity.AIPlayer;
import ca.carleton.blackjack.game.entity.Player;
import ca.carleton.blackjack.game.entity.card.Card;
import ca.carleton.blackjack.game.entity.card.Rank;
import ca.carleton.blackjack.game.entity.card.Suit;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootContextLoader;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ContextConfiguration;

import static org.hamcrest.CoreMatchers.both;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.lessThan;
import static org.hamcrest.core.Is.is;

/**
 * Step definitions for our AI logic testing - split hand.
 * <p/>
 * Created by Mike on 11/3/2015.
 */

public class AIStepDefs {

    private final AIPlayer ai = new AIPlayer(null);

    private Player otherPlayer;

    private int numberOfCards;

    @Autowired
    private BlackJackGame blackJackGame;

    @Given("a/another card in the AI's hand with the rank '{}' and suit '{}' and hidden '{}'")
    public void addCard(final Rank rank, final Suit suit, final boolean hidden) {
        this.ai.getHand().addCard(new Card(rank, suit, hidden));
    }

    @Given("a/another player with two cards in their hand consisting of '{}' of '{}', hidden '{}' and '{}' of '{}', hidden '{}'")
    public void addPlayerWithCards(final Rank rank, final Suit suit, final boolean hidden,
                                   final Rank rank2, final Suit suit2, final boolean hidden2) {
        this.blackJackGame.registerPlayer(null);
        // HACK but we only add 1 other player in the rest so it should work.
        this.otherPlayer = this.blackJackGame.getConnectedPlayers().get(0);
        this.otherPlayer.getHand().addCard(new Card(rank, suit, hidden));
        this.otherPlayer.getHand().addCard(new Card(rank2, suit2, hidden2));
    }

    @When("the player has decided to stay their turn")
    public void playerStay() {
        this.otherPlayer.setLastOption(GameOption.STAY);
    }

    @When("^it is the AI's turn to make a move")
    public void prepareTurn() {
        this.numberOfCards = this.ai.getHand().getCards().size();
    }

    @Then("the AI should have a hand value of between 18 and 20")
    public void checkHandSize() {
        assertThat(this.ai.getHand().getHandValue(), both(greaterThan(17L)).and(lessThan(20L)));
    }

    @Then("the AI should perform their turn")
    public void getOption() {
        this.blackJackGame.doAITurn(this.ai);
    }

    @Then("the AI's last move should be '{}'")
    public void verifyLastMove(final GameOption gameOption) {
        assertThat(this.ai.getLastOption(), is(gameOption));
    }

    @Then("the AI's last move will either be '{}' if their hand value is less than or equal to 21, else '{}'")
    public void verifyLastMoveConditional(final GameOption gameOption, final GameOption gameOption2) {
        if (this.ai.getHand().getHandValue() <= 21) {
            assertThat(this.ai.getLastOption(), is(gameOption));
        } else {
            assertThat(this.ai.getLastOption(), is(gameOption2));
        }
    }

    @Then("the other player's last move should be '{}'")
    public void verifyLastMoveOtherPlayer(final GameOption gameOption) {
        assertThat(this.otherPlayer.getLastOption(), is(gameOption));
    }

    @Then("the AI's hand should have one more card than before")
    public void verifyHandSizeChanged() {
        assertThat(this.numberOfCards + 1, is(this.ai.getHand().getCards().size()));
    }

    @Then("the AI's hand size should remained unchanged")
    public void verifyHandSizeUnchanged() {
        assertThat(this.numberOfCards, is(this.ai.getHand().getCards().size()));
    }

}
