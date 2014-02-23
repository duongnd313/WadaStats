import java.io.File;
import java.io.IOException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentNavigableMap;

import jvntextpro.JVnTextPro;

import org.mapdb.DB;
import org.mapdb.DBMaker;

import vn.com.datasection.util.WordTokenizer;

import com.google.gson.Gson;

/**
 * Hello world application to demonstrate storage open, commit and close
 * operations
 */
public class TrendDetection {
	File dbCrawlFile = new File("/home/duong/Desktop/crawldata/crawl.dat");
	File dbStatsFile = new File("/home/duong/Desktop/crawldata/stats.dat");
	DB dbStats;
	Gson gson = new Gson();
	SimpleDateFormat simpleDateFormat = new SimpleDateFormat("dd/MM/yyyy");

	public static void main(String[] args) throws IOException {
		TrendDetection t = new TrendDetection();
//		t.countWord();
		String example = "Nếu bạn đang tìm kiếm một chiếc điện thoại Android? Đây là, những smartphone đáng để bạn cân nhắc nhất. Thử linh tinh!";
 
		WordTokenizer token = new WordTokenizer("models/jvnsensegmenter", "data", true);
		token.setString(example);
		System.out.println(token.getString());
		
		
	}

	public void countWord() {
		iniStorage();
		JVnTextPro textPro = new JVnTextPro();
		textPro.initSenSegmenter("models/jvnsensegmenter");
		textPro.initSenTokenization();
		textPro.initSegmenter("models/jvnsegmenter");
		textPro.initPosTagger("models/jvnpostag/maxent");
		String example = "Nếu bạn đang tìm kiếm một chiếc điện thoại Android? Đây là, những smartphone đáng để bạn cân nhắc nhất. Thử linh tinh!";
		System.out.println(textPro.senSegment(example));
		System.out.println(textPro.senTokenize(example));
		System.out.println(textPro.wordSegment(example));
		System.out.println(textPro.posTagging(example));
		System.out.println(textPro.wordSegment(textPro.senTokenize(textPro
				.senSegment(example))));

		Set<Map.Entry<String, String>> crawlData = getCrawlData().entrySet();
		Iterator<Map.Entry<String, String>> i = crawlData.iterator();
		while (i.hasNext()) {
			Map.Entry<String, String> pageContent = i.next();
			PageData pageData = gson.fromJson(pageContent.getValue(),
					PageData.class);
			String wordSegment = textPro.wordSegment(
					textPro.senTokenize(textPro.senSegment(pageData
							.getContent()))).toUpperCase();
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
		debugStore();
	}

	public void iniStorage() {
		dbStats = DBMaker.newFileDB(dbStatsFile).closeOnJvmShutdown().make();
	}

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

		String s = "voca2";
		System.out.println("get Day " + s);
		ConcurrentNavigableMap<String, String> words = dbStats
				.getTreeMap("test");
		System.out.println(words);
		System.out.println(words.containsKey("a"));
		System.out.println(words.get("b"));
	}

	public void crawl() {
		MyCrawler crawler = new MyCrawler();
		crawler.startCrawl("http://tintuc.wada.vn/khoa-hoc-cong-nghe/?p=0", 0);
	}

	public ConcurrentNavigableMap<String, String> getCrawlData() {
		DB db = DBMaker.newFileDB(dbCrawlFile).closeOnJvmShutdown()
		// .encryptionEnable("password")
				.make();
		return db.getTreeMap("WadaTechData");
	}

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

	public void addToStatByDay(Date date, String word, int n) {
		String collection = simpleDateFormat.format(date);
		ConcurrentNavigableMap<String, String> listVocabulary = dbStats
				.getTreeMap("voca2");
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

	public void setData() {
		// DB db = DBMaker.newFileDB(dbFile).closeOnJvmShutdown()
		// .make();
		//
		// // open an collection, TreeMap has better performance then HashMap
		// ConcurrentNavigableMap<String, String> map = db
		// .getTreeMap("collectionName");
		//
		// map.put(1, "one");
		// map.put(2, "two");
		// // map.keySet() is now [1,2] even before commit
		//
		// db.commit(); // persist changes into disk
		//
		// map.put(3, "three");
		// // map.keySet() is now [1,2,3]
		// db.rollback(); // revert recent changes
		// // map.keySet() is now [1,2]
		//
		// db.close();

	}

}