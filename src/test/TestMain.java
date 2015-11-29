/*
 *  Copyright (c) 2015 Michal Ďuračík
 */
package test;

import dynhassubor.DynHashSubor;
import java.io.File;
import java.io.IOException;

/**
 *
 * @author Unlink
 */
public class TestMain {
	
	public static void main(String[] args) {
		
		MojZaznamMapper mapper = new MojZaznamMapper();
		File subor = new File("subor.bin");
		try (DynHashSubor<MojZaznam> s = new DynHashSubor(mapper, subor, 3)){
			for (int i = 10; i < 1000; i++) {
				
			}
		}
		catch (IOException e) {
		}
		
	}
	
}
