/*
 *  Copyright (c) 2015 Michal Ďuračík
 */
package test;

import dynhassubor.DynHashSubor;
import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.Random;

/**
 *
 * @author Unlink
 */
public class Test3 {
	public static void main(String[] args) {
		MojZaznamMapper mapper = new MojZaznamMapper();
		File subor = new File("subor3.bin");
		try (DynHashSubor<MojZaznam> s = new DynHashSubor(mapper, subor, 4)){
			
			for (int i = 0; i < 64; i++) {
				MojZaznam z = s.najdi(new IntovyKluc(i));
				if (z == null) {
					System.err.println("Nenasiel som "+i);
				}
				else if (!z.getString().equals("Zaznam cislo "+i)) {
					System.err.println("Chyba "+i+" -> "+z.getString());
				}
			}
			
			
			for (int i = 64; i < 128; i++) {
				int r = i;
				if (!s.vloz(new MojZaznam(r, "Zaznam cislo "+r))) {
					System.out.println("Nepodarilo sa vložiť akože :)");
				}
				MojZaznam z = s.najdi(new IntovyKluc(r));
				if (z == null) {
					System.err.println("Nenasiel som "+r);
				}
				else if (!z.getString().equals("Zaznam cislo "+r)) {
					System.err.println("Chyba "+r+" -> "+z.getString());
				}
			}
			
		}
		catch (Exception ex) {
			ex.printStackTrace();
		}
	}
}
