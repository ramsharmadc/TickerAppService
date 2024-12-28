package com.example.springboot;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.Time;
import java.util.ArrayList;
import java.util.List;

@RestController
public class DefaultController {

    @Autowired
    JdbcTemplate jdbcTemplate;

    @GetMapping("/")
    public String index() {
        return "Hello KDB+";
    }

//    @GetMapping("/quotes")
    @RequestMapping(
        value = "/quotes",
        method = {RequestMethod.GET},
        produces = {MediaType.APPLICATION_JSON_VALUE})
    public ResponseEntity<List<TickerData>> getTickers() {
        String sql = "q)  quote";
        List<TickerData> tickerList = new ArrayList<>();
        jdbcTemplate.query(sql, (ResultSet rowSet) -> {
            ResultSetMetaData m;
            m = rowSet.getMetaData();
            int columnCount = m.getColumnCount();

            String fmt[] = new String[columnCount + 1];
            int width = 0;

            for (int i = 1; i <= columnCount; i++) {
                fmt[i] = "%-" + m.getColumnDisplaySize(i) + "s";
                System.out.format(fmt[i], m.getColumnLabel(i));
                width += rowSet.getMetaData().getColumnDisplaySize(i);
            }

            System.out.print("\n");

            for (int i = 1; i <= width; i++) {
                System.out.print("-");
            }

            System.out.print("\n");

            do {
                TickerData tickerData = new TickerData();

                for (int i = 1; i <= columnCount; i++) {
                    String val = rowSet.getString(i);
                    if(i == 1) {
                        tickerData.setTimestamp(val);
                    }else if(i==2) {
                        tickerData.setSym(val);
                    }else if(i==3) {
                        tickerData.setBid(val);
                    }else if(i==4) {
                        tickerData.setAsk(val);
                    }
                    System.out.format(fmt[i], val);
                    tickerList.add(tickerData);
                }
                System.out.print("\n");
                if (rowSet.getRow() > 10) {
                    System.out.println("...");
                    rowSet.afterLast();
                }
            } while (rowSet.next());
        });
        System.out.println(tickerList);
        return ResponseEntity.ok().body(tickerList);
    }
}
