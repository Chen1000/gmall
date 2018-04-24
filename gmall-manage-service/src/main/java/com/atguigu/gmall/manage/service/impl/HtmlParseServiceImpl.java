package com.atguigu.gmall.manage.service.impl;

import com.atguigu.gmall.bean.BaseCatalog1;
import com.atguigu.gmall.bean.BaseCatalog2;
import com.atguigu.gmall.bean.BaseCatalog3;
import com.atguigu.gmall.manage.mapper.BaseCatalog1Mapper;
import com.atguigu.gmall.manage.mapper.BaseCatalog2Mapper;
import com.atguigu.gmall.manage.mapper.BaseCatalog3Mapper;
import com.atguigu.gmall.util.HttpClientUtil;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class HtmlParseServiceImpl {

    @Autowired
    BaseCatalog1Mapper baseCatalog1Mapper;

    @Autowired
    BaseCatalog2Mapper baseCatalog2Mapper;

    @Autowired
    BaseCatalog3Mapper baseCatalog3Mapper;


    @Test
    public void downloadFile(){
        HttpClientUtil.download("https://img20.360buyimg.com/mobilecms/s80x80_jfs/t10690/249/1626659345/69516/b3643998/59e4279aNff3d63ac.jpg","D://img/img.jpg");
    }

    @Test
    public void parseHtml(){
        String html = HttpClientUtil.doGet("https://www.jd.com/allSort.aspx");
        //   System.out.println("html = " + html);
        Document document = Jsoup.parse(html);
        Elements elements = document.select("div[class='category-item m']");
        for (Element element : elements) {
            String catalog1Name = element.select(".item-title span").text();

            BaseCatalog1 baseCatalog1= new BaseCatalog1();
            baseCatalog1.setName(catalog1Name);
            baseCatalog1Mapper.insertSelective(baseCatalog1);

            System.out.println("catalog1Name = " + catalog1Name);
            Elements catalog2s = element.select(".items .clearfix");
            for (Element catalog2 : catalog2s) {
                String catalog2Name = catalog2.select("dt a").text();
                System.out.println("-------catalog2Name = " + catalog2Name);

                BaseCatalog2 baseCatalog2= new BaseCatalog2();
                baseCatalog2.setName(catalog2Name);
                baseCatalog2.setCatalog1Id(baseCatalog1.getId());
                baseCatalog2Mapper.insertSelective(baseCatalog2);

                Elements catalog3s = catalog2.select("dd a");
                for (Element catalog3 : catalog3s) {
                    String catalog3Name = catalog3.text();
                    System.out.println("-----------------------catalog3Name = " + catalog3Name);

                    BaseCatalog3 baseCatalog3= new BaseCatalog3();
                    baseCatalog3.setName(catalog3Name);
                    baseCatalog3.setCatalog2Id(baseCatalog2.getId());
                    baseCatalog3Mapper.insertSelective(baseCatalog3);
                }
            }

        }

    }

}
