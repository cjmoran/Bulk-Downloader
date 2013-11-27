/*
	=====Bulk File Downloader=====
	-----by Cullin Moran----------
*/

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URL;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Scanner;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.swing.JFileChooser;
import javax.swing.filechooser.FileFilter;

import org.apache.commons.io.IOUtils;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;

import sun.net.www.protocol.http.HttpURLConnection;

public class FileDownloader {
	static LinkedList<String> extensions = new LinkedList<String>();
	static int totalImages;
	static String searchString = new String();
	
	public static void main(String[] args) {
		System.out.println("=====Bulk File Downloader=====\n-----by Cullin Moran----------");
		System.out.print("\nInitializing...");
		
		try {
			parseExtensions();
		} catch (FileNotFoundException e) {
			throw new ConfParserException("\nERROR: extensions.conf configuration file not found. " +
					"Please replace it or re-download this program.");
		}
		
		LinkedList<String> imageLinkList = getPage(searchString);
		
		String savePath = getSavePath();
		
		downloadAll(imageLinkList, savePath);
		
		System.out.print("\nAll done!");
	}
	
	private static void parseExtensions() throws FileNotFoundException {
		File extensionsFile = new File("extensions.conf");
		Scanner extensionsScanner = new Scanner(extensionsFile);
		
		String current;
		Matcher extMatcher;
		Pattern fileExtensionPattern = Pattern.compile("[\\d\\w\\-;]*");
		
		while(extensionsScanner.hasNext()) {
			current = extensionsScanner.nextLine();
			extMatcher = fileExtensionPattern.matcher(current);
			if(current.charAt(0) != '#') {
				if(extMatcher.matches()) {
					extensions.add(current);
				}
				else {
					throw new ConfParserException("\nERROR: Your extensions.conf file contains invalid characters. " +
							"See the instructions file.");
				}
			}
		}
		
		Iterator<String> extIterator = extensions.iterator();
		
		while(extIterator.hasNext()) {
			String currentExt = extIterator.next();
			searchString += currentExt;
			
			if(extIterator.hasNext()) {
				searchString += "|";
			}
		}
		
		System.out.println(" done.\n");
	}
	
	private static LinkedList<String> getPage(String searchString) {
		System.out.print("Give me a URL: ");
		Scanner scan = new Scanner(System.in);
		String pageUrl = scan.nextLine();
		Document pageDoc = null;
		try {
			pageDoc = Jsoup.connect(pageUrl).userAgent("Mozilla").get();
		} catch (IOException e) {
			e.printStackTrace();
			System.out.println("Couldn't get page.");
		}
		
		//Get all image links from page
		org.jsoup.select.Elements allLinks = pageDoc.select("a[href~=^.+\\.(" + searchString + "$)]");
		System.out.println("\nGot " + allLinks.size() + " file links.");
		
		//Add all image links to list, excluding duplicates
		LinkedList<String> imageLinkList = new LinkedList<>();
		
		Iterator<Element> linksIterator = allLinks.iterator();
		while(linksIterator.hasNext()) {
			String current = linksIterator.next().attr("abs:href");
			
			if(!imageLinkList.contains(current)) {
				imageLinkList.add(current);
			}
		}
		
		totalImages = imageLinkList.size();
		System.out.println(totalImages + " links remaining after removing duplicates.");
		System.out.println("\nWhere should I put the downloaded files?");
		
		return imageLinkList;
	}
	
	private static String getSavePath() {
		@SuppressWarnings("serial")
		JFileChooser chooser = new JFileChooser(".") {
			@Override
			public void approveSelection() {
				if(getSelectedFile().isFile())
		        {
		            return;
		        }
		        else
		            super.approveSelection();
			}
		};
		chooser.setCurrentDirectory(new File(System.getProperty("user.home") + "."));
		chooser.setFileSelectionMode(JFileChooser.FILES_AND_DIRECTORIES);
		chooser.setFileFilter(new FileFilter() {
			@Override
			public boolean accept(File file) {
				if(file.isDirectory()) {
					return true;
				}
				else {
					return false;
				}
			}

			@Override
			public String getDescription() {
				return "Directories only";
			}
		});
		if(chooser.showOpenDialog(null) == JFileChooser.APPROVE_OPTION) {
			return chooser.getSelectedFile().getPath();
		}
		else {
			//If no directory chosen, terminate
			System.out.print("No directory chosen; exiting.");
			System.exit(0);
			return null;	//Shouldn't run
		}
	}
	
	private static void downloadAll(LinkedList<String> imageLinkList, String savePath) {
		Iterator<String> imageLinkListIterator = imageLinkList.iterator();
		InputStream is;
		OutputStream os;
		URL currentUrl = null;
		File fl;
		int lastSlash;
		String fileName;
		String fullUrl;
		HttpURLConnection conn;
		int count = 1;
		int qTest;
		int hTest;
		while(imageLinkListIterator.hasNext()) {
			try {
				System.out.print("Downloading file " + count + " of " + totalImages + "...");
				
				//Get file name
				fullUrl = imageLinkListIterator.next();
				lastSlash = fullUrl.lastIndexOf('/');
				fileName = fullUrl.substring(lastSlash);
				
				currentUrl = new URL(fullUrl);
				
				//Problems arise if a '?' or '#' come after the link
				qTest = fileName.indexOf('?');
				hTest = fileName.indexOf('#');
				if(qTest != -1) {
					fileName = fileName.substring(0, qTest);
				}
				if(hTest != -1) {
					fileName = fileName.substring(0, hTest);
				}
				fl = new File(savePath + fileName);
				
				os = new FileOutputStream(fl);
				conn = new HttpURLConnection(currentUrl, null);
				conn.setRequestProperty("User-Agent", "Mozilla/4.0 (compatible; MSIE 4.01; Windows NT)");
				is = conn.getInputStream();
				IOUtils.copy(is, os);
				os.close();
				is.close();
				count++;
				System.out.println(" done.");
			}
			catch(IOException e) {
				e.printStackTrace();
				System.out.println("Error on file: " + currentUrl.getFile());
			}
		}
	}
}