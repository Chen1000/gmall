package com.atguigu.gmall.list;

import io.searchbox.client.JestClient;
import io.searchbox.core.Search;
import io.searchbox.core.SearchResult;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;

@RunWith(SpringRunner.class)
@SpringBootTest
public class GmallListServiceApplicationTests {

	@Test
	public void contextLoads() {
	}

	@Autowired
	JestClient jestClient;

	@Test
	public void testEs() throws IOException {
		String query = "{\n" +
				"  \"query\": {\n" +
				"    \"match\": {\n" +
				"      \"name\": \"湄公河\"\n" +
				"    }\n" +
				"  }\n" +
				"}";
		Search search = new Search.Builder(query).addIndex("movies_index_chn")
				.addType("movies_type_chn").build();

		SearchResult searchResult = jestClient.execute(search);

		List<SearchResult.Hit<HashMap, Void>> hits = searchResult.getHits(HashMap.class);

		for (SearchResult.Hit<HashMap, Void> hit : hits) {
			HashMap source = hit.source;
			System.err.println("source = " + source);
			String name = (String) source.get("name");
			System.err.println("name = " + name);
		}

	}

}
