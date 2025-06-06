package com.ramblerag.servlet;

import java.io.File;
import java.io.IOException;
import java.net.URL;

import javax.servlet.Servlet;
import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;

import org.apache.commons.io.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;

import com.ramblerag.db.core.GlobalConstants;

/**
 * Servlet implementation class InitServlet
 */
// @ WebServlet(urlPatterns = { "/initializer/*" }, loadOnStartup = 2)
public class Initializer extends HttpServlet {

	private static final long serialVersionUID = 1L;
	
	private static final Logger Log = LoggerFactory.getLogger(Initializer.class);

	private static ApplicationContext applicationContext;

	/**
	 * Default constructor.
	 */
	public Initializer() {
		Log.info("Initialize servlet loaded");
	}

	/**
	 * @see Servlet#init(ServletConfig)
	 */
	public void init(ServletConfig config) throws ServletException {
		
		try {
			copyDb(GlobalConstants.DATABASE_BITS, GlobalConstants.PROTOTYPE_NEO4J_STORE);
		} catch (IOException e) {
			throw new ServletException(e);
		}
		super.init();
	}

	private void copyDb(String targetPath, String sourcePath)
			throws IOException {

		URL graphUrl = this.getClass().getClassLoader().getResource(sourcePath);
		File sourceFile = new File(graphUrl.getFile());
		File targetFile = new File(targetPath);
		
		// Delete target if existing
		try {
			FileUtils.deleteDirectory(targetFile);
		} catch (Exception e) {
			Log.error(String.format("Could not find %s to delete old database at %s.", targetFile.getAbsolutePath()));
		}

		Log.info(String.format("Copying %s to %s", sourceFile.getAbsolutePath(), targetFile.getAbsolutePath()));
		FileUtils.copyDirectory(sourceFile, targetFile);
	}

	public synchronized static ApplicationContext getApplicationContext(){
		if (Initializer.applicationContext == null){
			Initializer.applicationContext = new ClassPathXmlApplicationContext(new String[] {GlobalConstants.APPLICATION_CONTEXT_XML});
		}
		return applicationContext;
	}

}
