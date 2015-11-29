/*
 *  Copyright (c) 2015 Michal Ďuračík
 */
package dynhassubor;

import java.util.LinkedList;

/**
 *
 * @author Unlink
 */
public class Blok<T extends IZaznam> {
	
	private int index;
	
	private int platnych;
	
	private LinkedList<T> zaznamy;

	public Blok(int paIndex) {
		this.index = paIndex;
		this.platnych = 0;
		zaznamy = new LinkedList<>();
	}

	public int getIndex() {
		return index;
	}

	public int getPlatnych() {
		return platnych;
	}
	
	
	
	public T najdiZaznam(IKluc kluc) {
		for (T zaznam:zaznamy) {
			if (zaznam.dajKluc().equals(kluc)) {
				return zaznam;
			}
		}
		return null;
	}
	
	public T vymazZaznam(IKluc kluc) {
		T old = najdiZaznam(kluc);
		if (old != null) {
			zaznamy.remove(old);
			platnych--;
			return old;
		}
		return null;
	}
	
	public void pridajZaznam(T zaznam) {
		platnych++;
		zaznamy.add(zaznam);
	}

	public LinkedList<T> dajZaznamy() {
		return zaznamy;
	}
	
}
