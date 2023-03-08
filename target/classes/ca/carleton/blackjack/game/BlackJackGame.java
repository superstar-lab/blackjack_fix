package ca.carleton.blackjack.game;

import ca.carleton.blackjack.game.entity.AIPlayer;
import ca.carleton.blackjack.game.entity.Player;
import ca.carleton.blackjack.game.entity.card.Card;
import ca.carleton.blackjack.game.entity.card.HandStatus;
import ca.carleton.blackjack.game.entity.card.Rank;
import ca.carleton.blackjack.game.message.MessageUtil;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;

import javax.annotation.PostConstruct;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collector;
import java.util.stream.Collectors;

import static ca.carleton.blackjack.game.message.MessageUtil.message;
import static java.util.stream.Collectors.toList;
import static org.apache.commons.collections4.CollectionUtils.size;
import static org.apache.commons.collections4.MapUtils.isNotEmpty;
import static org.springframework.util.CollectionUtils.containsAny;

/**
 * Model class for the game.
 * <p/>
 * Created by Mike on 10/7/2015.
 */
@Component
@Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
public class BlackJackGame {

    private static final Logger LOG = LoggerFactory.getLogger(BlackJackGame.class);

    private static final int DEFAULT_MAX_PLAYERS = 3;

    private final AtomicInteger counter = new AtomicInteger(1243512);

    private int roundMaxPlayers = -1;

    private State gameState;

    private Map<String, Player> players;

    @Autowired
    private Deck deck;

    @Autowired
    private TurnHandler turnHandler;

    @Autowired
    private BlackJackService blackJackService;

    private boolean waitingOnReal;

    /**
     * The game state we're in *
     */
    public enum State {
        WAITING_FOR_ADMIN,
        WAITING_FOR_PLAYERS,
        PLAYING
    }

    @PostConstruct
    public void init() {
        this.players = new HashMap<>();
        this.gameState = State.WAITING_FOR_ADMIN;
        this.turnHandler.clearAll();
        this.deck.reset();
        this.roundMaxPlayers = -1;
    }

    /**
     * Reset for another round.
     */
    public void resetRound() {
        for (final Player player : this.getConnectedPlayers()) {
            player.getHand().clearHand();
            player.getHand().setHandStatus(null);
            player.setLastOption(null);
        }
        this.setGameState(State.WAITING_FOR_PLAYERS);
        this.turnHandler.clearAll();
        this.deck.reset();
        LOG.info("Round reset.");
    }

    /**
     * Get the next player to go.
     *
     * @return the player.
     */
    public Player getNextPlayer() {
        if (this.turnHandler.requiresReInitialization()) {
            this.turnHandler.initializeNewRound(this.getConnectedPlayers());
        }
        return this.turnHandler.getNextPlayer();
    }

    public boolean isNextPlayerAI() {
        return this.turnHandler.isNextPlayerAI();
    }

    /**
     * Start the game by dealing out the initial round of cards.
     */
    public void dealInitialHands() {
        this.gameState = State.PLAYING;
        this.players.forEach((uid, player) -> {
            final Card hiddenCard = this.deck.draw();
            hiddenCard.setHidden(true);
            player.getHand().addCard(hiddenCard);
            player.getHand().addCard(this.deck.draw());
            LOG.info("Dealt {} to {}.", player.getHand(), uid);
        });
    }

    /**
     * Build a list of messages to send to each player session, which contains the hand states of each card so far.
     *
     * @return the map of player keyed to their list of messages.
     */
    public Map<Player, List<TextMessage>> buildHandMessages() {
        final Map<Player, List<TextMessage>> messages = new HashMap<>();

        int otherPlayerIndex = 1;

        // We only need to do this for real players.
        for (final Player player : this.getConnectedRealPlayers()) {

            messages.putIfAbsent(player, new ArrayList<>());
            final List<TextMessage> playerMessages = messages.get(player);

            // Step 0, build the message that we're dealing the cards.
            playerMessages.add(message(MessageUtil.Message.DEALING_CARDS).build());

            // Step 1, build the messages to send the player their cards.
            player.getHand()
                    .getCards()
                    .forEach(card -> {
                        // Make it temporarily visible to the player (i.e we want to show it to the person).
                        if (card.isHidden()) {
                            card.setHidden(false);
                            playerMessages.add(message(MessageUtil.Message.ADD_PLAYER_CARD,
                                    card.toHTMLString()).build());
                            card.setHidden(true);
                        } else {
                            playerMessages.add(message(MessageUtil.Message.ADD_PLAYER_CARD,
                                    card.toHTMLString()).build());
                        }
                    });

            playerMessages.add(message(MessageUtil.Message.PLAYER_VALUE, player.getHand().getHandValue()).build());

            // Step 2, build the messages to send the player the dealer's cards.
            this.getDealer().getHand()
                    .getCards()
                    .forEach(card -> playerMessages.add(message(MessageUtil.Message.ADD_DEALER_CARD,
                            card.toHTMLString()).build()));

            playerMessages.add(message(MessageUtil.Message.DEALER_VALUE,
                    this.getDealer().getHand().getVisibleHandValue()).build());

            final List<Player> playersOtherThanCurrent = this.getConnectedPlayers().stream()
                    .filter(other -> !player.equals(other))
                    .filter(other -> (other instanceof AIPlayer && !((AIPlayer) other).isDealer()) || other.isReal())
                    .collect(Collectors.toList());

            // Step 3, build the messages to send the player the other player's (AI's) cards.
            for (final Player playerOtherThanCurrent : playersOtherThanCurrent) {
                for (final Card card : playerOtherThanCurrent.getHand().getCards()) {
                    playerMessages.add(message(MessageUtil.Message.ADD_OTHER_PLAYER_CARD,
                            card.toHTMLString(),
                            otherPlayerIndex,
                            this.getSessionIdFor(playerOtherThanCurrent))
                            .build());
                }

                playerMessages.add(message(MessageUtil.Message.OTHER_VALUE,
                        otherPlayerIndex,
                        playerOtherThanCurrent.getHand().getVisibleHandValue()).build());

                otherPlayerIndex++;
            }

            otherPlayerIndex = 1;
        }

        return messages;
    }

    public void openLobby(final int numberOfPlayers) {
        if (numberOfPlayers < 1 || numberOfPlayers > 3) {
            this.roundMaxPlayers = 3;
        }
        this.roundMaxPlayers = numberOfPlayers;
        this.gameState = State.WAITING_FOR_PLAYERS;
        LOG.info("Prepared new blackjack round for {} players.", numberOfPlayers);
    }

    /**
     * Whether or not we're ready to start the game.
     *
     * @return true if the correct amount of players have joined.
     */
    public boolean readyToStart() {
        final int numberRequired = this.roundMaxPlayers == -1 ? DEFAULT_MAX_PLAYERS : this.roundMaxPlayers;
        LOG.info("Current number of players is {}. Required number is {}.", size(this.players), numberRequired);
        return size(this.players) == numberRequired;
    }

    /**
     * Populate the remaining slots with AI.
     */
    public void registerAI() {
        // EX: User enters '2' players --> this.roundmax = 2, DEFAULT = 3 ---> ADD 1 AI, need to register Dealer after.
        final int numberOfAIToAdd = this.roundMaxPlayers == -1 ? 0 : DEFAULT_MAX_PLAYERS - this.roundMaxPlayers;
        for (int i = 0; i < numberOfAIToAdd; i++) {
            this.registerPlayer(null);
        }
        this.registerDealer();
    }

    /**
     * Register a new player in the game.
     *
     * @param session the user's session.
     * @return true if the player was added successfully.
     */
    public boolean registerPlayer(final WebSocketSession session) {
        if (size(this.players) == DEFAULT_MAX_PLAYERS) {
            LOG.warn("Max players already reached!");
            return false;
        }
        if (session == null) {
            final String id = this.newAIID();
            LOG.info("Adding AI {} to the game.", id);
            return this.players.putIfAbsent(id, new AIPlayer(null)) == null;
        } else {
            LOG.info("Adding {} to the game.", session.getId());

            if (size(this.players) == 0) {
                LOG.info("Setting first player as admin.");
                final Player admin = new Player(session);
                admin.setAdmin(true);
                return this.players.putIfAbsent(session.getId(), admin) == null;
            }

            return this.players.putIfAbsent(session.getId(), new Player(session)) == null;
        }
    }

    public String newAIID() {
        final int next = this.counter.incrementAndGet();
        return String.format("AI-%d", next);
    }

    /**
     * Replace an existing player with an AI.
     *
     * @param session the old player's sesion.
     */
    public boolean registerReplacementAI(final WebSocketSession session) {
        final AIPlayer aiPlayer = new AIPlayer(null);
        final Player old = this.getPlayerFor(session);
        if (old.getHand().isSplitHand()) {
            aiPlayer.getHand().splitHand();
            old.getHand().getSplitCards().forEach(card -> aiPlayer.getHand().addSplitCard(card));
        }
        old.getHand().getCards().forEach(card -> aiPlayer.getHand().addCard(card));
        aiPlayer.getHand().setHandStatus(old.getHand().getHandStatus());
        aiPlayer.setLastOption(old.getLastOption());

        //What do about the admin?
        if (old.isAdmin()) {
            LOG.info("Because the player was administrator, resetting to clean state.");
            return false;
        }

        if (!this.turnHandler.replaceDisconnectedPlayer(old, aiPlayer)) {
            LOG.info("AI will process their turn with the rest.");
        }

        this.players.remove(session.getId());
        final String id = this.newAIID();
        this.players.put(id, aiPlayer);
        LOG.info("Replaced old player with new AI - copied cards.");
        return true;
    }

    /**
     * Register the dealer.
     */
    public void registerDealer() {
        final AIPlayer dealer = new AIPlayer(null);
        dealer.setDealer(true);
        this.players.putIfAbsent("AI-DEALER", dealer);
        LOG.info("Added AI-DEALER to the game.");
    }

    /**
     * Check to see if the session exists.
     *
     * @param session the session.
     * @return true if yes.
     */
    public boolean isPlayerRegistered(final WebSocketSession session) {
        return this.players.get(session.getId()) != null;
    }

    /**
     * Remove a new player from the game.
     *
     * @param session the user's session.
     * @return true if the player was removed successfully.
     */
    public boolean deregisterPlayer(final WebSocketSession session) {
        if (this.isPlaying()) {
            if (this.isPlayerRegistered(session)) {
                LOG.info("Replacing {} with an AI prior to removing.", session.getId());
                return this.registerReplacementAI(session);
            } else {
                return true;
            }
        } else {
            return !this.isPlayerRegistered(session) || this.players.remove(session.getId()) != null;
        }
    }

    /**
     * Remove all AI from the players list.
     *
     * @return true if we removed at least one.
     */
    public boolean deregisterAI() {
        final List<String> aiIds = this.players.entrySet().stream()
                .filter(entry -> !entry.getValue().isReal())
                .map(Map.Entry::getKey)
                .collect(toList());
        aiIds.forEach(this.players::remove);
        return size(aiIds) != 0;
    }

    /**
     * Perform the turn for the AI.
     *
     * @param ai the ai.
     */
    public void doAITurn(final AIPlayer ai) {
        final GameOption option;
        if (ai.isDealer()) {
            option = this.blackJackService.getDealerOption(ai);
        } else {
            option = this.blackJackService.getAIOption(ai, this.getAllPlayersExceptFor(ai));
        }
        LOG.info("{} will be using option {}!", this.getSessionIdFor(ai), option);
        this.performOption(ai, option, false);

        // Only do split hand on on the turn after we split.
        if (option != GameOption.SPLIT) {
            if (ai.getHand().isSplitHand()) {
                final GameOption splitOption = this.blackJackService.getAIOption(ai, this.getAllPlayersExceptFor(ai));
                LOG.info("{} will be using option {} for their split hand!", ai, option);
                this.performOption(ai, splitOption, true);
            }
        }
    }

    /**
     * Perform the option the user selected.
     *
     * @param player the player.
     * @param option the option.
     */
    public void performOption(@NotNull final Player player, @NotNull final GameOption option) {
        this.performOption(player, option, player.getHand().isSplitHand());
    }

    public void performOption(@NotNull final Player player, @NotNull final GameOption option, final boolean splitHand) {
        switch (option) {
            case SPLIT:
                // TODO
                if (splitHand) {
                    throw new IllegalStateException("can't split a split hand!");
                }
                player.getHand().splitHand();
                break;
            case HIT:
                final Card drawn = this.deck.draw();
                LOG.info("Drew {}.", drawn);
                if (drawn != null) {
                    if (splitHand) {
                        player.getHand().addSplitCard(drawn);
                    } else {
                        player.getHand().addCard(drawn);
                    }

                    this.swapAceValuesIfBenefit(player);

                } else {
                    LOG.warn("No cards remaining! {} tried to hit.", this.getSessionIdFor(player));
                }
                break;
            case STAY:
                LOG.info("{} is staying - do nothing.", this.getSessionIdFor(player));
                break;
            default:
                throw new IllegalArgumentException("No valid argument passed to execute option.");
        }
        player.setLastOption(option);

        if (player.getHand().getHandValue() > 21) {
            player.setLastOption(GameOption.BUST);
            LOG.info("{} busted!", this.getSessionIdFor(player));
            this.revealCards(player);
        } else if (player.getHand().getHandValue() == 21 && player.getHand().getCards().size() == 7) {
            player.setLastOption(GameOption.SEVEN_CARD_CHARLIE);
            LOG.info("{} got a seven card charlie!", this.getSessionIdFor(player));
            this.revealCards(player);
        }

    }

    /**
     * Return true if the game state should be reset and a winner declared.
     */
    public boolean isGameResolved() {
        int notMakingTurn = 0;
        for (final Player player : this.getConnectedPlayers()) {
            if (player.getLastOption() == GameOption.STAY ||
                    player.getLastOption() == GameOption.BUST ||
                    player.getLastOption() == GameOption.SEVEN_CARD_CHARLIE) {
                notMakingTurn++;
                if (player.getLastOption() == GameOption.SEVEN_CARD_CHARLIE) {
                    LOG.info("Game over because someone has seven card charlie.");
                    return true;
                }
            }
        }
        return notMakingTurn == this.getConnectedPlayers().size();
    }

    /**
     * Set the final statuses so we can send it out.
     */
    public void resolveRound() {
        // If we get to this point, no one should have a seven card charlie.

        // Get highest value.
        final long[] highest = new long[1];
        highest[0] = 0L;
        this.getConnectedPlayers().stream()
                .filter(player -> player.getLastOption() != GameOption.BUST)
                .forEach(player -> {
                    if (player.getHand().getHandValue() >= highest[0]) {
                        highest[0] = player.getHand().getHandValue();
                    }
                });

        if (highest[0] >= 0L) {
            for (final Player player : this.getConnectedPlayers()) {
                // Players with least cards and highest value is winner
                if (player.getHand().getHandValue() == highest[0]) {
                    player.getHand().setHandStatus(HandStatus.WINNER);
                } else {
                    player.getHand().setHandStatus(HandStatus.LOSER);
                }
            }

            // Lowest hand size wins if multiple winners
            int numberWinners = 0;
            for (final Player player : this.getConnectedPlayers()) {
                if (player.getHand().getHandStatus() == HandStatus.WINNER) {
                    numberWinners++;
                }
            }
            if (numberWinners > 0) {
                // Get lowest amount of cards
                final int[] leastCards = new int[1];
                leastCards[0] = 30;
                this.getConnectedPlayers().stream()
                        .filter(player -> player.getHand().getHandStatus() == HandStatus.WINNER)
                        .forEach(player -> {
                            if (player.getHand().getCards().size() <= leastCards[0]) {
                                leastCards[0] = player.getHand().getCards().size();
                            }
                        });
                this.getConnectedPlayers().stream()
                        .filter(player -> player.getHand().getHandStatus() == HandStatus.WINNER)
                        .filter(player -> player.getHand().getCards().size() != leastCards[0])
                        .forEach(player -> player.getHand().setHandStatus(HandStatus.LOSER));
            }
        } else {
            // Everyone bust and lost...
            this.getConnectedPlayers().forEach(player -> player.getHand().setHandStatus(HandStatus.LOSER));
        }
        LOG.info("Set hand resolutions.");
    }

    /**
     * Resolve the round for the given player with a seven card charlie.
     *
     * @param player the player.
     */
    public void resolveRoundSevenCardCharlie(final Player player) {
        this.getConnectedPlayers().stream()
                .filter(other -> !player.equals(other))
                .forEach(other -> other.getHand().setHandStatus(HandStatus.LOSER));
        player.getHand().setHandStatus(HandStatus.SEVEN_CARD_CHARLIE);
        LOG.info("Set hand resolutions for seven card charlie.");
    }

    public void revealCards(final Player player) {
        player.getHand().getCards().forEach(card -> card.setHidden(false));
    }

    /**
     * Get all the players except the one listed.
     *
     * @param exclude the player to exclude.
     * @return the list of players, or empty.
     */
    public List<Player> getAllPlayersExceptFor(final Player exclude) {
        return isNotEmpty(this.players) ? this.players.values()
                .stream()
                .filter(player -> !player.equals(exclude))
                .collect(
                        Collectors.toList()) : Collections.emptyList();
    }

    /**
     * Get the player sessions connected to this game including AI.
     *
     * @return the sessions.
     */
    public Collection<WebSocketSession> getConnectedPlayerSessions() {
        return this.players.values().stream()
                .map(Player::getSession)
                .collect(toList());
    }

    /**
     * Get the player sessions connected to this game including AI.
     *
     * @return the sessions.
     */
    public List<Player> getConnectedPlayers() {
        return this.players.values().stream()
                .collect(toList());
    }

    /**
     * Get the real players that are connected.
     *
     * @return the real players.
     */
    public Collection<Player> getConnectedRealPlayers() {
        return this.players.values().stream()
                .filter(Player::isReal)
                .collect(toList());
    }

    /**
     * Get the admin from the current list of players.
     *
     * @return the admin player.
     */
    public Player getAdmin() {
        return this.players.values().stream()
                .filter(Player::isAdmin)
                .collect(uniqueResult());
    }

    /**
     * Get the dealer from the current list of players.
     *
     * @return the dealer AI.
     */
    public Player getDealer() {
        return this.players.values().stream()
                .filter(player -> !player.isReal() && ((AIPlayer) player).isDealer())
                .collect(uniqueResult());
    }

    /**
     * Get the session id for the given player
     *
     * @param player the player..
     * @return the string id.
     */
    public String getSessionIdFor(final Player player) {
        for (final Map.Entry<String, Player> entry : this.players.entrySet()) {
            if (entry.getValue().equals(player)) {
                return entry.getKey();
            }
        }
        return "Invalid UID";
    }

    /**
     * Get the player for the given session.
     *
     * @param session the session.
     * @return the player.
     */
    public Player getPlayerFor(final WebSocketSession session) {
        return this.players.get(session.getId());
    }

    public boolean isWaitingForPlayers() {
        return this.gameState == State.WAITING_FOR_PLAYERS;
    }

    public boolean isPlaying() {
        return this.gameState == State.PLAYING;
    }

    public static <T> Collector<T, ?, T> uniqueResult() {
        return Collectors.collectingAndThen(
                Collectors.toList(),
                list -> {
                    if (list.size() != 1) {
                        throw new IllegalStateException();
                    }
                    return list.get(0);
                }
        );
    }

    public void setGameState(final State gameState) {
        this.gameState = gameState;
    }

    private void swapAceValuesIfBenefit(final Player player) {
        if (containsAny(player.getHand().getCards(), Arrays.asList(Rank.ACE_HIGH, Rank.ACE_LOW))) {
            return;
        }

        if (player.getHand().getHandValue() > 21) {
            LOG.info("Player bust - checking to see if we can replace ACE.");
            this.checkAndReplace(player);
        } else if (player.getHand().getHandValue() >= 18 && player.getHand().getHandValue() <= 20) {
            LOG.info("Player between 18 and 20 - checking to see if we can replace ACE.");
            this.checkAndReplace(player);
        }

    }

    private void checkAndReplace(final Player player) {
        boolean doReplace = false;
        int indexOf = -1;
        for (final Card card : player.getHand().getCards()) {
            if (card.getRank() == Rank.ACE_HIGH) {
                LOG.info("Player has an ACE_HIGH - replacing with value 1.");
                indexOf = player.getHand().getCards().indexOf(card);
                doReplace = true;
                break;
            }
        }
        if (doReplace) {
            final Card card = player.getHand().getCards().remove(indexOf);
            player.getHand().getCards().add(indexOf, new Card(Rank.ACE_LOW, card.getSuit(), card.isHidden()));
            LOG.info("Replaced {} with {}.", card, player.getHand().getCards().get(indexOf));
        }
    }

    public boolean isWaitingOnReal() {
        return this.waitingOnReal;
    }

    public void setWaitingOnReal(final boolean waitingOnReal) {
        this.waitingOnReal = waitingOnReal;
    }


}
