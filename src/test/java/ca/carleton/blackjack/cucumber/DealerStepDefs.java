package ca.carleton.blackjack.cucumber;

import ca.carleton.blackjack.BlackJackApplication;
import ca.carleton.blackjack.game.BlackJackGame;
import ca.carleton.blackjack.game.GameOption;
import ca.carleton.blackjack.game.entity.AIPlayer;
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

import javax.annotation.PostConstruct;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;

/**
 * Step definitions for the dealer when he hits because his hand is less than 17.
 * <p/>
 * Created by Mike on 11/3/2015.
 */
public class DealerStepDefs {

    private AIPlayer dealer;

    private int numberOfCards;

    @Autowired
    private BlackJackGame blackJackGame;

    @PostConstruct
    public void init() {
        this.dealer = new AIPlayer(null);
        this.dealer.setDealer(true);
    }

    @Given("a/another card in the dealer's hand with the rank '{}' and suit '{}' and hidden '{}'")
    public void addCard(final Rank rank, final Suit suit, final boolean hidden) {
        this.dealer.getHand().addCard(new Card(rank, suit, hidden));
    }

    @When("^it is the dealer's turn to make a move")
    public void prepareTurn() {
        this.numberOfCards = this.dealer.getHand().getCards().size();
    }

    @Then("the dealer should perform their turn")
    public void getOption() {
        this.blackJackGame.doAITurn(this.dealer);
    }

    @Then("the dealer's last move should be '{}'")
    public void verify(final GameOption gameOption) {
        assertThat(this.dealer.getLastOption(), is(gameOption));
    }

    @Then("the dealer's last move will either be '{}' if their hand value is less than or equal to 21, else '{}'")
    public void verify(final GameOption gameOption, final GameOption gameOption2) {
        if (this.dealer.getHand().getHandValue() <= 21) {
            assertThat(this.dealer.getLastOption(), is(gameOption));
        } else {
            assertThat(this.dealer.getLastOption(), is(gameOption2));
        }
    }

    @Then("the dealer's hand should have one more card than before")
    public void verifyHandSizeDifferent() {
        assertThat(this.dealer.getHand().getCards().size(), is(this.numberOfCards + 1));
    }

    @Then("the dealer's hand should have the same number of cards")
    public void verifyHandSizeSame() {
        assertThat(this.dealer.getHand().getCards().size(), is(this.numberOfCards));
    }
}
