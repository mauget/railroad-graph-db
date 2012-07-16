package com.ramblerag.db.util;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

import org.apache.log4j.Logger;
import org.springframework.context.support.ClassPathXmlApplicationContext;

import com.ancientprogramming.fixedformat4j.exception.FixedFormatException;
import com.ancientprogramming.fixedformat4j.format.FixedFormatManager;
import com.ancientprogramming.fixedformat4j.format.impl.FixedFormatManagerImpl;
import com.ramblerag.db.core.ApplicationException;
import com.ramblerag.db.core.DbInserter;
import com.ramblerag.db.core.DbWrapper;
import com.ramblerag.db.core.GlobalConstants;
import com.ramblerag.domain.Domain;
import com.ramblerag.domain.Lnk;
import com.ramblerag.domain.Nod;

/**
 * Load a graph database from E00 railroad data. See
 * http://www.bts.gov/publications/national_transportation_atlas_database/2011/
 * 
 */
public class Loader {

	private static Logger log = Logger.getLogger(Loader.class);

	// Injected
	private DbWrapper dbWrapper;
	
	public static void main(String[] args) {

		try {
			ClassPathXmlApplicationContext appContext = new ClassPathXmlApplicationContext(
					new String[] { GlobalConstants.APPLICATION_CONTEXT_XML });

			Loader loader = appContext.getBean(Loader.class);

			loader.load("rail100k.nod", "rail100k.lnk");
		} catch (ApplicationException e) {
			e.printStackTrace();
		}
	}

	public void load(String nodeFileName, String linkFileName)
			throws ApplicationException {
		
		log.info("Remove existing DB ...");
		boolean wasRemoved = removeOldDbIfThere(DbWrapper.DB_PATH);
		log.info(String.format("... any existing DB  was %sremoved.", wasRemoved ? "" : "not "));
		
		log.info("Beginning DB load ...");
		
		DbInserter dbi = DbInserter.getInstance();
		dbi.startDbInserter(new String[]{DbWrapper.DB_PATH});

		loadFlatFile(Nod.class, nodeFileName);
		loadFlatFile(Lnk.class, linkFileName);

		dbi.flushToIndex();
		dbi.shutdownDbInserter();

		log.info("... end of DB load");
	
	}

	private boolean removeOldDbIfThere(String dir) {
		boolean wasDeleted = false;
		if (null != dir && dir.trim().length() > 0){
			File dbFolder = new File(dir);
			if (dbFolder.canWrite() && dbFolder.isDirectory()){
				
				wasDeleted = deleteDir(dbFolder);
			}
			log.info(String.format("Database at %s found, but not deleted.", dir));
		} else {
			log.info(String.format("Database at %s not found.", dir));
		}
		return wasDeleted;
	}
	
	private boolean deleteDir(File dir) {
	    if (dir.isDirectory()) {
	        String[] children = dir.list();
	        for (int i=0; i<children.length; i++) {
	            boolean success = deleteDir(new File(dir, children[i]));
	            if (!success) {
	                return false;
	            }
	        }
	    }

	    // The directory is now empty so delete it
	    return dir.delete();
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
				//log.info(String.format("Record %s", obj));
				//log.info(String.format("%s: key %d", obj.getRecType(), obj.getDomainId()));
				
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

	public DbWrapper getDbWrapper() {
		return dbWrapper;
	}

	public void setDbWrapper(DbWrapper dbWrapper) {
		this.dbWrapper = dbWrapper;
	}
}
