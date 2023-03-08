package ca.carleton.blackjack.game;

import ca.carleton.blackjack.game.entity.AIPlayer;
import ca.carleton.blackjack.game.entity.Player;
import ca.carleton.blackjack.game.entity.card.Card;
import ca.carleton.blackjack.game.entity.card.Rank;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

import static ca.carleton.blackjack.game.BlackJackGame.uniqueResult;
import static java.util.Arrays.asList;
import static java.util.stream.Collectors.toList;
import static org.springframework.util.CollectionUtils.containsAny;

/**
 * Service class implementing the logic of our program.
 * <p/>
 * Created by Mike on 10/7/2015.
 */
@Service
public class BlackJackService {

    private static final Logger LOG = LoggerFactory.getLogger(BlackJackService.class);

    /**
     * The action the dealer will take according to our game rules.
     *
     * @param dealer the dealer.
     * @return the option they will use for their next move.
     */
    public GameOption getDealerOption(final AIPlayer dealer) {
        final int handValue = (int) dealer.getHand().getHandValue();
        if (handValue < 17) {
            return GameOption.HIT;
        }
        final List<Card> cards = dealer.getHand().getCards();
        final List<Rank> cardRanks = cards.stream().map(Card::getRank).collect(toList());

        if (handValue == 17 && containsAny(cardRanks, asList(Rank.ACE_HIGH, Rank.ACE_LOW))) {
            return GameOption.HIT;
        } else if (handValue == 17) {
            return GameOption.STAY;
        }

        return GameOption.HIT;
    }

    /**
     * The action the AI will take according to our game rules.
     *
     * @param player       the AI.
     * @param otherPlayers the other players.
     * @return the option they will use for their next move.
     */
    public GameOption getAIOption(final AIPlayer player, final List<Player> otherPlayers) {

        if (this.shouldAISplit(player.getHand().getCards()) && !player.getHand().isSplitHand()) {
            return GameOption.SPLIT;
        }

        final int handValue = (int) player.getHand().getHandValue();
        if (handValue == 21) {
            LOG.info("Staying because AI has 21");
            return GameOption.STAY;
        }

        for (final Player other : otherPlayers) {
            if (other.getLastOption() == GameOption.STAY) {
                if (other.getHand().getCards().size() == 2) {
                    // should only have 1 visible if their two initial cards.
                    final Card visibleCard = other.getHand()
                            .getCards()
                            .stream()
                            .filter(card -> !card.isHidden())
                            .collect(uniqueResult());
                    if (visibleCard.getRank().getValue() == 10
                            || visibleCard.getRank() == Rank.ACE_LOW
                            || visibleCard.getRank() == Rank.ACE_HIGH) {
                        LOG.info("Hitting because AI saw that another player stayed with 2 cards (10 visible).");
                        return GameOption.HIT;
                    }
                }
            }
        }

        final List<Player> othersThatAreNotBust = otherPlayers.stream()
                .filter(otherGuy -> otherGuy.getLastOption() == null || (otherGuy.getLastOption() != null && otherGuy.getLastOption() != GameOption.BUST))
                .collect(Collectors.toList());

        if (handValue >= 18 && handValue <= 20) {
            for (final Player other : othersThatAreNotBust) {
                final List<Card> visibleCards = other.getHand()
                        .getCards()
                        .stream()
                        .filter(card -> !card.isHidden())
                        .collect(Collectors.toList());
                final int valueOfVisibleCards = (int) (long) visibleCards.stream()
                        .mapToInt(card -> card.getRank().getValue())
                        .sum();
                LOG.info("Value of visible cards for {} is {}", other, valueOfVisibleCards);
                if (valueOfVisibleCards > (handValue - 10)) {
                    LOG.info("Hitting because value of visible cards > (hand value - 10). Checked against {}", other);
                    return GameOption.HIT;
                }
            }
            LOG.info("Staying because value is between 18 and 20)");
            return GameOption.STAY;
        }

        LOG.info("Hitting because ran out of options.");
        return GameOption.HIT;
    }

    /**
     * We only split if the initial cards are the same rank.
     */
    private boolean shouldAISplit(final List<Card> cards) {
        return cards.size() == 2 && cards.get(0).getRank() == cards.get(1).getRank();
    }

}
