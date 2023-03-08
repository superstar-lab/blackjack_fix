package ca.carleton.blackjack.game.entity;

import ca.carleton.blackjack.game.GameOption;
import ca.carleton.blackjack.game.entity.card.Hand;
import org.springframework.web.socket.WebSocketSession;

/**
 * Represents a player.
 * <p/>
 * Created by Mike on 10/6/2015.
 */
public class Player {

    private final WebSocketSession session;

    private boolean isAdmin;

    private final Hand hand;

    private GameOption lastOption = null;

    public Player(final WebSocketSession session) {
        this.session = session;
        this.hand = new Hand();
    }

    public Hand getHand() {
        return this.hand;
    }

    public boolean isReal() {
        return this.session != null;
    }

    public boolean isAdmin() {
        return this.isAdmin;
    }

    public void setAdmin(final boolean isAdmin) {
        this.isAdmin = isAdmin;
    }

    public WebSocketSession getSession() {
        return this.session;
    }

    public GameOption getLastOption() {
        return this.lastOption;
    }

    public void setLastOption(final GameOption lastOption) {
        this.lastOption = lastOption;
    }

    @Override
    public boolean equals(final Object rhs) {
        return rhs instanceof Player && ((Player) rhs).getHand().equals(this.hand);
    }
}
