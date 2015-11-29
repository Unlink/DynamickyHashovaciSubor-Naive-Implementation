/*
 *  Copyright (c) 2015 Michal Ďuračík
 */
package test;

import dynhassubor.IKluc;
import dynhassubor.IZaznam;

/**
 *
 * @author Unlink
 */
public class MojZaznam implements IZaznam {
	
	private int id;

	private String string;

	public MojZaznam(int paId, String paString) {
		this.id = paId;
		this.string = paString;
	}

	public int getId() {
		return id;
	}

	public void setId(int paId) {
		this.id = paId;
	}

	public String getString() {
		return string;
	}

	public void setString(String paString) {
		this.string = paString;
	}

	@Override
	public IKluc dajKluc() {
		return new IntovyKluc(id);
	}

}
