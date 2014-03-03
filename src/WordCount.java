import java.io.File;
import java.io.IOException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.ConcurrentNavigableMap;

import org.mapdb.DB;
import org.mapdb.DBMaker;

import vn.com.datasection.util.WordTokenizer;

import com.google.gson.Gson;

/**
 * @author duong Count word from crawled data
 */
public class WordCount {
	File dbCrawlFile = new File("output/crawl.dat");
	File dbStatsFile = new File("output/stats.dat");
	DB dbStats;
	Gson gson = new Gson();
	SimpleDateFormat simpleDateFormat = new SimpleDateFormat("dd/MM/yyyy");

	public static void main(String[] args) throws IOException {
		WordCount t = new WordCount();
		t.debug();
		// t.crawl();
		// Remember to delete old data file to prevent error
		// t.iniStorage();
		// t.countWord();
		// t.closeStorage();
		t.debugStore();
	}

	/**
	 * Debug module WordTokenizer
	 */
	public void debug() {
		WordTokenizer token = new WordTokenizer("models/jvnsensegmenter",
				"data", true);
		String example = "Nếu bạn đang tìm kiếm một chiếc điện thoại Android? Đây là, những smartphone đáng để bạn cân nhắc nhất. Thử linh tinh!";
		token.setString(example);
		System.out.println(token.hasNext());
		System.out.println(token.getString());
	}

	/**
	 * Count Word from crawled Data
	 */
	public void countWord() {
		iniStorage();
		WordTokenizer token = new WordTokenizer("models/jvnsensegmenter",
				"data", true);
		// String example =
		// "Nếu bạn đang tìm kiếm một chiếc điện thoại Android? Đây là, những smartphone đáng để bạn cân nhắc nhất. Thử linh tinh!";
		// token.setString(example);
		// System.out.println(token.getString());

		Set<Map.Entry<String, String>> crawlData = getCrawlData().entrySet();
		Iterator<Map.Entry<String, String>> i = crawlData.iterator();
		while (i.hasNext()) {
			Map.Entry<String, String> pageContent = i.next();
			System.out.println(pageContent.getKey());
			PageData pageData = gson.fromJson(pageContent.getValue(),
					PageData.class);
			token.setString(pageData.getContent().toUpperCase());
			String wordSegment = token.getString();
			String[] listWord = wordSegment.split(" ");
			for (String word : listWord) {
				addToDictionary(word, 1);
				Date date;
				try {
					date = simpleDateFormat.parse(pageData.getTime().substring(
							0, 10));
				} catch (ParseException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
					continue;
				}
				addToStatByDay(date, word, 1);
			}
		}
	}

	public void iniStorage() {
		dbStats = DBMaker.newFileDB(dbStatsFile).closeOnJvmShutdown().make();
	}

	/**
	 * close Storage after write
	 */
	public void closeStorage() {
		dbStats.commit();
		dbStats.close();
	}

	public void debugStore() {
		System.out.println("Get Vocabulary");
		iniStorage();
		ConcurrentNavigableMap<String, String> listVocabulary = dbStats
				.getTreeMap("voca");
		System.out.println(listVocabulary.containsKey("CHO"));
		System.out.println(listVocabulary.get("CHO"));

		String s = "28/02/2014";
		System.out.println("get Day " + s);
		ConcurrentNavigableMap<String, String> words = dbStats.getTreeMap(s);
		System.out.println(words.containsKey("CHO"));
		System.out.println(words.get("CHO"));

		Map<String, String> m = sortByComparator(listVocabulary, true);
		printMap(m);
	}

	private static Map<String, String> sortByComparator(
			Map<String, String> unsortMap, final boolean order) {

		List<Entry<String, String>> list = new LinkedList<Entry<String, String>>(
				unsortMap.entrySet());

		// Sorting the list based on values
		Collections.sort(list, new Comparator<Entry<String, String>>() {
			public int compare(Entry<String, String> o1,
					Entry<String, String> o2) {
				int io1 = Integer.parseInt(o1.getValue());
				int io2 = Integer.parseInt(o2.getValue());
				if (order) {
					return io1 > io2 ? -1 : (io1 == io2? 0: 1);
				} else {
					return io1 < io2 ? -1 : 1;
				}
			}
		});

		// Maintaining insertion order with the help of LinkedList
		Map<String, String> sortedMap = new LinkedHashMap<String, String>();
		for (Entry<String, String> entry : list) {
			sortedMap.put(entry.getKey(), entry.getValue());
		}
		return sortedMap;
	}

	/**
	 * Crawl wada
	 */
	public void crawl() {
		MyCrawler crawler = new MyCrawler();
		crawler.startCrawl("http://tintuc.wada.vn/khoa-hoc-cong-nghe/?p=0", 0,
				100);
	}

	public static void printMap(Map<String, String> map) {
		int count = 0;
		for (Entry<String, String> entry : map.entrySet()) {
			if (count++ > 200) {
				break;
			}
			System.out.println("Key : " + entry.getKey() + " Value : "
					+ entry.getValue());
		}
	}

	/**
	 * Read data from file
	 * 
	 * @return: A map url -> content
	 */
	public ConcurrentNavigableMap<String, String> getCrawlData() {
		DB db = DBMaker.newFileDB(dbCrawlFile).closeOnJvmShutdown().make();
		return db.getTreeMap("WadaTechData");
	}

	/**
	 * Add word to dictionary
	 * 
	 * @param word
	 *            : Word in UPPERCASE
	 * @param n
	 *            : n time
	 */
	public void addToDictionary(String word, int n) {
		ConcurrentNavigableMap<String, String> listVocabulary = dbStats
				.getTreeMap("voca");
		String strTimes = listVocabulary.get(word);
		int times = 0;
		if (strTimes != null) {
			times = Integer.parseInt(strTimes);
			times = times + n;
		} else {
			times = n;
		}
		listVocabulary.put(word, Integer.toString(times));
	}

	/**
	 * Count word by date
	 * 
	 * @param date
	 *            : Format: dd/MM/yyyy
	 * @param word
	 *            : Word in UPPERCASE
	 * @param n
	 *            : n time
	 */
	public void addToStatByDay(Date date, String word, int n) {
		String collection = simpleDateFormat.format(date);
		ConcurrentNavigableMap<String, String> listVocabulary = dbStats
				.getTreeMap(collection);
		String strTimes = listVocabulary.get(word);
		int times = 0;
		if (strTimes != null) {
			times = Integer.parseInt(strTimes);
			times = times + n;
		} else {
			times = n;
		}
		listVocabulary.put(word, Integer.toString(times));
	}
}