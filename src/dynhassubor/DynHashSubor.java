/*
 *  Copyright (c) 2015 Michal Ďuračík
 */
package dynhassubor;

import java.io.Closeable;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.util.TreeSet;

/**
 *
 * @author Unlink
 */
public class DynHashSubor<T extends IZaznam> implements Closeable {
	
	private IZaznamMapper<T> mapper;
	
	private File subor;
	
	private RandomAccessFile ra;
	
	private int pocZaznamov;
	
	private int velkostBloku;
	
	private int alokovanychBlokov;
	
	private TreeSet<Integer> volneBloky;
	
	private BinStrom strom;

	public DynHashSubor(IZaznamMapper<T> paMapper, File paSubor, int paPocZaznamov) throws IOException {
		this.mapper = paMapper;
		this.subor = paSubor;
		this.pocZaznamov = paPocZaznamov;
		this.strom = new BinStrom();
		this.velkostBloku = 4 + paPocZaznamov*paMapper.dajVelkostZaznamu(); //Pocet validnych zaznamov + n zaznamov
		
		this.volneBloky = new TreeSet<>();
		
		if (subor.exists()) {
			nacitajExistujuci();
		}
		else {
			vytvorPrazdny();
		}
	}

	@Override
	public void close() throws IOException {
		ra.close();
		//Uložime metadata
		ObjectOutputStream ois = new ObjectOutputStream(new FileOutputStream(new File(subor.getAbsolutePath()+".metadata")));
		ois.writeInt(alokovanychBlokov);
		ois.writeInt(volneBloky.size()); // Pocet volnych blokov
		for (Integer addr : volneBloky) {
			ois.writeInt(addr); //Zapiseme volne bloky
		}
		strom.serializeTo(ois);
		ois.close();
	}

	private void nacitajExistujuci() throws IOException {
		ra = new RandomAccessFile(subor, "rw");
		ObjectInputStream ois = new ObjectInputStream(new FileInputStream(new File(subor.getAbsolutePath()+".metadata")));
		alokovanychBlokov = ois.readInt();
		int volnych = ois.readInt();
		for (int i = 0; i < volnych; i++) {
			volneBloky.add(ois.readInt());
		}
		strom.inicializujZo(ois);
		ois.close();
	}

	private void vytvorPrazdny() throws IOException {
		subor.createNewFile();
		ra = new RandomAccessFile(subor, "rw");
		alokovanychBlokov = 2;
		ra.setLength(alokovanychBlokov*velkostBloku);
		strom.inicializuj(0, 1);
	}
	
	public T najdi(IKluc kluc) throws IOException {
		int index = strom.najdiBlok(kluc);
		Blok<T> blok = nacitajBlok(index);
		return blok.najdiZaznam(kluc);
	}
	
	public boolean vloz(T zaznam) throws IOException {
		int index = strom.najdiBlok(zaznam.dajKluc());
		Blok<T> blok = nacitajBlok(index);
		if (blok.najdiZaznam(zaznam.dajKluc()) != null) {
			return false; //Akože duplikatny kluc
		}
		while (blok.getPlatnych() >= pocZaznamov) {
			Blok<T> nula = new Blok<>(blok.getIndex());
			Blok<T> jedna = new Blok<>(alokujBlok());
			
			int bit = strom.rozdelUzol(zaznam.dajKluc(), nula.getIndex(), jedna.getIndex());
			
			for (T z: blok.dajZaznamy()) {
				//Prerozdelenie zaznamov do blokov
				if (((z.dajKluc().dajHash() >> bit) & 0x1) == 0) {
					nula.pridajZaznam(z);
				}
				else {
					jedna.pridajZaznam(z);
				}
			}
			zapisBlok(nula);
			zapisBlok(jedna);
			if (((zaznam.dajKluc().dajHash() >> bit) & 0x1) == 0) {
				blok = nula;
			}
			else {
				blok = jedna;
			}
			
		}
		
		blok.pridajZaznam(zaznam);
		zapisBlok(blok);
		
		return true;
	}
	
	public boolean vymaz(IKluc kluc) throws IOException {
		int index = strom.najdiBlok(kluc);
		Blok<T> blok = nacitajBlok(index);
		if (blok.vymazZaznam(kluc) == null) {
			return false; //Akože som taky kluc nenašiel
		}
		
		if (blok.getPlatnych() > 0) {
			zapisBlok(blok);
		}
		else {
			if (strom.spojBloky(kluc)) {
				volneBloky.add(blok.getIndex());
				vacumm();
			}
			else {
				zapisBlok(blok);
			}
		}
		
		return true;
	}

	private Blok nacitajBlok(int index) throws IOException {
		Blok<T> blok = new Blok<>(index);
		ra.seek(index*velkostBloku);
		byte[] raw = new byte[velkostBloku];
		ra.read(raw);
		ByteBuffer wrapped = ByteBuffer.wrap(raw, 0, 4);
		int pocetZaznamov = wrapped.getInt();
		
		for (int i=0; i<pocetZaznamov; i++) {
			byte[] zaznam = new byte[mapper.dajVelkostZaznamu()];
			System.arraycopy(raw, 4+i*mapper.dajVelkostZaznamu(), zaznam, 0, mapper.dajVelkostZaznamu()); //preskočíme prvé 4 lebo to je počet zaznamov a pozom zvyšok je jeden zaznam
			blok.pridajZaznam(mapper.nacitajZaznam(zaznam));
		}
		return blok;
	}
	
	private void zapisBlok(Blok<T> blok) throws IOException {
		ra.seek(blok.getIndex()*velkostBloku);
		byte[] raw = new byte[velkostBloku];
		ByteBuffer dbuf = ByteBuffer.allocate(4);
		dbuf.putInt(blok.getPlatnych());
		byte[] pocetPlatnych = dbuf.array();
		System.arraycopy(pocetPlatnych, 0, raw, 0, 4);
		
		int i=0;
		for (T z : blok.dajZaznamy()) {
			byte[] zaznam = mapper.serializujZaznam(z);
			System.arraycopy(zaznam, 0, raw, 4+i*mapper.dajVelkostZaznamu(), mapper.dajVelkostZaznamu());
			i++;
		}
		ra.write(raw);
		
	}
	
	private int alokujBlok() throws IOException {
		if (!volneBloky.isEmpty()) {
			return volneBloky.pollFirst();
		}
		else {
			alokovanychBlokov++;
			ra.setLength(alokovanychBlokov*velkostBloku);
			return alokovanychBlokov-1;
		}
	}
	
	private void vacumm() throws IOException {
		boolean vacumed = false;
		while (!volneBloky.isEmpty() && volneBloky.last() == (alokovanychBlokov-1)) {
			volneBloky.pollLast();
			alokovanychBlokov--;
			vacumed = true;
		}
		if (vacumed) {
			ra.setLength(alokovanychBlokov*velkostBloku);
		}
		
	}
}
