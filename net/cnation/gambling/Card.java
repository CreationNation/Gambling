package net.cnation.gambling;

public class Card {
    int value;
    int suit;

    public Card(int v, int s) {
        value = v;
        suit = s;
    }

    public int getSuitNumber() {
        return suit;
    }

    public int getValueNumber() {
        return value;
    }

    public String getSuit() {
        switch (suit) {
        case 0:
            return "Hearts";
        case 1:
            return "Clubs";
        case 2:
            return "Spades";
        case 3:
            return "Diamonds";
        default:
            return "Bug";
        }
    }

    public String getValue() {
        switch (value) {
        case 1:
            return "Ace";
        case 11:
            return "Jack";
        case 12:
            return "Queen";
        case 13:
            return "King";
        default:
            return "" + value;
        }
    }

}
