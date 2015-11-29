/*
 *  Copyright (c) 2015 Michal Ďuračík
 */
package dynhassubor;

import java.io.EOFException;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.LinkedList;
import java.util.List;
import java.util.Stack;

/**
 *
 * @author Unlink
 */
public class BinStrom {
	
	private Uzol koren;
	
	public static final int EMPTY_ADDR = -1;
	private static final int INNER_NODE = -2;

	public BinStrom() {
		this.koren = new Uzol();
	}
	
	public void inicializuj(int addr0, int addr1) {
		koren.nula = new Uzol();
		koren.nula.adresa = addr0;
		
		koren.jedna = new Uzol();
		koren.jedna.adresa = addr1;
	}

	private Stack<Uzol> travrzujStrom(int hash) {
		Stack<Uzol> cesta = new Stack<>();
		Uzol u = koren;
		cesta.push(u);
		
		while (u.adresa == INNER_NODE) {
			if ((hash &0x1) == 0) {
				u = u.nula;
			} 
			else {
				u = u.jedna;
			}
			hash = hash >> 1;
			cesta.push(u);
		}
		
		return cesta;
	}
	
	public int najdiBlok(IKluc paKluc) {
		return travrzujStrom(paKluc.dajHash()).pop().adresa;
	}

	public int rozdelUzol(IKluc paKluc, int addr0, int addr1) {
		Stack<Uzol> cesta = travrzujStrom(paKluc.dajHash());
		Uzol u = cesta.pop();
		u.adresa = INNER_NODE;
		u.nula = new Uzol();
		u.nula.adresa = addr0;
		
		u.jedna = new Uzol();
		u.jedna.adresa = addr1;
		return cesta.size();
	}
	
	public void nastavAdresu(int hash, int adresa) {
		travrzujStrom(hash).pop().adresa =  adresa;
	}

	public List<Integer> spojBloky(IKluc paKluc) {
		Stack<Uzol> cesta = travrzujStrom(paKluc.dajHash());
		Uzol u = cesta.pop();
		Uzol p = cesta.pop();
		LinkedList<Integer> bloky = new LinkedList<>();
		
		while (p != koren) {
			if (u.adresa >= 0) {
				bloky.add(u.adresa);
			}
			
			if (u == p.nula) {
				if (p.jedna.adresa == INNER_NODE) { //Brat je vnutorny uzol -> nemôžme spajať
					u.adresa = EMPTY_ADDR;
					break;
				}
				p.adresa = p.jedna.adresa;
			}
			else {
				if (p.nula.adresa == INNER_NODE) { //Brat je vnutorny uzol -> nemôžme spajať
					u.adresa = EMPTY_ADDR;
					break;
				}
				p.adresa = p.nula.adresa;
			}
			p.nula = null;
			p.jedna = null;

			u = p;
			p = cesta.pop();
			
			if (u == p.jedna && p.nula.adresa == EMPTY_ADDR) {
				u = p.nula;
			}
			else if (u == p.nula && p.jedna.adresa == EMPTY_ADDR) {
				u = p.jedna;
			}
			else {
				break;
			}
		}
		
		return bloky;
	}

	public void serializeTo(ObjectOutputStream paOis) throws IOException {
		Stack<StackedUzol> zasobnik = new Stack<>();
		StackedUzol k = new StackedUzol();
		k.uzol = koren;
		
		zasobnik.push(k);
		
		while (!zasobnik.isEmpty()) {
			StackedUzol u = zasobnik.pop();
			if (u.uzol.adresa == INNER_NODE) {
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
				//System.out.println(u.level+"::"+cesta+"::"+u.uzol.adresa);
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
		int adresa = INNER_NODE;
	}
	
	public class StackedUzol {
		int cesta;
		int level;
		Uzol uzol;
	}
	
}
