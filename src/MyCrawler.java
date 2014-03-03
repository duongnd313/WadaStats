import java.io.File;
import java.io.IOException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.LinkedList;
import java.util.Queue;
import java.util.concurrent.ConcurrentNavigableMap;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.mapdb.DB;
import org.mapdb.DBMaker;

import com.google.gson.Gson;

/**
 * Crawl Data from wada
 * @author duong
 * 
 */
public class MyCrawler {
	private int depthLimit;
	Queue<MyUrl> links = new LinkedList<MyUrl>();
	File dbFile = new File("output/crawl.dat");
	ConcurrentNavigableMap<String, String> pagesData;
	DB db;
	Gson gson = new Gson();

	/**
	 * Crawl data and store in output/crawl.dat
	 * @param url
	 * @param depth
	 * @param nitem_limit
	 */
	public void startCrawl(String url, int depth, int nitem_limit) {
		System.out.println("Craw: url=" + url + " depth: " + depth);
		iniStorage();
		depthLimit = depth;
		MyUrl currentUrl = new MyUrl(url, 0);
		PageData pageData;
		int nItem = 0;
		int nList = 0;
		while (nItem < nitem_limit) {
			pageData = getContent(currentUrl);
			if (!isNew(pageData)) {
				break;
			}
			storePageData(pageData);
			nItem++;
			if (pageData != null && pageData.getDepth() < depthLimit) {
				for (String link : pageData.getLinks()) {
					addLink(new MyUrl(link, pageData.getDepth() + 1));
				}
			}
			currentUrl = getNextLink();
			if (currentUrl == null) {
				nList++;
				getContent(new MyUrl(
						"http://tintuc.wada.vn/khoa-hoc-cong-nghe/?p=" + nList
								* 10, 0));
				currentUrl = getNextLink();
				if (currentUrl == null) {
					System.out.println("End data?");
					break;
				}
			}
		}

		closeStorage();
		System.out.println("Finish");
	}

	/**
	 * ini Storage
	 */
	private void iniStorage() {
		db = DBMaker.newFileDB(dbFile).closeOnJvmShutdown()
		// .encryptionEnable("password")
				.make();
		pagesData = db.getTreeMap("WadaTechData");
	}

	/**
	 * close Storage after write data
	 */
	private void closeStorage() {
		db.commit();
		db.close();
	}

	/**
	 * Store pageData in database
	 * @param pageData
	 */
	private void storePageData(PageData pageData) {
		// TODO Auto-generated method stub
		if (pageData != null) {
			pagesData.put(pageData.getUrl(), gson.toJson(pageData));
		}
	}

	private boolean isExistUrl(MyUrl url) {
		return pagesData.containsKey(url.url);
	}

	private MyUrl getNextLink() {
		// TODO Auto-generated method stub
		MyUrl url = null;
		do {
			url = links.poll();
			if (url == null) {
				return url;
			}
		} while (isExistUrl(url));
		return url;
	}

	public PageData getContent(MyUrl curUrl) {
		String url = curUrl.url;
		int depth = curUrl.depth;
		PageData data = new PageData();
		data.setUrl(url);
		data.setDepth(depth);
		try {
			Document doc = connect(url);
			if (url.contains("tintuc.wada.vn/e")) {
				// Parse news
				System.out.println("parse new " + url);
				Elements eTitles = doc
						.getElementsByClass("b-wasen_newselement-header");
				data.setTitle(eTitles.get(0).text());
				Elements content = doc
						.getElementsByClass("b-wasen_newselement-content");
				data.setContent(content.get(0).text());
				Elements time = doc.select("time");
				System.out.println(time.attr("datetime"));
				data.setTime(time.attr("datetime"));
			} else if (url.contains("tintuc.wada.vn/khoa-hoc-cong-nghe/?p=")) {
				System.out.println("parse url " + url);
				// parse link
				// b-wasen_results-block
				Elements eLinks = doc.getElementsByClass(
						"b-wasen_results-block").select(
						"a[href~=//tintuc.wada.vn/e/");
				for (Element e : eLinks) {
					String link = e.attr("href");
					if (link.startsWith("//tintuc.wada")) {
						link = "http:" + link;
					}
					addLink(new MyUrl(link, 0));
				}
				return null;
			}
		} catch (Exception e) {
			// TODO Auto-generated catch block
			System.out.println("Page " + url + "error when parsing content!");
			System.out.println(e.toString());
			data = null;
		}
		return data;
	}

	public Document connect(String url) {
		Document doc = null;
		for (int i = 0; i < 5; i++) {
			try {
				doc = Jsoup.connect(url).get();
				return doc;
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
				try {
					Thread.sleep(1000);
				} catch (InterruptedException e1) {
					// TODO Auto-generated catch block
					e1.printStackTrace();
				}
			}
		}
		return doc;
	}

	public void addLink(MyUrl url) {
		links.add(url);
	}

	public boolean isNew(PageData pageData) {
		if (pageData == null || pageData.getTime() == null) {
			return true;
		}
		String s = pageData.getTime();

		SimpleDateFormat simpleDateFormat = new SimpleDateFormat(
				"dd/MM/yyyy HH:mm");
		try {
			Date outDate = simpleDateFormat.parse("28/12/2013 00:00");
			Date date = simpleDateFormat.parse(s);

			if (date.compareTo(outDate) < 0) {
				// date before outDate
				return false;
			} else {
				// date after outDate
				return true;
			}
		} catch (ParseException ex) {
			System.out.println("Exception " + ex);
			return true;
		}
	}

}
