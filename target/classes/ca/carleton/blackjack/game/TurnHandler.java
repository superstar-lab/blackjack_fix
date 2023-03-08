package ca.carleton.blackjack.game;

import ca.carleton.blackjack.game.entity.AIPlayer;
import ca.carleton.blackjack.game.entity.Player;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import static ca.carleton.blackjack.game.BlackJackGame.uniqueResult;
import static java.util.Collections.shuffle;
import static org.apache.commons.collections4.CollectionUtils.isEmpty;

/**
 * Handle the order of the turns.
 * <p/>
 * Created by Mike on 11/4/2015.
 */
@Service
public class TurnHandler {

    private static final Logger LOG = LoggerFactory.getLogger(TurnHandler.class);

    private List<Player> ordering;

    /**
     * Initialize a new round with the given players.
     *
     * @param players the players.
     */
    public void initializeNewRound(final List<Player> players) {

        this.ordering = new ArrayList<>();

        // Add admin
        this.ordering.addAll(players.stream()
                .filter(Player::isAdmin)
                .collect(Collectors.toList()));

        // Add real players minus the admin
        final List<Player> realPlayers = players.stream()
                .filter(Player::isReal)
                .filter(player -> !player.isAdmin())
                .collect(Collectors.toList());
        // Don't shuffle - ordering is who joined first (aka the admin goes first always...)
        this.ordering.addAll(realPlayers);

        // Add ai players next
        final List<Player> aiPlayers = players.stream()
                .filter(player -> player instanceof AIPlayer && !((AIPlayer) player).isDealer())
                .collect(Collectors.toList());
        shuffle(aiPlayers);
        this.ordering.addAll(aiPlayers);

        // Add dealer
        final Player dealer = players.stream()
                .filter(player -> player instanceof AIPlayer && ((AIPlayer) player).isDealer())
                .collect(uniqueResult());
        this.ordering.add(dealer);
        LOG.info("New ordering: {}", this.ordering);
    }

    public boolean replaceDisconnectedPlayer(final Player old, final AIPlayer ai) {
        final int indexOf = this.ordering.indexOf(old);
        if (indexOf == -1) {
            LOG.warn("Warning! Player that disconnect was currently taking his turn - need to force a new result.");
            return false;
        }
        this.ordering.remove(indexOf);

        if (this.ordering.size() == 3) {
            // should always be true - put them before [player 0, ai 1, <INSERT> 2 , DEALER]
            this.ordering.add(2, ai);
        }
        LOG.info("Replaced ordering is {}", this.ordering);
        return true;
    }

    /**
     * Get the next player to go.
     *
     * @return the next player.
     */
    public Player getNextPlayer() {
        if (this.ordering.size() == 0) {
            throw new IllegalStateException("No players remaining!");
        }
        return this.ordering.remove(0);
    }

    public void clearAll() {
        this.ordering = null;
    }

    public boolean isNextPlayerAI() {
        return !isEmpty(this.ordering) && this.ordering.get(0) instanceof AIPlayer;
    }

    /**
     * true if we need to re-initialize the ordering.
     */
    public boolean requiresReInitialization() {
        return this.ordering == null || this.ordering.size() == 0;
    }
}

