package com.ramblerag.db.load;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

import org.apache.log4j.Logger;

import com.ancientprogramming.fixedformat4j.exception.FixedFormatException;
import com.ancientprogramming.fixedformat4j.format.FixedFormatManager;
import com.ancientprogramming.fixedformat4j.format.impl.FixedFormatManagerImpl;
import com.ramblerag.db.ApplicationException;
import com.ramblerag.db.DbInserter;
import com.ramblerag.db.DbWrapper;
import com.ramblerag.domain.Domain;
import com.ramblerag.domain.Lnk;
import com.ramblerag.domain.Nod;

/**
 * Load a graph database from E00 railroad data See
 * http://www.bts.gov/publications/national_transportation_atlas_database/2011/
 * 
 */
public class Load {

	private static Logger log = Logger.getLogger(Load.class);

	public static void main(String[] args) {
		try {
			new Load().load("rail100k.nod", "rail100k.lnk");
		} catch (ApplicationException e) {
			e.printStackTrace();
		}
	}

	public void load(String nodeFileName, String linkFileName)
			throws ApplicationException {
		
		log.info("Beginning DB load ...");
		
		DbInserter dbi = DbInserter.getInstance();
		dbi.startDbInserter(new String[]{DbWrapper.DB_PATH});

		loadFlatFile(Nod.class, nodeFileName);
		loadFlatFile(Lnk.class, linkFileName);

		dbi.flushToIndex();
		dbi.shutdownDbInserter();

		log.info("... end of DB load");

	}

	/**
	 * Overloaded to load either a transportation "Nod" domain file or a "Link"
	 * domain file. Delegates to db wrapper to determine insertion
	 * implementation specific to each.
	 * 
	 * @param clazz
	 * @param flatFileName
	 * @throws ApplicationException
	 */
	private <T> void loadFlatFile(Class<T> clazz, String flatFileName)
			throws ApplicationException {

		log.info(String.format("Loading file %s to object type %s",
				flatFileName, clazz.toString()));
		
		DbInserter dbi = DbInserter.getInstance();
		
		FixedFormatManager manager = new FixedFormatManagerImpl();
		BufferedReader br = null;
		int count = 0;
		try {
			InputStream ins = this.getClass().getClassLoader()
					.getResourceAsStream(flatFileName);
			br = new BufferedReader(new InputStreamReader(ins));
			String record;

			log.info("Reading, inserting records.");
			while (null != (record = br.readLine())) {
				Domain obj = (Domain) manager.load(clazz, record);
//				log.info(String.format("Record %s", obj));
//				log.info(String.format("%s: key %d", obj.getRecType(), obj.getDomainId()));
				
				dbi.insert(obj);
				
				count++;
			}
			log.info(String.format("Read %d records from object.", count,
					clazz.toString()));
			
			DbInserter.getInstance().flushToIndex();

		} catch (FixedFormatException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		} finally {
			if (null != br) {
				try {
					br.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		}
	}
}
