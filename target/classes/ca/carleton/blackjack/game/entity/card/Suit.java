package ca.carleton.blackjack.game.entity.card;

import static org.apache.commons.lang3.StringUtils.capitalize;

/**
 * The suit of a card.
 * <p/>
 * Created by Mike on 10/27/2015.
 */
public enum Suit {
    HEARTS("hearts"),
    CLUBS("clubs"),
    DIAMONDS("diams"),
    SPADES("spades");

    private final String html;

    Suit(final String html) {
        this.html = html;
    }

    public String getHtml() {
        return this.html;
    }

    @Override
    public String toString() {
        return capitalize(this.name().toLowerCase());
    }
}
