package net.cnation.gambling;

import java.util.ArrayList;

public class Hand {
    String deckOwner;
    ArrayList<Card> cards;

    public Hand(String dl, ArrayList<Card> c) {
        deckOwner = dl;
        cards = c;
    }

    public String getDeckOwner() {
        return deckOwner;
    }

    public ArrayList<Card> getCards() {
        return cards;
    }

    public void addCard(Card c) {
        cards.add(c);
    }

    public void clearHand() {
        cards.clear();
    }

    public Card removeCard(int i) {
        Card c = cards.get(i);
        cards.remove(i);
        return c;
    }
}
