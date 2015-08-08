package WoWAddonUpdater.main;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.PrintWriter;
import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Scanner;
import java.util.function.Consumer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.swing.filechooser.FileSystemView;

import net.lingala.zip4j.exception.ZipException;
import net.lingala.zip4j.core.ZipFile;

public class Updater {
	private static final String[] CURSE_FORGE_RELEASE = {"http://wow.curseforge.com/addons/", 
		"user-action user-action-download.*?href=\"/addons/(.*?)\">Download",
		"user-action user-action-download.*?href=\"(.*?)\">Download", 
		"## X-Curse-Project-ID: (.*)[\\W]*",
		"Uploaded on.*?title=\"(.*?)\""};
	private static final String[] CURSE_FORGE_ALPHA = {"http://wow.curseforge.com/addons/%s/files/", 
		"<td class=\"col-file\"><a href=\".*?/files/(.*?)\">",
		"user-action user-action-download.*?href=\"(.*?)\">Download", 
		"## X-Curse-Project-ID: (.*)[\\W]*",
		"Uploaded on.*?title=\"(.*?)\""};
	private String rootPath;
	private boolean thorough = true;
	private static final Pattern PATTERN_FOLDER_INPUT = Pattern.compile("(.*World of Warcraft).*");
	private static final Pattern PATTERN_FOLDER_SEARCH = Pattern.compile("(.*World of Warcraft)$");
	private static final Pattern DETECT_DOWNLOAD_ZIP = Pattern.compile(".*/([^/]*zip)");
	private static final int SEARCH_DEPTH = 2;
	private static final String ADDONS_PATH_POSTFIX = "/Interface/Addons";
	private static final String UPDATER_PATH_POSTFIX = "/Interface/WoWAddonUpdater";
	private static final String TITLE = "WoWAddonUpdater";
	private static final String SETTINGS_FILE_NAME = "WoWAddonUpdater.config";
	private boolean getAlpha = true;
	private HashMap<String, String[]> addonStorage = new HashMap<>();
	private HashMap<String, Boolean> urlStorage = new HashMap<>();
	private boolean setUp = false;

	public static void main(String[] args) {
		System.out.println("\n" + TITLE + "\n");
		Object[] loaded = loadFromSettingsOrCreateFromScratch(SETTINGS_FILE_NAME);
		Updater updater = (Updater)loaded[0];
		updater.update(loaded);
		System.out.println("\nall done - bye!\n");
	}
		
	public void update(Object[] loaded) {
		if (loaded[1] != null) { // no settings found
			ArrayList<String> detectedPaths = searchRoots(0, SEARCH_DEPTH, PATTERN_FOLDER_SEARCH);
			rootPath = AutoExecuteOrPrompt(detectedPaths, System.in, s -> System.out.println(s));
		}
		download(generateDownloadLinks(parseFolder()), rootPath + UPDATER_PATH_POSTFIX);
		unzip(rootPath + UPDATER_PATH_POSTFIX, rootPath + ADDONS_PATH_POSTFIX);
		saveSettings();
	}
	
	private void saveSettings() {
		
	}
	
	private static String AutoExecuteOrPrompt(ArrayList<String> detectedRootPaths, InputStream in, Consumer<String> out) {
		int detectedInstalls = detectedRootPaths.size();
		if (detectedInstalls == 1) {
			out.accept("WoW install found\n=> " + detectedRootPaths.get(0));
			return detectedRootPaths.get(0);
		} else if (detectedInstalls > 1) {
			out.accept("multiple WoW installs found\n");
			int rootCount = 1;
			for (String root : detectedRootPaths) {
				out.accept(String.format("=>\t%s\t[%d]", root, rootCount++));
			}
			out.accept("\nenter the number to the right of the results");
			try (Scanner scan = new Scanner(in)) {
				while (scan.hasNext()) {
					int input = Integer.parseInt(scan.next().trim().toLowerCase());
					if (input >= 0 && input < detectedRootPaths.size()) {
						out.accept("default WoW install\n=>\t" + detectedRootPaths.get(input));
						return detectedRootPaths.get(input);
					} 
				}
			}
		} else {
			out.accept("no WoW install found\npls enter the root folder of your WoW install\ne.g. C:\\Program Files\\World of Warcraft");
			try (Scanner scan = new Scanner(in)) {
				while (scan.hasNextLine()) {
					String input = scan.nextLine().trim(); 
					Matcher m = PATTERN_FOLDER_INPUT.matcher(input);
					System.out.println(input);
					if (new File(input).exists() && m.find()) {
						System.out.println(new File(input).exists());
						String rootPath = m.group(1);
						out.accept("default WoW install\n=>\t"+ rootPath);
						return rootPath;
					} 
				}
			}
		}
		return null;
	}
	
	private static String getRootOfUpdater() {
//		return new Object().getClass().getProtectionDomain().getCodeSource().getLocation().toString();
		try {
			return Updater.class.getProtectionDomain().getCodeSource().getLocation().toURI().getPath();
		} catch (URISyntaxException e) {
			e.printStackTrace();
		}
		return null;
	}
	
	private static Object[] loadFromSettingsOrCreateFromScratch(String settingsFileName) {
		File settings = new File(getRootOfUpdater());
		if (settings.exists()) {
				try (ObjectInputStream ois = new ObjectInputStream(new FileInputStream(settings))) {
					return new Object[]{ois.readObject(), null};
			} catch(Exception e) {
				return new Object[]{new Updater(), new Object()};
			}	
		}
		return new Object[]{new Updater(), new Object()};
	}
	
	private static ArrayList<String> searchRoots(int depth, int maxDepth, Pattern p) {
		ArrayList<String> detectedPaths = new ArrayList<>();
		FileSystemView fsv = FileSystemView.getFileSystemView();
		File[] roots = File.listRoots();
		for (File root : roots) {
			if (fsv.getSystemTypeDescription(root).equals("Local Disk")) {
				detectedPaths.addAll(search(root, 0, maxDepth, p));
			}
		}
		return detectedPaths;
	}
	
	private static ArrayList<String> search(File file, int depth, int maxDepth, Pattern p) {
		ArrayList<String> detectedPaths = new ArrayList<>();
		class Recursor {
			public void recurse(File file, int depth, Pattern p) {
			if (depth <= maxDepth) {
					System.out.println("scanning " + file.toString());
					Matcher m = p.matcher(file.toString());
					if (m.find()) {
						detectedPaths.add(m.group(1));
					}
					if (file != null && file.isDirectory() && file.listFiles() != null) {
						for (File f : file.listFiles()) {
							search(f, depth + 1, maxDepth, p);
						}
					}
				}
			}
		}
		new Recursor().recurse(file, depth, p);
		return detectedPaths;
	}

	private  ArrayList<String> parseFolder() {
		String[] patterns = getAlpha ? CURSE_FORGE_ALPHA : CURSE_FORGE_RELEASE;
		System.out.println("\ndetecting addons...\n");
		ArrayList<String> addons = new ArrayList<>();
		File[] files = new File(rootPath).listFiles();
		int totalCount = files.length;
		int currentCount = 0;
		String n;
		File f;
		for (File file : files) {
			boolean hasId = false;
			n = file.getName();
			f = new File(rootPath + "/" + n + "/" + n + ".toc");
			try (Scanner c = new Scanner(f)){
				Pattern p = Pattern.compile(patterns[3]);
				while (c.hasNextLine()) {
					Matcher m = p.matcher(c.nextLine());
					if (m.find()) {
						n = m.group(1);
						addons.add(n);
						hasId = true;
						break;
					}
				}
			} catch (FileNotFoundException e) {
			}
			if (thorough && !hasId) {
				addons.add(n);
			}
			System.out.println(String.format("[%d of %d] detected: %s",  ++currentCount, totalCount, n));
		}
		ArrayList<String> result = new ArrayList<String>(new HashSet<String>(addons));
		for (int i = result.size() - 1; i >= 0; i--) {
			if (setUp && addonStorage.get(result.get(i)) != null && addonStorage.get(result.get(i))[3] == null) {
				result.remove(i);
			}
		}
		return result;
	}
	
	private ArrayList<URL> generateDownloadLinks(ArrayList<String> addons) {
		String[] patterns = getAlpha ? CURSE_FORGE_ALPHA : CURSE_FORGE_RELEASE;
		System.out.println("\nparsing...\n");
		if (addons == null || addons.size() == 0) return null;
		int totalCount = addons.size();
		int currentCount = 0;
		ArrayList<URL> downloadLinks = new ArrayList<>();
		for (String addon : addons) {
			if (setUp && addonStorage.get(addon) != null && addonStorage.get(addon)[3] == null) {
				totalCount--;
				continue;
			}
			try {
				URL u = getAlpha ? new URL(String.format(patterns[0], addon)) : 
					new URL(patterns[0] + addon + "/");
				String c = dumpUrl(u);
				Pattern p = Pattern.compile(patterns[1]);
				Matcher m = p.matcher(c);
				if (m.find()) {
					String s = m.group(1);
					u = getAlpha ? new URL(String.format(patterns[0], addon) + s) : 
						new URL(patterns[0] + s);
					c = dumpUrl(u);
					p = Pattern.compile(patterns[2]);
					m = p.matcher(c);
					if (m.find()) {
						s = m.group(1);
						downloadLinks.add(new URL(s));
						System.out.println(String.format("[%d of %d] parsed: %s\t%s", ++currentCount, totalCount, addon, s));
						p = Pattern.compile(patterns[4]);
						m = p.matcher(c);
						c = m.find() ? m.group(1) : null;
						String update = (addonStorage.get(addon) != null && !addonStorage.get(addon)[1].equals(c)) ? "u" : null;
						addonStorage.put(addon, new String[]{s, c, update, "u"});
						urlStorage.put(s, update != null);
					}
				}
			} catch (Exception e) {
				totalCount--;
				addonStorage.put(addon, new String[]{null, null, null, null});
			} 
		}
		setUp = true;
		return downloadLinks;
	}
	
	private static String dumpUrl(URL u) {
		try (Scanner scan = new Scanner(u.openStream())){
			StringBuffer c = new StringBuffer();
			while (scan.hasNextLine()) {
				c.append(scan.nextLine());
			}
			return c.toString();
		} catch (IOException e) {
		}
		return null;
	}
	
	private void download(ArrayList<URL> urls, String downloadPath) {
		System.out.println("\ndownloading...\n");
		refreshFolder(downloadPath);
		int totalCount = urls.size();
		int currentCount = 0;
		for (URL u : urls) {
			if (urlStorage.get(u) != null && urlStorage.get(u)) {
				totalCount--;
				continue;
			}
			Matcher m = DETECT_DOWNLOAD_ZIP.matcher(u.toString());
			if (m.find()) {
				try {
					Files.copy(u.openConnection().getInputStream(), Paths.get(downloadPath + "/" + m.group(1)));
					System.out.println(String.format("[%d of %d] downloaded: %s", ++currentCount, totalCount, m.group(1)));
				} catch (IOException e) {
					totalCount--;
				}
			}
		}
	}
	
	private void unzip(String from, String to) {
		System.out.println("\nextracting...\n");
		File[] files = new File(from).listFiles();
		int totalCount = files.length;
		int currentCount = 0;
		for (File f : files) {
			try {
		        new ZipFile(f).extractAll(to);
		        System.out.println(String.format("[%d of %d] extracted: %s",  ++currentCount, totalCount, f));
		    } catch (ZipException e) {
		    }
		}
		refreshFolder(rootPath + ADDONS_PATH_POSTFIX);
	}
	
	private void refreshFolder(String downloadPath) {
		File folder = new File(downloadPath);
		if (folder.exists()) {
			File[] files = folder.listFiles();
			for (File f : files) {
				f.delete();
			}
		} else {
			folder.mkdir();
		}
	}
}
