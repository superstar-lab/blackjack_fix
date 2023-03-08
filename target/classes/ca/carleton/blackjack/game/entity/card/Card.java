package ca.carleton.blackjack.game.entity.card;

/**
 * Represents a single playing card.
 * <p/>
 * Created by Mike on 10/27/2015.
 */
public class Card {

    private final Rank rank;

    private final Suit suit;

    private boolean hidden;

    public Card(final Rank rank, final Suit suit, final boolean hidden) {
        this.rank = rank;
        this.suit = suit;
        this.hidden = hidden;
    }

    @Override
    public boolean equals(final Object rhs) {
        return rhs instanceof Card && ((Card) rhs).getRank() == this.rank && ((Card) rhs).getSuit() == this.suit;
    }


    @Override
    public String toString() {
        return this.rank + " of " + this.suit + String.format("[hidden:%b]", this.hidden);
    }

    /**
     * <div class="card rank-7 spades">
     * <span class="rank">7</span>
     * <span class="suit">&spades;</span>
     * </div>
     *
     * @return the HTML representation of this card.
     */
    public String toHTMLString() {
        return this.isHidden() ? "<div class=\"card back\">*</div>" : String.format("<div class=\"card rank-%s %s\">\n" +
                        "                        <span class=\"rank\">%s</span>\n" +
                        "                        <span class=\"suit\">&%s;</span>\n" +
                        "                    </div>",
                this.rank.getHtml(),
                this.suit.getHtml(),
                this.rank.getHtml(),
                this.suit.getHtml());
    }

    public boolean isHidden() {
        return this.hidden;
    }

    public void setHidden(final boolean hidden) {
        this.hidden = hidden;
    }

    public Rank getRank() {
        return this.rank;
    }

    public Suit getSuit() {
        return this.suit;
    }
}
