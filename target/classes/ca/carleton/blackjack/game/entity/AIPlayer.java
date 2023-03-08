package ca.carleton.blackjack.game.entity;

import org.springframework.web.socket.WebSocketSession;

/**
 * An AI player.
 * <p/>
 * Created by Mike on 10/7/2015.
 */
public class AIPlayer extends Player {

    private boolean dealer;

    public AIPlayer(final WebSocketSession session) {
        super(session);
    }

    public boolean isDealer() {
        return this.dealer;
    }

    public void setDealer(final boolean dealer) {
        this.dealer = dealer;
    }

}
