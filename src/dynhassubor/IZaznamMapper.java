/*
 *  Copyright (c) 2015 Michal Ďuračík
 */
package dynhassubor;

/**
 *
 * @author Unlink
 */
public interface IZaznamMapper<T> {
	
	public int dajVelkostZaznamu();
	
	public T nacitajZaznam(byte[] bytes);
	
	public byte[] serializujZaznam(T zaznam);
	
}
