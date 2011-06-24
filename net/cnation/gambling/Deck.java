package net.cnation.gambling;

import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Collections;

public class Deck {
    SecureRandom r = new SecureRandom();

    ArrayList<Card> cards = new ArrayList<Card>();

    public Deck() {
        reloadDeck();
    }

    public String drawCard() {
        if (cards.size() != 0) {
            int high = cards.size() - 1;
            int low = 0;
            int rnd = (int) (r.nextDouble() * (high - low + 1) + low);
            Card c = cards.get(rnd);
            cards.remove(rnd);
            return c.getValue() + " of " + c.getSuit();
        } else {
            return null;
        }
    }

    public Card drawCardObject() {
        if (cards.size() != 0) {
            int high = cards.size() - 1;
            int low = 0;
            int rnd = (int) (r.nextDouble() * (high - low + 1) + low);
            Card c = cards.get(rnd);
            cards.remove(rnd);
            return c;
        } else {
            return null;
        }
    }

    public int getCardsRemaining() {
        return cards.size();
    }

    public void reloadDeck() {
        cards.clear();
        addDeck();

    }

    public void addDeck() {
        for (int i = 1; i <= 13; i++) {
            for (int j = 0; j < 4; j++) {
                cards.add(new Card(i, j));
            }
        }
        for (int i = 0; i < 10; i++) {
            Collections.shuffle(cards);
        }
    }

}
