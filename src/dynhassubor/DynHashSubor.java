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
import java.util.LinkedList;
import java.util.List;
import java.util.Stack;
import java.util.TreeSet;
import javafx.geometry.Insets;

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
	
	private int size;
	
	private int maxLevel;

	public DynHashSubor(IZaznamMapper<T> paMapper, File paSubor, int paPocZaznamov, int maxLevel) throws IOException {
		this.mapper = paMapper;
		this.subor = paSubor;
		this.pocZaznamov = paPocZaznamov;
		this.maxLevel = maxLevel;
		this.strom = new BinStrom();
		this.velkostBloku = 4 + 4 + paPocZaznamov*paMapper.dajVelkostZaznamu(); //Pocet validnych zaznamov + n zaznamov + adresa dalsieho
		
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
		//System.out.println("Blokov: "+alokovanychBlokov);
		ois.writeInt(alokovanychBlokov);
		//System.out.println("Size: "+size);
		ois.writeInt(size);
		ois.writeInt(volneBloky.size()); // Pocet volnych blokov
		for (Integer addr : volneBloky) {
			ois.writeInt(addr); //Zapiseme volne bloky
			//System.out.println("Addr:"+addr);
		}
		strom.serializeTo(ois);
		ois.close();
	}

	private void nacitajExistujuci() throws IOException {
		ra = new RandomAccessFile(subor, "rw");
		ObjectInputStream ois = new ObjectInputStream(new FileInputStream(new File(subor.getAbsolutePath()+".metadata")));
		alokovanychBlokov = ois.readInt();
		size = ois.readInt();
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
		alokovanychBlokov = 0;
		size = 0;
		ra.setLength(alokovanychBlokov*velkostBloku);
		strom.inicializuj(BinStrom.EMPTY_ADDR, BinStrom.EMPTY_ADDR);
	}
	
	public T najdi(IKluc kluc) throws IOException {
		int index = strom.najdiBlok(kluc);
		while (index != BinStrom.EMPTY_ADDR) {
			Blok<T> blok = nacitajBlok(index);
			T zaznam = blok.najdiZaznam(kluc);
			if (zaznam != null) {
				return zaznam;
			}
			else {
				index = blok.getDalsi();
			}
		}
		return null;
	}
	
	public boolean vloz(T zaznam) throws IOException {
		int index = strom.najdiBlok(zaznam.dajKluc());
		Blok<T> blok;
		if (index == BinStrom.EMPTY_ADDR) {
			blok = new Blok<>(alokujBlok());
			strom.nastavAdresu(zaznam.dajKluc().dajHash(), blok.getIndex());
		}
		else {
			blok = nacitajBlok(index);
		}
		while (strom.dajLevel(zaznam.dajKluc()) < maxLevel && blok.getPlatnych() >= pocZaznamov) {
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
			if (((zaznam.dajKluc().dajHash() >> bit) & 0x1) == 0) {
				blok = nula;
			}
			else {
				blok = jedna;
			}
			
			/**
			 * Magic :)
			 */
			
			//Ak sa aj po rozdelení všetko narvalo do jedného, tak ten druhý dealokujeme ak don nepride novy zaznam
			if (nula.getPlatnych() == 0 && blok != nula) {
				strom.nastavAdresu(zaznam.dajKluc().dajHash() ^ (0x1 << bit), BinStrom.EMPTY_ADDR); //Adresa toho uzla na smer nula (musime vynulovať rozhodovací bit)
				volneBloky.add(nula.getIndex());
				zapisBlok(jedna);
			}
			else if (jedna.getPlatnych() == 0 && blok != jedna) {
				strom.nastavAdresu(zaznam.dajKluc().dajHash() ^ (0x1 << bit), BinStrom.EMPTY_ADDR); //Adresa toho uzla na smer jedna (musime dat na 1 rozhodovací bit)
				volneBloky.add(jedna.getIndex());
				zapisBlok(nula);
			}
			else {
				zapisBlok(nula);
				zapisBlok(jedna);
			}
		}
		
		while (blok.getPlatnych() >= pocZaznamov) {
			if (blok.najdiZaznam(zaznam.dajKluc()) != null) {
				return false; //Akože duplikatny kluc
			}
			if (blok.getDalsi() != BinStrom.EMPTY_ADDR) {
				blok = nacitajBlok(blok.getDalsi());
			}
			else {
				int nextBlok = alokujBlok();
				blok.setDalsi(nextBlok);
				zapisBlok(blok);
				blok = new Blok<>(nextBlok);
			}
		}
		if (blok.najdiZaznam(zaznam.dajKluc()) != null) {
			return false; //Akože duplikatny kluc
		}
		
		blok.pridajZaznam(zaznam);
		zapisBlok(blok);
		
		size++;
		return true;
	}
	
	public boolean vymaz(IKluc kluc) throws IOException {
		int index = strom.najdiBlok(kluc);
		if (index == BinStrom.EMPTY_ADDR) {
			return false;
		}
		Blok<T> blok = nacitajBlok(index);
		Stack<Blok<T>> bloky = new Stack<>();
		bloky.add(blok);
		while (blok.vymazZaznam(kluc) == null) {
			if (blok.getDalsi() != BinStrom.EMPTY_ADDR) {
				blok = nacitajBlok(blok.getDalsi());
				bloky.push(blok);
			}
			else {
				return false;
			}
		}
		
		if (blok.getPlatnych() > 0) {
			zapisBlok(blok);
		}
		else if (bloky.size() > 1) {
			bloky.pop();
			volneBloky.add(blok.getIndex());
			int next = blok.getDalsi();
			blok = bloky.pop();
			blok.setDalsi(next);
			zapisBlok(blok);
			vacumm();
		}
		else if (blok.getDalsi() != BinStrom.EMPTY_ADDR) {
			strom.nastavAdresu(kluc.dajHash(), blok.getDalsi());
			volneBloky.add(blok.getIndex());
			vacumm();
		}
		else {
			int brat = strom.najdiBrata(kluc);
			if (brat == BinStrom.EMPTY_ADDR || nacitajBlok(brat).getDalsi() == BinStrom.EMPTY_ADDR) {
				List<Integer> uvolneneBloky = strom.spojBloky(kluc);
				if (uvolneneBloky.size() > 0) {
					volneBloky.addAll(uvolneneBloky);
					vacumm();
				}
				else {
					zapisBlok(blok);
				}
			}
			else {
				volneBloky.add(blok.getIndex());
				vacumm();
				strom.nastavAdresu(kluc.dajHash(), BinStrom.EMPTY_ADDR);
			}
		}
		
		size--;
		return true;
	}
	
	public List<T> dajVsetko() throws IOException {
		LinkedList<T> list = new LinkedList<>();
		for (int index : strom.dajAlokovaneBloky()) {
			while (index != BinStrom.EMPTY_ADDR) {
				Blok<T> blok = nacitajBlok(index);
				for (T zaznam : blok.dajZaznamy()) {
					list.add(zaznam);
				}
				index = blok.getDalsi();
			}
		}
		return list;
	}

	private Blok nacitajBlok(int index) throws IOException {
		Blok<T> blok = new Blok<>(index);
		ra.seek(index*velkostBloku);
		byte[] raw = new byte[velkostBloku];
		ra.read(raw);
		ByteBuffer wrapped = ByteBuffer.wrap(raw, 0, 8);
		int pocetZaznamov = wrapped.getInt();
		blok.setDalsi(wrapped.getInt());
		
		for (int i=0; i<pocetZaznamov; i++) {
			byte[] zaznam = new byte[mapper.dajVelkostZaznamu()];
			System.arraycopy(raw, 8+i*mapper.dajVelkostZaznamu(), zaznam, 0, mapper.dajVelkostZaznamu()); //preskočíme prvé 4 lebo to je počet zaznamov a pozom zvyšok je jeden zaznam
			blok.pridajZaznam(mapper.nacitajZaznam(zaznam));
		}
		return blok;
	}
	
	private void zapisBlok(Blok<T> blok) throws IOException {
		ra.seek(blok.getIndex()*velkostBloku);
		byte[] raw = new byte[velkostBloku];
		ByteBuffer dbuf = ByteBuffer.allocate(8);
		dbuf.putInt(blok.getPlatnych());
		dbuf.putInt(blok.getDalsi());
		byte[] pocetPlatnych = dbuf.array();
		System.arraycopy(pocetPlatnych, 0, raw, 0, 8);
		
		int i=0;
		for (T z : blok.dajZaznamy()) {
			byte[] zaznam = mapper.serializujZaznam(z);
			System.arraycopy(zaznam, 0, raw, 8+i*mapper.dajVelkostZaznamu(), mapper.dajVelkostZaznamu());
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
