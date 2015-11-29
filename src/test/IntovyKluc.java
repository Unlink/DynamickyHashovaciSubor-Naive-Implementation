/*
 *  Copyright (c) 2015 Michal Ďuračík
 */
package test;

import dynhassubor.IKluc;

/**
 *
 * @author Unlink
 */
public class IntovyKluc implements IKluc {
	
	private int kluc;

	public IntovyKluc(int paKluc) {
		this.kluc = paKluc;
	}

	@Override
	public int dajHash() {
		return kluc;
	}

	public int getKluc() {
		return kluc;
	}

	@Override
	public boolean equals(IKluc paKluc) {
		if (paKluc instanceof IntovyKluc) {
			return kluc == ((IntovyKluc) paKluc).getKluc();
		}
		return false;
	}
	
}
