/*
 *  Copyright (c) 2015 Michal Ďuračík
 */
package test;

import dynhassubor.IZaznamMapper;
import java.nio.ByteBuffer;

/**
 *
 * @author Unlink
 */
public class MojZaznamMapper implements IZaznamMapper<MojZaznam> {

	@Override
	public int dajVelkostZaznamu() {
		return 4 + 100; //4 idčko a 100 na 50 znakov
	}

	@Override
	public MojZaznam nacitajZaznam(byte[] paBytes) {
		ByteBuffer wrapped = ByteBuffer.wrap(paBytes, 0, dajVelkostZaznamu());
		
		MojZaznam z = new MojZaznam(wrapped.getInt(), "");
		char[] znaky = new char[50];
		for (int i = 0; i < 50; i++) {
			znaky[i] =  wrapped.getChar();
		}
		z.setString((new String(znaky)).trim());
		return z;
	}

	@Override
	public byte[] serializujZaznam(MojZaznam paZaznam) {
		ByteBuffer buffer = ByteBuffer.allocate(dajVelkostZaznamu());
		buffer.putInt(paZaznam.getId());
		char[] znaky = paZaznam.getString().toCharArray();
		for (int i = 0; i < znaky.length; i++) {
			buffer.putChar(znaky[i]);
		}
		//Zvysne chary do 50
		for (int i = znaky.length; i < 50; i++) {
			buffer.putChar(' ');
		}
		return buffer.array();
	}
	
}
