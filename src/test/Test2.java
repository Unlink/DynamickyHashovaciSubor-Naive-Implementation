/*
 *  Copyright (c) 2015 Michal Ďuračík
 */
package test;

import dynhassubor.DynHashSubor;
import dynhassubor.IKluc;
import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Random;

/**
 *
 * @author Unlink
 */
public class Test2 {
	public static void main(String[] args) {
		MojZaznamMapper mapper = new MojZaznamMapper();
		File subor = new File("subor2.bin");
		//subor.delete();
		Random rn = new Random(1);
		try (DynHashSubor<MojZaznam> s = new DynHashSubor(mapper, subor, 4)){
			
			System.out.println("Vkladám 2000 cisel");
			HashSet<Integer> ints = new HashSet<>();
			for (int i = 0; i < 2000; i++) {
				int r;
				while (ints.contains(r = rn.nextInt()));
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
				ints.add(r);
			}
			
			System.out.println("Skusam vsetky nasjt");
			
			//Skusime všetky najsť
			for (Integer aInt : ints) {
				MojZaznam z = s.najdi(new IntovyKluc(aInt));
				if (z == null) {
					System.err.println("Nenasiel som "+aInt);
				}
				else if (!z.getString().equals("Zaznam cislo "+aInt)) {
					System.err.println("Chyba "+aInt+" -> "+z.getString());
				}
			}
			
			ArrayList<Integer> listIntov = new ArrayList<>(ints);
			Collections.shuffle(listIntov);
			 
			System.out.println("Mazem random 1000");
			
			for (int i = 0; i < 1000; i++) {
				if (!s.vymaz(new IntovyKluc(listIntov.get(i)))) {
					System.err.println("Nevedel som vymazať :/");
				}
				ints.remove(listIntov.get(i));
			}
			
			System.out.println("Skusam nasjt zvysok");
			
			for (Integer aInt : ints) {
				MojZaznam z = s.najdi(new IntovyKluc(aInt));
				if (z == null) {
					System.err.println("Nenasiel som "+aInt);
				}
				else if (!z.getString().equals("Zaznam cislo "+aInt)) {
					System.err.println("Chyba "+aInt+" -> "+z.getString());
				}
			}
			
			System.out.println("Mazem vsetko");
			
			for (Integer aInt : ints) {
				if (!s.vymaz(new IntovyKluc(aInt))) {
					System.err.println("Nevedel som vymazať :/");
				}
			}
			
			/*System.out.println("Vkladám 100 cisel");
			ints.clear();
			for (int i = 0; i < 100; i++) {
				int r;
				while (ints.contains(r = rn.nextInt()));
				s.vloz(new MojZaznam(r, "Zaznam cislo "+r));
			}
			
			System.out.println("Skusam vsetky nasjt");
			
			//Skusime všetky najsť
			for (Integer aInt : ints) {
				MojZaznam z = s.najdi(new IntovyKluc(aInt));
				if (z == null) {
					System.err.println("Nenasiel som "+aInt);
				}
				else if (!z.getString().equals("Zaznam cislo "+aInt)) {
					System.err.println("Chyba "+aInt+" -> "+z.getString());
				}
			}*/
			
		}
		catch (Exception ex) {
			ex.printStackTrace();
		}
	}
}
