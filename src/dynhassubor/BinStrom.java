/*
 *  Copyright (c) 2015 Michal Ďuračík
 */
package dynhassubor;

import java.io.EOFException;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.Stack;

/**
 *
 * @author Unlink
 */
public class BinStrom {
	
	private Uzol koren;

	public BinStrom() {
		this.koren = new Uzol();
	}
	
	public void inicializuj(int addr0, int addr1) {
		koren.nula = new Uzol();
		koren.nula.adresa = addr0;
		
		koren.jedna = new Uzol();
		koren.jedna.adresa = addr1;
	}

	public int najdiBlok(IKluc paKluc) {
		int hash = paKluc.dajHash();
		Uzol u = koren;
		
		while (u.adresa < 0) {
			if ((hash &0x1) == 0) {
				u = u.nula;
			} 
			else {
				u = u.jedna;
			}
			hash = hash >> 1;
		}
		
		return u.adresa;
	}

	public int rozdelUzol(IKluc paKluc, int addr0, int addr1) {
		int hash = paKluc.dajHash();
		Uzol u = koren;
		int level = 0;
		while (u.adresa < 0) {
			if ((hash &0x1) == 0) {
				u = u.nula;
			} 
			else {
				u = u.jedna;
			}
			hash = hash >> 1;
			level++;
		}
		
		u.adresa = -1;
		u.nula = new Uzol();
		u.nula.adresa = addr0;
		
		u.jedna = new Uzol();
		u.jedna.adresa = addr1;
		return level;
	}

	public boolean spojBloky(IKluc paKluc) {
		int hash = paKluc.dajHash();
		Uzol u = koren;
		Uzol p = null;
		
		while (u.adresa < 0) {
			p = u;
			if ((hash &0x1) == 0) {
				u = u.nula;
			} 
			else {
				u = u.jedna;
			}
			hash = hash >> 1;
		}
		
		if (p == koren) {
			return false;
		}
		else {
			if (u == p.nula) {
				if (p.jedna.adresa < 0) { //Brat ma potomkov, -> nemôžme spojit
					return false;
				}
				p.adresa = p.jedna.adresa;
			}
			else {
				if (p.nula.adresa < 0) { //Brat ma potomkov, -> nemôžme spojit
					return false;
				}
				p.adresa = p.nula.adresa;
			}
			p.nula = null;
			p.jedna = null;
			return true;
		}
	}

	public void serializeTo(ObjectOutputStream paOis) throws IOException {
		Stack<StackedUzol> zasobnik = new Stack<>();
		StackedUzol k = new StackedUzol();
		k.uzol = koren;
		
		zasobnik.push(k);
		
		while (!zasobnik.isEmpty()) {
			StackedUzol u = zasobnik.pop();
			if (u.uzol.adresa < 0) {
				StackedUzol nula = new StackedUzol();
				nula.uzol = u.uzol.nula;
				nula.level = u.level+1;
				nula.cesta = u.cesta << 1;
				zasobnik.push(nula);
				
				StackedUzol jedna = new StackedUzol();
				jedna.uzol = u.uzol.jedna;
				jedna.level = u.level+1;
				jedna.cesta = (u.cesta << 1)+1;
				zasobnik.push(jedna);
			}
			else {
				//Level
				paOis.writeInt(u.level);
				int cesta = 0;
				for (int i=0; i<u.level; i++) {
					cesta = cesta << 1;
					if ((u.cesta & 0x1) == 1) {
						cesta += 1;
					}
					u.cesta = u.cesta >> 1;
				}
				paOis.writeInt(cesta);
				paOis.writeInt(u.uzol.adresa);
				
			}
		}
	}

	public void inicializujZo(ObjectInputStream paOis) throws IOException {
		try {
			while (true) {
				int level = paOis.readInt();
				int cesta = paOis.readInt();
				int adresa = paOis.readInt();
				
				Uzol u = koren;
			
				
				for (int i = 0; i < level; i++) {
					if ((cesta & 0x1) == 0) {
						if (u.nula == null) {
							u.nula = new Uzol();
						}
						u = u.nula;
					}
					else {
						if (u.jedna == null) {
							u.jedna = new Uzol();
						}
						u = u.jedna;
					}
					cesta = cesta >> 1;
				}
				u.adresa = adresa;

			}
		}
		catch (EOFException e) {
		}
	}
	
	public class Uzol {
		Uzol nula;
		Uzol jedna;
		int adresa = -1;
	}
	
	public class StackedUzol {
		int cesta;
		int level;
		Uzol uzol;
	}
	
}
