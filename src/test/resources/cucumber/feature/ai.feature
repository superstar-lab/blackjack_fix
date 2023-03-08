Feature: AI
  To determine the option that the AI of the game will use when it is their turn.

  Scenario: AI should split given two initial cards of the same rank
    Given a card in the AI's hand with the rank 'QUEEN' and suit 'DIAMONDS' and hidden 'true'
    And another card in the AI's hand with the rank 'QUEEN' and suit 'HEARTS' and hidden 'false'
    When it is the AI's turn to make a move
    Then the AI should perform their turn
    And the AI's last move should be 'SPLIT'
    And the AI's hand size should remained unchanged

  Scenario: AI should stay with a card value of 21
    Given a card in the AI's hand with the rank 'QUEEN' and suit 'DIAMONDS' and hidden 'true'
    And another card in the AI's hand with the rank 'ACE_HIGH' and suit 'HEARTS' and hidden 'false'
    When it is the AI's turn to make a move
    Then the AI should perform their turn
    And the AI's last move should be 'STAY'
    And the AI's hand size should remained unchanged

  Scenario: AI should hit if another player has stayed with two cards with the visible one being a 10
    Given a card in the AI's hand with the rank 'QUEEN' and suit 'DIAMONDS' and hidden 'true'
    And another card in the AI's hand with the rank 'THREE' and suit 'HEARTS' and hidden 'false'
    And a player with two cards in their hand consisting of 'QUEEN' of 'SPADES', hidden 'false' and 'SEVEN' of 'CLUBS', hidden 'true'
    And the player has decided to stay their turn
    When it is the AI's turn to make a move
    Then the AI should perform their turn
    And the AI's last move will either be 'HIT' if their hand value is less than or equal to 21, else 'BUST'
    And the other player's last move should be 'STAY'

  Scenario: AI should hit if their value is between 18 and 20 and another player has visible card value greater than AI's minus 10
    Given a card in the AI's hand with the rank 'QUEEN' and suit 'DIAMONDS' and hidden 'true'
    And another card in the AI's hand with the rank 'EIGHT' and suit 'HEARTS' and hidden 'false'
    And a player with two cards in their hand consisting of 'NINE' of 'SPADES', hidden 'false' and 'FOUR' of 'CLUBS', hidden 'false'
    When it is the AI's turn to make a move
    Then the AI should have a hand value of between 18 and 20
    Then the AI should perform their turn
    And the AI's last move will either be 'HIT' if their hand value is less than or equal to 21, else 'BUST'

  Scenario: AI should stay if their value is between 18 and 20 and another player's card value is not greater than AI's minus 10
    Given a card in the AI's hand with the rank 'QUEEN' and suit 'DIAMONDS' and hidden 'true'
    And another card in the AI's hand with the rank 'EIGHT' and suit 'HEARTS' and hidden 'false'
    And a player with two cards in their hand consisting of 'NINE' of 'SPADES', hidden 'false' and 'THREE' of 'CLUBS', hidden 'false'
    When it is the AI's turn to make a move
    Then the AI should have a hand value of between 18 and 20
    Then the AI should perform their turn
    And the AI's last move will either be 'HIT' if their hand value is less than or equal to 21, else 'BUST'

  Scenario: AI should hit in other cases
    Given a card in the AI's hand with the rank 'TWO' and suit 'DIAMONDS' and hidden 'true'
    And another card in the AI's hand with the rank 'THREE' and suit 'HEARTS' and hidden 'false'
    And a player with two cards in their hand consisting of 'TEN' of 'SPADES', hidden 'false' and 'THREE' of 'CLUBS', hidden 'false'
    When it is the AI's turn to make a move
    Then the AI should perform their turn
    And the AI's last move will either be 'HIT' if their hand value is less than or equal to 21, else 'BUST'